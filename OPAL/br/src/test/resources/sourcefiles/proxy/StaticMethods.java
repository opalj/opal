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
package proxy;

/**
 * A simple test class that provides a set of static methods for the proxy test.
 * 
 * @author Arne Lottmann
 */
public class StaticMethods {
	
	/* ********************************************************
	 * 
	 * Methods without a return type.
	 * 
	 ******************************************************** */
	
	public static void noArgumentAndNoReturnValue() {}
	
	public static void intAndNoReturnValue(int i) {}
	
	public static void intLongAndNoReturnValue(int i, long l) {}
	
	public static void floatIntAndNoReturnValue(float f, int i) {}
	
	public static void doubleIntAndNoReturnValue(double d, int i) {}
	
	public static void intDoubleBooleanAndNoReturnValue(int i, double d, boolean b) {}
	
	public static void longIntLongAndNoReturnValue(long l, int i, long k) {}
	
	public static void longObjectAndNoReturnValue(long l, Object o) {}
	
	public static void stringDoubleAndNoReturnValue(String s, double d) {}
	
	public static void stringStringLongAndNoReturnValue(String s, String t, long l) {}
	
	public static void intVarintAndNoReturnValue(int i, int...is) {}
	
	public static void intVardoubleAndNoReturnValue(int i, double...ds) {}
	
	public static void doubleIntStringAndNoReturnValue(double d, int i, String s) {}
	
	public static void intIntIntIntIntAndNoReturnValue(int i, int j, int k, int l, int m) {}
	
	public static void doubleDoubleDoubleDoubleDoubleAndNoReturnValue(double d, double e, double f, double g, double h) {}
	
	public static void intDoubleIntDoubleIntAndNoReturnValue(int i, double d, int j, double e, int k) {}
	
	public static void doubleIntDoubleIntDoubleAndNoReturnValue(double d, int i, double e, int j, double f) {}
	
	public static void methodWithManyParametersAndNoReturnValue(double d, float f, long l, int i, short s, byte b, char c, boolean bo, String str, Object[] array) {}
	
	/* ********************************************************
	 * 
	 * Methods with a return type.
	 * 
	 ******************************************************** */
	
	public static int noArgumentAndIntReturnValue() { return 0; }
	
	public static double intAndDoubleReturnValue(int i) { return 0d; }
	
	public static long intLongAndLongReturnValue(int i, long l) { return 0l;}
	
	public static char floatIntAndCharReturnValue(float f, int i) { return '\0'; }
	
	public static boolean doubleIntAndBooleanReturnValue(double d, int i) { return true; }
	
	public static String intDoubleBooleanAndStringReturnValue(int i, double d, boolean b) { return ""; }
	
	public static Object[] longIntLongAndArrayReturnValue(long l, int i, long k) { return new Object[0]; }
	
	public static float longObjectAndFloatReturnValue(long l, Object o) { return 0f; }
	
	public static short stringDoubleAndShortReturnValue(String s, double d) { return (short) 0; }
	
	public static byte stringStringLongAndByteReturnValue(String s, String t, long l) { return (byte) 0; }
	
	public static int intVarintAndIntReturnValue(int i, int...is) { return 0; }
	
	public static double intVardoubleAndDoubleReturnValue(int i, double...ds) { return 0d; }
	
	public static long doubleIntStringAndLongReturnValue(double d, int i, String s) { return 0l; }
	
	public static char intIntIntIntIntAndCharReturnValue(int i, int j, int k, int l, int m) { return '\0'; }
	
	public static boolean doubleDoubleDoubleDoubleDoubleAndBooleanReturnValue(double d, double e, double f, double g, double h) { return false; }
	
	public static byte intDoubleIntDoubleIntAndStringReturnValue(int i, double d, int j, double e, int k) { return (byte) 0; }
	
	public static float doubleIntDoubleIntDoubleAndFloatReturnValue(double d, int i, double e, int j, double f) { return 0f; }
	
	public static short methodWithManyParametersAndShortReturnValue(double d, float f, long l, int i, short s, byte b, char c, boolean bo, String str, Object[] array) { return (short) 0; }
}