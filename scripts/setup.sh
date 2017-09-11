#!/bin/bash

# Build Janala2 and copy into lib directory
set -e

pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

ROOT=`dirname $SCRIPT_DIR`

mkdir -p "$ROOT/submodules"

JANALA_DIR="$ROOT/submodules/janala2"
JUNIT_QUICKCHECK_DIR="$ROOT/submodules/junit-quickcheck-guided"

mkdir -p "$ROOT/lib/dependencies"


if [[ ! -e "$ROOT/lib/asm-all-5.2.jar" ]]; then
  echo "Downloading ASM and Junit from Maven Central"
  curl -o "$ROOT/lib/asm-all-5.2.jar" "http://central.maven.org/maven2/org/ow2/asm/asm-all/5.2/asm-all-5.2.jar"
  curl -o "$ROOT/lib/junit-4.12.jar" "http://central.maven.org/maven2/junit/junit/4.12/junit-4.12.jar"
  echo "Done!"
fi

if [[ ! -d "$JANALA_DIR" ]]; then
  git clone https://github.com/rohanpadhye/janala2-gradle/ "$JANALA_DIR"
fi

if [[ ! -d "$JUNIT_QUICKCHECK_DIR" ]]; then
  git clone https://github.com/rohanpadhye/junit-quickcheck -b guided "$JUNIT_QUICKCHECK_DIR"
fi


echo "Building Janala2 in directory:  $JANALA_DIR..."
pushd "$JANALA_DIR" > /dev/null
gradle jar
popd > /dev/null
echo "Success! Copying JAR into lib directory..."
cp -f "$JANALA_DIR"/build/libs/janala2-*.jar "$ROOT/lib/janala.jar"

echo "Building junit-quickcheck in directory:  $JUNIT_QUICKCHECK_DIR..."
pushd "$JUNIT_QUICKCHECK_DIR" > /dev/null
mvn -DskipTests package 
echo "Success! Copying JARs into lib directory..."
cp "$JUNIT_QUICKCHECK_DIR"/core/target/junit-quickcheck-core-*-SNAPSHOT.jar             "$ROOT/lib/junit-quickcheck-guided.jar"
cp "$JUNIT_QUICKCHECK_DIR"/generators/target/junit-quickcheck-generators-*-SNAPSHOT.jar "$ROOT/lib/junit-quickcheck-generators.jar"

echo "Pulling transitive dependencies..."
mvn dependency:copy-dependencies
cp "$JUNIT_QUICKCHECK_DIR"/core/target/dependency/*.jar "$ROOT/lib/dependencies"
cp "$JUNIT_QUICKCHECK_DIR"/generators/target/dependency/*.jar "$ROOT/lib/dependencies"
echo "Success! Transitive dependencies copied."
popd > /dev/null




echo " --- Setup completed ---"

