/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability
package part

import org.opalj.br.DeclaredMethod
import org.opalj.br.PC
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.cg.uVarForDefSites

/**
 * Determines the assignability of a field based on whether it is possible for a path to exist from a read to a write
 * of the same field on the same instance (or class for static fields).
 *
 * This part will always return a value, and will soundly abort if it cannot guarantee that no such path exists. This
 * may be the case e.g. if a path from some read of the field to the write exists, but the analysis cannot prove that
 * the two instances are disjoint.
 *
 * @author Maximilian Rüsch
 */
sealed trait ReadWritePathAnalysisPart private[fieldassignability]
    extends FieldAssignabilityAnalysisPart {

    registerPart(PartInfo(
        onInitializerRead = onInitializerRead(_, _, _, _)(using _),
        onInitializerWrite = onInitializerWrite(_, _, _, _)(using _),
        continuation = onContinue(_)(using _)
    ))

    /**
     * Allows users of this trait to specify whether the write context is provably unreachable from the read context.
     * Implementations must be soundy, i.e. abort with 'false' when the framework does not provide enough information to
     * eventually compute a precise answer.
     *
     * @note Assumes that the two contexts point to different methods, i.e. that intraprocedural path existence is
     *       handled separately.
     */
    protected def isContextUnreachableFrom(
        readPC:       Int,
        readContext:  Context,
        writeContext: Context
    )(implicit state: AnalysisState): Boolean

    protected def onContinue(eps: SomeEPS)(implicit state: AnalysisState): Option[FieldAssignability] = None

    private def onInitializerRead(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        readPC:   PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Some[FieldAssignability] = {
        // Initializer reads are incompatible with arbitrary writes in other methods
        if (state.nonInitializerWrites.nonEmpty)
            return Some(Assignable);

        // First, check whether there are any interprocedural writes where paths are not excluded from existence
        if (state.initializerWrites.exists {
                case (writeContext, _) =>
                    (writeContext.method ne context.method) &&
                        !isContextUnreachableFrom(readPC, context, writeContext)
            }
        )
            return Some(Assignable);

        // Second, check read-write paths intraprocedurally, context sensitive to the same access context as the read
        val pathFromReadToSomeWriteExists = state.initializerWrites(context).exists {
            case (writePC, _) =>
                pathExists(readPC, writePC, tac)
        }

        if (pathFromReadToSomeWriteExists)
            Some(Assignable)
        else
            Some(NonAssignable)
    }

    private def onInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Some[FieldAssignability] = {
        val method = context.method.definedMethod
        // Writing fields outside their initialization scope is not supported.
        if (state.field.isStatic && method.isConstructor || state.field.isNotStatic && method.isStaticInitializer)
            return Some(Assignable);

        // For instance writes, there might be premature value escapes through arbitrary use sites.
        // If we cannot guarantee that all use sites of the written instance are safe, we must soundly abort.
        if (state.field.isNotStatic) {
            // We only support modifying fields in initializers on newly created instances (which is always "this")
            if (receiver.isEmpty || receiver.get.definedBy != SelfReferenceParameter)
                return Some(Assignable);

            if (!tac.params.thisParameter.useSites.forall(isWrittenInstanceUseSiteSafe(tac, writePC, _)))
                return Some(Assignable);
        }

        if (state.initializerReads.exists {
                case (readContext, readAccesses) =>
                    (readContext.method ne context.method) &&
                        readAccesses.exists(access => !isContextUnreachableFrom(access._1, readContext, context))
            }
        )
            return Some(Assignable);

        val pathFromSomeReadToWriteExists = state.initializerReads(context).exists {
            case (readPC, readReceiver) =>
                val readReceiverVar = readReceiver.map(uVarForDefSites(_, tac.pcToIndex))
                if (readReceiverVar.isDefined && isSameInstance(tac, receiver.get, readReceiverVar.get).isNo) {
                    false
                } else {
                    pathExists(readPC, writePC, tac)
                }
        }

        if (pathFromSomeReadToWriteExists)
            Some(Assignable)
        else
            Some(NonAssignable)
    }
}

/**
 * @inheritdoc
 *
 * The simple path analysis considers every interprocedural path to be harmful, causing the field to be assignable.
 */
trait SimpleReadWritePathAnalysis private[fieldassignability] extends ReadWritePathAnalysisPart {

    override protected def isContextUnreachableFrom(
        readPC:       Int,
        readContext:  Context,
        writeContext: Context
    )(implicit state: AnalysisState): Boolean = false
}

/**
 * @inheritdoc
 *
 * The extensive path analysis considers interprocedural static field writes as safe when the writing static initializer
 * is in the same class as the field declaration.
 */
trait ExtensiveReadWritePathAnalysis private[fieldassignability]
    extends SimpleReadWritePathAnalysis {

    override protected def isContextUnreachableFrom(
        readPC:       Int,
        readContext:  Context,
        writeContext: Context
    )(implicit state: AnalysisState): Boolean = {
        if (super.isContextUnreachableFrom(readPC, readContext, writeContext))
            return true;

        val writeMethod = writeContext.method.definedMethod
        // We can only guarantee that no paths exist when the writing static initializer is guaranteed to run before
        // the reading method, which is in turn only guaranteed when the writing static initializer is in the same
        // type as the field declaration, i.e. it is run when the field is used for the first time.

        // Note that this also implies that there may be no supertype static initializer that can read the field,
        // preventing read-write paths in the inheritance chain.
        writeMethod.isStaticInitializer && (writeContext.method.declaringClassType eq state.field.classFile.thisType)
    }
}

trait CalleesBasedReadWritePathAnalysisState extends AbstractFieldAssignabilityAnalysisState {
    private[part] var calleeDependees: Map[Context, EOptionP[DeclaredMethod, Callees]] = Map.empty
}

/**
 * Based on failure of the [[ExtensiveReadWritePathAnalysis]], marks two constructor method contexts as unreachable
 * when (based on the available call graph) the reading constructor does either not invoke any other constructors or
 * any such invocation comes after the read.
 *
 * This result is considered sound as once a call string leaves a constructor, it cannot reenter a constructor on the
 * same instance. Thus, when the reading constructor does not call any other constructor, it is impossible for the
 * instance under construction to reach the writing constructor.
 */
trait CalleesBasedReadWritePathAnalysis private[fieldassignability]
    extends ExtensiveReadWritePathAnalysis {

    override type AnalysisState <: CalleesBasedReadWritePathAnalysisState

    implicit val contextProvider: ContextProvider

    override protected final def isContextUnreachableFrom(
        readPC:       Int,
        readContext:  Context,
        writeContext: Context
    )(implicit state: AnalysisState): Boolean = {
        if (super.isContextUnreachableFrom(readPC, readContext, writeContext))
            return true;

        val readMethod = readContext.method.definedMethod
        val writeMethod = writeContext.method.definedMethod
        if (!readMethod.isConstructor || !writeMethod.isConstructor)
            return false;

        val readCalleesProperty = state.calleeDependees.get(readContext) match {
            case Some(callees) => callees
            case None          =>
                val callees = ps(readContext.method, Callees.key)
                state.calleeDependees = state.calleeDependees.updated(readContext, callees)
                callees
        }

        if (readCalleesProperty.hasUBP) {
            if (readCalleesProperty.ub.hasIncompleteCallSites(readContext)) {
                false
            } else {
                val tac = state.tacDependees(readContext.method.asDefinedMethod).ub.tac.get

                readCalleesProperty.ub.callSites(readContext).forall {
                    case (callSitePC, potentialCallees) =>
                        // Either the reading constructor is not calling any other constructors ...
                        potentialCallees.forall(!_.method.definedMethod.isConstructor) ||
                            // or the call is irrelevant since there is no way it is executed after the read
                            !pathExists(readPC, callSitePC, tac)
                }
            }
        } else {
            true
        }
    }

    override protected def onContinue(eps: SomeEPS)(implicit state: AnalysisState): Option[FieldAssignability] = {
        eps match {
            case UBP(callees: Callees) =>
                val contexts = state.calleeDependees.iterator.filter(_._2.e eq eps.e).map(_._1).toSeq
                val dangerousCallSiteFound = contexts.exists { context =>
                    state.calleeDependees =
                        state.calleeDependees.updated(context, eps.asInstanceOf[EOptionP[DeclaredMethod, Callees]])

                    callees.hasIncompleteCallSites(context) || {
                        val tac = state.tacDependees(context.method.asDefinedMethod).ub.tac.get
                        callees.callSites(context).exists {
                            case (callSitePC, potentialCallees) =>
                                // Either the reading constructor is not calling any other constructors ...
                                potentialCallees.exists(_.method.definedMethod.isConstructor) ||
                                    // or the call is irrelevant since there is no read after which it is executed
                                    state.initializerReads(context).exists(read => pathExists(read._1, callSitePC, tac))
                        }
                    }
                }

                if (dangerousCallSiteFound)
                    Some(Assignable)
                else
                    Some(NonAssignable)

            case _ =>
                super.onContinue(eps)
        }
    }
}
