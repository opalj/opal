/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.br.ClassHierarchy
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

/**
 * This is the most primitive domain that can be used to transform Java bytecode to the
 * three address representation offered by OPAL, which is build upon the result of a lightweight
 * abstract interpretation.
 */
class PrimitiveTACAIDomain(
        val classHierarchy: ClassHierarchy,
        val method:         Method
) extends TypeLevelDomain
    with ThrowAllPotentialExceptionsConfiguration
    with IgnoreSynchronization
    with DefaultTypeLevelHandlingOfMethodResults
    with TheMethod
    with RecordDefUse {

    def this(project: SomeProject, method: Method) =
        this(project.classHierarchy, method)
}

