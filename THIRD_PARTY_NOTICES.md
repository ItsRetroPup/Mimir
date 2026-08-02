# Third-party notices

## MAME CHDMan

This app includes an Android ARM64 build of CHDMan from the MAME project.

- Source: https://github.com/mamedev/mame
- Source revision: `ecf0add29f06ba131994dca5b88c3a0edf6c2ad8`
- License: GPL-2.0-or-later; the license text is included at
  `app/src/main/assets/licenses/MAME-GPL-2.0.txt`.

For every distributed Mimir build containing CHDMan, distribute the complete corresponding source
for this exact MAME revision, including the build scripts and Android build instructions, or provide
a valid GPLv2 written offer for that source.

## DolphinTool

This app includes an Android ARM64 build of DolphinTool from the Dolphin Emulator project.

- Source: https://github.com/dolphin-emu/dolphin
- Source revision: `73de7b8d3e59d4bef685f582d305b65bdb85a707`
- License: GPL-2.0-or-later; the license text is included at
  `app/src/main/assets/licenses/Dolphin-GPL-2.0.txt`.

For every distributed Mimir build containing DolphinTool, distribute the complete corresponding
source for this exact revision, including the Android build instructions and local compatibility
patches used to produce the executable, or provide a valid GPLv2 written offer for that source.

## Azahar compression CLI

This app includes an Android ARM64 build of Azahar's Z3DS/ZCCI compression implementation.

- Source: https://github.com/azahar-emu/azahar
- Source revision: `e11f3da49346a45c400001ea424de298649618c8` (`2126.0-alpha1`)
- Local patch: `docs/azahar-compression-cli.patch`
- Build instructions: `docs/azahar-compression-build.md`
- License: GPL-2.0-or-later; the license text is included at
  `app/src/main/assets/licenses/Azahar-GPL-2.0.txt`.

For every distributed Mimir build containing this binary, distribute the complete corresponding
Azahar source for this exact revision, its recursive submodules, and Mimir's local patch/build
instructions, or provide a valid GPLv2 written offer for that source. The corresponding source also
contains the copyright and license notices for Azahar's statically linked third-party components.
