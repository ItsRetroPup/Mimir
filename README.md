# Mimir Android Companion

Mimir is an open source Android toolkit for retro handheld owners.

It is built to help organise ROM libraries safely on-device, with an initial focus on multi-disc cleanup and bulk ROM preparation for Android frontends used on devices like the AYN Odin and Thor.

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

### PS Vita Shortcuts
Mimir includes a built-in searchable PS Vita shortcut database based on `.psvita` shortcut mappings.

Current behaviour:
- search by game title or app ID
- select an output directory on-device
- create shortcuts directly from search results
- detect existing `.psvita` files already present in the selected output folder
- allow mistaken additions to be removed immediately from the same screen

## Design Goals

- local-first
- no cloud sync
- no account system
- safe preview before apply
- deterministic file operations
- frontend compatibility first

## Tech Stack

- Kotlin
- Jetpack Compose
- Gradle
- Android SDK

## Requirements

- Android `minSdk 29`
- Java 17
- Android Studio or Gradle CLI

## Build

### Debug APK
```bash
./gradlew assembleDebug
```

### Release APK
```bash
./gradlew assembleRelease
```

Signed release output:

```text
app/build/outputs/apk/release/app-release.apk
```

## Project Status

Mimir is currently focused on ROM organisation workflows.

Current scope:
- multi-disc normalisation
- frontend preset output generation
- ROM zip conversion
- PS Vita shortcut database and on-device shortcut creation
- Android-native UI for safe local operations

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

## RetroPup

I run the RetroPup YouTube channel, where you can subscribe for tips, tricks, and tutorials for retro handhelds.

YouTube:
https://youtube.com/@ItsRetroPup

Support me on Ko-Fi:
https://ko-fi.com/retropup84752

## License

MIT. See [LICENSE](/Users/alex/Documents/Mimir/LICENSE).
