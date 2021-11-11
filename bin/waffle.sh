#!/bin/sh

JAR_URL="https://desk.tkms.jp/resource/WjjMKKgC2MdI4GpepkzGOy4yQ3dWEOxM/waffle-all.jar"
WAFFLE_INTERNAL="WAFFLE/.INTERNAL"
START_FLAG=${WAFFLE_INTERNAL}/AUTO_START
mkdir -p $WAFFLE_INTERNAL

JAR_FILE="${WAFFLE_INTERNAL}/waffle-all.jar"
if [ ! -e "$JAR_FILE" ];then
  if [ -e "waffle-all.jar" ];then
    cp waffle-all.jar $JAR_FILE
  else
    if which curl >/dev/null 2>&1; then
      curl -o "$JAR_FILE" "$JAR_URL"
    else
      if which wget >/dev/null 2>&1; then
        wget -O "$JAR_FILE" "$JAR_URL"
      else
        echo "Please download manually waffle-all.jar because cURL and Wget are not available."
        exit 1;
      fi
    fi
  fi
fi

JAVA="java"
if [ -n "${JAVA_HOME}" ];then
  JAVA="${JAVA_HOME}/bin/${JAVA}"
fi

touch $START_FLAG
while [ -e "$START_FLAG" ];do
  rm -f $START_FLAG
  ${JAVA} -jar ${JAR_FILE}
done

