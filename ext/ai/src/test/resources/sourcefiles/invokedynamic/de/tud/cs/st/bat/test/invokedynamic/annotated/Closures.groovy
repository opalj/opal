/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package de.tud.cs.st.bat.test.invokedynamic.annotated;

import de.tud.cs.st.bat.test.invokedynamic.annotations.*;

/**
 * Test class for resolving invokedynamic calls to closures.
 * 
 * Main problem with closures is that they are compiled into individual, anonymous classes, so the receiverType depends on the order in which the closures are compiled.
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
