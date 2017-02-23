#!/bin/bash

# Build Janala2 and copy into lib directory
set -e

if [ $# -lt 1 ]; then
  echo "Usage: $0 JANALA2_DIR " >&2
  exit -1
fi

JANALA_DIR="$1"

pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

ROOT=`dirname $SCRIPT_DIR`

if [[ ! -d "$JANALA_DIR" ]]; then
  git clone https://github.com/zhihan/janala2-gradle/ "$JANALA_DIR"
fi

echo "Attempting to build Janala2 in directory:  $JANALA_DIR..."
pushd "$JANALA_DIR"
gradle jar
popd > /dev/null
echo "Success! Copying JAR into lib directory..."

cp -f "$JANALA_DIR"/build/libs/*.jar "$ROOT/lib/janala.jar"
echo "Setup completed."

