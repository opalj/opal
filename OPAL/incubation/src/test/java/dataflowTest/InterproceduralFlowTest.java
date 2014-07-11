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
package dataflowTest;

public final class InterproceduralFlowTest {

	@Sink
	public static void sink(Object target) {
		// All the evil you can think of
	}

	/***
	 * Simple identity
	 * 
	 * @param target
	 * @return
	 */
	private static Object identity(Object target) {
		return target;
	}

	/***
	 * Simple null constant on any parameter
	 * 
	 * @param target
	 * @return
	 */
	private static Object makeNull(Object target) {
		return null;
	}

	/***
	 * Source taking a step through an identity function before leaking
	 * 
	 * @param target
	 */
	@Source
	public static void identityStep(Object target) {
		sink(identity(target));
	}

	@Source
	public static void noFlow(Object target) {
		sink(makeNull(target));
	}

	@Source
	public static void simpleRecursion(Object target, int n) {
		if (n > 0)
			simpleRecursion(target, n - 1);
		else
			sink(target);
	}

	@Source
	public static void crossRecursion(Object target, int n) {
		crossRecursionInt1(target, n);
	}

	private static void crossRecursionInt1(Object target, int n) {
		if (n > 0)
			crossRecursionInt2(target, n - 1);
		else
			sink(target);
	}

	private static void crossRecursionInt2(Object target, int n) {
		if (n > 0)
			crossRecursionInt1(target, n - 1);
		else
			sink(target);
	}

	
	@Source
	public static void theOnlyfooCaller(Object target) {
		foo(null,target);
	}
	
	private static void foo(Object irr, Object target) {
		if (irr != null)
			sink(target);
	}
}
