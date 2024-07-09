/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package groovy;

import org.opalj.ai.test.invokedynamic.annotations.*;

/**
 * Test class for resolving invokedynamic calls to closures.
 * 
 * Main problem with closures is that they are compiled into individual, anonymous classes, so the receiverType depends on the order in which the closures are compiled.
 * 
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * -->
 * 
 * @author Arne Lottmann
 */
public class Closures {
    public def zeroParametersClosure = {  -> ; }
    public def primitiveParameterClosure = { int primitive -> ; }
    public def objectParameterClosure = { Object object -> ; }
    public def mixedObjectPrimitiveParametersClosure = { int primitive, Object object -> ; }
    public def boxedPrimitiveMixedParametersClosure = { Integer boxed, Object object, int primitive -> ; }
    public def primitiveVarargsParameterClosure = { int...primitives -> ; }
    public def objectVargarsParameterClosure = { Object...objects -> ; }
    public def singlePrimitiveAndObjectVarargsParametersClosure = { int primitive, Object...objects -> ; }
    public def singleObjectAndPrimitiveVarargsParametersClosure = { Object object, int...primitives -> ; }
    public def singlePrimitiveAndPrimitiveVarargsParametersClosure = { int primitive, int...primitives -> ; }
    public def singleObjectAndObjectVarargsParametersClosure = { Object object, Object...objects -> ; }
    public def primitiveReturnTypeClosure = { return 0; }
    public def objectReturnTypeClosure = { return new Object(); }
    public def arrayReturnTypeClosure = { return [ 0 ] as int[]; }

    public void runClosures() {
        Integer i = Integer.valueOf(0);
        Object o = new Object();

        this.zeroParametersClosure();
        this.primitiveParameterClosure(0);
        this.objectParameterClosure(o);
        this.mixedObjectPrimitiveParametersClosure(0, o);
        this.boxedPrimitiveMixedParametersClosure(i, o, 0);
        this.primitiveVarargsParameterClosure(0, 1);
        this.objectVarargsParameterClosure(o, o);
        this.singlePrimitiveAndObjectVarargsParametersClosure(0, o, o);
        this.singleObjectAndPrimitiveVarargsParametersClosure(o, 0, 1);
        this.singlePrimitiveAndPrimitiveVarargsParametersClosure(0, 1, 2);
        this.singleObjectAndObjectVarargsParametersClosure(o, o, o);
        int r1 = this.primitiveReturnTypeClosure();
        Object r2 = this.objectReturnTypeClosure();
        int[] r3 = this.arrayReturnTypeClosure();
    }

    public static void runZeroParametersClosure(def c) { c(); }
    public static void runPrimitiveParameterClosure(def c, int primitive) {c(primitive);}
    public static void runObjectParameterClosure(def c, Object object) { c(object); }
    public static void runMixedObjectPrimitiveParametersClosure(def c, int primitive, Object object) { c(primitive, object); }
    public static void runBoxedPrimitiveMixedParametersClosure(def c, Integer boxed, Object object, int primitive) { c(boxed, object, primitive); }
    public static void runPrimitiveVarargsParameterClosure(def c, int...primitives) { c(primitives); }
    public static void runObjectVargarsParameterClosure(def c, Object...objects) { c(objects); }
    public static void runSinglePrimitiveAndObjectVarargsParametersClosure(def c, int primitive, Object...objects) { c(primitive, objects); }
    public static void runSingleObjectAndPrimitiveVarargsParametersClosure(def c, Object object, int...primitives) { c(object, primitives); }
    public static void runSinglePrimitiveAndPrimitiveVarargsParametersClosure(def c, int primitive, int...primitives) { c(primitive, primitives); }
    public static void runSingleObjectAndObjectVarargsParametersClosure(def c, Object object, Object...objects) { c(object, objects); }
    public static int runPrimitiveReturnTypeClosure(def c) { return c(); }
    public static Object runObjectReturnTypeClosure(def c) { return c(); }
    public static int[] runArrayReturnTypeClosure(def c) { return c(); }

    public static void runClosuresFromParameters() {
        Closures c = new Closures();

        Integer i = Integer.valueOf(0);
        Object o = new Object();

        runZeroParametersClosure(c.zeroParametersClosure);
        runPrimitiveParameterClosure(c.primitiveParameterClosure, 0);
        runObjectParameterClosure(c.objectParameterClosure, o);
        runMixedObjectPrimitiveParametersClosure(c.mixedObjectPrimitiveParametersClosure, 0, o);
        runBoxedPrimitiveMixedParametersClosure(c.boxedPrimitiveMixedParametersClosure, i, o, 0);
        runPrimitiveVarargsParameterClosure(c.primitiveVarargsParameterClosure, 0, 1);
        runObjectVarargsParameterClosure(c.objectVarargsParameterClosure, o, o);
        runSinglePrimitiveAndObjectVarargsParametersClosure(c.singlePrimitiveAndObjectVarargsParametersClosure, 0, o, o);
        runSingleObjectAndPrimitiveVarargsParametersClosure(c.singleObjectAndPrimitiveVarargsParametersClosure, o, 0, 1);
        runSinglePrimitiveAndPrimitiveVarargsParametersClosure(c.singlePrimitiveAndPrimitiveVarargsParametersClosure, 0, 1, 2);
        runSingleObjectAndObjectVarargsParametersClosure(c.singleObjectAndObjectVarargsParametersClosure, o, o, o);
        int r1 = runPrimitiveReturnTypeClosure(c.primitiveReturnTypeClosure);
        Object r2 = runObjectReturnTypeClosure(c.objectReturnTypeClosure);
        int[] r3 = runArrayReturnTypeClosure(c.arrayReturnTypeClosure);
    }
}
