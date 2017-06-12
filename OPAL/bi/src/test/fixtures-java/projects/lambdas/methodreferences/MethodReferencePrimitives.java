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

/**
 * This class contains method reference examples used with primitives.
 *
 * <!--
 * <p>
 * <p>
 * <p>
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE.
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

    public double sumDoubleDouble() {
        FIDoubleDouble bf = MixedDoubleParamters::sum;
        return bf.apply(2d, 2d);
    }

    // Return double, parameter double, float
    @FunctionalInterface public interface FIDoubleDoubleFloat {

        double apply(double a, float b);
    }

    public double sumDoubleFloat() {
        FIDoubleDoubleFloat bf = MixedDoubleParamters::sum;
        return bf.apply(2d, 3.14f);
    }

    // Return double, parameter float, double
    @FunctionalInterface public interface FIDoubleFloatDouble {

        double apply(float a, double b);
    }

    public double sumFloatDouble() {
        FIDoubleFloatDouble bf = MixedDoubleParamters::sum;
        return bf.apply(4.2f, 2d);
    }

    // Return float, parameter float, float
    @FunctionalInterface public interface FIFloatFloatFloat {

        float apply(float a, float b);
    }

    public float sumFloatFloat() {
        FIFloatFloatFloat bf = MixedDoubleParamters::sum;
        return bf.apply(2.7f, 2.5f);
    }

    // Return double, parameter double, float, double
    @FunctionalInterface public interface FIDoubleFloatDoubleFloat {

        double apply(float a, double b, float c);
    }

    public double sumFloatDoubleFloat() {
        FIDoubleFloatDoubleFloat tf = MixedDoubleParamters::sum;
        return tf.apply(3.14f, 42d, 2.5f);
    }

    // Return double, parameter int, double
    @FunctionalInterface public interface FIDoubleIntDouble {

        double apply(int a, double b);
    }

    public double sumDoubleInt() {
        FIDoubleIntDouble bf = MixedDoubleParamters::sum;
        return bf.apply(42, 42d);
    }

    // Return double, parameter long, double
    @FunctionalInterface public interface FIDoubleLongDouble {

        double apply(long a, double b);
    }

    public double sumLongDouble() {
        FIDoubleLongDouble bf = MixedDoubleParamters::sum;
        return bf.apply(42l, 42.3d);
    }

    // Return long, parameter long, long
    @FunctionalInterface public interface FILongLongLong {

        long apply(long a, long b);
    }

    public long sumLongLong() {
        FILongLongLong bf = MixedLongParamters::sum;
        return bf.apply(42l, 42l);
    }

    // Return long, parameter long, int
    @FunctionalInterface public interface FILongLongInt {

        long apply(long a, int b);
    }

    public long sumLongInt() {
        FILongLongInt bf = MixedLongParamters::sum;
        return bf.apply(42l, 24);
    }

    // Return long, parameter int, int
    @FunctionalInterface public interface FILongIntInt {

        long apply(int a, int b);
    }

    public long sumIntInt() {
        FILongIntInt bf = MixedLongParamters::sum;
        return bf.apply(42, 314);
    }

    // Return double, parameter double, long
    @FunctionalInterface public interface FIDoubleDoubleLong {

        double apply(double a, long b);
    }

    public double sumDoubleLong() {
        FIDoubleDoubleLong bf = MixedLongParamters::sum;
        return bf.apply(5.5d, 42l);
    }

    // Return long, parameter int, long, long
    @FunctionalInterface public interface FILongIntLongLong {

        long apply(int a, long b, long c);
    }

    public long sumIntLongLong() {
        FILongIntLongLong tf = MixedLongParamters::sum;
        return tf.apply(3, 4l, 5l);
    }
}
