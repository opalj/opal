#!/bin/sh
# Assumes that the Annotations are built using SBT
if [ -z "$JAVA_HOME"]
then
  export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which javac))))"
fi


clang -shared -fpic -I$JAVA_HOME/include -I$JAVA_HOME/include/linux/ -o libtainttest.so TaintTest.c
clang -S -I$JAVA_HOME/include -I$JAVA_HOME/include/linux/ TaintTest.c -emit-llvm
javac ../../../../java/org/opalj/fpcf/fixtures/taint/xlang/TaintTest.java \
  --class-path ../../../../../../target/scala-2.13/it-classes/ -d .
java -Djava.library.path=. org.opalj.fpcf.fixtures.taint.xlang.TaintTest