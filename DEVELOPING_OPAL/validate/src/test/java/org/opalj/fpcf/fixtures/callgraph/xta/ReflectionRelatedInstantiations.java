package org.opalj.fpcf.fixtures.callgraph.xta;

import org.opalj.fpcf.properties.callgraph.AvailableTypes;

/**
 * The test in this class asserts that constructor calls via reflection are correctly
 * recognized as instantiations.
 *
 * @author Andreas Bauer
 */
public class ReflectionRelatedInstantiations {

    public static void sink(Object obj) {}

    @AvailableTypes({"[Ljava/lang/String;"})
    public static void main(String[] args) {
        // Both methods call A.<init> via reflection. We use two caller methods
        // to assure that the analysis handles multiple reflective callers correctly.
        test();
        test2();
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/ReflectionRelatedInstantiations$A")
    private static void test() {
        A obj = constructorCallProxy();
        sink(obj);
    }

    @AvailableTypes("org/opalj/fpcf/fixtures/callgraph/xta/ReflectionRelatedInstantiations$A")
    private static void test2() {
        A obj = constructorCallProxy2();
        sink(obj);
    }

    // The actual reflective instantiations are contained within the following two methods.
    // This is due to the fact that Class.forName is a method in the ExternalWorld and has
    // a return type of "Class" which is an external type. Since the tests are run without
    // adding the standard library class files, the analysis does not know the relationship
    // between Class and other external types in the ExternalWorld type set. Conservatively,
    // they are back-propagated. As a consequence, the type set of the two proxy methods are
    // polluted with spurious external types, which we do not care about for this test.

    private static A constructorCallProxy() {
        String name = "org.opalj.fpcf.fixtures.callgraph.xta.ReflectionRelatedInstantiations$A";
        A obj = null;
        try {
            obj = (A) Class.forName(name).newInstance();
        } catch (Exception ignored) {}

        return obj;
    }

    private static A constructorCallProxy2() {
        String name = "org.opalj.fpcf.fixtures.callgraph.xta.ReflectionRelatedInstantiations$A";
        A obj = null;
        try {
            obj = (A) Class.forName(name).newInstance();
        } catch (Exception ignored) {}

        return obj;
    }

    // Due to how this test is constructed, this class is never instantiated
    // normally, but only via reflection.
    public static class A {}
}
