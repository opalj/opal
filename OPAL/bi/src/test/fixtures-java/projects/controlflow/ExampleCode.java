/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package controlflow;

/**
 * Created to test the computation of control flow graphs.
 * 
 * @author Erich Wittenbeck
 */
@SuppressWarnings("all")
public class ExampleCode {
	
	boolean simpleConditional(int a){
		if(a < 100)
			return true;
		else
			return false;
	}
	
	void forLoop(int a){
		for(int i = 0; i < a; i++){
			int useless = i;
		}
	}
	
	void tryCatch(int a){
		try{
		int b = a + 2;
		int c = a % 2;
		int d = a * 2;
		}catch(ArithmeticException e){
			int f = 1;
		}
		
		return;
	}
	
	void otherTryCatch(int[] array){
		
		try{
			int a = array[2] * 5;
			int b = 6 / a;
		}catch(ArithmeticException ae){
			int c = -1;
		}catch(ArrayIndexOutOfBoundsException aobe){
			int d = -2;
		}
		
	}
	
	void tryCatchFinally(int[] array){
		
		try{
			int a = array[2] * 5;
			int b = 6 / a;
		}catch(ArithmeticException ae){
			int c = -1;
		}finally{
			int d = -2;
		}
		
	}
	
	int example(int a, int b) {
		int index = 0;
		
		try{
			int c = a/b;
			
			do{
				System.out.println("increase");
				index++;
			}while(index <= c);
		}catch(ArithmeticException e){
			System.out.println("Division by Zero");
		}
		
		return index;
	}
	
	void tryCatchConditional(int[] array, boolean trigger){
		
		try{
			if(trigger){
				try{
					int a = array[2];
					int b = 5 / a;
				}catch(ArithmeticException ae){
					int c = 5 + 1;
				}
			}else{
				int d = - 1;
			}
		}catch(ArrayIndexOutOfBoundsException aiobe){
			int e = -2;
		}
	}
	
	void tryCatchDoWhile(int a, int b){
		
		try{
			int i = 0;
			try{
				do{
					int useless = i / b;
					i++;
				}while(i<a);
			}catch(ArithmeticException ae1){
				int c = b / i;
			}
		}catch(ArithmeticException ae2){
			int d = -1;
		}
	}
	
	int tryFinally(int a){
		int b = 5 + a;
		int c = 5 * a;
		try{
			try{
				c = c / a;
			}catch(ArithmeticException e){
				c = a / c;
			}
			b = b / c;
		}finally{
			if(c != 0)
				return c;
			else
				return b;
		}
	}
	
	void provoke(int input) {
        int a = 100;

        try {
            a = a / input;
        } finally {
            int b = 2;

            if (a * (a + b) > 50) {
                int d = 3;
            } else {
                int e = 5;
            }
        }

        return;
    }
	
	void helloWorld(){
		int a = 1;
		int b = a * 2;
		int c = b + 7;
	}
}