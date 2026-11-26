"""Small Android-facing adapter for NSZ's compression and decompression APIs."""

from pathlib import Path
from time import monotonic

from nsz.Decompressor import decompress
from nsz.nut import Keys


_loaded_keys_signature = None


class _Cancelled(Exception):
    pass


class _ProgressStatus(dict):
    def __init__(self, callback):
        super().__init__()
        self.callback = callback
        self.last_report_time = 0.0
        self.last_report_processed = -1

    def __setitem__(self, key, value):
        super().__setitem__(key, value)
        if self.callback.isCancellationRequested():
            raise _Cancelled("Conversion stopped by user.")
        processed = int(value[0])
        total = max(int(value[2]), 1)
        now = monotonic()
        if (
            processed >= total
            or now - self.last_report_time >= 0.25
            or processed - self.last_report_processed >= 8 * 1024 * 1024
        ):
            self.last_report_time = now
            self.last_report_processed = processed
            self.callback.onProgress(min(max(processed / total, 0.0), 1.0))


def _load_keys(keys_path):
    global _loaded_keys_signature
    keys_file = Path(keys_path)
    if not keys_file.is_file():
        raise RuntimeError("No prod.keys file has been imported into Mimir.")
    signature = (keys_file.stat().st_size, keys_file.stat().st_mtime_ns)
    if signature == _loaded_keys_signature and Keys.keys_loaded:
        return
    if not Keys.load_default(str(keys_file)):
        raise RuntimeError("Mimir could not load the imported prod.keys file.")
    _loaded_keys_signature = signature


def decompress_nsz(source_path, output_path, keys_path, callback):
    """Decompress one NSZ into the exact output path requested by Mimir."""
    source = Path(source_path)
    output = Path(output_path)
    generated_output = output.parent / f"{source.stem}.nsp"
    status = _ProgressStatus(callback)

    print(f"[Mimir NSZ] Starting {source.name}", flush=True)
    _load_keys(keys_path)
    callback.onProgress(0.0)
    if callback.isCancellationRequested():
        raise _Cancelled("Conversion stopped by user.")

    decompress(
        source,
        str(output.parent),
        False,
        statusReportInfo=[status, 0],
        # NSZ expects this to be None or a multiprocessing-style counter;
        # a boolean causes Print.info to call .value() on it and fail.
        pleaseNoPrint=None,
    )

    if callback.isCancellationRequested():
        generated_output.unlink(missing_ok=True)
        raise _Cancelled("Conversion stopped by user.")
    if not generated_output.is_file():
        raise RuntimeError(f"NSZ did not create {generated_output.name}.")

    if generated_output != output:
        output.unlink(missing_ok=True)
        generated_output.replace(output)
    callback.onProgress(1.0)
    print(f"[Mimir NSZ] Completed {output.name}", flush=True)


# --- Compression (NSP to NSZ) -------------------------------------------------

# NSZ's defaults for block compression (`nsz -B`): zstd level 18 in 1 MiB blocks. Block NSZ files can
# be read in place, which is what lets Eden and other emulators list and run them without installing.
_COMPRESSION_LEVEL = 18
_BLOCK_SIZE_EXPONENT = 20
_INCOMPRESSIBLE_HEADER_SIZE = 0x4000
_COPY_CHUNK_SIZE = 0x100000
# Compressing takes most of the time; the quick verify afterwards fills the last part of the bar.
_COMPRESS_SHARE = 0.9


class _CompressCancelled(KeyboardInterrupt):
    """Raised from a progress update to stop NSZ mid-file.

    It subclasses KeyboardInterrupt because NSZ treats that as a clean stop rather than an error.
    """


class _OverallProgress(dict):
    """Turns NSZ's per-NCA progress into one bar for the whole package.

    NSZ reports progress for one NCA (a file inside the NSP) at a time: [read, written, size, step].
    When a report starts a new NCA, the previous NCA's size is banked, so the bar keeps moving
    forward across the package instead of restarting for every NCA.
    """

    def __init__(self, callback, total_bytes, start, share):
        super().__init__()
        self.callback = callback
        self.total_bytes = max(int(total_bytes), 1)
        self.start = start
        self.share = share
        self.banked = 0
        self.current_size = 0
        self.current_read = 0
        self.last_report_time = 0.0

    def __setitem__(self, key, value):
        super().__setitem__(key, value)
        if self.callback.isCancellationRequested():
            raise _CompressCancelled()
        read, size = int(value[0]), int(value[2])
        if size != self.current_size or read < self.current_read:
            self.banked += self.current_size
            self.current_size = size
        self.current_read = read
        now = monotonic()
        if now - self.last_report_time >= 0.25:
            self.last_report_time = now
            fraction = min((self.banked + read) / self.total_bytes, 1.0)
            self.callback.onProgress(self.start + fraction * self.share)


def _compress_block(compressor, block):
    """One block, compressed, or stored as-is when compressing would not make it smaller."""
    compressed = compressor.compress(block)
    return compressed if len(compressed) < len(block) else block


def _block_compress_container(read_container, write_container, threads, status):
    """NSZ's block compression (BlockCompressor.blockCompressContainer) without multiprocessing.

    NSZ normally spreads blocks over child processes, which Android apps cannot start. Here the
    blocks go to a thread pool instead: zstd releases Python's GIL while it works, so the threads
    still compress in parallel. The file layout written is exactly the one NSZ writes.
    """
    from concurrent.futures import ThreadPoolExecutor
    from nsz.Fs import Nca, Type
    from nsz.SectionFs import isNcaPacked, sortedFs
    from zstandard import ZstdCompressionParameters, ZstdCompressor

    block_size = 2 ** _BLOCK_SIZE_EXPONENT
    params = ZstdCompressionParameters.from_level(_COMPRESSION_LEVEL)
    # One compressor per thread: a zstd compressor must not be shared between threads.
    compressors = [ZstdCompressor(compression_params=params) for _ in range(threads)]
    batch_size = threads * 4

    with ThreadPoolExecutor(max_workers=threads) as pool:
        for nspf in read_container:
            is_nca = isinstance(nspf, Nca.Nca) and nspf.header is not None
            # Like NSZ without --keep: update delta fragments are dropped.
            if is_nca and nspf.header.contentType == Type.Content.DATA:
                continue
            if not (
                is_nca
                and nspf.size is not None
                and nspf.header.contentType in (Type.Content.PROGRAM, Type.Content.PUBLICDATA)
                and nspf.size > _INCOMPRESSIBLE_HEADER_SIZE
                and isNcaPacked(nspf)
            ):
                # Everything else is copied across unchanged.
                f = write_container.add(nspf._path, nspf.size)
                nspf.seek(0)
                while not nspf.eof():
                    f.write(nspf.read(_COPY_CHUNK_SIZE))
                continue

            new_name = nspf._path[0:-1] + "z"  # .nca becomes .ncz
            first_section_offset = sortedFs(nspf)[0].offset
            f = write_container.add(new_name, nspf.size)
            start_pos = f.tell()
            nspf.seek(0)
            f.write(nspf.read(_INCOMPRESSIBLE_HEADER_SIZE))

            sections = []
            for fs in sortedFs(nspf):
                sections += fs.getEncryptionSections()
            if not sections:
                raise RuntimeError("An NCA can't be decrypted. Is prod.keys up to date?")

            # NCZSECTN: how to re-encrypt each section after decompressing.
            header = b"NCZSECTN" + len(sections).to_bytes(8, "little")
            for fs in sections:
                if not isinstance(fs.cryptoKey, (bytes, bytearray)) or len(fs.cryptoKey) != 0x10:
                    raise RuntimeError(f"{nspf._path} can't be compressed: its title key is missing.")
                header += fs.offset.to_bytes(8, "little")
                header += fs.size.to_bytes(8, "little")
                header += fs.cryptoType.to_bytes(8, "little")
                header += b"\x00" * 8
                header += fs.cryptoKey
                header += fs.cryptoCounter
            f.write(header)

            # NCZBLOCK: the block table. Sizes are filled in once every block is written.
            to_compress = nspf.size - _INCOMPRESSIBLE_HEADER_SIZE
            block_count = to_compress // block_size + (to_compress % block_size > 0)
            block_table_pos = f.tell()
            f.write(
                b"NCZBLOCK"
                + b"\x02"  # version
                + b"\x01"  # type
                + b"\x00"  # unused
                + _BLOCK_SIZE_EXPONENT.to_bytes(1, "little")
                + block_count.to_bytes(4, "little")
                + to_compress.to_bytes(8, "little")
                + b"\x00" * (block_count * 4)
            )

            # Decrypted views of the NCA body, read one after another.
            partitions = []
            if first_section_offset - _INCOMPRESSIBLE_HEADER_SIZE > 0:
                partitions.append(
                    nspf.partition(
                        offset=_INCOMPRESSIBLE_HEADER_SIZE,
                        size=first_section_offset - _INCOMPRESSIBLE_HEADER_SIZE,
                        cryptoType=Type.Crypto.CTR.NONE,
                        autoOpen=True,
                    )
                )
            for section in sections:
                partitions.append(
                    nspf.partition(
                        offset=section.offset,
                        size=section.size,
                        cryptoType=section.cryptoType,
                        cryptoKey=section.cryptoKey,
                        cryptoCounter=bytearray(section.cryptoCounter),
                        autoOpen=True,
                    )
                )
            if _INCOMPRESSIBLE_HEADER_SIZE - first_section_offset > 0:
                partitions[0].seek(_INCOMPRESSIBLE_HEADER_SIZE - first_section_offset)

            part = 0

            def next_block():
                nonlocal part
                buffer = partitions[part].read(block_size)
                while len(buffer) < block_size and part < len(partitions) - 1:
                    partitions[part].close()
                    part += 1
                    buffer += partitions[part].read(block_size - len(buffer))
                return buffer

            block_sizes = []
            read_bytes = _INCOMPRESSIBLE_HEADER_SIZE
            status[0] = [read_bytes, 0, nspf.size, "Compressing"]
            done = False
            while not done:
                # Read a batch of blocks, compress them in parallel, write them in order.
                batch = []
                while len(batch) < batch_size:
                    block = next_block()
                    if not block:
                        done = True
                        break
                    batch.append(block)
                    read_bytes += len(block)
                results = _compress_batch(pool, compressors, batch)
                for result in results:
                    block_sizes.append(len(result))
                    f.write(result)
                status[0] = [read_bytes, f.tell() - start_pos, nspf.size, "Compressing"]
            partitions[part].close()

            end_pos = f.tell()
            if len(block_sizes) != block_count:
                raise RuntimeError(f"{nspf._path}: expected {block_count} blocks, wrote {len(block_sizes)}.")
            f.seek(block_table_pos + 24)
            f.write(b"".join(size.to_bytes(4, "little") for size in block_sizes))
            f.seek(end_pos)
            write_container.resize(new_name, end_pos - start_pos)


def _compress_batch(pool, compressors, batch):
    """Compress a batch of blocks across the pool, each thread using its own compressor."""
    threads = len(compressors)
    lanes = [batch[i::threads] for i in range(threads)]

    def run_lane(lane_index):
        compressor = compressors[lane_index]
        return [_compress_block(compressor, block) for block in lanes[lane_index]]

    lane_results = list(pool.map(run_lane, range(threads)))
    # Put the blocks back in their original order.
    ordered = []
    for i in range(len(batch)):
        ordered.append(lane_results[i % threads][i // threads])
    return ordered


def _compression_threads():
    """One compression thread per CPU core, like `nsz -B`, capped to keep memory use modest."""
    import os

    return max(1, min(os.cpu_count() or 1, 8))


def compress_nsp(source_path, output_path, keys_path, callback):
    """Block-compress one NSP into the exact NSZ path requested by Mimir, then quick-verify it."""
    from nsz.Decompressor import verify
    from nsz.Fs import Nsp, Pfs0

    source = Path(source_path)
    output = Path(output_path)

    print(f"[Mimir NSZ] Compressing {source.name}", flush=True)
    _load_keys(keys_path)
    callback.onProgress(0.0)
    if callback.isCancellationRequested():
        raise _Cancelled("Conversion stopped by user.")

    container = Nsp.Nsp()
    container.open(str(source), "rb")
    try:
        status = _OverallProgress(callback, source.stat().st_size, 0.0, _COMPRESS_SHARE)
        with Pfs0.Pfs0Stream(
            container.getFirstFileOffset(),
            container.getStringTableSize(),
            str(output),
        ) as nsz_stream:
            _block_compress_container(container, nsz_stream, _compression_threads(), status)
    except _CompressCancelled:
        output.unlink(missing_ok=True)
        raise _Cancelled("Conversion stopped by user.")
    except BaseException:
        output.unlink(missing_ok=True)
        raise
    finally:
        container.close()

    if not output.is_file():
        raise RuntimeError(f"NSZ did not create {output.name}.")

    # Quick verify: decompress the new NSZ in memory and check every NCA's hash, so a damaged
    # NSZ is reported now rather than when the game is installed. Nothing extra is written.
    print(f"[Mimir NSZ] Verifying {output.name}", flush=True)
    verify_status = _OverallProgress(callback, output.stat().st_size, _COMPRESS_SHARE, 1.0 - _COMPRESS_SHARE)
    try:
        verify(output, False, True, False, None, [verify_status, 0], None)
    except _CompressCancelled:
        output.unlink(missing_ok=True)
        raise _Cancelled("Conversion stopped by user.")
    except BaseException as error:
        output.unlink(missing_ok=True)
        raise RuntimeError(f"The new NSZ failed verification: {error}") from error

    if callback.isCancellationRequested():
        output.unlink(missing_ok=True)
        raise _Cancelled("Conversion stopped by user.")
    callback.onProgress(1.0)
    print(f"[Mimir NSZ] Completed {output.name}", flush=True)
