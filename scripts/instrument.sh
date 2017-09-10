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
LIB="${ROOT}/lib"

bcp="${ROOT}/build/classes/main"
for f in ${LIB}/*.jar; do
  bcp=$bcp:$f
done
for f in ${LIB}/dependencies/*.jar; do
  bcp=$bcp:$f
done



java \
  -Xbootclasspath/a:"$bcp" \
  -javaagent:${ROOT}/lib/janala.jar \
  -Djanala.conf="${SCRIPT_DIR}/janala.conf" \
  -cp "${ROOT}/build/classes/test:." \
  $@

