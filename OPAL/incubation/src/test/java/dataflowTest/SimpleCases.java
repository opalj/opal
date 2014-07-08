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

public final class SimpleCases {
	@Sink
	public static void sink(Object target) {
		// Here the evil things may happen. Mark this as a sink.
	}

	/**
	 * Source with constant target passed to sink.
	 */
	@Source
	public static void constantSource() {
		Object target = new Object();

		sink(target);
	}

	/***
	 * Source with parameter plainly passed to sink
	 * 
	 * @param target
	 */
	@Source
	public static void passingSource(Object target) {
		sink(target);
	}

	/***
	 * Source passing a parameter only on a non-trivial condition
	 * 
	 * @param target
	 */
	@Source
	public static void conditionalSource(Object target) {
		if (System.currentTimeMillis() % 1000 == 0)
			sink(target);
	}

	/***
	 * Source passing a parameter in a loop
	 * 
	 * @param target
	 */
	@Source
	public static void loopingSource(Object target) {
		for (int i = 0; i < 10; i++)
			sink(target);
	}

	/***
	 * Source passing a parameter in a loop only on a non-trivial condition
	 * 
	 * @param target
	 */
	@Source
	public static void conditionalLoopingSource(Object target) {
		if (System.currentTimeMillis() % 1000 == 0)
			for (int i = 0; i < 10; i++)
				sink(target);
	}

	/***
	 * Source passing a parameter, but preceded by a trivial condition
	 * 
	 * @param target
	 */
	@Source
	public static void trivialConditionalSource(Object target) {
		if (true)
			sink(target);
	}
	
	/***
	 * Source not passing a parameter, but preceded by a trivial condition
	 * 
	 * @param target
	 */
	@Source
	public static void trivialConditionalCounterexampleSource(Object target) {
		if (false)
			sink(target);
	}

	/***
	 * Source passing a parameter on all cases of a branch
	 * 
	 * @param target
	 */
	@Source
	public static void allConditionsLeak(Object target) {
		if (System.currentTimeMillis() % 1000 == 0) {
			sink(target);
		} else if (System.currentTimeMillis() % 365 == 0) {
			sink(target);
		} else {
			sink(target);
		}
	}

	/***
	 * Source passing a parameter on some cases of a branch
	 * 
	 * @param target
	 */
	@Source
	public static void someConditionsLeak(Object target) {
		if (System.currentTimeMillis() % 1000 == 0) {
			sink(target);
		} else if (System.currentTimeMillis() % 365 == 0) {
			// do nothing
		} else {
			sink(target);
		}
	}
	
	@Source
	public static void leakOnException(Object target) {
		try {
			int i;
			if (target == null) 
				i = 12 / 0;
		} catch (Exception e) {
			sink(target);
		}
		
	}

}
