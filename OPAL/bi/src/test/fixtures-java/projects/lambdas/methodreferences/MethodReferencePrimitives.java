/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas.methodreferences;

import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.DYNAMIC;

/**
 * This class contains method reference to methods with primitive type parameters.
 *
 * <!--
 * <p>
 * <p>
 * <p>
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * <p>
 * <p>
 * <p>
 * -->
 *
 * @author Andreas Muttscheller
 */
public class MethodReferencePrimitives {

    public static class MixedDoubleParamters {

        public static double sum(double a, double b) {
            return a + b;
        }

        public static double sum(double a, float b) {
            return a + b;
        }

        public static double sum(float a, double b) {
            return a + b;
        }

        public static float sum(float a, float b) {
            return a + b;
        }

        public static double sum(float a, double b, float c) {
            return a + b + c;
        }

        public static double sum(int a, double b) {
            return a + b;
        }

        public static double sum(long a, double b) {
            return a + b;
        }
    }

    public static class MixedLongParamters {

        public static long sum(long a, long b) {
            return a + b;
        }

        public static long sum(long a, int b) {
            return a + b;
        }

        public static long sum(int a, int b) {
            return a + b;
        }

        public static double sum(double a, long b) {
            return a + b;
        }

        public static long sum(int a, long b, long c) {
            return a + b + c;
        }
    }

    // Return double, parameter double, double
    @FunctionalInterface public interface FIDoubleDouble {
        double apply(double a, double b);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedDoubleParamters", name = "sum", parameterTypes = { double.class, double.class }, line = 112)
    public double sumDoubleDouble() {
        FIDoubleDouble bf = MixedDoubleParamters::sum;
        return bf.apply(2d, 2d);
    }

    // Return double, parameter double, float
    @FunctionalInterface public interface FIDoubleDoubleFloat {
        double apply(double a, float b);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedDoubleParamters", name = "sum", parameterTypes = { double.class, float.class }, line = 123)
    public double sumDoubleFloat() {
        FIDoubleDoubleFloat bf = MixedDoubleParamters::sum;
        return bf.apply(2d, 3.14f);
    }

    // Return double, parameter float, double
    @FunctionalInterface public interface FIDoubleFloatDouble {
        double apply(float a, double b);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedDoubleParamters", name = "sum", parameterTypes = { float.class, double.class }, line = 134)
    public double sumFloatDouble() {
        FIDoubleFloatDouble bf = MixedDoubleParamters::sum;
        return bf.apply(4.2f, 2d);
    }

    // Return float, parameter float, float
    @FunctionalInterface public interface FIFloatFloatFloat {
        float apply(float a, float b);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedDoubleParamters", name = "sum", parameterTypes = { float.class, float.class }, line = 145)
    public float sumFloatFloat() {
        FIFloatFloatFloat bf = MixedDoubleParamters::sum;
        return bf.apply(2.7f, 2.5f);
    }

    // Return double, parameter double, float, double
    @FunctionalInterface public interface FIDoubleFloatDoubleFloat {
        double apply(float a, double b, float c);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedDoubleParamters", name = "sum", parameterTypes = { float.class, double.class, float.class }, line = 156)
    public double sumFloatDoubleFloat() {
        FIDoubleFloatDoubleFloat tf = MixedDoubleParamters::sum;
        return tf.apply(3.14f, 42d, 2.5f);
    }

    // Return double, parameter int, double
    @FunctionalInterface public interface FIDoubleIntDouble {
        double apply(int a, double b);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedDoubleParamters", name = "sum", parameterTypes = { int.class, double.class }, line = 167)
    public double sumDoubleInt() {
        FIDoubleIntDouble bf = MixedDoubleParamters::sum;
        return bf.apply(42, 42d);
    }

    // Return double, parameter long, double
    @FunctionalInterface public interface FIDoubleLongDouble {
        double apply(long a, double b);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedDoubleParamters", name = "sum", parameterTypes = { long.class, double.class }, line = 178)
    public double sumLongDouble() {
        FIDoubleLongDouble bf = MixedDoubleParamters::sum;
        return bf.apply(42l, 42.3d);
    }

    // Return long, parameter long, long
    @FunctionalInterface public interface FILongLongLong {
        long apply(long a, long b);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedLongParamters", name = "sum", parameterTypes = { long.class, long.class }, line = 189)
    public long sumLongLong() {
        FILongLongLong bf = MixedLongParamters::sum;
        return bf.apply(42l, 42l);
    }

    // Return long, parameter long, int
    @FunctionalInterface public interface FILongLongInt {
        long apply(long a, int b);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedLongParamters", name = "sum", parameterTypes = { long.class, int.class }, line = 200)
    public long sumLongInt() {
        FILongLongInt bf = MixedLongParamters::sum;
        return bf.apply(42l, 24);
    }

    // Return long, parameter int, int
    @FunctionalInterface public interface FILongIntInt {
        long apply(int a, int b);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedLongParamters", name = "sum", parameterTypes = { int.class, int.class }, line = 211)
    public long sumIntInt() {
        FILongIntInt bf = MixedLongParamters::sum;
        return bf.apply(42, 314);
    }

    // Return double, parameter double, long
    @FunctionalInterface public interface FIDoubleDoubleLong {
        double apply(double a, long b);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedLongParamters", name = "sum", parameterTypes = { double.class, long.class }, line = 222)
    public double sumDoubleLong() {
        FIDoubleDoubleLong bf = MixedLongParamters::sum;
        return bf.apply(5.5d, 42l);
    }

    // Return long, parameter int, long, long
    @FunctionalInterface public interface FILongIntLongLong {
        long apply(int a, long b, long c);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferencePrimitives$MixedLongParamters", name = "sum", parameterTypes = { int.class, long.class, long.class }, line = 233)
    public long sumIntLongLong() {
        FILongIntLongLong tf = MixedLongParamters::sum;
        return tf.apply(3, 4l, 5l);
    }
}
