/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import scala.annotation.switch
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
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
import org.opalj.fpcf.SomeInterimEP
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.AllocationFreeMethod
import org.opalj.br.fpcf.properties.AllocationFreeness
import org.opalj.br.fpcf.properties.MethodWithAllocations
import org.opalj.br.instructions._

/**
 * A simple analysis that identifies methods that never allocate any objects/arrays.
 *
 * @author Dominik Helm
 */
class L0AllocationFreenessAnalysis private[analyses] (
        final val project: SomeProject
)
    extends FPCFAnalysis {

    import project.nonVirtualCall

    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Retrieves and commits the methods allocation freeness as calculated for its declaring class
     * type for the current DefinedMethod that represents the non-overwritten method in a subtype.
     */
    def baseMethodAllocationFreeness(dm: DefinedMethod): ProperPropertyComputationResult = {

        def c(eps: SomeEOptionP): ProperPropertyComputationResult = eps match {
            case FinalP(af) => Result(dm, af)
            case ep @ InterimLUBP(lb, ub) =>
                InterimResult(dm, lb, ub, Set(ep), c)
            case epk =>
                InterimResult(dm, MethodWithAllocations, AllocationFreeMethod, Set(epk), c)
        }

        c(propertyStore(declaredMethods(dm.definedMethod), AllocationFreeness.key))
    }

    /**
     * Determines the allocation freeness of the method.
     *
     * This function encapsulates the continuation.
     */
    def determineAllocationFreeness(
        definedMethod: DefinedMethod
    ): ProperPropertyComputationResult = {

        if (definedMethod.definedMethod.body.isEmpty)
            return Result(definedMethod, MethodWithAllocations);

        val method = definedMethod.definedMethod
        val declaringClassType = method.classFile.thisType

        // If thhis is not the method's declaration, but a non-overwritten method in a subtype,
        // don't re-analyze the code
        if (declaringClassType ne definedMethod.declaringClassType)
            return baseMethodAllocationFreeness(definedMethod.asDefinedMethod);

        // Synchronized methods may raise IllegalMonitorStateExceptions when invoked.
        if (method.isSynchronized)
            return Result(definedMethod, MethodWithAllocations);

        val methodDescriptor = method.descriptor
        val methodName = method.name
        val body = method.body.get
        val instructions = body.instructions
        val maxPC = instructions.length

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty

        var overwritesSelf = false
        var mayOverwriteSelf = true

        def prevPC(pc: Int): Int = {
            body.pcOfPreviousInstruction(pc)
        }

        // We need this for numberOfPoppedOperands, but the actual result is irrelevant as we care
        // only for whether ANY operand is popped, not how many exactly.
        def someTypeCategory(i: Int): ComputationalTypeCategory = Category2ComputationalTypeCategory

        var currentPC = 0
        while (currentPC < maxPC) {
            val instruction = instructions(currentPC)
            (instruction.opcode: @switch) match {
                case NEW.opcode | NEWARRAY.opcode | MULTIANEWARRAY.opcode | ANEWARRAY.opcode =>
                    return Result(definedMethod, MethodWithAllocations);

                case INVOKESPECIAL.opcode | INVOKESTATIC.opcode => instruction match {
                    case MethodInvocationInstruction(`declaringClassType`, _, `methodName`, `methodDescriptor`) =>
                    // We have a self-recursive call; such calls do not influence the allocation
                    // freeness and are ignored.
                    // Let's continue with the evaluation of the next instruction.

                    case mii: NonVirtualMethodInvocationInstruction =>
                        nonVirtualCall(declaringClassType, mii) match {
                            case Success(callee) =>
                                /* Recall that self-recursive calls are handled earlier! */
                                val allocationFreeness =
                                    propertyStore(declaredMethods(callee), AllocationFreeness.key)

                                allocationFreeness match {
                                    case FinalP(AllocationFreeMethod) => /* Nothing to do */

                                    // Handling cyclic computations
                                    case ep @ InterimUBP(AllocationFreeMethod) =>
                                        dependees += ep

                                    case _: SomeEPS =>
                                        return Result(definedMethod, MethodWithAllocations);

                                    case epk =>
                                        dependees += epk
                                }

                            case _ /* Empty or Failure */ =>
                                // We know nothing about the target method (it is not
                                // found in the scope of the current project).
                                return Result(definedMethod, MethodWithAllocations);

                        }
                }

                case ASTORE_0.opcode if !method.isStatic =>
                    if (mayOverwriteSelf) overwritesSelf = true
                    else // A GETFIELD/PUTFIELD may result in a NPE raised (and therefore allocated)
                        return Result(definedMethod, MethodWithAllocations)

                case GETFIELD.opcode => // may allocate NPE (but not on `this`)
                    if (method.isStatic || overwritesSelf)
                        return Result(definedMethod, MethodWithAllocations);
                    else if (instructions(prevPC(currentPC)).opcode != ALOAD_0.opcode ||
                        body.cfJoins.contains(currentPC))
                        return Result(definedMethod, MethodWithAllocations);
                    else mayOverwriteSelf = false

                case PUTFIELD.opcode => // may allocate NPE (but not on `this`)
                    if (method.isStatic || overwritesSelf)
                        return Result(definedMethod, MethodWithAllocations);
                    else {
                        val previousPC = prevPC(currentPC)
                        val previousInst = instructions(previousPC)
                        val prevPrevInst = instructions(prevPC(previousPC))
                        // If there is a branch target here, if the previous instruction pops an
                        // operand, or if the second last instruction is not an ALOAD_0, we
                        // cannot guarantee that the receiver is `this`.
                        if (body.cfJoins.contains(currentPC) || body.cfJoins.contains(previousPC) ||
                            previousInst.numberOfPoppedOperands(someTypeCategory) != 0 ||
                            prevPrevInst.opcode != ALOAD_0.opcode)
                            return Result(definedMethod, MethodWithAllocations)
                        else mayOverwriteSelf = false
                    }

                case INVOKEDYNAMIC.opcode | INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode =>
                    // We don't handle these calls here, just treat them as having allocations
                    return Result(definedMethod, MethodWithAllocations);

                case ARETURN.opcode | IRETURN.opcode | FRETURN.opcode | DRETURN.opcode |
                    LRETURN.opcode | RETURN.opcode =>
                // if we have a monitor instruction the method has allocations anyway..
                // hence, we can ignore the monitor related implicit exception

                case _ =>
                    // All other instructions (IFs, Load/Stores, Arith., etc.) allocate no objects
                    // as long as no implicit exceptions are raised.
                    if (instruction.jvmExceptions.nonEmpty) {
                        // JVM Exceptions result in the exception object being allocated.
                        return Result(definedMethod, MethodWithAllocations);
                    }
            }
            currentPC = body.pcOfNextInstruction(currentPC)
        }

        if (dependees.isEmpty)
            return Result(definedMethod, AllocationFreeMethod);

        // This function computes the “allocation freeness for a method based on the allocation
        // freeness of its callees
        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            // Let's filter the entity.
            dependees = dependees.filter(_.e ne eps.e)

            (eps: @unchecked) match {
                case _: SomeInterimEP =>
                    dependees += eps
                    InterimResult(
                        definedMethod,
                        MethodWithAllocations,
                        AllocationFreeMethod,
                        dependees,
                        c
                    )

                case FinalP(AllocationFreeMethod) =>
                    if (dependees.isEmpty)
                        Result(definedMethod, AllocationFreeMethod)
                    else {
                        InterimResult(
                            definedMethod,
                            MethodWithAllocations,
                            AllocationFreeMethod,
                            dependees,
                            c
                        )
                    }

                case FinalP(MethodWithAllocations) =>
                    Result(definedMethod, MethodWithAllocations)

            }
        }

        InterimResult(
            definedMethod, MethodWithAllocations, AllocationFreeMethod,
            dependees, c
        )
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineAllocationFreeness(e: Entity): ProperPropertyComputationResult = {
        e match {
            case m: DefinedMethod  => determineAllocationFreeness(m)
            case m: DeclaredMethod => Result(m, MethodWithAllocations)
            case _                 => throw new UnknownError(s"$e is not a method")
        }
    }
}

trait L0AllocationFreenessAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    final override def uses: Set[PropertyBounds] = Set.empty

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(AllocationFreeness)

}

object EagerL0AllocationFreenessAnalysis
    extends L0AllocationFreenessAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0AllocationFreenessAnalysis(p)
        val declaredMethods = p.get(DeclaredMethodsKey).declaredMethods.iterator.collect {
            case dm if dm.hasSingleDefinedMethod && dm.definedMethod.body.isDefined => dm.asDefinedMethod
        }
        ps.scheduleEagerComputationsForEntities(declaredMethods)(analysis.determineAllocationFreeness)
        analysis
    }
}

object LazyL0AllocationFreenessAnalysis
    extends L0AllocationFreenessAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0AllocationFreenessAnalysis(p)
        ps.registerLazyPropertyComputation(
            AllocationFreeness.key,
            analysis.doDetermineAllocationFreeness
        )
        analysis
    }
}
