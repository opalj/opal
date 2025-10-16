/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import org.opalj.br.ClassType
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.collection.mutable.TypesSet as BRMutableTypesSet
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.ThrownExceptions
import org.opalj.br.fpcf.properties.ThrownExceptions.AnalysisLimitation
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodBodyIsNotAvailable
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodCalledThrowsUnknownExceptions
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodIsAbstract
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodIsNative
import org.opalj.br.fpcf.properties.ThrownExceptions.SomeException
import org.opalj.br.fpcf.properties.ThrownExceptions.UnknownExceptionIsThrown
import org.opalj.br.fpcf.properties.ThrownExceptions.UnresolvedInvokeDynamicInstruction
import org.opalj.br.fpcf.properties.ThrownExceptionsFallback
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.instructions.ALOAD_0
import org.opalj.br.instructions.ARETURN
import org.opalj.br.instructions.ASTORE
import org.opalj.br.instructions.ASTORE_0
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.DRETURN
import org.opalj.br.instructions.DSTORE
import org.opalj.br.instructions.DSTORE_0
import org.opalj.br.instructions.FRETURN
import org.opalj.br.instructions.FSTORE
import org.opalj.br.instructions.FSTORE_0
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.IDIV
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.IREM
import org.opalj.br.instructions.IRETURN
import org.opalj.br.instructions.ISTORE
import org.opalj.br.instructions.ISTORE_0
import org.opalj.br.instructions.LDCInt
import org.opalj.br.instructions.LDIV
import org.opalj.br.instructions.LoadLong
import org.opalj.br.instructions.LREM
import org.opalj.br.instructions.LRETURN
import org.opalj.br.instructions.LSTORE
import org.opalj.br.instructions.LSTORE_0
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.MONITORENTER
import org.opalj.br.instructions.MONITOREXIT
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.RETURN
import org.opalj.br.instructions.StackManagementInstruction
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP

/**
 * Analysis of thrown exceptions; computes the [[org.opalj.br.fpcf.properties.ThrownExceptions]]
 * property.
 *
 * @author Andreas Muttscheller
 */
class L1ThrownExceptionsAnalysis(
    final val project: SomeProject
) extends FPCFAnalysis {

    protected implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)

    private[analyses] def lazilyDetermineThrownExceptions(
        e: Entity
    ): ProperPropertyComputationResult = {
        e match {
            case c: Context => determineThrownExceptions(c)
            case e          => throw new UnknownError(s"$e is not a context")
        }
    }

    /**
     * Determines the exceptions a method throws. This analysis also follows invocation instructions
     * and adds the exceptions thrown by the called method into its own result.
     * The given method must have a body!
     */
    def determineThrownExceptions(context: Context): ProperPropertyComputationResult = {
        val dm = context.method
        if (!dm.hasSingleDefinedMethod)
            return Result(context, MethodBodyIsNotAvailable);

        val m = dm.definedMethod
        if (m.isNative)
            return Result(context, MethodIsNative);
        if (m.isAbstract)
            return Result(context, MethodIsAbstract);
        val body = m.body
        if (body.isEmpty)
            return Result(context, MethodBodyIsNotAvailable);

        //
        // ... when we reach this point the method is non-empty
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

        var callPCs = IntTrieSet.empty
        lazy val callees = propertyStore(context.method, Callees.key)

        def handleCall(callees: EOptionP[DeclaredMethod, Callees], pc: Int): Unit = {
            if (callees.hasUBP) {
                if (callees.ub.hasIncompleteCallSites(context))
                    result = MethodCalledThrowsUnknownExceptions
                else
                    callees.ub.callees(context, pc).foreach { callee =>
                        if (callee != context)
                            ps(callee, ThrownExceptions.key) match {
                                case UBP(MethodIsAbstract |
                                    MethodBodyIsNotAvailable |
                                    MethodIsNative |
                                    UnknownExceptionIsThrown |
                                    AnalysisLimitation |
                                    UnresolvedInvokeDynamicInstruction) =>
                                    result = MethodCalledThrowsUnknownExceptions

                                case eps @ UBP(te: ThrownExceptions) =>
                                    // Copy the concrete exception types to our initial
                                    // exceptions set. Upper type bounds are only used
                                    // for `SomeExecption`, which are handled above, and
                                    // don't have to be added to this set.
                                    initialExceptions ++= te.types.concreteTypes
                                    if (eps.isRefinable) {
                                        dependees += eps
                                    }

                                case epk => dependees += epk
                            }
                    }
            }
            if (callees.isRefinable) {
                dependees += callees
                callPCs += pc
            }
        }

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
                    val MethodInvocationInstruction(declaringClass, _, name, descriptor) = instruction: @unchecked

                    if ((declaringClass eq ClassType.Object) && (
                            (name == "<init>" && descriptor == MethodDescriptor.NoArgsAndReturnVoid) ||
                            (name == "hashCode" && descriptor == MethodDescriptor.JustReturnsInteger) ||
                            (name == "equals" &&
                            descriptor == ThrownExceptionsFallback.ObjectEqualsMethodDescriptor) ||
                            (name == "toString" && descriptor == MethodDescriptor.JustReturnsString)
                        )
                    ) {
                        true
                    } else {
                        handleCall(callees, pc)
                        result == null
                    }

                case INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode =>
                    handleCall(callees, pc)
                    result == null

                case INVOKEDYNAMIC.opcode =>
                    result = UnresolvedInvokeDynamicInstruction
                    false

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
            return Result(context, result);
        }
        if (fieldAccessMayThrowNullPointerException ||
            (isFieldAccessed && isLocalVariable0Updated)
        ) {
            initialExceptions += ClassType.NullPointerException
        }
        if (isSynchronizationUsed) {
            initialExceptions += ClassType.IllegalMonitorStateException
        }

        var exceptions = initialExceptions.toImmutableTypesSet

        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            dependees = dependees.filter { d => d.e != eps.e || d.pk != eps.pk }
            // If the property is not final we want to keep updated of new values
            if (eps.isRefinable) {
                dependees = dependees + eps
            }
            eps.ub match {
                // Properties from ThrownExceptions.Key

                // Check if we got some unknown exceptions. We can terminate the analysis if
                // that's the case as we cannot compute a more precise result.
                case MethodIsAbstract |
                    MethodBodyIsNotAvailable |
                    MethodIsNative |
                    UnknownExceptionIsThrown |
                    AnalysisLimitation |
                    UnresolvedInvokeDynamicInstruction =>
                    return Result(context, MethodCalledThrowsUnknownExceptions);

                case te: ThrownExceptions =>
                    exceptions = exceptions ++ te.types.concreteTypes

                case _: Callees =>
                    callPCs.foreach(handleCall(eps.asInstanceOf[EPS[DeclaredMethod, Callees]], _))
                    if (result != null)
                        return Result(context, result);
            }
            if (dependees.isEmpty) {
                Result(context, new ThrownExceptions(exceptions))
            } else {
                InterimResult(context, SomeException, new ThrownExceptions(exceptions), dependees, c)
            }
        }

        if (dependees.isEmpty) {
            Result(context, new ThrownExceptions(exceptions))
        } else {
            InterimResult(context, SomeException, new ThrownExceptions(exceptions), dependees, c)
        }
    }
}

abstract class ThrownExceptionsAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    override final def uses: Set[PropertyBounds] = PropertyBounds.ubs(ThrownExceptions, Callees)

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(ThrownExceptions)

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
