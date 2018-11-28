/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.instructions._
import org.opalj.fpcf.properties.StaticDataUsage
import org.opalj.fpcf.properties.CompileTimeConstancy
import org.opalj.fpcf.properties.UsesVaryingData
import org.opalj.fpcf.properties.UsesNoStaticData
import org.opalj.fpcf.properties.UsesConstantDataOnly
import org.opalj.fpcf.properties.NoVaryingDataUse
import org.opalj.fpcf.properties.CompileTimeConstantField
import org.opalj.fpcf.properties.CompileTimeVaryingField

import scala.annotation.switch

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
    def baseMethodStaticDataUsage(dm: DefinedMethod): PropertyComputationResult = {

        def c(eps: SomeEOptionP): PropertyComputationResult = eps match {
            case FinalP(_, sdu) ⇒ Result(dm, sdu)
            case ep @ InterimP(_, lb, ub) ⇒
                InterimResult(
                    dm, lb, ub,
                    Seq(ep), c, CheapPropertyComputation
                )
            case epk ⇒
                InterimResult(
                    dm, UsesVaryingData, UsesNoStaticData,
                    Seq(epk), c, CheapPropertyComputation
                )
        }

        c(propertyStore(declaredMethods(dm.definedMethod), StaticDataUsage.key))
    }

    /**
     * Determines the allocation freeness of the method.
     *
     * This function encapsulates the continuation.
     */
    def determineUsage(definedMethod: DefinedMethod): PropertyComputationResult = {

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
                case GETSTATIC.opcode ⇒
                    val GETSTATIC(declaringClass, fieldName, fieldType) = instruction

                    maxLevel = UsesConstantDataOnly

                    resolveFieldReference(declaringClass, fieldName, fieldType) match {

                        // ... we have no support for arrays at the moment
                        case Some(field) ⇒
                            propertyStore(field, CompileTimeConstancy.key) match {
                                case FinalP(_, CompileTimeConstantField) ⇒
                                case FinalP(_, _) ⇒
                                    return Result(definedMethod, UsesVaryingData);
                                case ep ⇒
                                    dependees += ep
                            }

                        case _ ⇒
                            // We know nothing about the target field (it is not
                            // found in the scope of the current project).
                            return Result(definedMethod, UsesVaryingData);
                    }

                case INVOKESPECIAL.opcode | INVOKESTATIC.opcode ⇒ instruction match {
                    case MethodInvocationInstruction(`declaringClassType`, _, `methodName`, `methodDescriptor`) ⇒
                    // We have a self-recursive call; such calls do not influence the allocation
                    // freeness and are ignored.
                    // Let's continue with the evaluation of the next instruction.

                    case mii: NonVirtualMethodInvocationInstruction ⇒
                        nonVirtualCall(declaringClassType, mii) match {
                            case Success(callee) ⇒
                                /* Recall that self-recursive calls are handled earlier! */
                                val constantUsage =
                                    propertyStore(declaredMethods(callee), StaticDataUsage.key)

                                constantUsage match {
                                    case FinalP(_, UsesNoStaticData) ⇒ /* Nothing to do */

                                    case FinalP(_, UsesConstantDataOnly) ⇒
                                        maxLevel = UsesConstantDataOnly

                                    // Handling cyclic computations
                                    case ep @ InterimP(_, _, _: NoVaryingDataUse) ⇒
                                        dependees += ep

                                    case EPS(_, _, _) ⇒
                                        return Result(definedMethod, UsesVaryingData);

                                    case epk ⇒
                                        dependees += epk
                                }

                            case _ /* Empty or Failure */ ⇒
                                // We know nothing about the target method (it is not
                                // found in the scope of the current project).
                                return Result(definedMethod, UsesVaryingData);

                        }
                }

                case INVOKEDYNAMIC.opcode | INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode ⇒
                    // We don't handle these calls here, just treat them as having allocations
                    return Result(definedMethod, UsesVaryingData);

                case _ ⇒
                // Other instructions (IFs, Load/Stores, Arith., etc.) do not use static data
            }
            currentPC = body.pcOfNextInstruction(currentPC)
        }

        if (dependees.isEmpty)
            return Result(definedMethod, maxLevel);

        // This function computes the “static data usage" for a method based on the usage of its
        // callees and the compile-time constancy of its static field reads
        def c(eps: SomeEPS): PropertyComputationResult = {
            // Let's filter the entity.
            dependees = dependees.filter(_.e ne eps.e)

            eps match {
                case FinalP(_, du: NoVaryingDataUse) ⇒
                    if (du eq UsesConstantDataOnly) maxLevel = UsesConstantDataOnly
                    if (dependees.isEmpty)
                        Result(definedMethod, maxLevel)
                    else {
                        InterimResult(
                            definedMethod, UsesVaryingData, maxLevel,
                            dependees, c, CheapPropertyComputation
                        )
                    }

                case FinalP(_, UsesVaryingData) ⇒ Result(definedMethod, UsesVaryingData)

                case FinalP(_, CompileTimeConstantField) ⇒
                    if (dependees.isEmpty)
                        Result(definedMethod, maxLevel)
                    else {
                        InterimResult(
                            definedMethod, UsesVaryingData, maxLevel,
                            dependees, c, CheapPropertyComputation
                        )
                    }

                case FinalP(_, CompileTimeVaryingField) ⇒ Result(definedMethod, UsesVaryingData)

                case InterimP(_, _, UsesConstantDataOnly) ⇒
                    maxLevel = UsesConstantDataOnly
                    dependees += eps
                    InterimResult(
                        definedMethod, UsesVaryingData, maxLevel,
                        dependees, c, CheapPropertyComputation
                    )

                case InterimP(_, _, _) ⇒
                    dependees += eps
                    InterimResult(
                        definedMethod, UsesVaryingData, maxLevel,
                        dependees, c, CheapPropertyComputation
                    )
            }
        }

        InterimResult(
            definedMethod, UsesVaryingData, maxLevel,
            dependees, c, CheapPropertyComputation
        )
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineUsage(e: Entity): PropertyComputationResult = {
        e match {
            case m: DefinedMethod  ⇒ determineUsage(m)
            case m: DeclaredMethod ⇒ Result(m, UsesVaryingData)
            case _ ⇒
                throw new UnknownError("static constant usage is only defined for methods")
        }
    }
}

trait StaticDataUsageAnalysisScheduler extends ComputationSpecification {

    final override def derives: Set[PropertyKind] = Set(StaticDataUsage)

    final override def uses: Set[PropertyKind] = Set(CompileTimeConstancy)

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}

object EagerStaticDataUsageAnalysis
    extends StaticDataUsageAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new StaticDataUsageAnalysis(p)
        val declaredMethods = p.get(DeclaredMethodsKey).declaredMethods.collect {
            case dm if dm.hasSingleDefinedMethod && dm.definedMethod.body.isDefined ⇒ dm.asDefinedMethod
        }
        ps.scheduleEagerComputationsForEntities(declaredMethods)(analysis.determineUsage)
        analysis
    }
}

object LazyStaticDataUsageAnalysis
    extends StaticDataUsageAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    override def startLazily(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): FPCFAnalysis = {
        val analysis = new StaticDataUsageAnalysis(p)
        ps.registerLazyPropertyComputation(StaticDataUsage.key, analysis.doDetermineUsage)
        analysis
    }
}
