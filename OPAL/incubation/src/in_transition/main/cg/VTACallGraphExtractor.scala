/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import scala.annotation.switch

import scala.collection.Set
import scala.collection.Map
import scala.collection.mutable.HashSet

import org.opalj.collection.immutable.UIDSet
import org.opalj.br._
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.TheMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.VirtualMethodInvocationInstruction

/**
 * The `VTACallGraphExtractor` extracts call edges using the type information at hand.
 * I.e., it does not use the specified declaring class type, but instead uses the
 * type information about the receiver value that are available.
 *
 * @author Michael Eichberg
 */
class VTACallGraphExtractor[TheDomain <: Domain with TheProject with TheMethod](
        val cache: CallGraphCache[MethodSignature, Set[Method]],
        Domain:    Method ⇒ TheDomain
) extends CallGraphExtractor {

    protected[this] class AnalysisContext(val domain: TheDomain) extends super.AnalysisContext {

        def project = domain.project
        def classHierarchy = project.classHierarchy
        def classFile = domain.classFile
        def method = domain.method

        def staticCall(
            pc:                 PC,
            declaringClassType: ObjectType,
            isInterface:        Boolean,
            name:               String,
            descriptor:         MethodDescriptor,
            operands:           domain.Operands
        ): Unit = {

            project.staticCall(declaringClassType, isInterface, name, descriptor) match {
                case Success(callee) ⇒
                    addCallEdge(pc, HashSet(callee))
                case _ ⇒
                    addUnresolvedMethodCall(method, pc, declaringClassType, name, descriptor)
            }
        }

        def nonNullInstanceCall(
            pc:                 PC,
            declaringClassType: ObjectType,
            name:               String,
            descriptor:         MethodDescriptor,
            operands:           domain.Operands
        ): Unit = {
            val callerType = domain.method.classFile.thisType
            project.instanceCall(callerType, declaringClassType, name, descriptor) match {
                case Success(callee) ⇒
                    addCallEdge(pc, HashSet(callee))
                case _ ⇒
                    addUnresolvedMethodCall(method, pc, declaringClassType, name, descriptor)
            }
        }

        def specialCall(
            pc:                 PC,
            declaringClassType: ObjectType,
            isInterface:        Boolean,
            name:               String,
            descriptor:         MethodDescriptor,
            operands:           domain.Operands
        ): Unit = {

            project.specialCall(declaringClassType, isInterface, name, descriptor) match {
                case Success(callee) ⇒
                    val callees = HashSet(callee)
                    addCallEdge(pc, callees)
                case _ ⇒
                    addUnresolvedMethodCall(method, pc, declaringClassType, name, descriptor)
            }
        }

        def arrayCall(
            pc:         PC,
            arrayType:  ArrayType,
            name:       String,
            descriptor: MethodDescriptor,
            operands:   domain.Operands
        ): Unit = {

            val isNull = domain.refIsNull(pc, operands(descriptor.parametersCount))

            if (isNull.isYesOrUnknown) {
                addCallToNullPointerExceptionConstructor(method, pc)
            }

            if (isNull.isNoOrUnknown) {
                val callerType = domain.method.classFile.thisType
                project.instanceCall(callerType, ObjectType.Object, name, descriptor) match {
                    case Success(callee) ⇒
                        addCallEdge(pc, HashSet(callee))
                    case _ ⇒
                        addUnresolvedMethodCall(method, pc, arrayType, name, descriptor)
                }
            }
        }

        protected[this] def doVirtualCall(
            pc:                 PC,
            declaringClassType: ObjectType,
            isInterface:        Boolean,
            name:               String,
            descriptor:         MethodDescriptor,
            operands:           domain.Operands
        ): Unit = {
            // the nullness of the receiver is already checked for

            val callees: Set[Method] = this.callees(domain.method, declaringClassType, isInterface, name, descriptor)
            if (callees.isEmpty) {
                addUnresolvedMethodCall(method, pc, declaringClassType, name, descriptor)
            } else {
                addCallEdge(pc, callees)
            }
        }

        def virtualCall(
            pc:                 PC,
            declaringClassType: ObjectType,
            isInterface:        Boolean,
            name:               String,
            descriptor:         MethodDescriptor,
            operands:           domain.Operands
        ): Unit = {
            // MODIFIED CHA - we used the type information that is readily available
            val domain.DomainReferenceValue(receiver) = operands(descriptor.parametersCount)
            val receiverIsNull = receiver.isNull

            assert(
                classHierarchy.isInterface(declaringClassType).isUnknown ||
                    classHierarchy.isInterface(declaringClassType) == Answer(isInterface),
                "virtual call - inconsistent isInterface information ("+
                    s"${classHierarchy.isInterface(declaringClassType)} vs. "+
                    s"${Answer(isInterface)}) for $declaringClassType"
            )

            // Possible Cases:
            //  - the value is precise and has a single type => non-virtual call
            //  - the value is not precise but has an upper type bound that is a subtype
            //    of the declaringClassType
            //
            //  - the value is null => call to the constructor of NullPointerException
            //  - the value maybe null => additional call to the constructor of NullPointerException
            //
            //  - the value is not precise and the upper type bound is a supertype
            //    of the declaringClassType => the type hierarchy information is not complete;
            //    the central factory method already "handles" this issue - hence, we don't care

            if (receiverIsNull.isYes) {
                addCallToNullPointerExceptionConstructor(method, pc)
                return ;
            }

            if (receiverIsNull.isUnknown) {
                addCallToNullPointerExceptionConstructor(method, pc)
                // ... and continue!
            }

            @inline def handleVirtualNonNullCall(
                upperTypeBound:    UpperTypeBound,
                receiverIsPrecise: Boolean
            ): Unit = {

                assert(upperTypeBound.nonEmpty)

                if (upperTypeBound.isSingletonSet) {
                    val theType = upperTypeBound.head
                    if (theType.isArrayType)
                        arrayCall(
                            pc, theType.asArrayType, name, descriptor,
                            operands
                        )
                    else if (receiverIsPrecise)
                        nonNullInstanceCall(
                            pc, theType.asObjectType, name, descriptor,
                            operands
                        )
                    else {
                        val receiverType = theType.asObjectType
                        classHierarchy.isInterface(receiverType) match {
                            case Yes ⇒
                                doVirtualCall(
                                    pc, receiverType, true, name, descriptor, operands
                                )
                            case No ⇒
                                doVirtualCall(
                                    pc, receiverType, false, name, descriptor, operands
                                )
                            case _ ⇒
                                addUnresolvedMethodCall(method, pc, receiverType, name, descriptor)
                        }

                    }
                } else {
                    // Recall that the types defining the upper type bound are not in an
                    // inheritance relationship; however, they still may define
                    // the respective method.

                    def fallback() = {
                        doVirtualCall(
                            pc, declaringClassType, isInterface, name, descriptor, operands
                        )
                    }

                    val potentialRuntimeTypes =
                        classHierarchy.directSubtypesOf(upperTypeBound.asInstanceOf[UIDSet[ObjectType]])

                    val allCallees =
                        if (potentialRuntimeTypes.nonEmpty) {
                            val t = potentialRuntimeTypes.head.asObjectType

                            val isInterface = classHierarchy.isInterface(t) match {
                                case Yes ⇒ true
                                case No  ⇒ false
                                case _   ⇒ fallback(); return ;
                            }
                            val callees = this.callees(method, t, isInterface, name, descriptor)
                            potentialRuntimeTypes.tail.foldLeft(callees) { (r, nextUpperTypeBound) ⇒
                                val t = nextUpperTypeBound.asObjectType
                                val isInterface = classHierarchy.isInterface(t) match {
                                    case Yes ⇒ true
                                    case No  ⇒ false
                                    case _   ⇒ fallback(); return ;
                                }
                                r ++ this.callees(method, t, isInterface, name, descriptor)
                            }
                        } else {
                            Set.empty[Method]
                        }

                    if (allCallees.isEmpty) {
                        // Fallback to ensure that the call graph does not miss an
                        // edge; it may be the case that the (unknown) subtypes actually
                        // just inherit one of the methods of the (known) supertype.
                        fallback()
                    } else {
                        addCallEdge(pc, allCallees)
                    }
                }
            }

            val receiverUpperTypeBound = receiver.upperTypeBound.toUIDSet[ReferenceType]
            val receivers = receiver.baseValues
            if (receivers.nonEmpty) {
                // the reference value is a "MultipleReferenceValue"

                // The following numbers are created using ExtVTA for JDK 1.8.0_25
                // and refer to a call graph created without explicit support for
                // multiple reference values:
                //     Creating the call graph took: ~28sec (Mac Pro; 3 GHz 8-Core Intel Xeon E5)
                //     Number of call sites: 911.253
                //     Number of call edges: 6.925.997
                //
                // With explicit support, we get the following numbers:
                //     Number of call sites: 911.253
                //     Number of call edges: 6.923.015

                val receiversAreMorePrecise =
                    !receiver.isPrecise &&
                        // the receiver as a whole is not precise...
                        receivers.forall { aReceiver ⇒
                            val anUpperTypeBound = aReceiver.upperTypeBound
                            aReceiver.isPrecise || {
                                anUpperTypeBound != receiverUpperTypeBound &&
                                    classHierarchy.isSubtypeOf(anUpperTypeBound, receiverUpperTypeBound).isYes
                            }
                        }
                if (receiversAreMorePrecise) {
                    // THERE IS POTENTIAL FOR A MORE PRECISE CALL GRAPH SIMPLY
                    // BECAUSE OF THE TYPE INFORMATION!
                    val uniqueReceivers =
                        receivers.foldLeft(Map.empty[UpperTypeBound, Boolean]) { (results, rv) ⇒
                            val utb = rv.upperTypeBound.toUIDSet[ReferenceType]
                            if (utb.nonEmpty)
                                results.get(utb) match {
                                    case Some(isPrecise) ⇒
                                        if (isPrecise && !rv.isPrecise) {
                                            results.updated(utb, false)
                                        } else {
                                            results
                                        }
                                    case None ⇒
                                        results + ((utb, rv.isPrecise))
                                }
                            else
                                // empty upper type bounds (those of null values) are
                                // already handled
                                results
                        }
                    uniqueReceivers.foreach { rv ⇒
                        val (utb, isPrecise) = rv
                        handleVirtualNonNullCall(utb, isPrecise)
                    }
                } else {
                    // we did not get anything from analyzing the "MultipleReferenceValue"
                    // let's continue with the default handling
                    handleVirtualNonNullCall(receiverUpperTypeBound, receiver.isPrecise)
                }
            } else {
                // the value is not a "MultipleReferenceValue"
                handleVirtualNonNullCall(receiverUpperTypeBound, receiver.isPrecise)
            }
        }
    }

    protected def AnalysisContext(domain: TheDomain): AnalysisContext = new AnalysisContext(domain)

    private[this] val chaCallGraphExtractor = new CHACallGraphExtractor(cache /*it should not be used...*/ )

    def extract(
        method: Method
    )(
        implicit
        project: SomeProject
    ): CallGraphExtractor.LocalCallGraphInformation = {

        // The following optimization (which uses the plain CHA algorithm for all methods
        // that do not have virtual method calls) may lead to some additional edges (if
        // the underlying code contains dead code), but the improvement is worth the
        // very few additional edges due to statically identifiable dead code!
        val hasVirtualMethodCalls =
            method.body.get.instructions.exists { i ⇒
                i.isInstanceOf[VirtualMethodInvocationInstruction]
            }
        if (!hasVirtualMethodCalls)
            return chaCallGraphExtractor.extract(method)

        // There are virtual calls, hence, we now do the call graph extraction using
        // variable type analysis

        val result = BaseAI(method, Domain(method))
        val context = AnalysisContext(result.domain)
        try {
            result.domain.code iterate { (pc, instruction) ⇒
                (instruction.opcode: @switch) match {
                    case INVOKEVIRTUAL.opcode ⇒
                        val INVOKEVIRTUAL(declaringClass, name, descriptor) = instruction
                        val operands = result.operandsArray(pc)
                        if (operands != null) {
                            if (declaringClass.isArrayType) {
                                context.arrayCall(
                                    pc, declaringClass.asArrayType, name, descriptor,
                                    operands.asInstanceOf[context.domain.Operands]
                                )
                            } else {
                                context.virtualCall(
                                    pc, declaringClass.asObjectType, isInterface = false, name, descriptor,
                                    operands.asInstanceOf[context.domain.Operands]
                                )
                            }
                        }
                    case INVOKEINTERFACE.opcode ⇒
                        val INVOKEINTERFACE(declaringClass, name, descriptor) = instruction
                        val operands = result.operandsArray(pc)
                        if (operands != null) {
                            context.virtualCall(
                                pc, declaringClass, isInterface = true, name, descriptor,
                                operands.asInstanceOf[context.domain.Operands]
                            )
                        }

                    case INVOKESPECIAL.opcode ⇒
                        val INVOKESPECIAL(declaringClass, isInterface, name, descriptor) = instruction
                        val operands = result.operandsArray(pc)
                        if (operands != null) {
                            context.specialCall(
                                pc, declaringClass, isInterface, name, descriptor,
                                operands.asInstanceOf[context.domain.Operands]
                            )
                        }

                    case INVOKESTATIC.opcode ⇒
                        val INVOKESTATIC(declaringClass, isInterface, name, descriptor) = instruction
                        val operands = result.operandsArray(pc)
                        if (operands != null) {
                            context.staticCall(
                                pc, declaringClass, isInterface, name, descriptor,
                                operands.asInstanceOf[context.domain.Operands]
                            )
                        }
                    case _ ⇒
                    // Nothing to do...
                }
            }
        } catch { case t: Throwable ⇒ println("\n\nFailed for: "+method.toJava); t.printStackTrace; throw t }

        (context.allCallEdges, context.unresolvableMethodCalls)
    }

}
