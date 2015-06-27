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

import java.util.*;

/**
 * Class with simple methods containing cast and typecheck instructions.
 * 
 * @author Roberts Kolosovs
 *
 */
public class CastInstructions {
	
	void typecheckString(String s){
		boolean result = s instanceof Object;
	}
	
	void typecheckList(ArrayList l){
		boolean result = l instanceof List;
	}
	
	void checkcast(Object o){
		List l = (List) o;
	}

	void d2f(double d){
		float result = (float) d;
	}
	
	void d2i(double d){
		int result = (int) d;
	}

	void d2l(double d){
		long result = (long) d;
	}
	
	void f2d(float f){
		double result = (double) f;
	}
	
	void f2i(float f){
		int result = (int) f;
	}
	
	void f2l(float f){
		long result = (long) f;
	}
	
	void l2d(long l){
		double result = (double) l;
	}
	
	void l2f(long l){
		float result = (float) l;
	}
	
	void l2i(long l){
		int result = (int) l;
	}
	
	void i2d(int i){
		double result = (double) i;
	}
	
	void i2f(int i){
		float result = (float) i;
	}
	
	void i2l(int i){
		long result = (long) i;
	}
}
