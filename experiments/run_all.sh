#!/bin/bash

if [ $# -lt 1 ]; then
  echo "Usage: $0 ID" 2>/dev/null
  exit 1
fi

id=$1
time=3h

./bench.sh ant ant.ProjectBuilderTest $id $time ant-project-afl.dict xml/build.xml
sleep $time 10s

./bench.sh maven maven.ModelReaderTest $id $time maven-model-afl.dict xml/pom.xml
sleep $time 10s

./bench.sh bcel bcel.ParserTest $id $time javaclass.dict javaclass/Hello.class
sleep $time 10s

./bench.sh closure closure.CompilerTest $id $time javascript.dict js/react.production.min.js
sleep $time 10s

./bench.sh rhino rhino.CompilerTest $id $time javascript.dict js/react.production.min.js
sleep $time 10s

./bench.sh chess chess.FENTest $id $time fen.dict fen/initial.fen
sleep $time 10s
