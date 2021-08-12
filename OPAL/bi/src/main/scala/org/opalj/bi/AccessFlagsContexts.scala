/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi

/**
 * Enumeration of the different contexts in which the JVM Spec. uses
 * `access_flags` fields and also an enumeration which `access_flags` are found
 * in which context.
 */
object AccessFlagsContexts extends Enumeration {

    val INNER_CLASS, CLASS, METHOD, FIELD, METHOD_PARAMETERS, MODULE = Value

    val INNER_CLASS_FLAGS: IndexedSeq[AccessFlag] =
        IndexedSeq(
            ACC_PUBLIC,
            ACC_PRIVATE,
            ACC_PROTECTED,
            ACC_STATIC,
            ACC_SUPER /*NOT SPECIFIED IN THE JVM SPEC. - MAYBE THIS BIT IS JUST SET BY THE SCALA COMPILER!*/ ,
            ACC_FINAL,
            ACC_INTERFACE,
            ACC_ABSTRACT,
            ACC_SYNTHETIC,
            ACC_ANNOTATION,
            ACC_ENUM
        )

    val CLASS_FLAGS: IndexedSeq[AccessFlag] =
        IndexedSeq(
            ACC_PUBLIC,
            ACC_FINAL,
            ACC_SUPER,
            ACC_INTERFACE,
            ACC_ABSTRACT,
            ACC_SYNTHETIC,
            ACC_ANNOTATION,
            ACC_ENUM,
            ACC_MODULE
        )

    val FIELD_FLAGS: IndexedSeq[AccessFlag] =
        IndexedSeq(
            ACC_PUBLIC,
            ACC_PRIVATE,
            ACC_PROTECTED,
            ACC_STATIC,
            ACC_FINAL,
            ACC_VOLATILE,
            ACC_TRANSIENT,
            ACC_SYNTHETIC,
            ACC_ENUM
        )

    val METHOD_FLAGS: IndexedSeq[AccessFlag] =
        IndexedSeq(
            ACC_PUBLIC,
            ACC_PRIVATE,
            ACC_PROTECTED,
            ACC_STATIC,
            ACC_FINAL,
            ACC_SYNCHRONIZED,
            ACC_BRIDGE,
            ACC_VARARGS,
            ACC_NATIVE,
            ACC_ABSTRACT,
            ACC_STRICT,
            ACC_SYNTHETIC
        )

    /**
     * Access flags related to Java 9 module definitions.
     */
    val MODULE_FLAGS: IndexedSeq[AccessFlag] = {
        IndexedSeq(
            ACC_OPEN,
            ACC_TRANSITIVE,
            ACC_STATIC_PHASE,
            ACC_SYNTHETIC,
            ACC_MANDATED
        )
    }

    val METHOD_PARAMETER_FLAGS: IndexedSeq[AccessFlag] = {
        IndexedSeq(ACC_FINAL, ACC_SYNTHETIC, ACC_MANDATED)
    }

    val CLASS_VISIBILITY_FLAGS: IndexedSeq[AccessFlag] = {
        IndexedSeq(ACC_PUBLIC)
    }

    val MEMBER_VISIBILITY_FLAGS: IndexedSeq[AccessFlag] = {
        IndexedSeq(ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED)
    }

    val INNER_CLASS_VISIBILITY_FLAGS: IndexedSeq[AccessFlag] = MEMBER_VISIBILITY_FLAGS

    val FIELD_VISIBILITY_FLAGS: IndexedSeq[AccessFlag] = MEMBER_VISIBILITY_FLAGS

    val METHOD_VISIBILITY_FLAGS: IndexedSeq[AccessFlag] = MEMBER_VISIBILITY_FLAGS

    def potentialAccessFlags(ctx: AccessFlagsContext): IndexedSeq[AccessFlag] = {
        ctx match {
            case INNER_CLASS       => INNER_CLASS_FLAGS
            case CLASS             => CLASS_FLAGS
            case METHOD            => METHOD_FLAGS
            case FIELD             => FIELD_FLAGS
            case METHOD_PARAMETERS => METHOD_PARAMETER_FLAGS
            case MODULE            => MODULE_FLAGS
        }
    }
}
