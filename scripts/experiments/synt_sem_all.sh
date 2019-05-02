#!/bin/bash
#$1: Runs

./synt_sem.sh maven "org/codehaus/plexus/util/xml" "org/apache/maven/model" $1
./synt_sem.sh ant "com/sun/org/apache/xerces" "org/apache/tools/ant" $1
./synt_sem.sh closure "com/google/javascript/jscomp/parsing" "com/google/javascript/jscomp/[A-Z]" $1
./synt_sem.sh rhino "org/mozilla/javascript/Parser" "org/mozilla/javascript/(optimizer|CodeGenerator)" $1
./synt_sem.sh chess "chess/format" "chess/variant" $1
./synt_sem.sh bcel "org/apache/bcel/classfile" " org/apache/bcel/verifier" $1
