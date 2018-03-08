#!/bin/bash

set -e

if [ $# -lt 3 ]; then
  echo "Usage: $0 <NAME> <TEST_CLASS> <IDX>"
  exit 1
fi

pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

JQF_DIR="$SCRIPT_DIR/.."
JQF_REPRO="$JQF_DIR/bin/jqf-repro"
NAME=$1
TEST_CLASS="edu.berkeley.cs.jqf.examples.$2"
IDX=$3
TIME=$4

e=$IDX

JQF_OUT_DIR="$NAME-jqf-results-$e"
AFL_OUT_DIR="$NAME-afl-results-$e"
RND_OUT_DIR="$NAME-rnd-results-$e"


export JVM_OPTS="-Djqf.repro.logUniqueBranches=true"
echo "Gathering coverage for $JQF_OUT_DIR..."
$JQF_REPRO -ic $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator $JQF_OUT_DIR/queue/id*cov | grep "^# Cov" | sort | uniq > $JQF_OUT_DIR.cov

echo "Gathering coverage for $RND_OUT_DIR..."
$JQF_REPRO -ic $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator $RND_OUT_DIR/queue/id*cov | grep "^# Cov" | sort | uniq > $RND_OUT_DIR.cov

echo "Gathering coverage for $AFL_OUT_DIR..."
$JQF_REPRO -ic $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithInputStream $AFL_OUT_DIR/queue/id*cov | grep "^# Cov" | sort | uniq > $AFL_OUT_DIR.cov

