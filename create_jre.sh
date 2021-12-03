#!/bin/sh

cd $(dirname $0)

sh -c 'cd sub_project/waffle-servant && ./gradlew build'
#rm -rf servant-jre
#jlink --no-header-files --no-man-pages --compress=2 --add-modules $(jdeps --list-deps --ignore-missing-deps sub_project/waffle-servant/build/libs/waffle-servant-all.jar | grep -v "java.base" | awk 'NR==1{printf "java.base"} {printf ","$1}') --output servant-jre
#tar zcf servant-jre.tar.gz servant-jre

./gradlew build
rm -rf waffle-jre
jlink --no-header-files --no-man-pages --compress=2 --add-modules $(jdeps --list-deps --ignore-missing-deps build/libs/waffle-all.jar | grep -v "java.base" | awk 'NR==1{printf "java.base"} {printf ","$1}') --output waffle-jre
tar zcf waffle-jre.tar.gz waffle-jre

