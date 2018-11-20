/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.Field

sealed trait CompileTimeConstancyPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = CompileTimeConstancy

}

/**
 * Describes whether a [[org.opalj.br.Field]] is initialized deterministically to the same value on
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
        (_: PropertyStore, _: FallbackReason, field: Field) ⇒ {
            if (field.isStatic && field.isFinal && field.constantFieldValue.isDefined)
                CompileTimeConstantField
            else
                CompileTimeVaryingField
        },
        (_: PropertyStore, eps: EPS[Field, CompileTimeConstancy]) ⇒ eps.ub,
        (_: PropertyStore, _: Entity) ⇒ None
    )
}

/**
 * The constant field is deterministically initialized to the same value on every program run.
 */
case object CompileTimeConstantField extends CompileTimeConstancy {

    override def checkIsEqualOrBetterThan(e: Entity, other: CompileTimeConstancy): Unit = {}
}

/**
 * The field is not a constant or may be initialized to different values on different program runs.
 */
case object CompileTimeVaryingField extends CompileTimeConstancy {

    override def checkIsEqualOrBetterThan(e: Entity, other: CompileTimeConstancy): Unit = {
        if (other ne CompileTimeVaryingField)
            throw new IllegalArgumentException(s"$e: impossible refinement: $other ⇒ $this")
    }
}
