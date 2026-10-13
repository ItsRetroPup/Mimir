"""Validate and install a supplied, architecture-specific backend bundle.

No downloads or host PATH fallbacks. Native tools must be built from the pinned
sources described in the repository's build documentation.
"""
import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

TARGETS = {f"{os}-{arch}" for os in ("windows", "linux", "macos") for arch in ("x64", "arm64")}
REVISIONS = {
    "chdman": "ecf0add29f06ba131994dca5b88c3a0edf6c2ad8",
    "dolphintool": "73de7b8d3e59d4bef685f582d305b65bdb85a707",
    "azahar": "e11f3da49346a45c400001ea424de298649618c8",
}


def sha256(path):
    with path.open("rb") as source:
        digest = hashlib.file_digest(source, "sha256")
    return digest.hexdigest()


def validate(bundle, target):
    manifest = json.loads((bundle / "manifest.json").read_text(encoding="utf-8"))
    if manifest["target"] != target:
        raise ValueError(f"Bundle target {manifest['target']} does not match {target}")
    suffix = ".exe" if target.startswith("windows") else ""
    required = [f"chdman{suffix}", f"dolphintool{suffix}", f"azahar{suffix}", f"python/python{suffix}", "mimir_nsz.py"]
    for relative in required:
        if not (bundle / relative).is_file():
            raise ValueError(f"Missing bundled backend: {relative}")
    checksums = manifest.get("sha256", {})
    for relative, checksum in checksums.items():
        path = (bundle / relative).resolve()
        if not path.is_relative_to(bundle.resolve()) or not path.is_file():
            raise ValueError(f"Invalid manifest path: {relative}")
        if sha256(path) != checksum:
            raise ValueError(f"Checksum mismatch: {relative}")
    if not all(relative in checksums for relative in required):
        raise ValueError("Manifest must include checksums for all backend executables and the NSZ adapter")
    for name in ("chdman", "dolphintool", "azahar"):
        metadata = manifest.get("sources", {}).get(name, {})
        if not all(metadata.get(key) for key in ("revision", "url", "build_instructions")):
            raise ValueError(f"Missing source provenance for {name}")
        if metadata["revision"] != REVISIONS[name]:
            raise ValueError(f"Unexpected source revision for {name}")
    for directory in ("licenses", "sources"):
        if not (bundle / directory).is_dir() or not any((bundle / directory).iterdir()):
            raise ValueError(f"Missing {directory} payload")
    return required


def smoke(bundle, target):
    suffix = ".exe" if target.startswith("windows") else ""
    python = bundle / "python" / f"python{suffix}"
    subprocess.run([str(python), "-I", "-c", "import nsz, zstandard, Crypto; print('NSZ dependencies OK')"], check=True, timeout=60)
    # Exercise real native tools with redistributable fixtures prepared by the builder.
    fixtures = bundle / "fixtures"
    cases = json.loads((fixtures / "conversions.json").read_text(encoding="utf-8"))
    covered = set()
    for case in cases:
        tool = case["tool"]
        if tool not in ("chdman", "dolphintool", "azahar"):
            raise ValueError("Unknown smoke-test backend")
        args = [arg.replace("{fixtures}", str(fixtures)) for arg in case["args"]]
        subprocess.run([str(bundle / f"{tool}{suffix}"), *args], check=True, timeout=300)
        output = (fixtures / case["output"]).resolve()
        if not output.is_relative_to(fixtures.resolve()) or not output.is_file() or output.stat().st_size == 0:
            raise ValueError(f"Smoke conversion produced no output: {tool}")
        if case.get("sha256") and sha256(output) != case["sha256"]:
            raise ValueError(f"Smoke output checksum mismatch: {tool}")
        covered.add(tool)
    if covered != {"chdman", "dolphintool", "azahar"}:
        raise ValueError("Every native backend needs a conversion fixture")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", choices=sorted(TARGETS), required=True)
    parser.add_argument("--bundle", type=Path, required=True)
    parser.add_argument("--destination", type=Path)
    parser.add_argument("--smoke", action="store_true")
    args = parser.parse_args()
    validate(args.bundle, args.target)
    if args.smoke:
        smoke(args.bundle, args.target)
    if args.destination:
        destination = args.destination.resolve()
        if destination.exists():
            raise ValueError("Destination already exists; choose a fresh packaging directory")
        shutil.copytree(args.bundle, destination)
    print(f"Validated {args.target} backend bundle")


if __name__ == "__main__":
    main()
