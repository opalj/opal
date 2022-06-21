/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Property
import org.opalj.fpcf.SomeEOptionP
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.tac.common.DefinitionSiteLike

/**
 * Stores the state associated with a specific [[AbstractEscapeAnalysisContext]] computed by an
 * [[AbstractEscapeAnalysis]].
 * This state contains a set of [[org.opalj.fpcf.EOptionP]]s from which the resulting escape
 * state depends. In addition to this, it holds the current most restrictive escape state
 * applicable.
 *
 * @author Florian Kuebler
 */
trait AbstractEscapeAnalysisState {

    // todo: use better _dependees Handling
    // we have dependencies to escape values of formal parameters, tac of methods and
    // callees of declared methods, i.e. different entities for each property kind.
    // therefore using a map is safe!
    private[this] var _dependees = Map.empty[Entity, EOptionP[Entity, Property]]
    private[this] var _dependeesSet = Set.empty[SomeEOptionP]

    private[this] var _mostRestrictiveProperty: EscapeProperty = NoEscape

    private[this] var _tacai: Option[TACode[TACMethodParameter, V]] = None

    private[this] var _uses: IntTrieSet = _

    private[this] var _defSite: Int = _

    /**
     * Sets mostRestrictiveProperty to the greatest lower bound of its current value and the
     * given one.
     */
    @inline private[escape] final def meetMostRestrictive(prop: EscapeProperty): Unit = {
        assert {
            (_mostRestrictiveProperty meet prop).lessOrEqualRestrictive(_mostRestrictiveProperty)
        }
        _mostRestrictiveProperty = _mostRestrictiveProperty meet prop
    }

    /**
     * Adds an entity property pair (or epk) into the set of dependees.
     */
    @inline private[escape] final def addDependency(eOptionP: EOptionP[Entity, Property]): Unit = {
        assert(!_dependees.contains(eOptionP.e))
        _dependees += eOptionP.e -> eOptionP
        _dependeesSet += eOptionP
    }

    /**
     * Removes the entity property pair (or epk) that correspond to the given ep from the set of
     * dependees.
     */
    @inline private[escape] final def removeDependency(
        ep: EOptionP[Entity, Property]
    ): Unit = {
        assert(_dependees.contains(ep.e))
        val oldEOptionP = _dependees(ep.e)
        _dependees -= ep.e
        _dependeesSet -= oldEOptionP
    }

    /**
     * Do we already registered a dependency to that entity?
     */
    @inline private[escape] final def containsDependency(
        ep: EOptionP[Entity, Property]
    ): Boolean = {
        _dependees.contains(ep.e)
    }

    @inline private[escape] final def getDependency(e: Entity): EOptionP[Entity, Property] = {
        _dependees(e)
    }

    /**
     * The set of open dependees.
     */
    private[escape] final def dependees: Set[SomeEOptionP] = {
        _dependeesSet
    }

    /**
     * Are there any dependees?
     */
    private[escape] final def hasDependees: Boolean = _dependees.nonEmpty

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
            case (_: Context, ds: DefinitionSiteLike) =>
                _defSite = tacai.properStmtIndexForPC(ds.pc)
                _uses = ds.usedBy(tacai)

            case (_: Context, fp: VirtualFormalParameter) =>
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
        params.exists { case UVar(_, defSites) => defSites.contains(_defSite) }
    }
}

/**
 * Stores the parameters to which the analyses depends on, and whose functions return value is used
 * any further.
 */
trait ReturnValueUseSites {
    private[escape] val hasReturnValueUseSites =
        scala.collection.mutable.Set[(Context, VirtualFormalParameter)]()
}
