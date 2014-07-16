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
package l1

import org.opalj.ai.Domain
import org.opalj.ai.IsLongValue
import org.opalj.ai.domain.Configuration
import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeLong

/**
 * Implements the shift operators for long values.
 *
 * @author Michael Eichberg
 * @author Riadh Chtara
 */
trait LongValuesShiftOperators extends LongValuesDomain {
    this: CoreDomain with ConcreteLongValues with ConcreteIntegerValues ⇒

    override def lshl(pc: PC, longValue: DomainValue, shift: DomainValue): DomainValue = {
        this.longValue(longValue) { v ⇒
            this.intValue(shift)(s ⇒ LongValue(pc, v << s)) { LongValue(pc) }
        } {
            LongValue(pc)
        }
    }

    override def lshr(pc: PC, longValue: DomainValue, shift: DomainValue): DomainValue = {
        this.longValue(longValue) { v ⇒
            this.intValue(shift)(s ⇒ LongValue(pc, v >> s)) { LongValue(pc) }
        } {
            LongValue(pc)
        }
    }

    override def lushr(pc: PC, longValue: DomainValue, shift: DomainValue): DomainValue = {
        this.longValue(longValue) { v ⇒
            this.intValue(shift)(s ⇒ LongValue(pc, v >>> s)) { LongValue(pc) }
        } {
            LongValue(pc)
        }
    }
}


