#!/bin/bash

set -e

if [ $# -lt 3 ]; then
  echo "Usage: $0 <NAME> <TEST_CLASS> <RUNS>"
  exit 1
fi

pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

JQF_DIR="$SCRIPT_DIR/.."
JQF_REPRO="$JQF_DIR/bin/jqf-repro -i"
NAME=$1
TEST_CLASS="edu.berkeley.cs.jqf.examples.$2"
RUNS="$3"
  

export JVM_OPTS="$JVM_OPTS -Djqf.repro.logUniqueBranches=true"

for e in $(seq $RUNS); do
  JQF_OUT_DIR="$NAME-jqf-results-$e"
  AFL_OUT_DIR="$NAME-afl-results-$e"
  RND_OUT_DIR="$NAME-rnd-results-$e"
  SEQ_OUT_DIR="$NAME-seq-results-$e"

  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $JQF_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $JQF_OUT_DIR/cov-all.log 
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $RND_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $RND_OUT_DIR/cov-all.log
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithInputStream $SEQ_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $SEQ_OUT_DIR/cov-all.log
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithInputStream $AFL_OUT_DIR/queue/id* 2>/dev/null | grep "^# Cov" | sort | uniq > $AFL_OUT_DIR/cov-all.log
done

export JVM_OPTS="$JVM_OPTS -Djqf.repro.ignoreInvalidCoverage=true"

for e in $(seq 3); do
  JQF_OUT_DIR="$NAME-jqf-results-$e"
  AFL_OUT_DIR="$NAME-afl-results-$e"
  RND_OUT_DIR="$NAME-rnd-results-$e"
  SEQ_OUT_DIR="$NAME-seq-results-$e"

  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $JQF_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $JQF_OUT_DIR/cov-valid.log 
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $RND_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $RND_OUT_DIR/cov-valid.log
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithInputStream $SEQ_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $SEQ_OUT_DIR/cov-valid.log
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithInputStream $AFL_OUT_DIR/queue/id* 2>/dev/null | grep "^# Cov" | sort | uniq > $AFL_OUT_DIR/cov-valid.log
done
