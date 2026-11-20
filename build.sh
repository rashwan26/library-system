#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
rm -rf build/classes
mkdir -p build/classes
find src -name '*.java' -print > build/sources.txt
javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt
jar --create --file build/library-system.jar --main-class main.Main -C build/classes .
printf '%s\n' 'Built build/library-system.jar'
