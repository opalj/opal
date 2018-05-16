package org.opalj.fpcf.fixtures.field_mutability;

import org.opalj.fpcf.properties.field_mutability.DeclaredFinal;
import org.opalj.fpcf.properties.field_mutability.NonFinal;

abstract class Super{
    public Super(){
        System.out.println(getD());
    }

    public abstract int getD();
}

public class DeclaredFinalFields extends Super {

    @DeclaredFinal("Initialized directly")
    private final int a = 1;

    @DeclaredFinal("Initialized through instance initializer")
    private final int b;

    @DeclaredFinal("Initialized through constructor")
    private final int c;

    @NonFinal(value = "Prematurely read through super constructor", prematurelyRead = true)
    private final int d;

    @NonFinal(value = "Prematurely read through own constructor", prematurelyRead = true)
    private final int e;

    public DeclaredFinalFields() {
        super();
        c=1;
        d=1;
        System.out.println(getE());
        e=1;
    }

    public int getD(){
        return d;
    }

    public int getE(){
        return e;
    }

    // Instance initializer!
    {
        b = 1;
    }
}
