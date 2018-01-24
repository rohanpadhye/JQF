#!/bin/bash

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

ROOT_DIR=`dirname $SCRIPT_DIR`

if [ "$#" -lt 3 ]; then
  echo "Usage: $0 [-i] BENCHMARK_CLASS_SUFFIX TEST_METHOD TEST_FILE" >&2
  exit 1
fi

args=""
if [ "$1" = "-i" ]; then
  args="-i"
  shift 1
fi

class="$1"
method="$2"

export CLASSPATH="$ROOT_DIR/examples/target/classes/:$ROOT_DIR/examples/target/test-classes/:$ROOT_DIR/examples/target/dependency/*"
export JVM_OPTS="-Djanala.instrumentHeapLoad=true -Djanala.instrumentAlloc=true"
  
"$ROOT_DIR/bin/jqf-repro" $args edu.berkeley.cs.jqf.examples."$class" "$method" "${@:3}"
