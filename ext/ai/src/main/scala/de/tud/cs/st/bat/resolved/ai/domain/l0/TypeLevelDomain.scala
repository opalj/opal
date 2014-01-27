/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai
package domain
package l0

/**
 * This domain performs all computations basically at the type level and does
 * not track the flow of concrete values. Given the very high level of abstraction,
 * an abstract interpretation using this domain typically terminates quickly.
 *
 * This domain can be used as a foundation/as an inspiration for building your
 * own `Domain`. For example, it is useful to, e.g., track which types of values
 * are actually created to calculate a more precise call graph.
 *
 * @author Michael Eichberg
 */
trait TypeLevelDomain[+I]
    extends Domain[I]    
    with DefaultDomainValueBinding[I]
    with Configuration
    with DefaultReferenceValuesBinding[I]
    with DefaultTypeLevelIntegerValues[I]
    with DefaultTypeLevelLongValues[I]
    with DefaultTypeLevelFloatValues[I]
    with DefaultTypeLevelDoubleValues[I]
    with TypeLevelFieldAccessInstructions
    with TypeLevelInvokeInstructions
    with PredefinedClassHierarchy

/**
 * This is a ready to use domain which sets the domain identifier to "BaseTypeLevelDomain".
 *
 * This domain is primarily useful for testing and debugging purposes.
 *
 * @author Michael Eichberg
 */
class BaseDomain
        extends TypeLevelDomain[String]
        with IgnoreMethodResults
        with IgnoreSynchronization {

    def identifier = "BaseTypeLevelDomain"

}

/**
 * A domain with a configurable identifier.
 *
 * @author Michael Eichberg
 */
class BaseConfigurableDomain[+I](
    val identifier: I)
        extends TypeLevelDomain[I]
        with IgnoreMethodResults
        with IgnoreSynchronization

class BaseRecordingDomain[I](
    val identifier: I)
        extends TypeLevelDomain[I]
        with IgnoreMethodResults
        with IgnoreSynchronization
        with RecordLastReturnedValues[I]
        with RecordAllThrownExceptions[I]
        with RecordReturnInstructions[I]
