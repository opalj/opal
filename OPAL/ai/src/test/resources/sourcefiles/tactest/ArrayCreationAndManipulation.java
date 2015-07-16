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
package tactest;

/**
 * Class with simple methods containing array creation and manipulation instructions.
 * 
 * @author Roberts Kolosovs
 *
 */
public class ArrayCreationAndManipulation {

	void refArray(){
		Object[] oa = new Object[5];
		oa[4] = new Object();
		Object o = oa[4];
	}
	
	void multidimArray(){
		int[][] mdia = new int[4][2];
		int lngth = mdia.length;
	}
	
	void doubleArray(){
		double[] da = new double[5];
		da[4] = 1.0d;
		double d = da[4];
	}
	
	void floatArray(){
		float[] fa = new float[5];
		fa[4] = 2.0f;
		float f = fa[4];
	}
	
	void intArray(){
		int[] ia = new int[5];
		ia[4] = 2;
		int i = ia[4];
	}
	
	void longArray(){
		long[] la = new long[5];
		la[4] = 1L;
		long l = la[4];
	}
	
	void shortArray(){
		short[] sa = new short[5];
		sa[4] = 2;
		short s = sa[4];
	}
	
	void byteArray(){
		byte[] ba = new byte[5];
		ba[4] = 2;
		byte b = ba[4];
	}
	
	void charArray(){
		char[] ca = new char[5];
		ca[4] = 2;
		char c = ca[4];
	}
}
