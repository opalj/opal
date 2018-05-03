package org.opalj.fpcf.fixtures.escape.interface_test;

public interface A {
    void foo(Object param);

    default void bar(Object param) {
        Main.global = param;
    }

    default void bazz(Object param) {
        Main.global = param;
    }
}
