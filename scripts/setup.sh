#!/bin/bash

set -e

pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

ROOT=`dirname $SCRIPT_DIR`

cd ${ROOT}

if [ -n "$AFL_DIR" ]; then
  make
fi

mvn -DskipTests package

