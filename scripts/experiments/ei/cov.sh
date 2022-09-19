#!/bin/bash

set -e

if [ $# -lt 3 ]; then
  echo "Usage: $0 <NAME> <TEST_CLASS> <RUNS>"
  exit 1
fi

pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

JQF_DIR="$SCRIPT_DIR/../../.."
JQF_REPRO="$JQF_DIR/bin/jqf-repro -i"
NAME=$1
TEST_CLASS="edu.berkeley.cs.jqf.examples.$2"
RUNS="$3"


export JVM_OPTS="$JVM_OPTS -Djqf.repro.logUniqueBranches=true -Xmx16g"

for e in $(seq 0 $RUNS); do
  ZEST_OUT_DIR="$NAME-zest-no-count-results-$e"
  EI_OUT_DIR="$NAME-ei-no-count-results-$e"
  ZEST_FAST_OUT_DIR="$NAME-zest-fast-results-$e"
  EI_FAST_OUT_DIR="$NAME-ei-fast-results-$e"

  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $ZEST_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $ZEST_OUT_DIR/cov-all.log  &
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $EI_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $EI_OUT_DIR/cov-all.log &
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $ZEST_FAST_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $ZEST_FAST_OUT_DIR/cov-all.log  &
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $EI_FAST_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $EI_FAST_OUT_DIR/cov-all.log &
done

for job in `jobs -p`
do
  echo $job
  wait $job || let "FAIL+=1"
done


export JVM_OPTS="$JVM_OPTS -Djqf.repro.ignoreInvalidCoverage=true"

for e in $(seq 0 $RUNS); do
  ZEST_OUT_DIR="$NAME-zest-no-count-results-$e"
  EI_OUT_DIR="$NAME-ei-no-count-results-$e"
  ZEST_FAST_OUT_DIR="$NAME-zest-fast-results-$e"
  EI_FAST_OUT_DIR="$NAME-ei-fast-results-$e"

  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $ZEST_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $ZEST_OUT_DIR/cov-valid.log &
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $EI_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $EI_OUT_DIR/cov-valid.log &
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $ZEST_FAST_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $ZEST_FAST_OUT_DIR/cov-valid.log &
  $JQF_REPRO -c $($JQF_DIR/scripts/examples_classpath.sh) $TEST_CLASS testWithGenerator   $EI_FAST_OUT_DIR/corpus/* 2>/dev/null | grep "^# Cov" | sort | uniq > $EI_FAST_OUT_DIR/cov-valid.log &
done

for job in `jobs -p`
do
  echo $job
  wait $job || let "FAIL+=1"
done
