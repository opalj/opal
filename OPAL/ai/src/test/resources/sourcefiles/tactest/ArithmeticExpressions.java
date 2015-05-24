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
 * Class with simple methods containing arithmetic expressions.
 * 
 * @author Roberts Kolosovs
 *
 */
public class ArithmeticExpressions {

	//Integer operations
	int integerAdd(int a, int b){
		return a + b;
	}
	
	int integerAnd(int a, int b){
		return a & b;
	}
	
	int integerDiv(int a, int b){
		return a / b;
	}
	
	int integerInc(int a){
		return a++;
	}
	
	int integerNeg(int a){
		return -a;
	}
	
	int integerMul(int a, int b){
		return a * b;
	}
	
	int integerOr(int a, int b){
		return a | b;
	}
	
	int integerRem(int a, int b){
		return a % b;
	}
	
	int integerShR(int a, int b){
		return a >> b;
	}
	
	int integerShL(int a, int b){
		return a << b;
	}
	
	int integerSub(int a, int b){
		return a - b;
	}
	
	int integerASh(int a, int b){
		return a >>> b;
	}
	
	int integerXOr(int a, int b){
		return a ^ b;
	}
	
	int integerTest(int a, int b){
		int c = 0;
		c = a + b;
		c = a & b;
		c = a / b;
		a++;
		c = -b;
		c = a * b;
		c = a | b;
		c = a % b;
		c = a >> b;
		c = a << b;
		c = a - b;
		c = a >>> b;
		c = a ^ b;
		return c;
	}
	
	//Double operations
	double doubleAdd(double a, double b){
		return a + b;
	}
	
	double doubleDiv(double a, double b){
		return a / b;
	}
	
	boolean doubleCmp(double a, double b){
		return a < b;
	}
	
	double doubleNeg(double a){
		return -a;
	}
	
	double doubleMul(double a, double b){
		return a * b;
	}
	
	double doubleRem(double a, double b){
		return a % b;
	}
	
	double doubleSub(double a, double b){
		return a - b;
	}
	
	boolean doubleTest(double a, double b){
		return a < b;
	}
	
	//Float operations
	float floatAdd(float a, float b){
		return a + b;
	}
	
	float floatDiv(float a, float b){
		return a / b;
	}
	
	boolean floatCmp(float a, float b){
		return a < b;
	}
	
	float floatNeg(float a){
		return -a;
	}
	
	float floatMul(float a, float b){
		return a * b;
	}
	
	float floatRem(float a, float b){
		return a % b;
	}
	
	float floatSub(float a, float b){
		return a - b;
	}
	
	//Long operations
	long longAdd(long a, long b){
		return a + b;
	}
	
	long longAnd(long a, long b){
		return a & b;
	}
	
	long longDiv(long a, long b){
		return a / b;
	}
	
	long longNeg(long a){
		return -a;
	}
	
	long longMul(long a, long b){
		return a * b;
	}
	
	long longOr(long a, long b){
		return a | b;
	}
	
	long longRem(long a, long b){
		return a % b;
	}
	
	long longShR(long a, long b){
		return a >> b;
	}
	
	long longShL(long a, long b){
		return a << b;
	}
	
	long longSub(long a, long b){
		return a - b;
	}
	
	long longASh(long a, long b){
		return a >>> b;
	}
	
	long longXOr(long a, long b){
		return a ^ b;
	}
}
