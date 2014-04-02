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
package ai.taint;

public class Aliasing {
	
	static class A {
		String f;
	}
	
	static class B {
		String f;
	}

	static class C extends A {
	}

	//here we assume that "name" may be tainted because we
	//only use a dumb pointer analysis
	//name2 should not be tainted, though, because B is of a different type
	public Class<?> nameInput(String name, String name2, String name3) throws ClassNotFoundException {
		A a1 = new A();
		A a2 = new A();
		B b = new B();
		C c = new C();
		
		c.f = name3;
		b.f = name2;
		a1.f = name;
		String arg = a2.f;
		return Class.forName(arg);
	}
	
	//name is tainted; name2 not
	public Class<?> nameInput2(String name, String name2) throws ClassNotFoundException {
		String[] a1 = new String[] {name};
		String[] a2 = new String[] {name2};
		@SuppressWarnings("unused")
		String s = a2[0];
		return Class.forName(a1[0]);
	}
}
