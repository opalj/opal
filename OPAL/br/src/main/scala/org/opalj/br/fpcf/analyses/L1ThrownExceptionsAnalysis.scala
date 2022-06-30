/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import org.opalj.fpcf.Entity
import org.opalj.fpcf.Result
import org.opalj.br.collection.mutable.{TypesSet => BRMutableTypesSet}
import org.opalj.br.ObjectType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.NonVirtualMethodInvocationInstruction
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.ISTORE_0
import org.opalj.br.instructions.LSTORE_0
import org.opalj.br.instructions.FSTORE_0
import org.opalj.br.instructions.DSTORE_0
import org.opalj.br.instructions.ASTORE_0
import org.opalj.br.instructions.ISTORE
import org.opalj.br.instructions.FSTORE
import org.opalj.br.instructions.LSTORE
import org.opalj.br.instructions.DSTORE
import org.opalj.br.instructions.ASTORE
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.ALOAD_0
import org.opalj.br.instructions.StackManagementInstruction
import org.opalj.br.instructions.MONITOREXIT
import org.opalj.br.instructions.MONITORENTER
import org.opalj.br.instructions.IRETURN
import org.opalj.br.instructions.DRETURN
import org.opalj.br.instructions.LRETURN
import org.opalj.br.instructions.FRETURN
import org.opalj.br.instructions.ARETURN
import org.opalj.br.instructions.RETURN
import org.opalj.br.instructions.IREM
import org.opalj.br.instructions.IDIV
import org.opalj.br.instructions.LDCInt
import org.opalj.br.instructions.LDIV
import org.opalj.br.instructions.LoadLong
import org.opalj.br.instructions.LREM
import org.opalj.br.fpcf.properties.ThrownExceptions
import org.opalj.br.fpcf.properties.ThrownExceptionsFallback
import org.opalj.br.fpcf.properties.ThrownExceptionsByOverridingMethods
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodIsAbstract
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodBodyIsNotAvailable
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodIsNative
import org.opalj.br.fpcf.properties.ThrownExceptions.UnknownExceptionIsThrown
import org.opalj.br.fpcf.properties.ThrownExceptions.AnalysisLimitation
import org.opalj.br.fpcf.properties.ThrownExceptions.UnresolvedInvokeDynamicInstruction
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodCalledThrowsUnknownExceptions
import org.opalj.br.fpcf.properties.ThrownExceptions.SomeException
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.analyses.ProjectInformationKeys

/**
 * Analysis of thrown exceptions; computes the [[org.opalj.br.fpcf.properties.ThrownExceptions]]
 * property.
 *
 * @author Andreas Muttscheller
 */
class L1ThrownExceptionsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    private[analyses] def lazilyDetermineThrownExceptions(
        e: Entity
    ): ProperPropertyComputationResult = {
        e match {
            case m: Method => determineThrownExceptions(m)
            case e         => throw new UnknownError(s"$e is not a method")
        }
    }

    /**
     * Determines the exceptions a method throws. This analysis also follows invocation instructions
     * and adds the exceptions thrown by the called method into its own result.
     * The given method must have a body!
     */
    def determineThrownExceptions(m: Method): ProperPropertyComputationResult = {
        if (m.isNative)
            return Result(m, MethodIsNative);
        if (m.isAbstract)
            return Result(m, MethodIsAbstract);
        val body = m.body
        if (body.isEmpty)
            return Result(m, MethodBodyIsNotAvailable);

        //
        //... when we reach this point the method is non-empty
        //
        val code = body.get
        val cfJoins = code.cfJoins
        val instructions = code.instructions
        val isStaticMethod = m.isStatic

        val initialExceptions = new BRMutableTypesSet(project.classHierarchy)

        var result: ThrownExceptions = null

        var isSynchronizationUsed = false

        var isLocalVariable0Updated = false
        var fieldAccessMayThrowNullPointerException = false
        var isFieldAccessed = false

        var dependees = Set.empty[EOptionP[Entity, Property]]

        /* Implicitly (i.e., as a side effect) collects the thrown exceptions in the exceptions set.
         *
         * @return `true` if it is possible to collect all potentially thrown exceptions.
         */
        def collectAllExceptions(pc: Int, instruction: Instruction): Boolean = {
            instruction.opcode match {

                case ATHROW.opcode =>
                    result = UnknownExceptionIsThrown
                    false

                case INVOKESPECIAL.opcode | INVOKESTATIC.opcode =>
                    val MethodInvocationInstruction(declaringClass, _, name, descriptor) =
                        instruction

                    if ((declaringClass eq ObjectType.Object) && (
                        (name == "<init>" && descriptor == MethodDescriptor.NoArgsAndReturnVoid) ||
                        (name == "hashCode" && descriptor == MethodDescriptor.JustReturnsInteger) ||
                        (name == "equals" &&
                            descriptor == ThrownExceptionsFallback.ObjectEqualsMethodDescriptor) ||
                            (name == "toString" && descriptor == MethodDescriptor.JustReturnsString)
                    )) {
                        true
                    } else {
                        instruction match {
                            case mii: NonVirtualMethodInvocationInstruction =>
                                project.nonVirtualCall(m.classFile.thisType, mii) match {
                                    case Success(`m`) => true // we basically ignore self-dependencies
                                    case Success(callee) =>
                                        // Query the store for information about the callee
                                        ps(callee, ThrownExceptions.key) match {
                                            case UBP(MethodIsAbstract) |
                                                UBP(MethodBodyIsNotAvailable) |
                                                UBP(MethodIsNative) |
                                                UBP(UnknownExceptionIsThrown) |
                                                UBP(AnalysisLimitation) |
                                                UBP(UnresolvedInvokeDynamicInstruction) =>
                                                result = MethodCalledThrowsUnknownExceptions
                                                false
                                            case eps: EPS[Entity, Property] =>
                                                // Copy the concrete exception types to our initial
                                                // exceptions set. Upper type bounds are only used
                                                // for `SomeExecption`, which are handled above, and
                                                // don't have to be added to this set.
                                                initialExceptions ++= eps.ub.types.concreteTypes
                                                if (eps.isRefinable) {
                                                    dependees += eps
                                                }
                                                true
                                            case epk =>
                                                dependees += epk
                                                true
                                        }
                                    case _ =>
                                        result = UnknownExceptionIsThrown
                                        false
                                }
                            case _ =>
                                result = UnknownExceptionIsThrown
                                false
                        }
                    }

                case INVOKEDYNAMIC.opcode =>
                    result = UnresolvedInvokeDynamicInstruction
                    false

                case INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode =>
                    // ThrownExceptionsByOverridingMethods checks if the method is overridable and
                    // returns `SomeException` if that is the case. Otherwise the concrete set of
                    // exceptions is returned.
                    val calleeOption = instruction match {
                        case iv: INVOKEVIRTUAL   => project.resolveMethodReference(iv)
                        case ii: INVOKEINTERFACE => project.resolveInterfaceMethodReference(ii)
                        case _                   => None
                    }
                    calleeOption match {
                        case Some(`m`) => // nothing to do...
                        case Some(callee) =>
                            // Check the class hierarchy for thrown exceptions
                            ps(callee, ThrownExceptionsByOverridingMethods.key) match {
                                case UBP(ThrownExceptionsByOverridingMethods.MethodIsOverridable) =>
                                    result = MethodCalledThrowsUnknownExceptions
                                case UBP(ThrownExceptionsByOverridingMethods.SomeException) =>
                                    result = MethodCalledThrowsUnknownExceptions
                                case eps: EPS[Entity, Property] =>
                                    // Copy the concrete exception types to our initial
                                    // exceptions set. Upper type bounds are only used
                                    // for `SomeExecption`, which are handled above, and
                                    // don't have to be added to this set.
                                    initialExceptions ++= eps.ub.exceptions.concreteTypes
                                    if (eps.isRefinable) {
                                        dependees += eps
                                    }
                                case epk => dependees += epk
                            }
                        case None =>
                            // We have no information about this method.
                            result = AnalysisLimitation
                    }
                    result == null

                // let's determine if the register 0 is updated (i.e., if the register which
                // stores the this reference in case of instance methods is updated)
                case ISTORE_0.opcode | LSTORE_0.opcode |
                    DSTORE_0.opcode | FSTORE_0.opcode |
                    ASTORE_0.opcode =>
                    isLocalVariable0Updated = true
                    true

                case ISTORE.opcode | LSTORE.opcode |
                    FSTORE.opcode | DSTORE.opcode |
                    ASTORE.opcode =>
                    val lvIndex = instruction.indexOfWrittenLocal
                    if (lvIndex == 0) isLocalVariable0Updated = true
                    true

                case GETFIELD.opcode =>
                    isFieldAccessed = true
                    fieldAccessMayThrowNullPointerException ||=
                        isStaticMethod || // <= the receiver is some object
                        isLocalVariable0Updated || // <= we don't know the receiver object at all
                        cfJoins.contains(pc) || // <= we cannot locally decide who is the receiver
                        instructions(code.pcOfPreviousInstruction(pc)) != ALOAD_0 // <= the receiver may be null..
                    true

                case PUTFIELD.opcode =>
                    isFieldAccessed = true
                    fieldAccessMayThrowNullPointerException =
                        fieldAccessMayThrowNullPointerException ||
                            isStaticMethod || // <= the receiver is some object
                            isLocalVariable0Updated || // <= we don't know the receiver object at all
                            cfJoins.contains(pc) || // <= we cannot locally decide who is the receiver
                            {
                                val predecessorPC = code.pcOfPreviousInstruction(pc)
                                val predecessorOfPredecessorPC =
                                    code.pcOfPreviousInstruction(predecessorPC)
                                val valueInstruction = instructions(predecessorPC)

                                instructions(predecessorOfPredecessorPC) != ALOAD_0 || // <= the receiver may be null..
                                    valueInstruction.isInstanceOf[StackManagementInstruction] ||
                                    // we have to ensure that our "this" reference is not used for something else... =>
                                    valueInstruction.numberOfPoppedOperands(NotRequired) > 0
                                // the number of pushed operands is always equal or smaller than 1
                                // except of the stack management instructions
                            }
                    true

                case MONITORENTER.opcode | MONITOREXIT.opcode =>
                    initialExceptions ++= instruction.jvmExceptions
                    isSynchronizationUsed = true
                    true
                case IRETURN.opcode | LRETURN.opcode |
                    FRETURN.opcode | DRETURN.opcode |
                    ARETURN.opcode | RETURN.opcode =>
                    // let's forget about the IllegalMonitorStateException for now unless we have
                    // a MONITORENTER/MONITOREXIT instruction
                    true

                case IREM.opcode | IDIV.opcode =>
                    if (!cfJoins.contains(pc)) {
                        val predecessorPC = code.pcOfPreviousInstruction(pc)
                        val valueInstruction = instructions(predecessorPC)
                        valueInstruction match {
                            case LDCInt(value) if value != 0 =>
                                // there will be no arithmetic exception
                                true
                            case _ =>
                                initialExceptions ++= instruction.jvmExceptions
                                true
                        }
                    } else {
                        initialExceptions ++= instruction.jvmExceptions
                        true
                    }

                case LREM.opcode | LDIV.opcode =>
                    if (!cfJoins.contains(pc)) {
                        val predecessorPC = code.pcOfPreviousInstruction(pc)
                        val valueInstruction = instructions(predecessorPC)
                        valueInstruction match {
                            case LoadLong(value) if value != 0L =>
                                // there will be no arithmetic exception
                                true
                            case _ =>
                                initialExceptions ++= instruction.jvmExceptions
                                true
                        }
                    } else {
                        initialExceptions ++= instruction.jvmExceptions
                        true
                    }

                case _ /* all other instructions */ =>
                    initialExceptions ++= instruction.jvmExceptions
                    true
            }
        }

        val areAllExceptionsCollected = code.forall(collectAllExceptions(_, _))

        if (!areAllExceptionsCollected) {
            assert(
                result ne null,
                "all exceptions are expected to be collected but the set is null"
            )
            return Result(m, result);
        }
        if (fieldAccessMayThrowNullPointerException ||
            (isFieldAccessed && isLocalVariable0Updated)) {
            initialExceptions += ObjectType.NullPointerException
        }
        if (isSynchronizationUsed) {
            initialExceptions += ObjectType.IllegalMonitorStateException
        }

        var exceptions = initialExceptions.toImmutableTypesSet

        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            dependees = dependees.filter { d =>
                d.e != eps.e || d.pk != eps.pk
            }
            // If the property is not final we want to keep updated of new values
            if (eps.isRefinable) {
                dependees = dependees + eps
            }
            eps.ub match {
                // Properties from ThrownExceptions.Key
                // They are queried if we got a static or special invokation instruction

                // Check if we got some unknown exceptions. We can terminate the analysis if
                // that's the case as we cannot compute a more precise result.
                case MethodIsAbstract |
                    MethodBodyIsNotAvailable |
                    MethodIsNative |
                    UnknownExceptionIsThrown |
                    AnalysisLimitation |
                    UnresolvedInvokeDynamicInstruction =>
                    return Result(m, MethodCalledThrowsUnknownExceptions);

                case te: ThrownExceptions =>
                    exceptions = exceptions ++ te.types.concreteTypes

                // Properties from ThrownExceptionsByOverridingMethods
                case ThrownExceptionsByOverridingMethods.SomeException |
                    ThrownExceptionsByOverridingMethods.MethodIsOverridable =>
                    return Result(m, MethodCalledThrowsUnknownExceptions);

                case tebom: ThrownExceptionsByOverridingMethods =>
                    exceptions = exceptions ++ tebom.exceptions.concreteTypes
            }
            if (dependees.isEmpty) {
                Result(m, new ThrownExceptions(exceptions))
            } else {
                InterimResult(m, SomeException, new ThrownExceptions(exceptions), dependees, c)
            }
        }

        if (dependees.isEmpty) {
            Result(m, new ThrownExceptions(exceptions))
        } else {
            InterimResult(m, SomeException, new ThrownExceptions(exceptions), dependees, c)
        }
    }
}

abstract class ThrownExceptionsAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    final override def uses: Set[PropertyBounds] = {
        Set(PropertyBounds.lub(ThrownExceptionsByOverridingMethods))
    }

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(ThrownExceptions)

}

/**
 * Factory and runner for the [[L1ThrownExceptionsAnalysis]].
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object EagerL1ThrownExceptionsAnalysis
    extends ThrownExceptionsAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    /**
     * Eagerly schedules the computation of the thrown exceptions for all methods with bodies;
     * in general, the analysis is expected to be registered as a lazy computation.
     */
    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L1ThrownExceptionsAnalysis(p)
        val allMethods = p.allMethods
        ps.scheduleEagerComputationsForEntities(allMethods)(analysis.determineThrownExceptions)
        analysis
    }
}

/**
 * Factory and runner for the [[L1ThrownExceptionsAnalysis]].
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object LazyL1ThrownExceptionsAnalysis
    extends ThrownExceptionsAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    /** Registers an analysis to compute the thrown exceptions lazily. */
    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L1ThrownExceptionsAnalysis(p)
        ps.registerLazyPropertyComputation(
            ThrownExceptions.key,
            analysis.lazilyDetermineThrownExceptions
        )
        analysis
    }
}
