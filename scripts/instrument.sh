#!/bin/bash

#
# Simple script to run catg to instrument classes, writes
# the instrumented classes and then run the file and print
# the instructions executed to the screen
#
# Example
#  ./instrument.sh MyClass

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

ROOT=`dirname $SCRIPT_DIR`

bcp="${ROOT}/target/classes"
for f in ${ROOT}/target/dependency/*.jar; do
  bcp=$bcp:$f
done

java -ea \
  -Xbootclasspath/a:"$bcp" \
  -javaagent:${ROOT}/target/jwig-1.0-SNAPSHOT.jar \
  -Djanala.conf="${SCRIPT_DIR}/janala.conf" \
  -cp "${ROOT}/target/test-classes:." \
  $@

