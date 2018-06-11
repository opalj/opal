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
package lambdas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.*;
import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.*;

/**
 * A few simple closures to test resolution of Java8 generated invokedynamic instructions.
 * <!--
 *
 *
 *
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 *
 *
 * -->
 * @author Arne Lottmann
 */
public class Lambdas {

	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/Lambdas", name = "lambda$0", isStatic = true, line = 54)
	public void plainLambda() {
		Runnable plainLambda = () -> System.out.println("Hello world!");
		plainLambda.run();
	}

	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/Lambdas", name = "lambda$1", parameterTypes = { int.class }, isStatic = true, line = 61)
	public void localClosure() {
		int x = 0;
		Runnable localClosure = () -> System.out.println(x);
		localClosure.run();
	}

	private int x;

	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/Lambdas", name = "$forward$lambda$2", line = 69)
	public void instanceClosure() {
		Runnable instanceClosure = () -> System.out.println(x);
		instanceClosure.run();
	}

	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/Lambdas", name = "$forward$lambda$3", line = 76)
	public void localAndInstanceClosure() {
		int y = 0;
		Runnable localAndInstanceClosure = () -> System.out.println(x + y);
		localAndInstanceClosure.run();
	}

	public void noParametersVoidResult() {
	    Runnable l = () -> {};
	    l.run();
    }

    public Integer noParametersExpressionBody1() throws Exception {
	    java.util.concurrent.Callable<Integer> l = () -> 42;
	    return l.call();
    }

    public Object noParametersExpressionBody2() throws Exception {
        java.util.concurrent.Callable<Object> l = () -> null;
        return l.call();
    }

    public Integer noParametersBlockWithReturn() throws Exception {
        java.util.concurrent.Callable<Integer> l = () -> { return 42; };
        return l.call();
    }

    public void noParametersVoidBlockBody() {
        Runnable l = () -> { System.gc(); };
        l.run();
    }

    public Integer complexBodyWithReturns() throws Exception {
	    boolean b = true;
        java.util.concurrent.Callable<Integer> l = () -> {
            if (b)
                return 42;
            else
                return -1;
        };
        return l.call();
    }

    public Integer singleParameterExpressionBody() {
        java.util.function.Function<Integer, Integer> l = (Integer x) -> x+1;
        return l.apply(3);
    }

    public Integer singleParameterBlockBody() {
        java.util.function.Function<Integer, Integer> l = (Integer x) -> { return x+1; };
        return l.apply(3);
    }

    public Integer singleParameterExpressionBodyInferredTypeParameter() {
        java.util.function.Function<Integer, Integer> l = (x) -> x+1;
        return l.apply(3);
    }

    public Integer singleParameterBlockBodyInferredTypeParameter() {
        java.util.function.Function<Integer, Integer> l = (x) -> { return x+1; };
        return l.apply(3);
    }

    public Integer singleParameterExpressionBodyInferredTypeParameterWithoutParentheses() {
        java.util.function.Function<Integer, Integer> l = x -> x+1;
        return l.apply(3);
    }

    public Integer multipleParametersDeclared() {
        java.util.function.BiFunction<Integer, Integer, Integer> l
                = (Integer x, Integer y) -> x+y;
        return l.apply(3, 2);
    }

    public Integer multipleParametersInferred() {
        java.util.function.BiFunction<Integer, Integer, Integer> l
                = (x, y) -> x+y;
        return l.apply(3, 2);
    }

    public Double singleDoubleParamter() {
        java.util.function.Function<Double, Double> l
                = (x) -> x+5d;
        return l.apply(3d);
    }

    public Double doubleDoubleParamter() {
        java.util.function.BiFunction<Double, Double, Double> l
                = (x, y) -> x+y;
        return l.apply(3d, 4d);
    }

    public Double doubleIntParamter() {
        BiFunction<Double, Integer, Double> l = (x, y) -> x+y;
        return l.apply(3d, 3);
    }

    public Double intDoubleParamter() {
        BiFunction<Integer, Double, Double> l = (x, y) -> x+y;
        return l.apply(3, 3d);
    }

    public Double doubleFloatParamter() {
        BiFunction<Double, Float, Double> l = (x, y) -> x+y;
        return l.apply(3d, 3.14f);
    }

    public double doubleFloatParamter2() {
        DoubleFloatInterface l = (x, y) -> x+y;
        return l.apply(3d, 3.14f);
    }

    public Long singleLongParamter() {
        Function<Long, Long> l = (x) -> x+5l;
        return l.apply(3l);
    }

    public Long longLongParamter() {
        BiFunction<Long, Long, Long> l = (x, y) -> x+y;
        return l.apply(3l, 4l);
    }

    public Long longIntParamter() {
        BiFunction<Long, Integer, Long> l = (x, y) -> x+y;
        return l.apply(3l, 3);
    }

    public Long intLongParamter() {
        BiFunction<Integer, Long, Long> l = (x, y) -> x+y;
        return l.apply(3, 3l);
    }

    public Float longFloatParamter() {
        BiFunction<Long, Float, Float> l = (x, y) -> x+y;
        return l.apply(3l, 3.14f);
    }

    public Double longDoubleParamter() {
        BiFunction<Long, Double, Double> l = (x, y) -> x+y;
        return l.apply(3l, 3.14d);
    }

    public ArrayList<Object> someBiConsumer() {
	    final ArrayList<Object> al = new ArrayList<>();
	    BiConsumer<HashMap<String, String>, HashMap<String, String>> bi = (x, y) -> {
	        al.addAll(x.keySet());
	        al.addAll(y.values());
        };
        al.add(bi);
        return al;
    }

    // TODO: Functional Interfaces for long / double parameters

    @FunctionalInterface
    interface DoubleFloatInterface {
	    double apply(double d, float f);
    }

    // TODO Add examples related to Java 9 and Strings.
}
