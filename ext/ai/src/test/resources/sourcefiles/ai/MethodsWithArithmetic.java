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
package ai;

/**
 * Methods that perform arithmetic operations.
 * 
 * <h2>NOTE</h2> This class is not meant to be (automatically) recompiled; it
 * just serves documentation purposes. The compiled class that is used by the
 * tests is found in the test-classfiles directory.
 * 
 * @author Michael Eichberg
 */
public class MethodsWithArithmetic {

	public static short simpleMathUsingShortValues(short value) {
		short s = (short) Integer.MAX_VALUE;
		short s2 = (short) (value - s);
		return (short) (s2 * 23);
	}

	public static int fak() {
		int MAX = 5;
		int r = 1;
		for (int i = 1; i < MAX; i++) {
			r = r * i;
		}
		return r;
	}

	public static int divIt(int denominator) {
		return 3 / denominator;
	}

	public static int divItSafe(int denominator) {
		if (denominator == 0)
			return 0;
		else
			return 3 / denominator;
	}

	public static short returnShortValue(short value) {
		return value;
	}

}
