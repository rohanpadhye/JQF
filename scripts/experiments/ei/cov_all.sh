#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )


# $SCRIPT_DIR/cov.sh chess chess.FENTest $1
$SCRIPT_DIR/cov.sh ant ant.ProjectBuilderTest $1
$SCRIPT_DIR/cov.sh maven maven.ModelReaderTest $1
$SCRIPT_DIR/cov.sh bcel bcel.ParserTest $1
$SCRIPT_DIR/cov.sh closure closure.CompilerTest $1
$SCRIPT_DIR/cov.sh rhino rhino.CompilerTest $1
