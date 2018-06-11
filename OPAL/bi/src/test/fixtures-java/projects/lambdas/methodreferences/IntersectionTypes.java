/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package lambdas.methodreferences;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.function.Function;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This class contains an example of a method reference dealing with intersection types.
 *
 * @see https://www.javaspecialists.eu/archive/Issue233.html
 *
 * <!--
 * <p>
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * <p>
 * -->
 *
 * @author Andreas Muttscheller
 */
public class IntersectionTypes {

    public interface MyMarkerInterface1 {}
    public interface MyMarkerInterface2 {}

    public Runnable createMarkerInterface() {
        return (Runnable & Serializable & MyMarkerInterface1 & MyMarkerInterface2)
                () -> System.out.println("Hello World");
    }

    public static void main(String[] args) {
        float y = 3.14f;
        String s = "foo";
        Function<Integer, String> lambda =
                (Function<Integer, String> & Serializable) (Integer x) -> "Hello World " + x + y + s;

        try {
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda sl = (SerializedLambda) writeReplace.invoke(lambda);

            System.out.println(sl.toString());
            for (int i=0;i<sl.getCapturedArgCount();i++) {
                System.out.println(sl.getCapturedArg(i) + " type: " + sl.getCapturedArg(i).getClass().getSimpleName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processIt(Object o) { /*empty*/}

    public static void t() {
        float f = 3.14f;
        Object[] o = {f};
        processIt(o);
    }

    public static String lambdaWithObjectCaptures() {
        Float y = 3.14f;
        String s = "foo";
        Function<Integer, String> lambda =
                (Function<Integer, String> & Serializable) (Integer x) -> "Hello World " + x + y + s;

        return doSerialization(lambda);
    }

    public static String lambdaWithObjectAndPrimitiveCaptures() {
        float y = 3.14f;
        String s = "foo";
        Function<Integer, String> lambda =
                (Function<Integer, String> & Serializable) (Integer x) -> "Hello World " + x + y + s;

        return doSerialization(lambda);
    }

    public static String lambdaWithObjectArray() {
        Float[] f = {3.14f, 42f};
        Function<Integer, String> lambda =
                (Function<Integer, String> & Serializable) (Integer x) -> "Hello World " + f[0] + f[1];

        return doSerialization(lambda);
    }

    public static String lambdaWithPrimitiveArray() {
        float[] f = {3.14f, 42f};
        Function<Integer, String> lambda =
                (Function<Integer, String> & Serializable) (Integer x) -> "Hello World " + f[0] + f[1];

        return doSerialization(lambda);
    }

    public static String lambdaWithPrimitiveArrayAndObject() {
        float[] f = {3.14f, 42f};
        String s = "foo";
        Function<Integer, String> lambda =
                (Function<Integer, String> & Serializable) (Integer x) -> "Hello World " + f[0] + f[1] + s;

        return doSerialization(lambda);
    }

    @SuppressWarnings("unchecked")
    private static <L> String doSerialization(L lambda) {
        try {
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda sl = (SerializedLambda) writeReplace.invoke(lambda);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try(ObjectOutputStream oos = new ObjectOutputStream(os)) { oos.writeObject(sl);}
            Function<Integer, String> dl;

            try(ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
                ObjectInputStream ois = new ObjectInputStream(is)) {
                dl=(Function<Integer, String>) ois.readObject();
            }

            return dl.apply(2);
        } catch (Exception e) {
            e.printStackTrace();
            return "failure";
        }
    }
}
