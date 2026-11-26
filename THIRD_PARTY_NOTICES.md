# Third-party notices

Mimir shows these notices in the app under **Licenses** in the home screen footer
(`app/src/main/java/pup/app/mimir/ui/LicensesScreen.kt`). Keep that list and this file in step when
a tool or dependency is added.

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

## NSZ

This app uses NSZ 5.0.0 to decompress NSZ packages to NSP and to compress NSP packages to NSZ.

- Source: https://github.com/nicoboss/nsz
- License: MIT; the license text is included at `app/src/main/assets/licenses/NSZ-MIT.txt`.

NSZ is run using the Chaquopy Python runtime and these Python packages, installed by pip at build
time. Their license texts are included in `app/src/main/assets/licenses/`.

| Component | Version | License | License file |
| --- | --- | --- | --- |
| [Chaquopy](https://github.com/chaquo/chaquopy) | 17.0.0 | MIT | `Chaquopy-MIT.txt` |
| [Python](https://github.com/python/cpython) | 3.13 | PSF-2.0 | `Python-PSF-2.0.txt` |
| [python-zstandard](https://github.com/indygreg/python-zstandard) (bundles Zstandard) | 0.23.0 | BSD-3-Clause | `zstandard-BSD-3-Clause.txt` |
| [PyCryptodome](https://github.com/Legrandin/pycryptodome) | 3.21.0 | BSD-2-Clause and public domain | `PyCryptodome-BSD-2-Clause.txt` |
| [Enlighten](https://github.com/Rockhopper-Technologies/enlighten) | resolved by pip | MPL-2.0 | `Enlighten-MPL-2.0.txt` |
| [Prefixed](https://github.com/Rockhopper-Technologies/prefixed) | resolved by pip | MPL-2.0 | `Prefixed-MPL-2.0.txt` |
| [Jinxed](https://github.com/Rockhopper-Technologies/jinxed) | resolved by pip | MPL-2.0 (terminfo data: ncurses MIT-style) | `Jinxed-MPL-2.0.txt` |
| [Blessed](https://github.com/jquast/blessed) | resolved by pip | MIT | `Blessed-MIT.txt` |
| [wcwidth](https://github.com/jquast/wcwidth) | resolved by pip | MIT | `wcwidth-MIT.txt` |

The MPL-2.0 packages are distributed unmodified; their source code is available from the linked
repositories and from PyPI.

## ES-DE Android custom systems

This app downloads and uses the Android custom-system definitions maintained by GlazedBelmont.

- Source: https://github.com/GlazedBelmont/es-de-android-custom-systems
- Attribution: GlazedBelmont
- The upstream `es_systems.xml` and `es_find_rules.xml` files are downloaded at runtime and
  installed in the user's ES-DE `custom_systems` directory. They remain subject to the terms of
  the upstream repository.

## Rubik typeface

The app's interface type is set in Rubik, bundled as `app/src/main/res/font/rubik.ttf` (the
variable-weight build from the Google Fonts repository).

- Source: https://github.com/googlefonts/rubik
- Copyright 2015 The Rubik Project Authors
- License: SIL Open Font License 1.1; the license text is included at
  `app/src/main/assets/licenses/Rubik-OFL-1.1.txt`.

## Android and Kotlin libraries

The app is built with AndroidX (including Jetpack Compose), Material Components for Android, the
Kotlin standard library and kotlinx.coroutines.

- Sources: https://github.com/androidx/androidx, https://github.com/material-components/material-components-android,
  https://github.com/JetBrains/kotlin, https://github.com/Kotlin/kotlinx.coroutines
- License: Apache-2.0; the license text is included at `app/src/main/assets/licenses/Apache-2.0.txt`.

## Mimir

Mimir itself is MIT-licensed. The license text is in `LICENSE` and is shown in the app from
`app/src/main/assets/licenses/Mimir-MIT.txt`.
