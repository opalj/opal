/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package instanceMethodVisibilityTest1;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

/**
 * 
 * Since this class is abstract it is not possible to create an instance of it.
 * But via inheritance through a subclass is it possible to get access to the methods
 * when there are not overridden. This class is package private, that implies that a client
 * that is not able to contribute to this given package can not access the already implemented
 * methods, no matter what visibility modifier they have. This class has no public subclass,
 * hence, all methods should be maximal package local under the closed packages assumption.
 * 
 * @note This comments refers to the use of the closed packages assumption (cpa)
 *
 */
abstract class PPAbstractClass {
	
	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
	public void publicMethod(){
	}
	
	@ProjectAccessibilityProperty(cpa=ProjectAccessibilityKeys.PackageLocal)
	void packagePrivateMethod(){
	}
	
	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
	protected void protectedMethod(){
	}
	
	@ProjectAccessibilityProperty(
			opa=ProjectAccessibilityKeys.ClassLocal,
			cpa=ProjectAccessibilityKeys.ClassLocal)
	private void privateMethod(){
	}
}
