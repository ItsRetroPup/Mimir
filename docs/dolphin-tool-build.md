# Building DolphinTool for Android ARM64

Mimir packages DolphinTool as `android/app/src/main/jniLibs/arm64-v8a/libdolphintool.so`. It is an Android
PIE executable despite the `.so` suffix, matching the packaging approach used for CHDMan.

The current binary was built from Dolphin revision `73de7b8d3e59d4bef685f582d305b65bdb85a707`.

## Build prerequisites

- Android NDK 28.2.13676358
- Android CMake 3.22.1 or newer
- Git with recursive submodule support

## Build procedure

Clone the source and its exact submodules, then configure the regular Android source set while
enabling only the command-line target. The normal Android configuration is important: Dolphin's
headless configuration selects desktop POSIX sources that cannot run on Android.

```sh
git clone https://github.com/dolphin-emu/dolphin.git dolphin
cd dolphin
git checkout 73de7b8d3e59d4bef685f582d305b65bdb85a707
git submodule update --init --recursive --force
```

Current Dolphin source needs two local Android/NDK compatibility fixes before this build:

- include `DiscIO/Blob.h` from `Source/Core/DiscIO/VolumeWad.h`
- include `Common/PcapFile.h` from `Source/Core/Core/DSP/DSPCaptureLogger.h`

Configure and build with:

```sh
cmake -S . -B build-android \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-29 \
  -DANDROID_STL=c++_static \
  -DENABLE_CLI_TOOL=ON -DENABLE_NOGUI=OFF -DENABLE_QT=OFF \
  -DENABLE_TESTS=OFF -DENABLE_VULKAN=OFF -DENABLE_CUBEB=OFF -DENABLE_LLVM=OFF \
  -DUSE_DISCORD_PRESENCE=OFF -DUSE_MGBA=OFF -DUSE_RETRO_ACHIEVEMENTS=OFF \
  -DUSE_UPNP=OFF -DENABLE_ANALYTICS=OFF -DENABLE_AUTOUPDATE=OFF
cmake --build build-android --target dolphin-tool -j 6
"$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-strip" --strip-unneeded \
  build-android/Binaries/dolphin-tool
cp build-android/Binaries/dolphin-tool \
  /path/to/Mimir/android/app/src/main/jniLibs/arm64-v8a/libdolphintool.so
```

The output links only Android system libraries. Verify it before release with `llvm-readelf -d`.
