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

public class WhiteboardGraph {

	public Class<?> vulnerable(String s3) throws ClassNotFoundException {
		String s2 = checkParam(s3);
		Class<?> r2 = loadIt(s2);
		Class<?> r3 = checkReturn(r2);
		return r3;
	}

	private Class<?> checkReturn(Class<?> y) {
		return y;
	}

	private Class<?> loadIt(String s) throws ClassNotFoundException {
		Class<?> ret = Class.forName(s);
		return ret;
	}

	private String checkParam(String x) {
		return x;
	}

	private Class<?> notVulnerable(String s3) throws ClassNotFoundException {
		if (s3.length() > 10) {
			Class<?> r1 = loadIt(s3);
			r1 = null;
			return r1;
		} else {
			Class<?> r2 = loadIt("constant");
			return r2;
		}
	}
	
	public Class<?> notVulnerableWrapper(String s3) throws ClassNotFoundException {
		return notVulnerable(s3);
	}
}
