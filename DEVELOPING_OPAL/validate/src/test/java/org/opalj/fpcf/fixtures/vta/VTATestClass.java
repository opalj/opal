package org.opalj.fpcf.fixtures.vta;

import org.opalj.fpcf.properties.vta.ExpectedCallee;
import org.opalj.fpcf.properties.vta.ExpectedType;

 /**
 * This is the test class for the IFDS based Variable Type Analysis. That analysis is only there to serve as an example
 * of an IFDS Analysis.
 *
 */
public class VTATestClass {

    @ExpectedType.List({ @ExpectedType(lineNumber = 15, value = "B", upperBound = false) })
    public void instantiationsAreConsidered() {
        A a = new B();
    }

    @ExpectedType.List({
            @ExpectedType(lineNumber = 22, value = "B", upperBound = false),
            @ExpectedType(lineNumber = 23, value = "C", upperBound = false) })
    public void factsAreRemembered() {
        A x = new B();
        A y = new C();
    }

    @ExpectedType.List({
            @ExpectedType(lineNumber = 30, value = "B[]", upperBound = false),
            @ExpectedType(lineNumber = 30, value = "C[]", upperBound = false) })
    public void arrayTypesAreConsidered_1() {
        A[] a = new A[2];
        a[0] = new B();
        a[1] = new C();
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 38, value = "B", upperBound = false)})
    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 39, value = "B", upperBound = false)})
    public void callTargetsAreConsidered() {
        A a = new B();
        a.doIt();
    }

    @ExpectedType.List({
            @ExpectedType(lineNumber = 47, value = "B", upperBound = false),
            @ExpectedType(lineNumber = 48, value = "C", upperBound = false) })
    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 50, value = "B", upperBound = false)})
    public void variableAssignmentsAreConsidered_1() {
        A b = new B();
        A c = new C();
        if(Math.random() < .5) b = c;
        b.doIt();
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 56, value = "B[]", upperBound = false)})
    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 57, value = "B", upperBound = false)})
    public void arrayLoadsAreConsidered() {
        A[] a = new A[] {new B()};
        a[0].doIt();
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 62, value = "B", upperBound = false)})
    public void typesOfParametersArePassed() {
        A a = new B();
        typesOfParametersArePassed_callee(a);
    }

    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 68, value = "B", upperBound = false)})
    private void typesOfParametersArePassed_callee(A a) {
        a.doIt();
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 74, value = "B", upperBound = false)})
    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 75, value = "B", upperBound = false)})
    public void returnFlowIsConsidered() {
        A a = returnB();
        a.doIt();
    }

    private A returnB() {
        return new B();
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 85, value = "A", upperBound = true)})
    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 86, value = "A", upperBound = true)})
    public void nativeCallsAreConsidered() {
        A a = nativeMethod();
        a.doIt();
    }

    public native A nativeMethod();

    @ExpectedType.List({@ExpectedType(lineNumber = 93, value = "String", upperBound = true)})
    public void staticFieldReadsAreConsidered() {
        Object o = A.STATIC_FIELD;
        System.out.println(o);
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 99, value = "String", upperBound = true)})
    public void fieldReadsAreConsidered() {
        Object o = new B().field;
        System.out.println(o);
    }

    @ExpectedType.List({ @ExpectedType(lineNumber = 105, value = "B", upperBound = false) })
    protected void protectedMethodsAreConsidered() {
        A a = new B();
    }

}

abstract class A {

    public static String STATIC_FIELD = "STATIC_FIELD";

    public String field = "field";

    public abstract void doIt();
}

class B extends A {
    @Override
    public void doIt() {
        System.out.println("B");
    }
}

class C extends A {
    @Override
    public void doIt() {
        System.out.println("C");
    }
}