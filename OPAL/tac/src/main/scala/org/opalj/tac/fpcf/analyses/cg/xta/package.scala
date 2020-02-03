/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.fpcf.Entity

/**
 * @author Andreas Bauer
 */
package object xta {
    /**
     * Type alias for Entity/AnyRef for better code comprehension.
     * Within the context of propagation-based call graph construction algorithms,
     * each entity has a corresponding "set entity". The type set of an entity is
     * attached to its set entity. The set entity may be itself, or some other entity.
     * The set of a single set entity may be shared among multiple entities.
     * The assignment of entity to set entity varies per algorithm. For example,
     * in XTA, the set entity of a DefinedMethod is the DefinedMethod itself, but
     * in CTA, the set entity is the method's class.
     */
    type TypeSetEntity = Entity
}

