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
package org.opalj
package ai
package domain
package l0

import org.opalj.util.{ Answer, Yes, No, Unknown }

/**
 * Base implementation of the `TypeLevelDoubleValues` trait that requires that
 * the domain`s `Value` trait is not extended. This implementation just satisfies
 * the basic requirements of OPAL w.r.t. the domain's computational type.
 *
 * @author Michael Eichberg
 */
trait DefaultTypeLevelDoubleValues
        extends DefaultDomainValueBinding
        with TypeLevelDoubleValues {

    /**
     * Represents a specific, but unknown double value.
     */
    case object ADoubleValue extends super.DoubleValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] =
            // Since `value` is guaranteed to have computational type double and we 
            // don't care about the precise value, as this DomainValue already 
            // just represents "some" double value, we can always safely return
            // NoUpdate. 
            NoUpdate

        override def summarize(pc: PC): DomainValue = this

        override def adapt(target: Domain, pc: PC): target.DomainValue =
            target.DoubleValue(pc)

    }

    final override def DoubleValue(pc: PC): DoubleValue = ADoubleValue

    final override def DoubleValue(pc: PC, value: Double): DoubleValue = ADoubleValue
}


