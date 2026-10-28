<img width="512" height="512" alt="mimir1" src="https://github.com/user-attachments/assets/7edde894-cf4a-4ad8-822e-5a50af306fd8" />

# Mimir

The Flutter rebuild is under [`flutter/`](flutter/BUILD.md), alongside the original
Android application. It contains shared tool logic and an adaptive workspace for
Android and desktops. Native build validation and desktop converter bundles are
still required before releasing it as a replacement.

## Original Android Companion

Mimir is an open source Android toolkit for retro handheld owners. It tidies, converts and shrinks
your game library right on the device. Every tool shows you what it will change before it writes anything.

## What Mimir does

**Utilities**
- **Multi-disc Organizer:** finds games that span more than one disc and writes an `.m3u` playlist
  for each, so your frontend lists the game once and can swap discs.
- **Vita Shortcuts:** search the built-in PS Vita title list and save a `.psvita` or `.dpt` shortcut,
  so your frontend can list and launch games installed in Vita3K.
- **ES-DE Systems:** downloads the latest community custom systems for ES-DE on Android, points each
  one at its ROM folder, and installs them into ES-DE.
- **NSZ to NSP:** decompresses Switch `.nsz` files into full `.nsp` packages for emulators that can't
  read NSZ. Needs your own `prod.keys`.
- **ROM Patcher:** applies an IPS, UPS or BPS patch (a translation, romhack or bug fix) and saves the
  patched copy next to the ROM, leaving the original as it is.

**Space savers**
- **RomZipper:** zips cartridge ROMs (Game Boy, GBA, DS, NES, SNES, N64, Genesis and more) one per
  `.zip`. Most emulators open the zip directly.
- **CHDMan:** compresses PS1, PS2, PS3, PSP, Saturn, Dreamcast and Sega CD disc images into one
  lossless `.chd` per game.
- **Dolphin RVZ:** compresses GameCube and Wii `.iso` files to `.rvz`, Dolphin's lossless format.
- **Azahar ZCCI:** compresses decrypted 3DS games to `.zcci`, which Azahar plays directly.
- **Xbox XISO:** trims the unused video partition and padding from Redump Xbox `.iso` files for xemu
  and Hakux.

**Stats** shows how much space the space savers have freed and how many games they've compressed.

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
option is selected, except Xbox XISO, which replaces each source image with its XISO under the same name.

#### CHDMan

Supported system and source formats:
- Dreamcast: `.gdi`, `.cue`, `.iso`
- PlayStation 1: `.cue`, `.iso`
- PlayStation 2: `.iso`
- PlayStation 3: `.iso`
- Sega CD: `.cue`, `.iso`
- Sega Saturn: `.cue`, `.iso`
- PlayStation Portable: `.iso`

PSP and PS3 always convert as DVD, and the other systems as CD. For PS2, choose CD for NetherSX2 or DVD for ARMSX2.
PS3 CHDs load in ARMSX3 1.0.6 and later; an encrypted image keeps using the `.dkey` beside it, which
Mimir leaves in place. CHDMan works on a copy in internal storage, so a conversion needs free space
there of about twice the image's size; Mimir checks this before starting.

Each scan includes compatible files in the selected folder plus matching system subfolders, case-insensitively:
`dreamcast`/`dc`, `playstation 1`/`ps1`/`psx`, `playstation 2`/`ps2`, `playstation 3`/`ps3`, `sega cd`/`segacd`/`mega cd`/`megacd`, `sega saturn`/`saturn`, and `playstation portable`/`psp`. `.bin` files are consumed through their `.cue` sheet and are not independent conversion targets.

Conversion progress reports completed images out of the selected total. Users can stop a scan or
conversion immediately, or finish the current conversion and stop before the next one. An optional
toggle deletes the successfully converted source image; for `.cue` and `.gdi` inputs it also deletes
the same-folder track files referenced by that descriptor.

Mimir bundles the Android ARM64 CHDMan executable at
`app/src/main/jniLibs/arm64-v8a/libchdman.so`, built from the official MAME source revision
`ecf0add29f06ba131994dca5b88c3a0edf6c2ad8`. The C++ runtime is linked statically so CHDMan can be launched directly from Mimir's native-library directory. Mimir detects a
missing executable and stops before changing files.

CHDMan is distributed under GPL-2.0-or-later. The included license is available in the app assets
at `licenses/MAME-GPL-2.0.txt`. Before publishing a release, publish the complete corresponding
MAME source for revision `ecf0add29f06ba131994dca5b88c3a0edf6c2ad8` (including the build instructions used for this binary) or a
valid GPLv2 source offer.

#### Native converter backends

Mimir bundles the following Android ARM64 command-line backends in
`app/src/main/jniLibs/arm64-v8a/`:

- DolphinTool: `.iso` to `.rvz`, using RVZ with 128 KiB blocks and Zstandard level 5.
  Expected binary: `libdolphintool.so` (GPL-2.0-or-later).
- Azahar: decrypted `.3ds` / `.cci` to `.zcci`, using `libazahar.so` built from Azahar
  `2126.0-alpha1` revision `e11f3da49346a45c400001ea424de298649618c8` (GPL-2.0-or-later).
  The reproducible standalone-target patch and build instructions are in
  `docs/azahar-compression-cli.patch` and `docs/azahar-compression-build.md`.

#### Xbox XISO

Mimir can trim original Xbox Redump `.iso` dumps to XISO for Hakux and xemu. Select a folder
containing `.iso` files directly or in an `xbox` / `microsoft xbox` subfolder. The scan only lists
images that still contain the Redump video partition (XGD1, XGD2, or XGD3); existing XISOs and
non-Xbox ISOs are left out.

The conversion rewrites the XDVDFS game partition with directories and files packed contiguously,
matching `extract-xiso -r`, and keeps the original file name. It is implemented in Kotlin and reads
the source in place, so no cache copy of the image is needed. Each XISO is written to a temporary
`.mimir-xiso` file beside its source; the original is deleted and the XISO renamed only after the
write succeeds. Make sure the storage has room for one extra XISO while converting.

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

### ROM Patcher
Pick a ROM and an `.ips`, `.ups` or `.bps` patch. Mimir names the result after the patch (for example
`Game (Hack).sfc`) and saves it in the ROM's own folder, asking once for access to that folder if
needed. UPS and BPS checksums are verified, so a damaged patch or one made for a different ROM version
is reported instead of written. The original ROM is never changed.

### Stats
The Stats link on the home screen shows lifetime totals for the five space savers (RomZipper,
CHDMan, Dolphin RVZ, Azahar ZCCI and Xbox XISO): space saved, games compressed, and a breakdown per
tool. Totals are kept on the device across restarts and updates, and counting starts the first time
Mimir 1.0 runs.

#
## Open Source

Mimir is an open source project, and suggestions are welcome. If there are tools or workflows you think would be useful for retro handheld owners, open an issue or suggest them through the project page.

GitHub:
https://github.com/ItsRetroPup/Mimir

## License

MIT. See [LICENSE](/Users/alex/Documents/Mimir/LICENSE).
