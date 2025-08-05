/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.alias.AliasEntity
import org.opalj.br.fpcf.properties.alias.AliasSourceElement
import org.opalj.fpcf.PropertyStore

/**
 * Encapsulates the context of an alias analysis computation.
 *
 * It contains the entity for which the aliasing information is computed, the current project, and the property store
 * used to query other properties.
 *
 * It can be overridden to provide additional information to the computation.
 *
 * @param entity The entity for which the aliasing information is computed.
 * @param project The current project.
 * @param propertyStore The property store.
 */
class AliasAnalysisContext(
    val entity:        AliasEntity,
    val project:       SomeProject,
    val propertyStore: PropertyStore
) {

    /**
     * @return The context of the first element of the [[AliasEntity]].
     */
    def context1: Context = entity.context1

    /**
     * @return The context of the second element of the [[AliasEntity]].
     */
    def context2: Context = entity.context2

    /**
     * @return The context of the given [[AliasSourceElement]].
     */
    def contextOf(ase: AliasSourceElement): Context = {
        if (isElement1(ase)) context1 else context2
    }

    /**
     * @return The first [[AliasSourceElement]] of the [[AliasEntity]].
     */
    def element1: AliasSourceElement = entity.element1

    /**
     * @return The second [[AliasSourceElement]] of the [[AliasEntity]]
     */
    def element2: AliasSourceElement = entity.element2

    /**
     * @return `true` if the given [[AliasSourceElement]] is the first element of the [[AliasEntity]].
     */
    def isElement1(e: AliasSourceElement): Boolean = element1 eq e

    /**
     * @return `true` if the given [[AliasSourceElement]] is the second element of the [[AliasEntity]].
     */
    def isElement2(e: AliasSourceElement): Boolean = element2 eq e

}
