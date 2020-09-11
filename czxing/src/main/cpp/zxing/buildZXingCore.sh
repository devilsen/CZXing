#!/bin/bash

export ANDROID_NDK=/Users/Devilsen/Library/Android/sdk/ndk/21.3.6528147

rm -r build
mkdir build
cd build || exit

TARGETS="armeabi-v7a"

for target in ${TARGETS}
do
  mkdir -p "${target}"
  cd "${target}" || exit
  cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI="${target}" \
    -DANDROID_NDK=$ANDROID_NDK \
    -DANDROID_PLATFORM=android-16 \
    ../..

  make

  cd .. || exit
done