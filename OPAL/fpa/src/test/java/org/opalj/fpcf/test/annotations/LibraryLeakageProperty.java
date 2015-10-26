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
package org.opalj.fpcf.test.annotations;

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.METHOD;

/**
 * 
 * Describes the MethodLeakage property of the OPAL FixpointAnalysis.
 * 
 * @author Michael Reif
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface LibraryLeakageProperty {
	
	/**
	 * This refers to the LibraryLeakageProperty when the property is computed for
	 * a library under the open package assumption. 
	 */
	LibraryLeakageKeys opa() default LibraryLeakageKeys.Leakage;
	
	/**
	 * This refers to the LibraryLeakageProperty when the property is computed for
	 * a library under the closed package assumption. 
	 */
	LibraryLeakageKeys cpa() default LibraryLeakageKeys.Leakage;
	
	/**
	 * This refers to the LibraryLeakageProperty when the property is computed for
	 * an application.
	 */
	LibraryLeakageKeys application() default LibraryLeakageKeys.Leakage;
}
