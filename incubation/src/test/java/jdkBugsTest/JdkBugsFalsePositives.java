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

import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a test for the JDKBugs Class.forName() analysis. It contains
 * previously false positives that were found while analyzing the jdk
 * 
 * @author Lars
 * 
 */
public class JdkBugsFalsePositives {

	// ================================================================================================
	// com/sun/org/apache/xml/internal/utils/UnImplNode
	// com/sun/org/apache/xml/internal/dtm/ref/dom2dtm/DOM2DTMdefaultNamespaceDeclarationNode
	// com/sun/org/apache/xerces/internal/dom/NodeImpl
	// com/sun/org/apache/xml/internal/dtm/ref/DTMNodeProxy
	// ================================================================================================

	public Object getFeature(String feature, String version) {
		// we don't have any alternate node, either this node does the job
		// or we don't have anything that does
		return isSupported(feature, version) ? this : null;
	}

	public boolean isSupported(String feature, String version) {
		return false;
	}

	// ================================================================================================
	// java/util/Objects
	// ================================================================================================

	public static <T> T requireNonNull(T obj, String message) {
		if (obj == null)
			throw new NullPointerException(message);
		return obj;
	}

	// ================================================================================================
	// java/lang/ClassLoader
	// ================================================================================================

	protected Object getClassLoadingLock(String className) {
		Object lock = this;
		if (parallelLockMap != null) {
			Object newLock = new Object();
			lock = parallelLockMap.putIfAbsent(className, newLock);
			if (lock == null) {
				lock = newLock;
			}
		}
		return lock;
	}

	private final ConcurrentHashMap<String, Object> parallelLockMap = new ConcurrentHashMap<>();;

}
