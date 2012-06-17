/* License (BSD Style License):
 * Copyright (c) 2012
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Software Technology Group or Technische
 *   Universität Darmstadt nor the names of its contributors may be used to
 *   endorse or promote products derived from this software without specific
 *   prior written permission.
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
 * A very large number of methods that do not contain any control-flow
 * statements.
 * 
 * NOTE<br />
 * This class is not meant to be (automatically) recompiled; it just serves
 * documentation purposes. The compiled class that is used by the tests is found
 * in the test-classfiles directory.
 * 
 * @author Michael Eichberg
 */
public class SimpleMethods {

	public static void nop() {
		return;
	}

	public static int one() {
		return 1;
	}

	public static int identity(int i) {
		return i;
	}

	public static int add(int i, int j) {
		return i + j;
	}

	public static byte toByte(int i) {
		return (byte) i;
	}

	public static short toShort(int i) {
		return (short) i;
	}

	public static double twice(double i) {
		return 2 * i;
	}

	public static double square(double i) {
		return i * i;
	}

	public static String objectToString(Object o) {
		return o.toString();
	}

	public static SimpleMethods asSimpleMethods(Object o) {
		return (SimpleMethods) o; // this is deliberately not (type) safe
	}

	// SEGMENT (START)
	private float value;

	public void setValue(float value) {
		this.value = value;
	}

	public float getValue() {
		return value;
	}

	// SEGMENT (END)

	public static <T> T create(Class<T> clazz) throws Exception {
		return clazz.newInstance();
	}

}
