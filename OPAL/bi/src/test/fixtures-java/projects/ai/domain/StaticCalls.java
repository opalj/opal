/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.domain;

public class StaticCalls {

    static void doNothing() {
        // empty
    }

    static int returnConstantIntValue() {
        return 1;
    }

    static Object returnObject() {
        return new Object();
    }

    static void throwException() {
        throw new UnsupportedOperationException();
    }

    static long returnObjectOrthrowException() {
        long t = System.currentTimeMillis();
        if (t % 100 == 0)
            throw new UnsupportedOperationException();
        else
            return t;
    }

    static double div(int a, int b) {
        if (b == 0)
            return Double.NaN;
        else
            return a / b;
    }

    static int divBy4(int a) {
        return a / 4;
    }

    static int complexMult(int a, int b) {
        return (a + b) * (a / b);
    }

    static long performCalculation() {
        return (long) div(1212, 23423);
    }

    static long doStuff() {
        returnObject();
        div(returnConstantIntValue(), 0);
        return performCalculation();
    }

    static Double alwaysFail() {
        throwException();
        return new Double(Double.NaN);
    }

    static int callDivBy4() {
        return divBy4(100);
    }

    static int aCallChain(int i) {
        return i * callDivBy4();
    }

    static int aLongerCallChain() {
        return aCallChain(returnConstantIntValue() * 7);
    }

    static int callComplexMult() {
        return complexMult(10, returnConstantIntValue());
    }

    static void mayFail() {
        returnObjectOrthrowException();
    }

    static void throwMultipleExceptions(int i) throws java.lang.Throwable {
        switch (i) {
        case 0:
        case 1:
            throw new IllegalArgumentException();
        case 2:
            throw new NullPointerException();
        default:
            System.out.println("Ok");
        }

        Throwable e = null;
        if (System.currentTimeMillis() % 100 == 0)
            e = new UnsupportedOperationException();
        else
            e = new UnknownError();

        throw e;
    }

    static void simpleRecursion(boolean b) {
        if (b)
            simpleRecursion(false);
        else
            System.out.println("done");
    }

    static void endless() {

        endless();
    }

    static void endlessWhile() {
        while (true) {
            // do nothing..
        }
    }

    static void endlessDueToExceptionHandling() {
        try {
            System.out.println(System.currentTimeMillis());
        } catch (Throwable t) {
            // t is NEVER null
            t.printStackTrace(System.out);
        }
        endlessDueToExceptionHandling();
    }

    static int fak(int i) {
        if (i > 1)
            return i * fak(i - 1);
        else
            return 1;
    }

    static boolean mutualRecursionA(boolean b) {
        if (b)
            return mutualRecursionB(!b);
        else
            return mutualRecursionC(b);
    }

    static boolean mutualRecursionB(boolean b) {
        if (!b)
            return mutualRecursionA(b);
        else
            return mutualRecursionC(!b);
    }

    static boolean mutualRecursionC(boolean b) {
        if (!b)
            return mutualRecursionA(b);
        else
            return mutualRecursionB(!b);
    }

    static boolean areEqual(int i, int b) {
        return i == b;
    }

    static boolean areNotEqual(int i, int b) {
        return i != b;
    }

    static boolean callAreEqual() {
        int v1 = (int) (System.currentTimeMillis() % 10000);
        int v2 = (int) (System.currentTimeMillis() % 10000);

        if (areEqual(v1, v1) && !areNotEqual(v2, v2)) {
            if (areNotEqual(v1, v2)) {
                return true;
            } else
                return true;
        } else
            return false; // we failed....
    }

    static Object id(Object o) {
        return o;
    }

    static boolean uselessReferenceTest() {
        Object o = new Object();
        return o == id(o);
    }

    static Object semiIdentity(Object x, boolean b) {
        if(b)
            return x;
        else
            return null;
    }

    static Object advancedUselessReferenceTest(boolean doCheck) {
        Object p = new Object();
        Object o = semiIdentity(p, doCheck);
        if(o != null) {
            return o; // <= should be "p"
        } else {
            return null;
        }
    }
}
