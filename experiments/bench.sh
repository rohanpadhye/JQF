#!/bin/bash

set -e

if [ $# -lt 5 ]; then
  echo "Usage: $0 <NAME> <TEST_CLASS> <IDX> <TIME> <DICT>"
  exit 1
fi

pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

JQF_DIR="$SCRIPT_DIR/.."
JQF_EI="$JQF_DIR/bin/jqf-ei"
JQF_AFL="$JQF_DIR/bin/jqf-afl-fuzz"
NAME=$1
TEST_CLASS="edu.berkeley.cs.jqf.examples.$2"
IDX=$3
TIME=$4
DICT="$JQF_DIR/examples/target/test-classes/dictionaries/$5"

e=$IDX

screen -S "$NAME" -dm 
JQF_OUT_DIR="$NAME-jqf-results-$e"
AFL_OUT_DIR="$NAME-afl-results-$e"
RND_OUT_DIR="$NAME-rnd-results-$e"
JFL_OUT_DIR="$NAME-jfl-results-$e"
screen -S "$NAME" -X screen -t jqf_$e
screen -S "$NAME" -X screen -t afl_$e
screen -S "$NAME" -X screen -t rnd_$e
screen -S "$NAME" -X screen -t jfl_$e
screen -S "$NAME" -p jqf_$e -X stuff "timeout $TIME $JQF_EI -c ($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator $JQF_OUT_DIR ^M"
screen -S "$NAME" -p rnd_$e -X stuff "env JVM_OPTS=\"\$JVM_OPTS -Djqf.ei.TOTALLY_RANDOM=true\" timeout $TIME $JQF_EI -c ($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator $RND_OUT_DIR ^M"
screen -S "$NAME" -p afl_$e -X stuff "timeout $TIME $JQF_AFL -c ($JQF_DIR/scripts/examples_classpath.sh) -x $DICT -o $AFL_OUT_DIR -T $NAME-seq $TEST_CLASS testWithInputStream ^M"
screen -S "$NAME" -p jfl_$e -X stuff "timeout $TIME $JQF_AFL -c ($JQF_DIR/scripts/examples_classpath.sh) -o $JFL_OUT_DIR -T $NAME-gen $TEST_CLASS testWithGenerator ^M"

