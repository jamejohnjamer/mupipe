#!/bin/sh
# MuPipe — minimal Gradle wrapper launcher (POSIX).
# Uses the wrapper jar to download/run Gradle 8.5 per gradle/wrapper/gradle-wrapper.properties.
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAVA_EXE="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}/bin/java"
if [ ! -x "$JAVA_EXE" ]; then JAVA_EXE="java"; fi
exec "$JAVA_EXE" -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
