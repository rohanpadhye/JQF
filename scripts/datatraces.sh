#!/bin/bash

# Script to instrument classes and log a trace of data reads/writes
# Example
#  ./datatraces.sh MyClass

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

$SCRIPT_DIR/instrument.sh $JVM_OPTS edu.berkeley.cs.jqf.fuzz.drivers.MainDriver $@

