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

ROOT_DIR=`dirname $SCRIPT_DIR`

project="jqf"
version="1.0-SNAPSHOT"

FUZZ_DIR="${ROOT_DIR}/fuzz/target/"
INST_DIR="${ROOT_DIR}/instrument/target/"

FUZZ_JAR="${FUZZ_DIR}/$project-fuzz-$version.jar"
INST_JAR="${INST_DIR}/$project-instrument-$version.jar"


bcp=$jar #"${ROOT}/target/classes"
for f in ${ROOT}/target/dependency/*.jar; do
  bcp=$bcp:$f
done


echo $bcp

java -ea \
  -Xbootclasspath/a:"${INST_DIR}/classes:${INST_JAR}:${INST_DIR}/dependency/asm-all-5.2.jar" \
  -javaagent:"${INST_JAR}" \
  -Djanala.conf="${SCRIPT_DIR}/janala.conf" \
  -cp "${FUZZ_DIR}/classes:${FUZZ_JAR}:${FUZZ_DIR}/test-classes:." \
  $@

