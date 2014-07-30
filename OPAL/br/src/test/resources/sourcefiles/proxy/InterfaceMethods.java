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
 * A simple test interface that provides a set of methods for the proxy test.
 * 
 * @author Arne Lottmann
 */
public interface InterfaceMethods {
	
	/* ********************************************************
	 * 
	 * Methods without a return type.
	 * 
	 ******************************************************** */
	
	public void noArgumentAndNoReturnValue();
	
	public void intAndNoReturnValue(int i);
	
	public void intLongAndNoReturnValue(int i, long l);
	
	public void floatIntAndNoReturnValue(float f, int i);
	
	public void doubleIntAndNoReturnValue(double d, int i);
	
	public void intDoubleBooleanAndNoReturnValue(int i, double d, boolean b);
	
	public void longIntLongAndNoReturnValue(long l, int i, long k);
	
	public void longObjectAndNoReturnValue(long l, Object o);
	
	public void stringDoubleAndNoReturnValue(String s, double d);
	
	public void stringStringLongAndNoReturnValue(String s, String t, long l);
	
	public void intVarintAndNoReturnValue(int i, int...is);
	
	public void intVardoubleAndNoReturnValue(int i, double...ds);
	
	public void doubleIntStringAndNoReturnValue(double d, int i, String s);
	
	public void intIntIntIntIntAndNoReturnValue(int i, int j, int k, int l, int m);
	
	public void doubleDoubleDoubleDoubleDoubleAndNoReturnValue(double d, double e, double f, double g, double h);
	
	public void intDoubleIntDoubleIntAndNoReturnValue(int i, double d, int j, double e, int k);
	
	public void doubleIntDoubleIntDoubleAndNoReturnValue(double d, int i, double e, int j, double f);
	
	public void methodWithManyParametersAndNoReturnValue(double d, float f, long l, int i, short s, byte b, char c, boolean bo, String str, Object[] array);
	
	/* ********************************************************
	 * 
	 * Methods with a return type.
	 * 
	 ******************************************************** */
	
	public int noArgumentAndIntReturnValue();
	
	public double intAndDoubleReturnValue(int i);
	
	public long intLongAndLongReturnValue(int i, long l);
	
	public char floatIntAndCharReturnValue(float f, int i);
	
	public boolean doubleIntAndBooleanReturnValue(double d, int i);
	
	public String intDoubleBooleanAndStringReturnValue(int i, double d, boolean b);
	
	public Object[] longIntLongAndArrayReturnValue(long l, int i, long k);
	
	public float longObjectAndFloatReturnValue(long l, Object o);
	
	public short stringDoubleAndShortReturnValue(String s, double d);
	
	public byte stringStringLongAndByteReturnValue(String s, String t, long l);
	
	public int intVarintAndIntReturnValue(int i, int...is);
	
	public double intVardoubleAndDoubleReturnValue(int i, double...ds);
	
	public long doubleIntStringAndLongReturnValue(double d, int i, String s);
	
	public char intIntIntIntIntAndCharReturnValue(int i, int j, int k, int l, int m);
	
	public boolean doubleDoubleDoubleDoubleDoubleAndBooleanReturnValue(double d, double e, double f, double g, double h);
	
	public byte intDoubleIntDoubleIntAndStringReturnValue(int i, double d, int j, double e, int k);
	
	public float doubleIntDoubleIntDoubleAndFloatReturnValue(double d, int i, double e, int j, double f);
	
	public short methodWithManyParametersAndShortReturnValue(double d, float f, long l, int i, short s, byte b, char c, boolean bo, String str, Object[] array);
}