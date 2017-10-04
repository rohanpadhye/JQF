#!/bin/bash

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

ROOT_DIR=`dirname $SCRIPT_DIR`

if [ -z "$AFL_DIR" ]; then
  echo "Set env variable AFL_DIR to point to where AFL is installed" >&2
  exit 2
fi

if [ "$@" -lt 3 ]; then
  echo "Usage: $0 BENCHMARK_CLASS_SUFFIX TEST_METHOD OUTPUT_DIR" >&2
  exit 1
fi

class="$1"
method="$2"
output="$3"

export AFL_SKIP_BIN_CHECK=1
export AFL_NO_AFFINITY=1
export CLASSPATH="benchmarks/target/test-classes/:benchmarks/target/dependency/*"

"$AFL_DIR"/afl-fuzz -i benchmarks/target/seeds/zeros -o "$output" -t 6000 -m 8192 -d \
  "$ROOT_DIR/bin/jqf-afl" benchmarks."$class" "$method" @@
