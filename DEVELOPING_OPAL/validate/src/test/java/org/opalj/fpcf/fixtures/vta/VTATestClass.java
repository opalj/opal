/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.vta;

import org.opalj.fpcf.properties.vta.ExpectedCallee;
import org.opalj.fpcf.properties.vta.ExpectedType;

 /**
 * This is the test class for the IFDS based Variable Type Analysis. That analysis is only there to serve as an example
 * of an IFDS Analysis.
 *
 * @author Marc Clement
 */
public class VTATestClass {

    @ExpectedType.List({ @ExpectedType(lineNumber = 17, value = "B", upperBound = false) })
    public void instantiationsAreConsidered() {
        A a = new B();
    }

    @ExpectedType.List({
            @ExpectedType(lineNumber = 24, value = "B", upperBound = false),
            @ExpectedType(lineNumber = 25, value = "C", upperBound = false) })
    public void factsAreRemembered() {
        A x = new B();
        A y = new C();
    }

    @ExpectedType.List({
            @ExpectedType(lineNumber = 32, value = "B[]", upperBound = false),
            @ExpectedType(lineNumber = 32, value = "C[]", upperBound = false) })
    public void arrayTypesAreConsidered_1() {
        A[] a = new A[2];
        a[0] = new B();
        a[1] = new C();
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 40, value = "B", upperBound = false)})
    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 41, value = "B", upperBound = false)})
    public void callTargetsAreConsidered() {
        A a = new B();
        a.doIt();
    }

    @ExpectedType.List({
            @ExpectedType(lineNumber = 49, value = "B", upperBound = false),
            @ExpectedType(lineNumber = 50, value = "C", upperBound = false) })
    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 52, value = "B", upperBound = false)})
    public void variableAssignmentsAreConsidered_1() {
        A b = new B();
        A c = new C();
        if(Math.random() < .5) b = c;
        b.doIt();
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 58, value = "B[]", upperBound = false)})
    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 59, value = "B", upperBound = false)})
    public void arrayLoadsAreConsidered() {
        A[] a = new A[] {new B()};
        a[0].doIt();
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 64, value = "B", upperBound = false)})
    public void typesOfParametersArePassed() {
        A a = new B();
        typesOfParametersArePassed_callee(a);
    }

    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 70, value = "B", upperBound = false)})
    private void typesOfParametersArePassed_callee(A a) {
        a.doIt();
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 76, value = "B", upperBound = false)})
    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 77, value = "B", upperBound = false)})
    public void returnFlowIsConsidered() {
        A a = returnB();
        a.doIt();
    }

    private A returnB() {
        return new B();
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 87, value = "A", upperBound = true)})
    @ExpectedCallee.List({@ExpectedCallee(lineNumber = 88, value = "A", upperBound = true)})
    public void nativeCallsAreConsidered() {
        A a = nativeMethod();
        a.doIt();
    }

    public native A nativeMethod();

    @ExpectedType.List({@ExpectedType(lineNumber = 95, value = "String", upperBound = true)})
    public void staticFieldReadsAreConsidered() {
        Object o = A.STATIC_FIELD;
        System.out.println(o);
    }

    @ExpectedType.List({@ExpectedType(lineNumber = 101, value = "String", upperBound = true)})
    public void fieldReadsAreConsidered() {
        Object o = new B().field;
        System.out.println(o);
    }

    @ExpectedType.List({ @ExpectedType(lineNumber = 107, value = "B", upperBound = false) })
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