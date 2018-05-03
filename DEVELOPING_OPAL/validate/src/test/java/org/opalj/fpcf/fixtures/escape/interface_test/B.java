package org.opalj.fpcf.fixtures.escape.interface_test;

public class B implements A{

    @Override
    public void foo(Object param) {
        Main.global = param;
    }

    @Override
    public void bar(Object param) {

    }
}
