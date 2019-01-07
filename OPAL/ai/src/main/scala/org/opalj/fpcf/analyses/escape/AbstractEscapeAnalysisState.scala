/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package escape

import org.opalj.ai.common.DefinitionSiteLike
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.cg.properties.Callees
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.VirtualMethodEscapeProperty
import org.opalj.tac.Expr
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.UVar
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

    private[this] var _tacai: Option[TACode[TACMethodParameter, V]] = None

    private[this] var _uses: IntTrieSet = _

    private[this] var _defSite: Int = _

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
        assert(_dependees.count(epk ⇒ (epk.e eq eOptionP.e) && epk.pk == eOptionP.pk) <= 1)
    }

    /**
     * Removes the entity property pair (or epk) that correspond to the given ep from the set of
     * dependees.
     */
    @inline private[escape] final def removeDependency(ep: EOptionP[Entity, Property]): Unit = {
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

    private[escape] def updateTACAI(
        tacai: TACode[TACMethodParameter, V]
    )(implicit context: AbstractEscapeAnalysisContext): Unit = {
        _tacai = Some(tacai)

        context.entity match {
            case ds: DefinitionSiteLike ⇒
                _defSite = tacai.pcToIndex(ds.pc)
                _uses = ds.usedBy(tacai)

            case fp: VirtualFormalParameter ⇒
                val param = tacai.params.parameter(fp.origin)
                _uses = param.useSites
                _defSite = param.origin
        }
    }

    private[escape] def tacai: Option[TACode[TACMethodParameter, V]] = _tacai

    private[escape] def uses: IntTrieSet = _uses

    private[escape] def defSite: Int = _defSite

    /**
     * Checks whether the expression is a use of the defSite.
     * This method is called on expressions within tac statements. We assume a flat hierarchy, so
     * the expression is expected to be a [[org.opalj.tac.Var]].
     */
    @inline private[escape] final def usesDefSite(expr: Expr[V]): Boolean = {
        assert(expr.isVar)
        expr.asVar.definedBy.contains(_defSite)
    }

    /**
     * If there exists a [[org.opalj.tac.UVar]] in the params of a method call that is a use of the
     * current entity's def-site return true.
     */
    @inline private[escape] final def anyParameterUsesDefSite(params: Seq[Expr[V]]): Boolean = {
        assert(params.forall(_.isVar))
        params.exists { case UVar(_, defSites) ⇒ defSites.contains(_defSite) }
    }
}

/**
 * Provides a cache for entities to which the [[InterProceduralEscapeAnalysis]] might depend to in
 * order to have consistent [[EOptionP]] results for a single context until a result is committed
 * to the [[PropertyStore]].
 */
trait DependeeCache {
    // TODO There is only ever one key in this map, the current method, so an Option should suffice
    private[escape] val calleesCache: mutable.Map[DeclaredMethod, EOptionP[Entity, Callees]] =
        mutable.Map()

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
