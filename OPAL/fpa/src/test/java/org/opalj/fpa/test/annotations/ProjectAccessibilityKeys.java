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
package org.opalj.fpa.test.annotations;

/**
 * 
 * Represents all possible property variants of the [[ProjectAccessibility]] property
 * defined in the [[SchadowingAnalysis]].
 * 
 * @Note This enum is used for test-only purposes. It is used as parameter in
 * the [[ProjectAccessibilityProperty]] annotation. Make sure, that the names
 * of the different variants of the Overridden property matches the enumeration
 * names exactly.
 * 
 * @author Michael Reif
 * 
 */
public enum ProjectAccessibilityKeys {
	
	/**
	 * This kind refers to all entities (methods, classes, etc.) that are globally visible to the client.
	 */
	Global,
	
	/**
	 * This kind refers to all entities (methods, classes, etc.) that are in scope of a package.
	 */
	PackageLocal,
	
	/**
	 * This kind refers to all entities (methods, classes, etc.) that are only visible within a class
	 * and doesn't escape of the class.
	 */
	ClassLocal
}