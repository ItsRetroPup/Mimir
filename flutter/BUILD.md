# Mimir Flutter development and release

The replacement application lives here; the Kotlin application remains the parity
reference. Use Flutter 3.47.5, then run `flutter pub get`, `flutter analyze --no-pub`,
`flutter test --no-pub`, and `flutter run -d windows` from this directory.

Windows desktop plugins require Developer Mode for symlinks. Native builds also
require Visual Studio's Desktop development with C++ workload. Build Linux on
Linux and macOS on macOS.

## Implementation

`lib/domain` contains pure Dart planners and Vita parsers. `lib/platform` owns
storage, settings, conversion, and execution. `lib/app` owns Riverpod state and
the adaptive Material workspace. Storage references are Android document-tree
URIs or desktop paths. Operations use validated library-relative paths.

The executor stages streamed copies and converter outputs, rechecks conflicts,
and records per-item results. A failed item restores earlier moves and writes.
Empty newly-created folders are removed during rollback. Output commit is allowed
to finish before reporting cancellation. This is not a crash-recovery journal.

Android keeps `pup.app.mimir`, API 29, and version `0.4.0+10`. Release builds
require the existing repository's `keystore.properties` and signing identity.
The bridge migrates `mimir_prefs`, folder references, and `files/nsz/prod.keys`
once. JNI binaries and Python source come from the existing app's directories.
Building Chaquopy requires Python 3.13 and its pinned dependencies.

Existing Android translations are bundled directly from their shared XML sources.
New workspace copy currently falls back to English.

## Desktop backend bundles

Each release needs architecture-specific native tools and a private, relocatable
Python runtime under `backends` beside its executable. On macOS this means
`Mimir.app/Contents/MacOS/backends`.

```text
backends/
  chdman[.exe]
  dolphintool[.exe]
  azahar[.exe]
  mimir_nsz.py
  python/python[.exe]
  licenses/
  sources/
  fixtures/conversions.json
  manifest.json
```

Python must include NSZ 5.0.0, zstandard 0.23.0, and pycryptodome 3.21.0. No user
keys belong in a bundle. The desktop adapter is `tool/mimir_nsz.py`.

Use the source revisions already used by Android:

- CHDMan: `ecf0add29f06ba131994dca5b88c3a0edf6c2ad8`.
- DolphinTool: `73de7b8d3e59d4bef685f582d305b65bdb85a707`.
- Azahar: `e11f3da49346a45c400001ea424de298649618c8`, with
  `../docs/azahar-compression-cli.patch`.

The existing `../docs/*-build.md` describes Android builds. Desktop builds and
dependent libraries need independent validation for Windows/Linux/macOS on x64
and ARM64. Desktop backend bundles are not present in this repository yet.

```sh
python tool/package_backends.py --target windows-x64 --bundle path/to/bundle --smoke --destination path/to/release/backends
```

The validator requires `manifest.json` with `target`, a `sha256` map of relative
paths, and `sources` entries for `chdman`, `dolphintool`, and `azahar`. Each source
entry contains `revision`, `url`, and `build_instructions`. Include complete
corresponding source, patches, instructions, and licenses in the payload.

`fixtures/conversions.json` is an array of cases with `tool`, `args`, `output`,
and optional output `sha256`. Arguments support `{fixtures}`. Include
redistributable conversion fixtures for all three native backends. Separately
validate NSZ conversion with developer-owned content and keys on each target;
the packaging smoke check verifies NSZ dependency imports only.

## Remaining release acceptance

- Compile and validate all six desktop backend bundles and their dependencies.
- Exercise Android document-provider operations and cancellation on a device.
- Test an in-place signed upgrade from 0.3.1, preserving settings and keys.
- Run every real converter and both cancellation modes on every target.
- Inspect native phone, handheld, and desktop screenshots; translate new copy
  and finish desktop icon packaging.
- Package corresponding source and licenses, then sign/notarize as applicable.

Flutter platform scaffolding is not a claim of validated release support.
