#!/bin/bash

./cov.sh chess chess.FENTest $1
./cov.sh ant ant.ProjectBuilderTest $1
./cov.sh maven maven.ModelReaderTest $1
./cov.sh bcel bcel.ParserTest $1
./cov.sh closure closure.CompilerTest $1
./cov.sh rhino rhino.CompilerTest $1
