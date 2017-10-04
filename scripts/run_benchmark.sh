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

afl_options="-d -t 6000 -m 8192"
jqf_options=""
while getopts ":avr" opt; do
  case $opt in
    r)
      afl_options="$afl_options -p -h"
      jqf_options="$jqf_options -r"
      ;;
    v)
      jqf_options="$jqf_options -v"
      ;;
    a)
      jqf_options="$jqf_options -a"
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
  esac
done
shift $((OPTIND-1))

if [ "$#" -lt 3 ]; then
  echo "Usage: $0 [-r] [-v] BENCHMARK_CLASS_SUFFIX TEST_METHOD OUTPUT_DIR" >&2
  exit 1
fi

class="$1"
method="$2"
output="$3"

export AFL_SKIP_BIN_CHECK=1
export AFL_NO_AFFINITY=1
export CLASSPATH="examples/target/classes/:examples/target/test-classes/:examples/target/dependency/*"

echo "Fuzzing method $class#$method..."
  
"$AFL_DIR"/afl-fuzz $afl_options -i examples/target/seeds/zeros -o "$output" -T "$class#$method" \
  "$ROOT_DIR/bin/jqf-afl" $jqf_options edu.berkeley.cs.jqf.examples."$class" "$method" @@
