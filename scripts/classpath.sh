#!/bin/bash

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

# The root dir is one up
ROOT_DIR=`dirname $SCRIPT_DIR`

# Create classpath
cp="$ROOT_DIR/fuzz/target/classes:$ROOT_DIR/fuzz/target/test-classes"

for jar in $ROOT_DIR/fuzz/target/dependency/*.jar; do
  cp="$cp:$jar"
done

echo $cp
