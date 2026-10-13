"""CLI adapter for the private desktop NSZ runtime. Never reads keys from the host."""
import sys
from pathlib import Path
from nsz.Decompressor import decompress
from nsz.nut import Keys


class Progress(dict):
    def __setitem__(self, key, value):
        super().__setitem__(key, value)
        processed, total = int(value[0]), max(int(value[2]), 1)
        print(f"{min(processed / total, 1) * 100:.1f}%", flush=True)


def main():
    source, output, keys = map(Path, sys.argv[1:4])
    if not keys.is_file() or not Keys.load_default(str(keys)):
        raise RuntimeError("Import a valid prod.keys file into Mimir before converting.")
    decompress(source, str(output.parent), False, statusReportInfo=[Progress(), 0], pleaseNoPrint=None)
    generated = output.parent / (source.stem + ".nsp")
    if not generated.is_file() or generated.stat().st_size == 0:
        raise RuntimeError("NSZ produced no output")
    if generated != output:
        generated.replace(output)


if __name__ == "__main__":
    main()
