/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ai;

import java.io.File;
import java.io.IOException;

/**
 * Just a very large number of methods that does something related to exception handling.
 * 
 * <h2>NOTE</h2> This class is not meant to be (automatically) recompiled; it just serves
 * documentation purposes. The compiled class that is used by the tests is found in the
 * test-classfiles directory.
 * 
 * @author Michael Eichberg
 */
public class MethodsWithExceptions {

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
    public static void withFinallyAndThrows(Throwable t) throws Throwable {
        try {
            if (t != null)
                throw t; // <= will throw t (non-null!)
            else {
                System.out.println("Nothing happening");
                // <= may throw NullPointerException which
                // will be replaced by a
                // NullPointerException in the finally
                // clause because t is null
            }
        } finally {
            t.printStackTrace(); // <= t may be null => may throw NullPointerException
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
}
