"""Small Android-facing adapter for NSZ's decompression API."""

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
