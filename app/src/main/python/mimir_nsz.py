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

# NSZ's own defaults for solid compression: zstd level 18 on 3 threads. Solid NSZ files are what
# every NSZ-aware installer and emulator expects, and these settings give the usual NSZ size.
_COMPRESSION_LEVEL = 18
_COMPRESSION_THREADS = 3
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


def _compression_threads():
    """Use NSZ's 3 zstd worker threads when this zstandard build supports them, otherwise 1."""
    try:
        from zstandard import ZstdCompressionParameters, ZstdCompressor

        params = ZstdCompressionParameters.from_level(1, threads=_COMPRESSION_THREADS)
        ZstdCompressor(compression_params=params).compress(b"mimir")
        return _COMPRESSION_THREADS
    except Exception:
        return 1


def compress_nsp(source_path, output_path, keys_path, callback):
    """Solid-compress one NSP into the exact NSZ path requested by Mimir, then quick-verify it."""
    from nsz.Decompressor import verify
    from nsz.Fs import Nsp, Pfs0
    from nsz.SolidCompressor import processContainer

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
        # This mirrors NSZ's solidCompressNsp, but lets errors reach Mimir with their real message
        # (for example a missing title key) instead of only printing them.
        status = _OverallProgress(callback, source.stat().st_size, 0.0, _COMPRESS_SHARE)
        with Pfs0.Pfs0Stream(
            container.getFirstFileOffset(),
            container.getStringTableSize(),
            str(output),
        ) as nsz_stream:
            processContainer(
                container,
                nsz_stream,
                _COMPRESSION_LEVEL,
                False,  # keep: drop update delta fragments, as NSZ does by default
                False,  # useLongDistanceMode: off, to keep memory use handheld-friendly
                _compression_threads(),
                status,
                0,
                None,
            )
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
