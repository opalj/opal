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
package jdkBugsTest;

/**
 * This is a test for the JDKBugs Class.forName() analysis. It has a call to
 * Class.forName(), creates a new Instance of that class and returns it.
 * 
 * @author Lars Schulte
 */
public class NewInstanceTest {

	static void doSomething() {

	}

	public static Object method1(String s) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		Object c = method2(s);
		return c;
	}

	static Object method2(String s) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		doSomething();
		return method3(s);
	}

	static Object method3(String s) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		Class c = Class.forName(s);
		Object d = method4(c);
		return d;
	}

	static Object method4(Class c) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		doSomething();
		doSomething();
		return method5(c);
	}

	static public Object MethodDex(String s) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		Object temp = method3(s);
		return null;
	}

	static Object method5(Class c) {
		try {
			return c.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
