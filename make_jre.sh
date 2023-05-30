#!/bin/bash
OUT="build/waffle-jre"
cd $(dirname $0)
./gradlew build
MODS=$(jdeps -s build/libs/waffle-all.jar | sed -e 's/.*-> //' | grep -v "not found" | grep -v "java.base" |awk 'BEGIN{s="java.base"}(""!=$1){s=s "," $1}END{print s}')
rm -rf $OUT
jlink --add-modules $MODS --compress=1 --strip-debug --no-header-files  --no-man-pages --output $OUT
cd build
tar cvf waffle-jre.tar.gz waffle-jre

