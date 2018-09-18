#!/bin/bash

./cov.sh chess chess.FENTest
./cov.sh ant ant.ProjectBuilderTest
./cov.sh maven maven.ModelReaderTest
./cov.sh bcel bcel.ParserTest
./cov.sh closure closure.CompilerTest
./cov.sh rhino rhino.CompilerTest
