/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.analyses

import org.opalj.br.{Field, Method, PCs}
import org.opalj.br.analyses.{FieldAccessInformationKey, SomeProject}
import org.opalj.br.analyses.cg.{ClosedPackagesKey, TypeExtensibilityKey}
import org.opalj.br.fpcf.{BasicFPCFEagerAnalysisScheduler, BasicFPCFLazyAnalysisScheduler, FPCFAnalysis, FPCFAnalysisScheduler}
import org.opalj.br.fpcf.properties._
import org.opalj.fpcf._
import org.opalj.tac.fpcf.analyses.escape.EagerInterProceduralEscapeAnalysis.V
import org.opalj.tac._
import org.opalj.tac.fpcf.properties.TACAI

/**
 * New implementation of the fieldimmutability analysis
 * @author Tobias Peter Roth
 */
class L3FieldMutabilityAnalysis_new private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)

    case class State() {
        var hasImmutableType: Boolean = false
        var isFinal = false
        var effectivelyFinal = true
        var influencedByMethod = false
        var escapesViaMethod = false
    }

    def doDetermineFieldMutability_new(entity: Entity): PropertyComputationResult = {
        entity match {
            case field: Field ⇒ determineFieldMutability_new(field)
            case _ ⇒
                val m = entity.getClass.getName+"is not an org.opalj.br.Field"
                throw new IllegalArgumentException(m)
        }
    }


    private[analyses] def determineFieldMutability_new(field: Field): PropertyComputationResult = {
        val state: State = new State()

        //Reference Immutablity
        if (field.isProtected || field.isPackagePrivate || field.isPublic) {
            return Result(field, MutableField)
        }
        state.isFinal = field.isFinal

        var dependencies: Set[EOptionP[Entity, Property]] = Set.empty
        dependencies += EPK(field.fieldType, TypeImmutability.key)

        /**
         * def isSetByConstructor(): Boolean = {
         * fieldAccessInformation.writeAccesses(field).
         * filter(_._1.isConstructor).
         * foreach(x ⇒
         * (
         * x._1.asMethod.descriptor.parameterTypes.foreach(x ⇒
         * if (x == field.fieldType) {
         * return true
         * })
         * ))
         * false
         * }*
         */

        // if constructor sets the field it could escape if it is not immutable
        def c(state: State)(eps: SomeEPS): ProperPropertyComputationResult = {
            //dependencies = dependencies.filter(_.e ne eps.e)
            (eps: @unchecked) match {
                //case _: InterimEP[_, _] ⇒
                //    dependencies += eps
                //    InterimResult(InterimEP(field, MutableField), dependencies, c(state))
                case FinalEP(_, t) if (t == ImmutableContainerType || t == ImmutableType) ⇒
                    if (!state.influencedByMethod & (state.isFinal || state.effectivelyFinal)) {
                        Result(field, DeepImmutableField)
                    } else {

                        return InterimResult(field, MutableField, DeepImmutableField, dependencies, c(state))
                    }
                case FinalEP(e, MutableType) ⇒ {
                    if ((state.isFinal || state.effectivelyFinal) && !state.influencedByMethod) {
                        return Result(field, ShallowImmutableField)
                    }
                    return Result(field, MutableField)
                }
            }
        }

        for {
            (method, pcs) ← fieldAccessInformation.writeAccesses(field)
            taCode = getTACAI(method, pcs)
        } {
            if (methodInfluencesField(method, taCode.get, pcs, field)) {
                state.influencedByMethod = true
            }
            if (escapesViaMethod(method, taCode.get, pcs, field)) {
                state.escapesViaMethod = true
            }
        }

        //determine field Type immutability
        val result = propertyStore(field.fieldType, TypeImmutability.key)
        println("Result "+result)

        result match {
            case FinalEP(e, t) if (t == ImmutableType || t == ImmutableContainerType) ⇒ {
                println("has immutable type")
                state.hasImmutableType = true
            }
            case x: InterimEP[_, _] ⇒ return InterimResult(field, MutableField, DeepImmutableField, dependencies, c(state))
            case _                  ⇒ {}
        }
        println("Escapes Via Method: "+state.escapesViaMethod)
        println("Effectively Final: "+state.effectivelyFinal)
        println("is final: "+state.isFinal)
        println("Has ImmutableType : "+state.hasImmutableType)
        println("influenced by method: "+state.influencedByMethod)
        println("State"+state.toString)

        if (field.isPublic || field.isPackagePrivate) {
            return Result(field, MutableField)
        }

        if (((state.isFinal) || state.effectivelyFinal) && state.escapesViaMethod && !state.hasImmutableType) {
            return Result(field, MutableField)
        }
        if (((state.isFinal) || state.effectivelyFinal) && state.escapesViaMethod && state.hasImmutableType) {
            return Result(field, DeepImmutableField)
        }
        if (state.influencedByMethod) {
            return Result(field, MutableField)
        }
        if (((state.isFinal) || state.effectivelyFinal) && !state.hasImmutableType) {
            return Result(field, ShallowImmutableField)
        }
        if (((state.isFinal) || state.effectivelyFinal) && state.hasImmutableType) {
            return Result(field, DeepImmutableField)
        }
        if (((state.isFinal) || state.effectivelyFinal) && !state.influencedByMethod) {
            if (state.hasImmutableType) {
                return Result(field, DeepImmutableField)
            } else {
                return Result(field, ShallowImmutableField)
            }
        }
        if (((state.isFinal) || state.effectivelyFinal) && state.escapesViaMethod && state.hasImmutableType) {

            return Result(field, DeepImmutableField)
        }

        // not final or eff final reference and/or not influenced by a method
        return Result(field, MutableField)
    }
    /**
     * Analyzes field writes for a single method, returning false if the field may still be
     * effectively final and true otherwise.
     */

    def escapesViaMethod(
        method: Method,
        taCode: TACode[TACMethodParameter, V],
        pcs:    PCs,
        field:  Field
    ): Boolean =
        {
            if (method.isConstructor) {
                method.asMethod.descriptor.parameterTypes.foreach(x ⇒ if (x == field.fieldType) return true)
            }
            println("method: "+method.toJava.toString)
            val stmts = taCode.stmts
            for (pc ← pcs) {
                val index = taCode.pcToIndex(pc)
                if (index >= 0) {
                    val stmt = stmts(index)
                    if (stmt.pc == pc) {
                        println(stmt.astID)
                        stmt.astID match {
                            case PutStatic.ASTID | PutField.ASTID ⇒
                                if (method.isInitializer) {
                                    println("field.isStatic "+field.isStatic)
                                    if (referenceHasEscaped(stmt.asPutField.objRef.asVar, stmts, method)) {
                                        return true
                                    }
                                    if (field.isStatic) {
                                    }
                                } else {

                                    if (referenceHasEscaped(stmt.asPutField.objRef.asVar, stmts, method)) {

                                        return true;
                                    }
                                    return false
                                }
                            case _ ⇒ throw new RuntimeException("unexpected field access");
                        }
                    } else {
                        // nothing to do as the put field is dead
                    }
                }
            }
            false
        }

    def methodInfluencesField(
        method: Method,
        taCode: TACode[TACMethodParameter, V],
        pcs:    PCs,
        field:  Field
    ): Boolean = {
        if (method.isConstructor) {
            method.asMethod.descriptor.parameterTypes.foreach(x ⇒ if (x == field.fieldType) return true)
        }
        println("method: "+method.toJava.toString)
        val stmts = taCode.stmts
        for (pc ← pcs) {
            val index = taCode.pcToIndex(pc)
            if (index >= 0) {
                val stmt = stmts(index)
                if (stmt.pc == pc) {
                    println(stmt.astID)
                    stmt.astID match {
                        case PutStatic.ASTID | PutField.ASTID ⇒
                            if (method.isInitializer) {
                                println("field.isStatic "+field.isStatic)
                                if (referenceHasEscaped(stmt.asPutField.objRef.asVar, stmts, method)) {
                                    return false
                                }
                                if (field.isStatic) {
                                }
                            } else {

                                if (referenceHasEscaped(stmt.asPutField.objRef.asVar, stmts, method)) {
                                    return false;
                                }
                                return true
                            }
                        case _ ⇒ throw new RuntimeException("unexpected field access");
                    }
                } else {
                    // nothing to do as the put field is dead
                }
            }
        }
        false
    }

    def referenceHasEscaped(
        reference: V,
        stmts:     Array[Stmt[V]],
        method:    Method
    ): Boolean = {
        reference.definedBy.forall { defSite ⇒
            {
                //TODO
                false
            }
        }
    }

    /**
     * Returns the TACode for a method if available, registering dependencies as necessary.
     */

    def getTACAI(
        method: Method,
        pcs:    PCs
    ): Option[TACode[TACMethodParameter, V]] = {
        propertyStore(method, TACAI.key) match {
            case finalEP: FinalEP[Method, TACAI] ⇒
                finalEP.ub.tac
            case eps: InterimEP[Method, TACAI] ⇒
                eps.ub.tac
            case epk ⇒
                None
        }
    }

}

trait L3FieldMutabilityAnalysisScheduler_new extends FPCFAnalysisScheduler {

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.lub(Purity),
        PropertyBounds.lub(FieldPrematurelyRead),
        PropertyBounds.ub(TACAI),
        PropertyBounds.lub(FieldMutability),
        PropertyBounds.lub(TypeImmutability),
        PropertyBounds.lub(FieldMutability_new),
        PropertyBounds.ub(EscapeProperty)
    )

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldMutability_new)

}

/**
 * Executor for the new field mutability analysis.
 */
object EagerL3FieldMutabilityAnalysis_new
    extends L3FieldMutabilityAnalysisScheduler_new
    with BasicFPCFEagerAnalysisScheduler {

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L3FieldMutabilityAnalysis_new(p)
        val fields = p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldMutability_new)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}

/**
 * Executor for the new lazy field mutability analysis.
 */
object LazyL3FieldMutabilityAnalysis_new
    extends L3FieldMutabilityAnalysisScheduler_new
    with BasicFPCFLazyAnalysisScheduler {

    final override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new L3FieldMutabilityAnalysis_new(p)
        //TODO
        //ps.registerLazyPropertyComputation(
        //FieldMutability_new.key, analysis.determineFieldMutability_new
        //)
        analysis
    }
    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

}
