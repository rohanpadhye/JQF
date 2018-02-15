#!/bin/bash

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

ROOT_DIR=`dirname $SCRIPT_DIR`

# Ensure that afl-fuzz can be found
if [ -n "$AFL_DIR" ]; then
  AFL_FUZZ="$AFL_DIR/afl-fuzz"
else
  AFL_FUZZ=$(which afl-fuzz)
fi
if [ ! -x "$AFL_FUZZ" ]; then
  echo "The program 'afl-fuzz' cannot be found" >&2
  echo "Fix this in one of two ways:" >&2
  echo "   1. Make sure 'afl-fuzz' is in your PATH" >&2
  echo "   2. Set the env var AFL_DIR to point to where AFL is installed" >&2
  exit 2
fi

afl_options="-t 6000 -m 8192 -d"
jqf_options=""
input_dir="$ROOT_DIR/examples/target/seeds/zeros"
suffix=""
while getopts ":abcdeihvrs:t:x:" opt; do
  case $opt in
    r)
      afl_options="$afl_options -p -s"
      jqf_options="$jqf_options -r"
      ;;
    v)
      jqf_options="$jqf_options -v -a"
      ;;
    a)
      export JVM_OPTS="$JVM_OPTS -Djqf.afl.perfFeedbackType=ALLOCATION_COUNTS -Djanala.instrumentAlloc=true"
      ;;
    b)
      export JVM_OPTS="$JVM_OPTS -Djqf.afl.perfFeedbackType=BRANCH_COUNTS"
      ;;
    c)
      export JVM_OPTS="$JVM_OPTS -Djqf.afl.perfFeedbackType=TOTAL_BRANCH_COUNT"
      ;;
    h)
      export JVM_OPTS="$JVM_OPTS -Djqf.afl.perfFeedbackType=REDUNDANCY_SCORES -Djanala.instrumentHeapLoad=true"
      ;;
    e)
      input_dir="-"
      ;;
    s)
      input_dir="$ROOT_DIR/examples/target/seeds/$OPTARG"
      ;;
    t)
      suffix="-$OPTARG"
      ;;
    x)
      afl_options="$afl_options -x $OPTARG"
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
  esac
done
shift $((OPTIND-1))

if [ "$#" -lt 3 ]; then
  echo "Usage: $0 [-abcrv] BENCHMARK_CLASS_SUFFIX TEST_METHOD OUTPUT_DIR" >&2
  exit 1
fi

class="$1"
method="$2"
output_dir="$3"


if [ -n "$4" ]; then
  input_file="$4"
else
  input_file="@@"
fi

export AFL_SKIP_BIN_CHECK=1
export AFL_NO_AFFINITY=1
export CLASSPATH="$ROOT_DIR/examples/target/classes/:$ROOT_DIR/examples/target/test-classes/:$ROOT_DIR/examples/target/dependency/*"

echo "Fuzzing method $class#$method..."
  
"$AFL_FUZZ" $afl_options -i $input_dir -o "$output_dir" -T "$class#$method$suffix" \
  "$ROOT_DIR/bin/jqf-afl-target" $jqf_options edu.berkeley.cs.jqf.examples."$class" "$method" "$input_file"
