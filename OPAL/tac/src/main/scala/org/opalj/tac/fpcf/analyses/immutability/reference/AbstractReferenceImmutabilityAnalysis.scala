/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.immutability.reference

import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.PCs
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.FieldPrematurelyRead
import org.opalj.br.fpcf.properties.ImmutableReference
import org.opalj.br.fpcf.properties.MutableReference
import org.opalj.br.fpcf.properties.NotPrematurelyReadField
import org.opalj.br.fpcf.properties.PrematurelyReadField
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.properties.ReferenceImmutability
import org.opalj.br.fpcf.properties.TypeImmutability_new
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.LBP
import org.opalj.fpcf.Property
import org.opalj.fpcf.UBP
import org.opalj.tac.DUVar
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation

/***
 *
 */
trait AbstractReferenceImmutabilityAnalysis extends FPCFAnalysis {

    type V = DUVar[ValueInformation]

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)
    final val definitionSites = project.get(DefinitionSitesKey)
    implicit final val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    case class State(
            field:                                  Field,
            var referenceImmutability:              ReferenceImmutability                           = ImmutableReference(true),
            var prematurelyReadDependee:            Option[EOptionP[Field, FieldPrematurelyRead]]   = None,
            var purityDependees:                    Set[EOptionP[DeclaredMethod, Purity]]           = Set.empty,
            var lazyInitInvocation:                 Option[(DeclaredMethod, PC)]                    = None,
            var calleesDependee:                    Option[EOptionP[DeclaredMethod, Callees]]       = None,
            var referenceImmutabilityDependees:     Set[EOptionP[Field, ReferenceImmutability]]     = Set.empty,
            var escapeDependees:                    Set[EOptionP[DefinitionSite, EscapeProperty]]   = Set.empty,
            var tacDependees:                       Map[Method, (EOptionP[Method, TACAI], PCs)]     = Map.empty,
            var notEscapes:                         Boolean                                         = true,
            var typeDependees:                      Set[EOptionP[ObjectType, TypeImmutability_new]] = Set.empty,
            var lazyInitializerIsDeterministicFlag: Boolean                                         = false
    ) {
        def hasDependees: Boolean = {
            prematurelyReadDependee.isDefined || purityDependees.nonEmpty ||
                calleesDependee.isDefined || referenceImmutabilityDependees.nonEmpty ||
                escapeDependees.nonEmpty || tacDependees.nonEmpty || typeDependees.nonEmpty
        }

        def dependees: Traversable[EOptionP[Entity, Property]] = {
            prematurelyReadDependee ++ purityDependees ++ calleesDependee ++
                referenceImmutabilityDependees ++ escapeDependees ++ tacDependees.valuesIterator.map(_._1) ++ typeDependees
        }
    }

    /**
     * Returns the TACode for a method if available, registering dependencies as necessary.
     */
    def getTACAI(
        method: Method,
        pcs:    PCs
    )(implicit state: State): Option[TACode[TACMethodParameter, V]] = {
        propertyStore(method, TACAI.key) match {
            case finalEP: FinalEP[Method, TACAI] ⇒
                finalEP.ub.tac
            case eps: InterimEP[Method, TACAI] ⇒
                state.tacDependees += method -> ((eps, pcs))
                eps.ub.tac
            case epk ⇒
                state.tacDependees += method -> ((epk, pcs))
                None
        }
    }

    /**
     * Checkes if the method that defines the value assigned to a (potentially) lazily initialized
     * field is deterministic, ensuring that the same value is written even for concurrent
     * executions.
     */
    def isNonDeterministic(
        eop: EOptionP[DeclaredMethod, Purity]
    )(implicit state: State): Boolean = eop match {
        case LBP(p: Purity) if p.isDeterministic ⇒
            println("XXX: false is non deterministic"); false
        case EUBP(e, p: Purity) if !p.isDeterministic ⇒
            println("e: "+e+",XXX: true is non deterministic"); true
        case _ ⇒
            println("XXX: else");
            state.purityDependees += eop
            false
    }

    def lazyInitializerIsDeterministic(
        method: Method,
        code:   Array[Stmt[V]]
    )(implicit state: State): Boolean = {

        val propertyStoreResult = propertyStore(declaredMethods(method), Purity.key)
        println("property store purity  result: "+propertyStoreResult)
        val result = (method.descriptor.parametersCount == 0 && !isNonDeterministic(
            propertyStoreResult
        ))
        result
    }

    /**
     * Checkes if the field the value assigned to a (potentially) lazily initialized field is final,
     * ensuring that the same value is written even for concurrent executions.
     */
    def isImmutableReference(
        eop: EOptionP[Field, ReferenceImmutability]
    )(implicit state: State): Boolean = eop match {
        case FinalEP(e, ImmutableReference(_)) ⇒ true
        case FinalEP(e, MutableReference)      ⇒ false
        //
        case LBP(ImmutableReference(_))        ⇒ true
        case UBP(MutableReference)             ⇒ false

        /**
         * case LBP(_: ImmutableReference) ⇒ //FinalField) ⇒
         * true
         * case UBP(_: MutableReference) ⇒ false // NonFinalField) ⇒ false *
         */
        case _ ⇒
            state.referenceImmutabilityDependees += eop
            true
    }

    /**
     * Checks if the field is prematurely read, i.e. read before it is initialized in the
     * constructor, using the corresponding property.
     */
    def isPrematurelyRead(
        eop: EOptionP[Field, FieldPrematurelyRead]
    )(implicit state: State): Boolean = {
        eop match {
            case LBP(NotPrematurelyReadField) ⇒
                state.prematurelyReadDependee = None
                false
            case UBP(PrematurelyReadField) ⇒ true
            case eps ⇒
                state.prematurelyReadDependee = Some(eps)
                false
        }
    }
}
