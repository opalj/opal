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
trait BaseDomain[+I]
    extends DefaultValueBinding[I]
    with DefaultPreciseReferenceValues[I]
    with DefaultTypeLevelIntegerValues[I]
    with DefaultTypeLevelLongValues[I]
    with DefaultTypeLevelFloatValues[I]
    with DefaultTypeLevelDoubleValues[I]
    with DefaultReturnAddressValues[I]

/**
 * A complete definition of a domain except of the domain's identifier.
 *
 * @author Michael Eichberg
 */
trait AbstractDefaultDomain[+I]
        extends BaseDomain[I]
        with TypeLevelArrayInstructions
        with TypeLevelFieldAccessInstructions
        with TypeLevelInvokeInstructions
        with DoNothingOnReturnFromMethod
        with BasicTypeHierarchy {

}

/**
 * This is a ready to use domain which sets the domain identifier to "DefaultDomain".
 *
 * This domain is primarily useful for testing and debugging purposes.
 *
 * @author Michael Eichberg
 */
class DefaultDomain extends AbstractDefaultDomain[String] {

    def identifier = "DefaultDomain"

}

trait ConfigurableDomain[+I] extends Domain[I] {
    val identifier: I
}

/**
 * A domain with a configurable identifier.
 *
 * @author Michael Eichberg
 */
class ConfigurableDefaultDomain[+I](
    val identifier: I)
        extends ConfigurableDomain[I]
        with AbstractDefaultDomain[I] {

}

class RecordingDomain[I](identifier: I)
        extends ConfigurableDefaultDomain[I](identifier)
        with RecordReturnValues[I] {

}

class ConfigurablePreciseDomain[+I](
    val identifier: I)
        extends ConfigurableDomain[I]
        with DefaultValueBinding[I]
        with DefaultPreciseIntegerValues[I]
        with DefaultPreciseReferenceValues[I]
        with StringValues[I]
        with DefaultTypeLevelLongValues[I]
        with DefaultTypeLevelFloatValues[I]
        with DefaultTypeLevelDoubleValues[I]
        with DefaultReturnAddressValues[I]
        with TypeLevelArrayInstructions
        with TypeLevelFieldAccessInstructionsWithNullPointerHandling
        with TypeLevelInvokeInstructionsWithNullPointerHandling
        with DoNothingOnReturnFromMethod
        with BasicTypeHierarchy {

}

class PreciseRecordingDomain[I](identifier: I)
        extends ConfigurablePreciseDomain[I](identifier)
        with RecordReturnValues[I] {

}

