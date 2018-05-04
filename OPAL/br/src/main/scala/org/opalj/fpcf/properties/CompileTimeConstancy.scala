/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package fpcf
package properties

import org.opalj.br.Field

sealed trait CompileTimeConstancyPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = CompileTimeConstancy

}

/**
 * Describes whether a [[org.opalj.br.Field]] is initialized deterministcally to the same value on
 * every execution of the program.
 *
 * @author Dominik Helm
 */
sealed abstract class CompileTimeConstancy
    extends OrderedProperty with CompileTimeConstancyPropertyMetaInformation {

    /**
     * The globally unique key of the [[CompileTimeConstancy]] property.
     */
    final def key: PropertyKey[CompileTimeConstancy] = CompileTimeConstancy.key

}

object CompileTimeConstancy extends CompileTimeConstancyPropertyMetaInformation {
    /**
     * The key associated with every compile-time constancy property. The name is
     * "CompileTimeConstancy"; the fallback is "CompileTimeVaryingField".
     */
    final val key = PropertyKey.create[Field, CompileTimeConstancy](
        "CompileTimeConstancy",
        (_: PropertyStore, field: Field) ⇒ {
            if (field.isStatic && field.isFinal && field.constantFieldValue.isDefined)
                CompileTimeConstantField
            else
                CompileTimeVaryingField
        },
        (_: PropertyStore, eps: EPS[Field, CompileTimeConstancy]) ⇒ eps.toUBEP
    )
}

/**
 * The constant field is deterministically initialized to the same value on every program run.
 */
case object CompileTimeConstantField extends CompileTimeConstancy {

    override def checkIsEqualOrBetterThan(other: CompileTimeConstancy): Unit = {}
}

/**
 * The field is not a constant or may be initialized to different values on different program runs.
 */
case object CompileTimeVaryingField extends CompileTimeConstancy {

    override def checkIsEqualOrBetterThan(other: CompileTimeConstancy): Unit = {
        if (other ne CompileTimeVaryingField)
            throw new IllegalArgumentException(s"impossible refinement: $other ⇒ $this")
    }
}