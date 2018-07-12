/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package escape

import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.VirtualMethodEscapeProperty

import scala.collection.mutable

/**
 * Stores the state associated with a specific [[AbstractEscapeAnalysisContext]] computed by an
 * [[AbstractEscapeAnalysis]].
 * This state contains a set of [[EOptionP]]s from which the resulting escape state depends.
 * In addition to this, it holds the current most restrictive escape state applicable.
 *
 * @author Florian Kuebler
 */
trait AbstractEscapeAnalysisState {

    private[this] var _dependees = Set.empty[EOptionP[Entity, Property]]
    private[this] var _mostRestrictiveProperty: EscapeProperty = NoEscape

    /**
     * Sets mostRestrictiveProperty to the greatest lower bound of its current value and the
     * given one.
     */
    @inline private[escape] final def meetMostRestrictive(prop: EscapeProperty): Unit = {
        assert((_mostRestrictiveProperty meet prop).lessOrEqualRestrictive(_mostRestrictiveProperty))
        _mostRestrictiveProperty = _mostRestrictiveProperty meet prop
    }

    /**
     * Adds an entity property pair (or epk) into the set of dependees.
     */
    @inline private[escape] final def addDependency(eOptionP: EOptionP[Entity, Property]): Unit = {
        _dependees += eOptionP
    }

    /**
     * Removes the entity property pair (or epk) that correspond to the given ep from the set of
     * dependees.
     */
    @inline private[escape] final def removeDependency(ep: EPS[Entity, Property]): Unit = {
        assert(_dependees.count(epk ⇒ (epk.e eq ep.e) && epk.pk == ep.pk) <= 1)
        _dependees = _dependees.filter(epk ⇒ (epk.e ne ep.e) || epk.pk != ep.pk)
    }

    /**
     * The set of open dependees.
     */
    private[escape] final def dependees: Set[EOptionP[Entity, Property]] = _dependees

    /**
     * The currently most restrictive escape property. It can get even more restrictive during the
     * analysis.
     */
    private[escape] final def mostRestrictiveProperty: EscapeProperty = _mostRestrictiveProperty
}

/**
 * Provides a cache for entities to which the [[InterProceduralEscapeAnalysis]] might depend to in
 * order to have consistent [[EOptionP]] results for a single context until a result is committed
 * to the [[PropertyStore]].
 */
trait DependeeCache {

    private[escape] val dependeeCache: mutable.Map[VirtualFormalParameter, EOptionP[Entity, EscapeProperty]] =
        mutable.Map()

    private[escape] val vdependeeCache: mutable.Map[VirtualFormalParameter, EOptionP[Entity, VirtualMethodEscapeProperty]] =
        mutable.Map()
}

/**
 * Stores the parameters to which the analyses depends on, and whose functions return value is used
 * any further.
 */
trait ReturnValueUseSites {
    private[escape] val hasReturnValueUseSites = scala.collection.mutable.Set[VirtualFormalParameter]()
}
