# Building CHDMan for Android ARM64

The binary in `app/src/main/jniLibs/arm64-v8a/libchdman.so` was built from
MAME revision `ecf0add29f06ba131994dca5b88c3a0edf6c2ad8` using Android NDK r28c.

From a macOS host with that NDK installed, clone the official source and generate the Android ARM64
projects:

```sh
git clone https://github.com/mamedev/mame.git mame
cd mame
git checkout ecf0add29f06ba131994dca5b88c3a0edf6c2ad8
make generate
ANDROID_NDK_HOME=/path/to/ndk SDL_INSTALL_ROOT=/tmp/mimir-sdl \
  3rdparty/genie/bin/darwin/genie --with-tools --OPTIMIZE=3 --target=mame \
  --subtarget=mame --build-dir=build --SDL_INSTALL_ROOT=/tmp/mimir-sdl \
  --gcc=android-arm64 --gcc_version=19.0.1 --osd=sdl --targetos=android \
  --PLATFORM=arm64 --NOASM=1 gmake
```

CHDMan does not need SDL at runtime on Android, but MAME's POSIX support source includes the SDL
header unconditionally. Create this compile-only placeholder:

```sh
mkdir -p /tmp/mimir-sdl/include/SDL2
printf '/* Android CHDMan does not call SDL. */\n' > /tmp/mimir-sdl/include/SDL2/SDL.h
```

Build only CHDMan (rather than the MAME emulator), then strip and copy the executable and C++
runtime into Mimir:

```sh
make -B -j6 -C build/projects/sdl/mame/gmake-android-arm64 config=release64 \
  CPPFLAGS=-I/tmp/mimir-sdl/include CXXFLAGS=-DSDLMAME_ANDROID \
  LDFLAGS=-static-libstdc++ chdman
mkdir -p /path/to/Mimir/app/src/main/jniLibs/arm64-v8a
cp chdman /path/to/Mimir/app/src/main/jniLibs/arm64-v8a/libchdman.so
$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-strip --strip-unneeded \
  /path/to/Mimir/app/src/main/jniLibs/arm64-v8a/libchdman.so
```

The emitted `libchdman.so` is an Android PIE executable with its C++ runtime statically linked; the
`.so` suffix lets Android package it under the app's native library directory. Mimir enables legacy
JNI packaging so the package manager extracts it to that directory before `ProcessBuilder` runs it.
