#!/bin/bash

set -e

pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

if [ -z "$AFL_DIR" ]; then
  echo "Please set env var AFL_DIR to point to the local AFL repository"
  exit 1
fi

ROOT=`dirname $SCRIPT_DIR`

cd ${ROOT}/afl
make

cd ${ROOT}

mvn -DskipTests package

