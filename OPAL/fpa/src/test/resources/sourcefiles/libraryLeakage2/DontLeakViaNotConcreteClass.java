/* BSD 2Clause License:
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
package libraryLeakage2;

import org.opalj.fpcf.test.annotations.CallabilityKeys;
import org.opalj.fpcf.test.annotations.CallabilityProperty;

/**
 *
 * This class is package private, hence, in a closed library scenario a client
 * can not access the the methods of this class. There are 2 subclasses in that
 * example:
 *  - DontLeakViaAbstractClass 
 *  - DontLeakViaInterface
 * 
 * This Tests, that the methods does not leak via non-abstract classes.
 * 
 * @author Michael Reif
 */
class DontLeakViaNotConcreteClass {

	/**
	 * This is weird. This method should be exposed to the client by the
	 * DontLeakViaAbstractClass but the compiler somehow introduces a method
	 * with the same name that prevent that method from being exposed.
	 */
	@CallabilityProperty(cpa = CallabilityKeys.NotCallable)
	public void iDoNotLeak() {
	}

	/**
	 * Package visible methods can not leak when the closed packages assumption
	 * is met.
	 */
	@CallabilityProperty(cpa = CallabilityKeys.NotCallable)
	void iCanNotLeakUnderCPA() {

	}

	/**
	 * This method can not leak because it is overridden in one subclass and the
	 * other one is package private.
	 */
	@CallabilityProperty(cpa = CallabilityKeys.NotCallable)
	protected void iDoNotLeakToo() {

	}
}

abstract class YouCantInheritFromMe extends DontLeakViaNotConcreteClass {

	protected void iDoNotLeakToo() {

	}
}
