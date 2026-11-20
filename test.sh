#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
sh ./build.sh
mkdir -p build/test-classes
javac --release 17 -encoding UTF-8 -cp build/classes -d build/test-classes test/LibraryRegressionTests.java
java -Djava.awt.headless=true -cp build/classes:build/test-classes LibraryRegressionTests
