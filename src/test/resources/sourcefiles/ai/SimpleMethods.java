package ai;
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

	//
	// RETURNS
	public static void nop() {
		return;
	}

	public static int iOne() {
		return 1;
	}

	public static long lOne() {
		return 1l;
	}

	public static double dOne() {
		return 1.0d;
	}

	public static float fOne() {
		return 1.0f;
	}

	//
	// LDC
	public static String sLDC() {
		return "LDC";
	}

	//
	// PARAMETER
	public static int identity(int i) {
		return i;
	}

	public static String sOne(String s) {
		return s;
	}

	// BINARY OPERATIONS ON INT
	public static int iAdd(int i, int j) {
		return i + j;
	}

	public static int iAnd(int i, int j) {
		return i & j;
	}

	public static int iDiv(int i, int j) {
		return i / j;
	}

	public static int iMul(int i, int j) {
		return i * j;
	}

	public static int iOr(int i, int j) {
		return i | j;
	}

	public static int iShl(int i, int j) {
		return i << j;
	}

	public static int iShr(int i, int j) {
		return i >> j;
	}

	public static int iRem(int i, int j) {
		return i % j;
	}

	public static int iSub(int i, int j) {
		return i - j;
	}

	public static int iushr(int i, int j) {
		return i >>> -2;
	}

	public static int iXor(int i, int j) {
		return i ^ j;
	}

	//
	// BINARY OPERATIONS ON LONG
	public static long lAdd(long i, long j) {
		return i + j;
	}

	public static long lAnd(long i, long j) {
		return i & j;
	}

	public static long lDiv(long i, long j) {
		return i / j;
	}

	public static long lMul(long i, long j) {
		return i * j;
	}

	public static long lOr(long i, long j) {
		return i | j;
	}

	public static long lShl(long i, long j) {
		return i << j;
	}

	public static long lShr(long i, long j) {
		return i >> j;
	}

	public static long lRem(long i, long j) {
		return i % j;
	}

	public static long lSub(long i, long j) {
		return i - j;
	}

	public static long lushr(long i, long j) {
		return i >>> -2;
	}

	public static long lXor(long i, long j) {
		return i ^ j;
	}

	//
	// BINARY OPERATIONS ON DOUBLE
	public static double dAdd(double i, double j) {
		return i + j;
	}

	public static double dSub(double i, double j) {
		return i - j;
	}

	public static double dMul(double i, double j) {
		return i * j;
	}

	public static double dDiv(double i, double j) {
		return i / j;
	}

	public static double dRem(double i, double j) {
		return i % j;
	}

	//
	// BINARY OPERATIONS ON FLOAT
	public static float fAdd(float i, float j) {
		return i + j;
	}

	public static float fSub(float i, float j) {
		return i - j;
	}

	public static float fMul(float i, float j) {
		return i * j;
	}

	public static float fDiv(float i, float j) {
		return i / j;
	}

	public static float fRem(float i, float j) {
		return i % j;
	}

	//
	// INTEGER TYPE CONVERSION
	public static byte iToByte(int i) {
		return (byte) i;
	}

	public static char iToChar(int i) {
		return (char) i;
	}

	public static double iToDouble(int i) {
		return (double) i;
	}

	public static float iToFloat(int i) {
		return (float) i;
	}

	public static long iToLong(int i) {
		return (long) i;
	}

	public static short iToShort(int i) {
		return (short) i;
	}

	//
	// LONG TYPE CONVERSION
	public static double lToDouble(long i) {
		return (double) i;
	}

	public static float lToFloat(long i) {
		return (float) i;
	}

	public static int lToInt(long i) {
		return (int) i;
	}

	//
	// DOUBLE TYPE CONVERSION
	public static float dToFloat(double i) {
		return (float) i;
	}

	public static int dToInt(double i) {
		return (int) i;
	}

	public static long dToLong(double i) {
		return (long) i;
	}

	//
	// FLOAT TYPE CONVERSION
	public static double fToDouble(float i) {
		return (double) i;
	}

	public static int fToInt(float i) {
		return (int) i;
	}

	public static long fToLong(float i) {
		return (long) i;
	}

	//
	// UNARY EXPRESSIONS
	public static float fNeg(float i) {
		return -i;
	}

	public static double dNeg(double i) {
		return -i;
	}

	public static long lNeg(long i) {
		return -i;
	}

	public static int iNeg(int i) {
		return -i;
	}


	//
	// TYPE CHECKS
	public static SimpleMethods asSimpleMethods(Object o) {
		return (SimpleMethods) o; // this is deliberately not (type) safe
	}

	public static boolean asSimpleMethodsInstance(Object o) {
		return o instanceof SimpleMethods ; // this is deliberately not (type) safe
	}

	/*
	 *
	 */
	public static double twice(double i) {
		return 2 * i;
	}

	public static double square(double i) {
		return i * i;
	}

	public static String objectToString(Object o) {
		return o.toString();
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

	public static <T> T asIs(T o) {
		return o; // the type of the return value directly depends on the input
					// value
	}

	public static void multipleCalls() {
		java.util.List<Object> o = asIs(new java.util.ArrayList<Object>(100));
		o.toString();
		o.hashCode();
	}

	public static <T> T create(Class<T> clazz) throws Exception {
		return clazz.newInstance();
	}

}
