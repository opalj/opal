#!/bin/sh
# Assumes that the Annotations are built using SBT
javac -h . \
  ../../../../java/org/opalj/fpcf/fixtures/taint/xlang/TaintTest.java \
  --class-path ../../../../../../target/scala-2.13/it-classes/

mv org_opalj_fpcf_fixtures_taint_xlang_TaintTest.h TaintTest.h