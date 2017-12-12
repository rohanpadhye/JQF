#!/bin/bash

set -e

pushd `dirname $0` > /dev/null
ROOT_DIR=`pwd`
popd > /dev/null

cd ${ROOT_DIR}

# Build AFL proxy
make

# Build JQF
mvn package

