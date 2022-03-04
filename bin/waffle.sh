#!/bin/sh

JRE_URL="https://files.tkms.jp/waffle-jre.tar.gz"
JAR_URL="https://desk.tkms.jp/resource/WjjMKKgC2MdI4GpepkzGOy4yQ3dWEOxM/waffle-all.jar"
JAVA_OPT="--add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED"

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

JRE_FILE="${WAFFLE_INTERNAL}/waffle-jre.tar.gz"
JRE_DIR="${WAFFLE_INTERNAL}/waffle-jre"
if [ ! -e "$JRE_DIR" ];then
  if [ -e "$JRE_FILE" ];then
    sh -c "cd ${WAFFLE_INTERNAL} && tar xf waffle-jre.tar.gz"
  else
    if [ -e "waffle-jre.tar.gz" ];then
      cp waffle-jre.tar.gz $JRE_FILE
      sh -c "cd ${WAFFLE_INTERNAL} && tar xf waffle-jre.tar.gz"
    else
      if which curl >/dev/null 2>&1; then
        curl -o "$JRE_FILE" "$JRE_URL"
        sh -c "cd ${WAFFLE_INTERNAL} && tar xf waffle-jre.tar.gz"
      else
        if which wget >/dev/null 2>&1; then
          wget -O "$JRE_FILE" "$JRE_URL"
          sh -c "cd ${WAFFLE_INTERNAL} && tar xf waffle-jre.tar.gz"
        else
          echo "Please download manually waffle-jre.tar.gz because cURL and Wget are not available."
          exit 1;
        fi
      fi
    fi
  fi
fi

if [ -n "${JAVA_HOME}" ];then
  JAVA="${JAVA_HOME}/bin/java"
else
  JAVA="${JRE_DIR}/bin/java"
fi

touch $START_FLAG
while [ -e "$START_FLAG" ];do
  rm -f $START_FLAG
  ${JAVA} ${JAVA_OPT} -jar ${JAR_FILE}
done

