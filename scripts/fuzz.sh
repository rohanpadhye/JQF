#!/bin/bash

# Example
#  ./fuzz.sh TestClass testMethod [inputFile AflToJavaPipe JavaToAflPipe]

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

$SCRIPT_DIR/instrument.sh $JVM_OPTS edu.berkeley.cs.jqf.fuzz.drivers.AFLDriver $@

