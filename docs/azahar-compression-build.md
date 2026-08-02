# Building the Azahar compression CLI for Android ARM64

Mimir packages Azahar's upstream Z3DS compression implementation as
`app/src/main/jniLibs/arm64-v8a/libazahar.so`. The file is an Android PIE executable despite the
`.so` suffix, which causes Android Gradle Plugin to extract it into the native-library directory.

The bundled binary is built from Azahar revision
`e11f3da49346a45c400001ea424de298649618c8` (`2126.0-alpha1`). The small patch in
`docs/azahar-compression-cli.patch` exposes the upstream compression CLI as a standalone target; it
does not change the ZCCI implementation or format.

## Prerequisites

- Android NDK 28.2.13676358
- CMake 3.25 or newer
- Ninja
- Git with recursive submodule support

## Build

```sh
git clone https://github.com/azahar-emu/azahar.git azahar
cd azahar
git checkout e11f3da49346a45c400001ea424de298649618c8
git submodule update --init --recursive --force
git apply /path/to/Mimir/docs/azahar-compression-cli.patch

cmake -S . -B build-android-cli -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 -DANDROID_STL=c++_static \
  -DANDROID_ARM_NEON=TRUE -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
  -DENABLE_COMPRESSION_CLI=ON -DENABLE_QT=OFF -DENABLE_SDL2=OFF \
  -DENABLE_TESTS=OFF -DENABLE_GDBSTUB=OFF -DENABLE_OPENAL=OFF \
  -DENABLE_CUBEB=OFF -DENABLE_LIBUSB=OFF -DENABLE_WEB_SERVICE=OFF \
  -DENABLE_SCRIPTING=OFF -DENABLE_VULKAN=ON -DENABLE_OPENGL=OFF \
  -DENABLE_LTO=ON -DCITRA_WARNINGS_AS_ERRORS=OFF
cmake --build build-android-cli --target azahar-compress -j 6

"$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-strip" \
  --strip-unneeded build-android-cli/bin/Release/azahar-compress
cp build-android-cli/bin/Release/azahar-compress \
  /path/to/Mimir/app/src/main/jniLibs/arm64-v8a/libazahar.so
```

Verify the result with `llvm-readelf -h -l -d`. It must be an AArch64 PIE executable, request
`/system/bin/linker64`, use 16 KiB-compatible load-segment alignment, and depend only on Android
system libraries.
