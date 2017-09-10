#!/bin/bash

# Script to run junit-quickcheck-guided
# Example
#  ./fuzz.sh TestClass testMethod [testInputFile]

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

$SCRIPT_DIR/instrument.sh -ea -Djanala.loggerClass=jwig.logging.DataTraceLogger $JVM_OPTS jwig.drivers.JUnitTestDriver $@

