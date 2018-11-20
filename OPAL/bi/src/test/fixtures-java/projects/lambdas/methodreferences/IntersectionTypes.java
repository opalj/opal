/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
