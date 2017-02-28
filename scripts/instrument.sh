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

java -Xbootclasspath/a:"${ROOT}/build/classes/main:${ROOT}/lib/asm-all-5.2.jar:${ROOT}/lib/janala.jar" -cp "${ROOT}/build/classes/test" -javaagent:${ROOT}/lib/janala.jar -Djanala.conf="${SCRIPT_DIR}/janala.conf" $@

