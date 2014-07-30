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
 * A simple test class that provides a set of constructors for the proxy test.
 * 
 * @author Arne Lottmann
 */
public class Constructors {
	
	/* ********************************************************
	 * 
	 * Methods without a return type.
	 * 
	 ******************************************************** */
	
	public Constructors() {}
	
	public Constructors(int i) {}
	
	public Constructors(int i, long l) {}
	
	public Constructors(float f, int i) {}
	
	public Constructors(double d, int i) {}
	
	public Constructors(int i, double d, boolean b) {}
	
	public Constructors(long l, int i, long k) {}
	
	public Constructors(long l, Object o) {}
	
	public Constructors(String s, double d) {}
	
	public Constructors(String s, String t, long l) {}
	
	public Constructors(int i, int...is) {}
	
	public Constructors(int i, double...ds) {}
	
	public Constructors(double d, int i, String s) {}
	
	public Constructors(int i, int j, int k, int l, int m) {}
	
	public Constructors(double d, double e, double f, double g, double h) {}
	
	public Constructors(int i, double d, int j, double e, int k) {}
	
	public Constructors(double d, int i, double e, int j, double f) {}
	
	public Constructors(double d, float f, long l, int i, short s, byte b, char c, boolean bo, String str, Object[] array) {}
}