/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import scala.annotation.switch

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.CompileTimeConstancy
import org.opalj.br.fpcf.properties.CompileTimeConstantField
import org.opalj.br.fpcf.properties.CompileTimeVaryingField
import org.opalj.br.fpcf.properties.NoVaryingDataUse
import org.opalj.br.fpcf.properties.StaticDataUsage
import org.opalj.br.fpcf.properties.UsesConstantDataOnly
import org.opalj.br.fpcf.properties.UsesNoStaticData
import org.opalj.br.fpcf.properties.UsesVaryingData
import org.opalj.br.instructions._

/**
 * A simple analysis that identifies methods that use global state that may vary during one or
 * between several program executions.
 *
 * @author Dominik Helm
 */
class StaticDataUsageAnalysis private[analyses] ( final val project: SomeProject)
    extends FPCFAnalysis {

    import project.nonVirtualCall
    import project.resolveFieldReference

    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Retrieves and commits the methods static data usage as calculated for its declaring class
     * type for the current DefinedMethod that represents the non-overwritten method in a subtype.
     */
    def baseMethodStaticDataUsage(dm: DefinedMethod): ProperPropertyComputationResult = {

        def c(eps: SomeEOptionP): ProperPropertyComputationResult = eps match {
            case FinalP(sdu) => Result(dm, sdu)
            case ep @ InterimLUBP(lb, ub) =>
                InterimResult(dm, lb, ub, Set(ep), c)
            case epk =>
                InterimResult(dm, UsesVaryingData, UsesNoStaticData, Set(epk), c)
        }

        c(propertyStore(declaredMethods(dm.definedMethod), StaticDataUsage.key))
    }

    /**
     * Determines the allocation freeness of the method.
     *
     * This function encapsulates the continuation.
     */
    def determineUsage(definedMethod: DefinedMethod): ProperPropertyComputationResult = {

        if (definedMethod.definedMethod.body.isEmpty)
            return Result(definedMethod, UsesVaryingData);

        val method = definedMethod.definedMethod
        val declaringClassType = method.classFile.thisType

        // If thhis is not the method's declaration, but a non-overwritten method in a subtype,
        // don't re-analyze the code
        if (declaringClassType ne definedMethod.declaringClassType)
            return baseMethodStaticDataUsage(definedMethod.asDefinedMethod);

        val methodDescriptor = method.descriptor
        val methodName = method.name
        val body = method.body.get
        val instructions = body.instructions
        val maxPC = instructions.length

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty
        var maxLevel: StaticDataUsage = UsesNoStaticData

        var currentPC = 0
        while (currentPC < maxPC) {
            val instruction = instructions(currentPC)
            (instruction.opcode: @switch) match {
                case GETSTATIC.opcode =>
                    val GETSTATIC(declaringClass, fieldName, fieldType) = instruction

                    maxLevel = UsesConstantDataOnly

                    resolveFieldReference(declaringClass, fieldName, fieldType) match {

                        // ... we have no support for arrays at the moment
                        case Some(field) =>
                            propertyStore(field, CompileTimeConstancy.key) match {
                                case FinalP(CompileTimeConstantField) =>
                                case _: FinalEP[_, _] =>
                                    return Result(definedMethod, UsesVaryingData);
                                case ep =>
                                    dependees += ep
                            }

                        case _ =>
                            // We know nothing about the target field (it is not
                            // found in the scope of the current project).
                            return Result(definedMethod, UsesVaryingData);
                    }

                case INVOKESPECIAL.opcode | INVOKESTATIC.opcode => instruction match {
                    case MethodInvocationInstruction(`declaringClassType`, _, `methodName`, `methodDescriptor`) =>
                    // We have a self-recursive call; such calls do not influence the allocation
                    // freeness and are ignored.
                    // Let's continue with the evaluation of the next instruction.

                    case mii: NonVirtualMethodInvocationInstruction =>
                        nonVirtualCall(declaringClassType, mii) match {
                            case Success(callee) =>
                                /* Recall that self-recursive calls are handled earlier! */
                                val constantUsage =
                                    propertyStore(declaredMethods(callee), StaticDataUsage.key)

                                constantUsage match {
                                    case FinalP(UsesNoStaticData) => /* Nothing to do */

                                    case FinalP(UsesConstantDataOnly) =>
                                        maxLevel = UsesConstantDataOnly

                                    // Handling cyclic computations
                                    case ep @ InterimUBP(_: NoVaryingDataUse) =>
                                        dependees += ep

                                    case _: EPS[_, _] =>
                                        return Result(definedMethod, UsesVaryingData);

                                    case epk =>
                                        dependees += epk
                                }

                            case _ /* Empty or Failure */ =>
                                // We know nothing about the target method (it is not
                                // found in the scope of the current project).
                                return Result(definedMethod, UsesVaryingData);

                        }
                }

                case INVOKEDYNAMIC.opcode | INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode =>
                    // We don't handle these calls here, just treat them as having allocations
                    return Result(definedMethod, UsesVaryingData);

                case _ =>
                // Other instructions (IFs, Load/Stores, Arith., etc.) do not use static data
            }
            currentPC = body.pcOfNextInstruction(currentPC)
        }

        if (dependees.isEmpty)
            return Result(definedMethod, maxLevel);

        // This function computes the “static data usage" for a method based on the usage of its
        // callees and the compile-time constancy of its static field reads
        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            // Let's filter the entity.
            dependees = dependees.filter(_.e ne eps.e)

            (eps: @unchecked) match {
                case FinalP(du: NoVaryingDataUse) =>
                    if (du eq UsesConstantDataOnly) maxLevel = UsesConstantDataOnly
                    if (dependees.isEmpty)
                        Result(definedMethod, maxLevel)
                    else {
                        InterimResult(
                            definedMethod, UsesVaryingData, maxLevel,
                            dependees, c
                        )
                    }

                case FinalP(UsesVaryingData) => Result(definedMethod, UsesVaryingData)

                case FinalP(CompileTimeConstantField) =>
                    if (dependees.isEmpty)
                        Result(definedMethod, maxLevel)
                    else {
                        InterimResult(
                            definedMethod, UsesVaryingData, maxLevel,
                            dependees, c
                        )
                    }

                case FinalP(CompileTimeVaryingField) => Result(definedMethod, UsesVaryingData)

                case InterimUBP(UsesConstantDataOnly) =>
                    maxLevel = UsesConstantDataOnly
                    dependees += eps
                    InterimResult(definedMethod, UsesVaryingData, maxLevel, dependees, c)

                case _: InterimEP[_, _] =>
                    dependees += eps
                    InterimResult(definedMethod, UsesVaryingData, maxLevel, dependees, c)
            }
        }

        InterimResult(definedMethod, UsesVaryingData, maxLevel, dependees, c)
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineUsage(e: Entity): ProperPropertyComputationResult = {
        e match {
            case m: DefinedMethod  => determineUsage(m)
            case m: DeclaredMethod => Result(m, UsesVaryingData)
            case _                 => throw new UnknownError(s"$e is not a method")
        }
    }
}

trait StaticDataUsageAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    final def derivedProperty: PropertyBounds = {
        // FIXME Just seems to derive the upper bound...
        PropertyBounds.lub(StaticDataUsage)
    }

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.lub(StaticDataUsage),
        PropertyBounds.lub(CompileTimeConstancy)
    )

}

object EagerStaticDataUsageAnalysis
    extends StaticDataUsageAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new StaticDataUsageAnalysis(p)
        val declaredMethods = p.get(DeclaredMethodsKey).declaredMethods.collect {
            case dm if dm.hasSingleDefinedMethod && dm.definedMethod.body.isDefined => dm.asDefinedMethod
        }
        ps.scheduleEagerComputationsForEntities(declaredMethods)(analysis.determineUsage)
        analysis
    }
}

object LazyStaticDataUsageAnalysis
    extends StaticDataUsageAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new StaticDataUsageAnalysis(p)
        ps.registerLazyPropertyComputation(StaticDataUsage.key, analysis.doDetermineUsage)
        analysis
    }
}
