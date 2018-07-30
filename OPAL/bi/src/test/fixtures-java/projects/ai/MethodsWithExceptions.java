/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * Methods that throw and catch <code>Exception</code>s.
 *
 * @author Michael Eichberg
 */
public class MethodsWithExceptions {

    @SuppressWarnings("all")
    public static class SuperException extends java.awt.HeadlessException {}

    @SuppressWarnings("all")
    public static class SubException extends SuperException {}

    // 0 new java.lang.RuntimeException [16]
    // 3 dup
    // 4 aload_0 [message]
    // 5 invokespecial java.lang.RuntimeException(java.lang.String) [18]
    // 8 athrow
    public static void alwaysThrows(String message) {
        throw new RuntimeException(message);
    }

    // 0 aload_0 [someThrowable]
    // 1 athrow
    // 2 astore_1 [t]
    // 3 aload_1 [t]
    // 4 invokevirtual java.lang.Throwable.printStackTrace() : void [24]
    // 7 return
    // Exception Table:
    // [pc: 0, pc: 2] -> 2 when : java.lang.Throwable
    public static void alwaysCatch(Throwable someThrowable) {
        try {
            throw someThrowable;
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void justThrow(Throwable someThrowable) throws Throwable {
        throw someThrowable; // abnormal return due to NullPointerException or Throwable!
    }

    // 0 aload_0 [t]
    // 1 ifnull 6
    // 4 aload_0 [t]
    // 5 athrow
    // 6 getstatic java.lang.System.out : java.io.PrintStream [34]
    // 9 ldc <String "Nothing happening"> [40]
    // 11 invokevirtual java.io.PrintStream.println(java.lang.String) : void [42]
    // 14 goto 24
    // 17 astore_1
    // 18 aload_0 [t]
    // 19 invokevirtual java.lang.Throwable.printStackTrace() : void [24]
    // 22 aload_1
    // 23 athrow
    // 24 aload_0 [t]
    // 25 invokevirtual java.lang.Throwable.printStackTrace() : void [24]
    // 28 return
    // Exception Table:
    // [pc: 0, pc: 17] -> 17 when : any
    // NOT NEEDED BY THE EMBEDDED ECLIPSE COMPILER: @SuppressWarnings("null")
    public static void withFinallyAndThrows(Throwable t) throws Throwable {
        try {
            if (t != null)
                throw t; // <= will throw t (non-null!)
            else {
                System.out.println("Nothing happening");
                // May throw some exception. However, it
                // will be replaced by a NullPointerException in the finally
                // clause because t is null; i.e., this potential NullPointerException
                // will never be visible outside of this method.
            }
        } finally {
            t.printStackTrace(); // <= t may be null => may throw NullPointerException
        }
    }

    public static void conditionInFinally(String name) throws SecurityException {
        boolean checked = false;
        try {
            java.io.File f = new java.io.File(name); // will throw an exception if name ==
                                                     // null
            f.canExecute();
            checked = true;
        } finally {
            if (checked)
                System.out.println("everything is fine");
        }
    }

    public static void throwsThisOrThatException(String message) throws IllegalArgumentException {
        if (message == null)
            throw new NullPointerException();
        else
            throw new IllegalArgumentException();
    }

    public static void leverageException(String message) {
        try {
            File f = new File("foo.bar");
            f.createNewFile();
        } /*
           * catch (Exception e) { throw e; }
           */
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } catch (SecurityException se) {
            throw new RuntimeException(se);
        }

    }

    public static void throwsSomeException(String message) throws Exception {
        Exception e = null;
        if (message == null)
            e = new NullPointerException();
        else
            e = new IllegalArgumentException();
        System.out.println(e);
        throw e;
    }

    public static void throwsNoException(String message) throws Exception {
        Exception e = null;
        if (message == null)
            e = new NullPointerException();
        else
            e = new IllegalArgumentException();
        try {
            System.out.println(e);
            throw e;
        } catch (NullPointerException npe) {
            // ...
        } catch (IllegalArgumentException iae) {
            // ...
        }
    }

    public static Object exceptionsAndNull(IOException o) throws Exception {
        try {
            throw o;
        } catch (NullPointerException npe) {
            System.out.println(o /*<=> null*/);
            return npe;
        }
    }

    public static Object exceptionsAndNull(Object o) throws Exception {
        try {
            o.toString();
        } catch (NullPointerException npe) {
            return o; // null or a NullPointerException thrown by toString...
        }
        o.toString();
        return o; // not-null
    }

    public static Object exceptionsWithMultipleReasonsForNull(Object o) throws Exception {
        try {
            o.toString();
        } catch (NullPointerException npe) {
            o.wait();
            return o; // null or a NullPointerException thrown by toString...
        }
        o.toString();
        return o; // not-null
    }

    public static int exceptionsAndNull(Object[] o) throws Exception {
        int r = 0;
        try {
            int l = o.length;
            r = l -1;
        } catch (NullPointerException npe) {
            o.toString(); // o === null
            return 0; // dead
        }
        o.toString(); // not-null
        return r;
    }

    public static int catchGame(Object o) throws Throwable {

        Throwable t = null;
        try {
            throw (Throwable) o;
        } catch (NullPointerException npe) {
            // this handler is reached if either o is null or is a NullPointerException
            t = npe;
        } catch (ClassCastException cce) {
            t = cce;
        } catch (IllegalArgumentException iae) {
            t = iae;
        }

        System.out.println("it was one of the three expected ones: "+t);
        return -1;
    }

    public static int extensiveCatchGame(Object o, int i) throws Throwable {
        Throwable t = null;
        try {
            if (i / o.hashCode() > 0 ) throw new UnknownError("hashCode was 0");
            throw (Throwable) o; // here, o cannot be null
        } catch (NullPointerException npe) {
            t = npe;
        } catch (ClassCastException cce) {
            t = cce;
        } catch (IllegalArgumentException iae) {
            t = iae;
        } catch (ArithmeticException dve) {
            return 0;
        }

        System.out.println("it was one of the three expected ones: "+t);
        return -1;
    }

    public static int catchGameWithSortedCatches(Object o) throws Throwable {
        try {
            throw new java.io.FileNotFoundException(o.toString());
        } catch (NullPointerException npe) {
            return 0;
        } catch (FileNotFoundException cce) {
            return -1;
        } catch (Exception ioe) {
            return -10;
        } catch (Error e) {
            return -10;
        }
    }

    public static int finallyGame(Object g) throws Throwable {
        Object o;
        Object z;
        try {
            Object io = null;
            try {
                throwsThisOrThatException(g.toString());
            } finally {
                try {
                    System.out.println("Did it - 1!");
                } finally {
                    o = new Object();
                    System.err.println("Everything is falling apart!");
                }
                io = new Object();
            }
            System.out.println(io.toString());
        } finally {
            z = "z";
            System.out.println("Did it -2!");
        }
        return z.hashCode() + o.hashCode();
    }

    public static int finallyAndCatchGame(Object g) throws Throwable {
        Object o;
        Object z;
        try {
            System.out.println("before o assinemnt...");
            o = new Object();
        } catch (java.lang.RuntimeException re){
            o = "re";
        } catch (java.lang.Exception e){
            o = "e";
        } finally {
            o = "finally";
            z = "z";
            System.out.println("Did it -2!");
        }
        return z.hashCode() + o.hashCode();
    }

    private static void doIt() {
        return;
    }

    private static void processIt(Object o) {
        return;
    }

    private static void processIt(int t) {
        return;
    }

    // inspired by java.util.concurrent.ForkJoinWorkerThread.run()
    // - standard compiler generate a lot of dead code in this example -
    public static void nestedTryFinally() throws Throwable {
        Throwable exception = null;
        try {
            doIt();
        } catch (Throwable ex) {
            exception = ex;
        } finally {
            try {
                processIt(exception);
            } catch (Throwable ex) {
                if (exception == null)
                    exception = ex;
            } finally {
                processIt(exception);
            }
        }
    }

    public static void exceptionAsControlFlow(int i) throws Throwable {
        try {
            // convoluted control flow...
            processIt(33/i); // if i is 0 throw an exception
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("");
        }

        processIt(i+1);
    }

    public static void onlyNewNoInitDueToException(int i) {
        int j = 1;
        if (i == 0)
            j = 0;
        else {
            j = j-1;
        }
        new java.util.HashMap<Integer,Integer>(i / j);
    }

    public static int differentExceptionsHandledBySameHandler(int i) {
        int j = 0;
        try {
            if(i == 0) {
                j = 10 / i; // <= ArithmethicException
            } else {
                throw new RuntimeException("unexpected value");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return j;
    }


    public static void differentExceptionsRelatedToArraysHandledBySameHandler(int i) {
        int j = 0;
        Object[] as = new String[10];
        Object value = null;
        try {
            if(i == 0) {
                j = 10; // <= will cause IndexOutOfBounds in the following
                value = "10";
            } else {
                j = 0;
                value = new Object(); // <= will cause ArrayStoreException in the following
            }
            as[j] = value;
        } catch (Exception e) {
            System.out.println(e);
        }
    }


}
