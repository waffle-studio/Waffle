#!/bin/sh

if [ ! -e "./lib/jdk-*" ];then
  cd lib
  tar xf ./openjdk.tar.gz
  cd ..
fi
cd lib/jdk-*/bin
JAVA_PATH="$(pwd)"
cd ../../../
PATH="${JAVA_PATH}:${PATH}" ./gradlew build
cp build/libs/waffle-all.jar WAFFLE/.INTERNAL/

