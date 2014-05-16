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

import java.lang.reflect.Array;

/**
 * A very large number of methods that deliberately do not contain any
 * control-flow statements.
 * 
 * <b>This class is not meant to be (automatically) recompiled; it just serves
 * documentation purposes. The compiled class that is used by the tests is found
 * in the test-classfiles directory.</b>
 * 
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
@SuppressWarnings("all")
public class MethodsPlain {

	//
	// RETURN "CONSTANT" VALUE
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

	public static String sLDC() {
		return "LDC";
	}

	public static Class<String> cLDC() {
		return String.class;
	}

	//
	// RETURN GIVEN PARAMETER
	public static int identity(int i) {
		return i;
	}

	public static String sOne(String s) {
		return s;
	}

	//
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
	// INTEGER VALUES TO X CONVERSIONS
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
		return /* (float) */i;
	}

	public static long iToLong(int i) {
		return /* (long) */i;
	}

	public static short iToShort(int i) {
		return (short) i;
	}

	//
	// LONG VALUES TO X CONVERSIONS
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
	// DOUBLE VALUES TO X CONVERSIONS
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
	// FLOAT VALIES TO X CONVERSION
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
	public static MethodsPlain asSimpleMethods(Object o) {
		return (MethodsPlain) o; // this is deliberately not (type) safe
	}

	public static boolean asSimpleMethodsInstance(Object o) {
		return o instanceof MethodsPlain; // this is deliberately not (type)
											// safe
	}

	//
	// IMPLICIT TYPE CONVERSIONS
	public static double twice(double i) {
		return 2 * i;
	}

	//
	// PRIVATE INSTANCE FIELD (START)
	private float value;

	public void setValue(float value) {
		this.value = value;
	}

	public float getValue() {
		return value;
	}

	// PRIVATE INSTANCE FIELD (END)

	//
	// PRIVATE STATIC FIELD (START)
	private static float sValue;

	public void setSValue(float value) {
		MethodsPlain.sValue = value;
	}

	public float getSValue() {
		return sValue;
	}

	// PRIVATE STATIC FIELD (END)

	//
	// LOADS AND STORES
	// ILOAD 1-5 & ISTORE 1-5
	public int localInt() {
		int i = 1;
		int j = i;
		int k = j;
		int l = k;
		int m = l;
		return m;
	}

	// LLOAD 1,3,5 & LSTORE 1,3,5
	public long localLongOdd() {
		long i = 1;
		long j = i;
		long k = j;
		long l = k;
		long m = l;
		return m;
	}

	// LLOAD 2,4 & LSTORE 2,4
	public long localLongEven() {
		int a = 1;
		long i = 1;
		long j = i;
		long k = j;
		long l = k;
		long m = l;
		return m;
	}

	// DLOAD 1,3,5 & DSTORE 1,3,5
	public double localDoubleOdd() {
		double i = 1;
		double j = i;
		double k = j;
		double l = k;
		double m = l;
		return m;
	}

	// DLOAD 2,4 & DSTORE 2,4
	public double localDoubleEven() {
		int a = 1;
		double i = 1;
		double j = i;
		double k = j;
		double l = k;
		double m = l;
		return m;
	}

	// FLOAD 1-5 & FSTORE 1-5
	public float localFloat() {
		float i = 1;
		float j = i;
		float k = j;
		float l = k;
		float m = l;
		return m;
	}

	// ALOAD 1-5 & ASTORE 1-5
	public MethodsPlain localSimpleMethod() {
		MethodsPlain simpleMethods1 = new MethodsPlain();
		MethodsPlain simpleMethods2 = simpleMethods1;
		MethodsPlain simpleMethods3 = simpleMethods2;
		MethodsPlain simpleMethods4 = simpleMethods3;
		MethodsPlain simpleMethods5 = simpleMethods4;
		return simpleMethods5;
	}

	//
	// PUSH CONSTANT VALUE
	public static Object pushNull() {
		return null;
	}

	public static byte pushBipush() {
		return 6;
	}

	public static short pushSipush() {
		return 128;
	}

	public static double pushDoubleConst0() {
		return 0.0;
	}

	public static double pushDoubleConst1() {
		return 1.0;
	}

	public static float pushFloatConst0() {
		return 0.0f;
	}

	public static float pushFloatConst1() {
		return 1.0f;
	}

	public static float pushFloatConst2() {
		return 2.0f;
	}

	public static int pushIntConstn1() {
		return -1;
	}

	public static int pushIntConst0() {
		return 0;
	}

	public static int pushIntConst1() {
		return 1;
	}

	public static int pushIntConst2() {
		return 2;
	}

	public static int pushIntConst3() {
		return 3;
	}

	public static int pushIntConst4() {
		return 4;
	}

	public static int pushIntConst5() {
		return 5;
	}

	public static long pushLongConst0() {
		return 0;
	}

	public static long pushLongConst1() {
		return 1;
	}

	//
	// CREATE ARRAY
	public boolean[] createNewBooleanArray() {
		boolean[] bs = new boolean[1];
		return bs;
	}

	public char[] createNewCharArray() {
		char[] cs = new char[1];
		return cs;
	}

	public float[] createNewFloatArray() {
		float[] fs = new float[1];
		return fs;
	}

	public double[] createNewDoubleArray() {
		double[] ds = new double[1];
		return ds;
	}

	public byte[] createNewByteArray() {
		byte[] bs = new byte[1];
		return bs;
	}

	public short[] createNewShortArray() {
		short[] ss = new short[1];
		return ss;
	}

	public int[] createNewIntArray() {
		int[] is = new int[1];
		return is;
	}

	public long[] createNewLongArray() {
		long[] ls = new long[1];
		return ls;
	}

	public MethodsPlain[] createNewSimpleMethodsArray() {
		MethodsPlain[] ls = new MethodsPlain[1];
		return ls;
	}

	public MethodsPlain[][] createNewMultiSimpleMethodsArray() {
		MethodsPlain[][] ls = new MethodsPlain[1][2];
		return ls;
	}

	//
	// LENGTH OF AN ARRAY
	public static int arrayLength(Array[] array) {
		return array.length;
	}

	//
	// LOAD FROM AND STORE VALUE IN ARRAYS
	//

	public static MethodsPlain objectArray(MethodsPlain[] simpleMethods) {
		simpleMethods[0] = new MethodsPlain();
		return simpleMethods[0];
	}

	public static byte byteArray(byte[] array) {
		array[0] = 0;
		return array[0];
	}

	public static char charArray(char[] array) {
		array[0] = 'c';
		return array[0];
	}

	public static double doubleArray(double[] array) {
		array[0] = 1.0;
		return array[0];
	}

	public static float floatArray(float[] array) {
		array[0] = 1.0f;
		return array[0];
	}

	public static int intArray(int[] array) {
		array[0] = 1;
		return array[0];
	}

	public static long longArray(long[] array) {
		array[0] = 1;
		return array[0];
	}

	public static short shortArray(short[] array) {
		array[0] = 1;
		return array[0];
	}

	//
	// METHOD CALLS
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
