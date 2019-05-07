package org.opalj.fpcf.fixtures.vta;

import org.opalj.fpcf.properties.vta.ExpectedType;

public class VTATestClass {

    @ExpectedType({"9", "B"})
    public void instantiationsAreConsidered() {
        A a = new B();
    }

}

abstract class A {}

class B extends A {}

class C extends A {}