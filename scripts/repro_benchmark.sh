#!/bin/bash

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

ROOT_DIR=`dirname $SCRIPT_DIR`

if [ "$#" -lt 3 ]; then
  echo "Usage: $0 BENCHMARK_CLASS_SUFFIX TEST_METHOD TEST_FILE" >&2
  exit 1
fi

class="$1"
method="$2"

export CLASSPATH="examples/target/classes/:examples/target/test-classes/:examples/target/dependency/*"

  
"$ROOT_DIR/bin/jqf-repro" -i edu.berkeley.cs.jqf.examples."$class" "$method" "${@:3}"
