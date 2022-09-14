#!/bin/bash

set -e

if [ $# -lt 6 ]; then
  echo "Usage: $0 <NAME> <TEST_CLASS> <IDX> <TIME> <DICT> <SEEDS>"
  exit 1
fi

pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

if [ ! -d "$AFL_DIR" ]; then
  echo "AFL_DIR is not set!"
  exit 2
fi

JQF_DIR="$SCRIPT_DIR/.."
JQF_EI="$JQF_DIR/bin/jqf-ei"
JQF_AFL="$JQF_DIR/bin/jqf-afl-fuzz"
NAME=$1
TEST_CLASS="edu.berkeley.cs.jqf.examples.$2"
IDX=$3
TIME=$4
DICT="$JQF_DIR/examples/target/test-classes/dictionaries/$5"
SEEDS="$JQF_DIR/examples/target/seeds/$6"
SEEDS_DIR=$(dirname "$SEEDS")

e=$IDX

JQF_OUT_DIR="$NAME-jqf-results-$e"
AFL_OUT_DIR="$NAME-afl-results-$e"
RND_OUT_DIR="$NAME-rnd-results-$e"
SEQ_OUT_DIR="$NAME-seq-results-$e"

if [ -d "$JQF_OUT_DIR" ]; then
  echo "Error! There is already a directory by the name of $JQF_OUT_DIR"
  exit 3
fi

# Do not let GC mess with fuzzing
export JVM_OPTS="$JVM_OPTS -XX:-UseGCOverheadLimit"

SNAME="$NAME-$e"

screen -S "$SNAME" -dm -t jqf_$e
#screen -S "$SNAME" -X screen -t jqf_$e
screen -S "$SNAME" -X screen -t afl_$e
screen -S "$SNAME" -X screen -t rnd_$e
screen -S "$SNAME" -X screen -t seq_$e
screen -S "$SNAME" -p jqf_$e -X stuff "timeout $TIME $JQF_EI -c \$($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator $JQF_OUT_DIR ^M"
screen -S "$SNAME" -p rnd_$e -X stuff "JVM_OPTS=\"\$JVM_OPTS -Djqf.ei.TOTALLY_RANDOM=true\" timeout $TIME $JQF_EI -c \$($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator $RND_OUT_DIR ^M"
screen -S "$SNAME" -p afl_$e -X stuff "timeout $TIME $JQF_AFL -t 10000 -c \$($JQF_DIR/scripts/examples_classpath.sh) -x $DICT -o $AFL_OUT_DIR -T $NAME-seq -i $SEEDS_DIR -v $TEST_CLASS testWithInputStream ^M"
screen -S "$SNAME" -p seq_$e -X stuff "JVM_OPTS=\"\$JVM_OPTS -Djqf.ei.MAX_INPUT_SIZE=10240 -Djqf.ei.GENERATE_EOF_WHEN_OUT=true\" timeout $TIME $JQF_EI -c \$($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithInputStream $SEQ_OUT_DIR $SEEDS ^M"

