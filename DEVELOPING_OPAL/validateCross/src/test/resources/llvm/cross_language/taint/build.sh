#!/bin/sh
if [ -z "$JAVA_HOME"]
then
  export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which javac))))"
fi

clang -shared -fpic -I$JAVA_HOME/include -I$JAVA_HOME/include/linux/ -o libtainttest.so TaintTest.c
clang -S -I$JAVA_HOME/include -I$JAVA_HOME/include/linux/ TaintTest.c -emit-llvm