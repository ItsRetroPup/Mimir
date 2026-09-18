<img width="512" height="512" alt="mimir1" src="https://github.com/user-attachments/assets/7edde894-cf4a-4ad8-822e-5a50af306fd8" />

# Mimir

Mimir is an open source, local-first toolkit for retro handheld owners.

It is built to help organise ROM libraries safely on-device or on desktop, with an initial focus on multi-disc cleanup and bulk ROM preparation for frontends used on devices like the AYN Odin and Thor.

[![Get it on Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium%3A%2F%2Fadd%2Fhttps%3A%2F%2Fgithub.com%2FItsRetroPup%2FMimir)

## RetroPup

I run the RetroPup YouTube channel, where you can subscribe for tips, tricks, and tutorials for retro handhelds.

[![Subscribe on YouTube](https://img.shields.io/badge/Subscribe-YouTube-FF0000?logo=youtube&logoColor=white)](https://youtube.com/@ItsRetroPup)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/Y8Y81SUTLJ)

## Current Modules

### ROM Organiser
Scans a user-selected ROM folder and detects multi-disc games using common naming patterns.

Current behaviour:
- previews intended changes before applying them
- requires confirmation before file operations
- skips conflicts instead of blocking non-conflicting operations
- applies deterministic frontend-specific output rules

### Frontend Presets
Current preset support includes:
- ES-DE
- Other

Current preset behaviour:
- ES-DE groups multi-disc games into a per-game folder and generates an `.m3u`
- Other keeps discs in the platform folder and generates an `.m3u`

### RomZipper
RomZipper scans ROM subfolders and converts supported ROM formats into `.zip` archives.

Current supported extensions:
- Nintendo handheld: `.gb`, `.gbc`, `.gba`, `.nds`
- Nintendo home console: `.nes`, `.fds`, `.fig`, `.sfc`, `.smc`, `.swc`
- Nintendo 64: `.n64`, `.v64`, `.z64`
- Atari: `.a26`, `.a52`, `.a78`, `.lnx`
- Sega home consoles: `.md`, `.gen`, `.smd`, `.32x`
- Sega 8-bit / portable: `.sms`, `.gg`, `.sg`, `.sc`
- NEC / other Japanese systems: `.pce`, `.sgx`, `.ws`, `.wsc`, `.ngp`, `.ngc`
- Computer / miscellaneous: `.col`, `.cv`, `.d64`, `.tap`, `.tzx`, `.z80`, `.sna`, `.rom`, `.mx1`, `.mx2`

Zip naming behaviour:
- strips the original file extension before creating the archive
- example: `Pokemon.gba` becomes `Pokemon.zip`

### Converter Tools

Mimir's Converter Tools page provides one scan, review, overwrite, progress, and cancellation flow
for each supported converter. Every conversion retains the source image unless the CHD delete-source
option is selected.

#### CHDMan

Supported system and source formats:
- Dreamcast: `.gdi`, `.cue`, `.iso`
- PlayStation 1: `.cue`, `.iso`
- PlayStation 2: `.iso`
- Sega CD: `.cue`, `.iso`
- Sega Saturn: `.cue`, `.iso`
- PlayStation Portable: `.iso`

Choose either CD or DVD conversion before scanning. For PSP, use DVD. For PS2, use CD with NetherSX2 and DVD with ARMSX2.

Each scan includes compatible files in the selected folder plus matching system subfolders, case-insensitively:
`dreamcast`/`dc`, `playstation 1`/`ps1`/`psx`, `playstation 2`/`ps2`, `sega cd`/`segacd`/`mega cd`/`megacd`, `sega saturn`/`saturn`, and `playstation portable`/`psp`. `.bin` files are consumed through their `.cue` sheet and are not independent conversion targets.

Conversion progress reports completed images out of the selected total. Users can stop a scan or
conversion immediately, or finish the current conversion and stop before the next one. An optional
toggle deletes the successfully converted source image; for `.cue` and `.gdi` inputs it also deletes
the same-folder track files referenced by that descriptor.

Mimir bundles the Android ARM64 CHDMan executable at
`android/app/src/main/jniLibs/arm64-v8a/libchdman.so`, built from the official MAME source revision
`ecf0add29f06ba131994dca5b88c3a0edf6c2ad8`. The C++ runtime is linked statically so CHDMan can be launched directly from Mimir's native-library directory. Mimir detects a
missing executable and stops before changing files.

CHDMan is distributed under GPL-2.0-or-later. The included license is available in the app assets
at `licenses/MAME-GPL-2.0.txt`. Before publishing a release, publish the complete corresponding
MAME source for revision `ecf0add29f06ba131994dca5b88c3a0edf6c2ad8` (including the build instructions used for this binary) or a
valid GPLv2 source offer.

#### Native converter backends

Mimir bundles the following Android ARM64 command-line backends in
`android/app/src/main/jniLibs/arm64-v8a/`:

- DolphinTool: `.iso` to `.rvz`, using RVZ with 128 KiB blocks and Zstandard level 5.
  Expected binary: `libdolphintool.so` (GPL-2.0-or-later).
- Azahar: decrypted `.3ds` / `.cci` to `.zcci`, using `libazahar.so` built from Azahar
  `2126.0-alpha1` revision `e11f3da49346a45c400001ea424de298649618c8` (GPL-2.0-or-later).
  The reproducible standalone-target patch and build instructions are in
  `docs/azahar-compression-cli.patch` and `docs/azahar-compression-build.md`.

#### NSZ to NSP

Mimir can decompress Nintendo Switch `.nsz` packages to `.nsp`. The converter uses the MIT-licensed
NSZ 5.0.0 Python package through the embedded Chaquopy Python runtime, with Android ARM64 builds of
`zstandard` and `pycryptodome`. Select a folder containing `.nsz` files directly or in a `switch` /
`nintendo switch` subfolder, scan it, and choose the packages to convert. Source `.nsz` files are
kept after a successful conversion. NSZ requires a user-provided `prod.keys` file; use the
**Import prod.keys** action on the NSZ to NSP page. Mimir copies the selected file into app-private
storage so it remains available after restarts and does not depend on the original document
location.

The converter runs entirely on-device. Mimir does not bundle or download Nintendo keys or game
content. Only use it with keys and content you are legally entitled to use.
### PS Vita Shortcuts
Mimir includes a built-in searchable PS Vita shortcut database.

Current behaviour:
- search by game title or app ID
- select an output directory on-device
- create shortcuts directly from search results
- choose either `.psvita` or `.dpt` shortcut files
- generate `.dpt` files with a `[vita_game_id]` section followed by the game ID
- detect existing shortcuts in the currently selected format
- allow mistaken additions to be removed immediately from the same screen

### ES-DE Custom Systems
The ES-DE Systems tool downloads the latest Android custom-system definitions from
[GlazedBelmont/es-de-android-custom-systems](https://github.com/GlazedBelmont/es-de-android-custom-systems), scans the selected ROM root for ES-DE system folders, lets users relocate each detected system with a folder picker, and installs the resulting `es_systems.xml` and `es_find_rules.xml` under the selected ES-DE folder's `custom_systems` directory. Default entries retain `%ROMPATH%/<system-folder>`; relocated entries use absolute Android storage paths. Restart ES-DE after installation so it reloads the definitions.

## Design Goals

- local-first
- no cloud sync
- no account system
- safe preview before apply
- deterministic file operations
- frontend compatibility first

## Tech Stack

- Flutter and Dart
- Riverpod
- Material 3
- Platform adapters for Android, Windows, macOS, and Linux
- Python 3.13 and Chaquopy (NSZ converter)

## Flutter Rebuild

The repository root is now the Flutter application. It targets Android, Windows, macOS, and Linux. Shared ROM scanning and planning logic is pure Dart in [`packages/mimir_core/`](packages/mimir_core); platform-specific filesystem access and converter execution live behind adapters.

Web and iOS are intentionally out of scope because Mimir requires direct local filesystem access and bundled converter executables.

Building the Android target requires a `python3.13` executable on `PATH` so Chaquopy can resolve and package the NSZ dependencies. On Homebrew systems, install it with `brew install python@3.13`.

## Requirements

- Flutter 3.47 or newer
- Dart 3.13 or newer
- Android SDK / Java 17 for Android builds
- Xcode for macOS builds
- Visual Studio with the Desktop C++ workload for Windows builds
- GTK development libraries and CMake for Linux builds

## Build

From the repository root:

```bash
flutter pub get
flutter analyze
flutter test
```

### Android debug APK
```bash
flutter build apk --debug --target-platform android-arm64
```

### Desktop debug builds
```bash
flutter build windows --debug
flutter build macos --debug
flutter build linux --debug
```

Desktop converter bundles are expected under `resources/bin/` for the target platform. See [`resources/README.md`](resources/README.md) and [`tool/release_check.dart`](tool/release_check.dart) before producing a release.

## Project Status

Mimir is now a Flutter application focused on ROM organisation workflows.

Current scope:
- multi-disc normalisation
- frontend preset output generation
- ROM zip conversion
- PS Vita shortcut database and on-device shortcut creation
- Flutter UI with native platform adapters for safe local operations

Planned later work may include:
- more frontend-specific rule validation
- broader file format support
- richer Vita shortcut filtering and metadata options
- UI polish and workflow refinement
- GitHub release workflow
- screenshots and user documentation

## Open Source

Mimir is an open source project, and suggestions are welcome. If there are tools or workflows you think would be useful for retro handheld owners, open an issue or suggest them through the project page.

GitHub:
https://github.com/ItsRetroPup/Mimir

## License

MIT. See [LICENSE](LICENSE).
