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
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.CompileTimePure
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.FinalField
import org.opalj.br.fpcf.properties.ImmutableContainerType
import org.opalj.br.fpcf.properties.ImmutableType
import org.opalj.br.fpcf.properties.ImpureByAnalysis
import org.opalj.br.fpcf.properties.ImpureByLackOfInformation
import org.opalj.br.fpcf.properties.NonFinalField
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.properties.SimpleContext
import org.opalj.br.fpcf.properties.SimpleContexts
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.br.instructions._

/**
 * Very simple, fast, sound but also imprecise analysis of the purity of methods. See the
 * [[org.opalj.br.fpcf.properties.Purity]] property for details regarding the precise
 * semantics of `(Im)Pure`.
 *
 * This analysis is a very, very shallow implementation that immediately gives
 * up, when something "complicated" (e.g., method calls which take objects)
 * is encountered. It also does not perform any significant control-/data-flow analyses.
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
class L0PurityAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {

    import project.nonVirtualCall
    import project.resolveFieldReference

    private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val simpleContexts: SimpleContexts = project.get(SimpleContextsKey)

    /** Called when the analysis is scheduled lazily. */
    def doDeterminePurity(e: Entity): ProperPropertyComputationResult = {
        e match {
            case c @ Context(_: DefinedMethod)         => determinePurity(c)
            case c @ Context(_: VirtualDeclaredMethod) => Result(c, ImpureByLackOfInformation)
            case _                                     => throw new IllegalArgumentException(s"$e is not a method")
        }
    }

    /**
     * Determines the purity of the method starting with the instruction with the given
     * pc. If the given pc is larger than 0 then all previous instructions (in particular
     * method calls) must not violate this method's purity.
     *
     * This function encapsulates the continuation.
     */
    def doDeterminePurityOfBody(
        context:          Context,
        initialDependees: Set[EOptionP[Entity, Property]]
    ): ProperPropertyComputationResult = {

        val method = context.method.definedMethod
        val declaringClassType = method.classFile.thisType
        val methodDescriptor = method.descriptor
        val methodName = method.name
        val body = method.body.get
        val instructions = body.instructions
        val maxPC = instructions.length

        var dependees = initialDependees

        var currentPC = 0
        while (currentPC < maxPC) {
            val instruction = instructions(currentPC)
            (instruction.opcode: @switch) match {
                case GETSTATIC.opcode =>
                    val GETSTATIC(declaringClass, fieldName, fieldType) = instruction

                    resolveFieldReference(declaringClass, fieldName, fieldType) match {

                        // ... we have no support for arrays at the moment
                        case Some(field) if !field.fieldType.isArrayType =>
                            // The field has to be effectively final and -
                            // if it is an object – immutable!
                            val fieldType = field.fieldType
                            if (fieldType.isArrayType) {
                                return Result(context, ImpureByAnalysis);
                            }
                            if (!fieldType.isBaseType) {
                                propertyStore(fieldType, TypeImmutability.key) match {
                                    case FinalP(ImmutableType) =>
                                    case _: FinalEP[_, TypeImmutability] =>
                                        return Result(context, ImpureByAnalysis);
                                    case ep =>
                                        dependees += ep
                                }
                            }
                            if (field.isNotFinal) {
                                propertyStore(field, FieldMutability.key) match {
                                    case FinalP(_: FinalField) =>
                                    case _: FinalEP[Field, FieldMutability] =>
                                        return Result(context, ImpureByAnalysis);
                                    case ep =>
                                        dependees += ep
                                }
                            }

                        case _ =>
                            // We know nothing about the target field (it is not
                            // found in the scope of the current project).
                            return Result(context, ImpureByAnalysis);
                    }

                case INVOKESPECIAL.opcode | INVOKESTATIC.opcode => instruction match {

                    case MethodInvocationInstruction(`declaringClassType`, _, `methodName`, `methodDescriptor`) =>
                    // We have a self-recursive call; such calls do not influence
                    // the computation of the method's purity and are ignored.
                    // Let's continue with the evaluation of the next instruction.

                    case mii: NonVirtualMethodInvocationInstruction =>

                        nonVirtualCall(declaringClassType, mii) match {

                            case Success(callee) =>
                                /* Recall that self-recursive calls are handled earlier! */
                                val purity = propertyStore(
                                    simpleContexts(declaredMethods(callee)), Purity.key
                                )

                                purity match {
                                    case FinalP(CompileTimePure | Pure) => /* Nothing to do */

                                    // Handling cyclic computations
                                    case ep @ InterimUBP(Pure)          => dependees += ep

                                    case _: EPS[_, _] =>
                                        return Result(context, ImpureByAnalysis);

                                    case epk =>
                                        dependees += epk
                                }

                            case _ /* Empty or Failure */ =>
                                // We know nothing about the target method (it is not
                                // found in the scope of the current project).
                                return Result(context, ImpureByAnalysis);

                        }
                }

                case GETFIELD.opcode |
                    PUTFIELD.opcode | PUTSTATIC.opcode |
                    AALOAD.opcode | AASTORE.opcode |
                    BALOAD.opcode | BASTORE.opcode |
                    CALOAD.opcode | CASTORE.opcode |
                    SALOAD.opcode | SASTORE.opcode |
                    IALOAD.opcode | IASTORE.opcode |
                    LALOAD.opcode | LASTORE.opcode |
                    DALOAD.opcode | DASTORE.opcode |
                    FALOAD.opcode | FASTORE.opcode |
                    ARRAYLENGTH.opcode |
                    MONITORENTER.opcode | MONITOREXIT.opcode |
                    INVOKEDYNAMIC.opcode | INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode =>
                    return Result(context, ImpureByAnalysis);

                case ARETURN.opcode |
                    IRETURN.opcode | FRETURN.opcode | DRETURN.opcode | LRETURN.opcode |
                    RETURN.opcode =>
                // if we have a monitor instruction the method is impure anyway..
                // hence, we can ignore the monitor related implicit exception

                // Reference comparisons may have different results for structurally equal values
                case IF_ACMPEQ.opcode | IF_ACMPNE.opcode =>
                    return Result(context, ImpureByAnalysis);

                case _ =>
                    // All other instructions (IFs, Load/Stores, Arith., etc.) are pure
                    // as long as no implicit exceptions are raised.
                    // Remember that NEW/NEWARRAY/etc. may raise OutOfMemoryExceptions.
                    if (instruction.jvmExceptions.nonEmpty) {
                        // JVM Exceptions reify the stack and, hence, make the method impure as
                        // the calling context is now an explicit part of the method's result.
                        return Result(context, ImpureByAnalysis);
                    }
                // else ok..

            }
            currentPC = body.pcOfNextInstruction(currentPC)
        }

        // IN GENERAL
        // Every method that is not identified as being impure is (conditionally)pure.
        if (dependees.isEmpty)
            return Result(context, Pure);

        // This function computes the “purity for a method based on the properties of its dependees:
        // other methods (Purity), types (immutability), fields (effectively final)
        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            // Let's filter the entity.
            dependees = dependees.filter(_.e ne eps.e)

            (eps: @unchecked) match {
                // We can't report any real result as long as we don't know that the fields are all
                // effectively final and the types are immutable.

                case _: InterimEP[_, _] =>
                    dependees += eps
                    InterimResult(context, ImpureByAnalysis, Pure, dependees, c)

                case FinalP(_: FinalField | ImmutableType) =>
                    if (dependees.isEmpty) {
                        Result(context, Pure)
                    } else {
                        // We still have dependencies regarding field mutability/type immutability;
                        // hence, we have nothing to report.
                        InterimResult(context, ImpureByAnalysis, Pure, dependees, c)
                    }

                case FinalP(ImmutableContainerType) =>
                    Result(context, ImpureByAnalysis)

                // The type is at most conditionally immutable.
                case FinalP(_: TypeImmutability) => Result(context, ImpureByAnalysis)
                case FinalP(_: NonFinalField)    => Result(context, ImpureByAnalysis)

                case FinalP(CompileTimePure | Pure) =>
                    if (dependees.isEmpty)
                        Result(context, Pure)
                    else {
                        InterimResult(context, ImpureByAnalysis, Pure, dependees, c)
                    }

                case FinalP(_: Purity) =>
                    // a called method is impure...
                    Result(context, ImpureByAnalysis)
            }
        }

        InterimResult(context, ImpureByAnalysis, Pure, dependees, c)
    }

    def determinePurityStep1(context: Context): ProperPropertyComputationResult = {
        val method = context.method.definedMethod

        // All parameters either have to be base types or have to be immutable.
        // IMPROVE Use plain object type once we use ObjectType in the store!
        var referenceTypedParameters = method.parameterTypes.iterator.collect[ObjectType] {
            case t: ObjectType => t
            case _: ArrayType  => return Result(context, ImpureByAnalysis);
        }
        val methodReturnType = method.descriptor.returnType
        if (methodReturnType.isArrayType) {
            // we currently have no logic to decide whether the array was created locally
            // and did not escape or was created elsewhere...
            return Result(context, ImpureByAnalysis);
        }
        if (methodReturnType.isObjectType) {
            referenceTypedParameters ++= Iterator(methodReturnType.asObjectType)
        }

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty
        referenceTypedParameters foreach { e =>
            propertyStore(e, TypeImmutability.key) match {
                case FinalP(ImmutableType) => /*everything is Ok*/
                case _: FinalEP[_, _] =>
                    return Result(context, ImpureByAnalysis);
                case InterimUBP(ub) if ub ne ImmutableType =>
                    return Result(context, ImpureByAnalysis);
                case epk => dependees += epk
            }
        }

        doDeterminePurityOfBody(context, dependees)
    }

    /**
     * Retrieves and commits the methods purity as calculated for its declaring class type for the
     * current DefinedMethod that represents the non-overwritten method in a subtype.
     */
    def baseMethodPurity(context: Context): ProperPropertyComputationResult = {

        def c(eps: SomeEOptionP): ProperPropertyComputationResult = eps match {
            case FinalP(p) => Result(context, p)
            case ep @ InterimLUBP(lb, ub) =>
                InterimResult.create(context, lb, ub, Set(ep), c)
            case epk =>
                InterimResult(context, ImpureByAnalysis, CompileTimePure, Set(epk), c)
        }

        c(propertyStore(simpleContexts(declaredMethods(context.method.definedMethod)), Purity.key))
    }

    /**
     * Determines the purity of the given method.
     */
    def determinePurity(context: Context): ProperPropertyComputationResult = {
        val method = context.method.definedMethod

        // If this is not the method's declaration, but a non-overwritten method in a subtype,
        // don't re-analyze the code
        if ((method.classFile.thisType ne context.method.declaringClassType) &&
            context.isInstanceOf[SimpleContext])
            return baseMethodPurity(context);

        if (method.body.isEmpty)
            return Result(context, ImpureByAnalysis);

        if (method.isSynchronized)
            return Result(context, ImpureByAnalysis);

        // 1. step (will schedule 2. step if necessary):
        determinePurityStep1(context)
    }

}

trait L0PurityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, SimpleContextsKey)

    final override def uses: Set[PropertyBounds] = {
        Set(PropertyBounds.ub(TypeImmutability), PropertyBounds.ub(FieldMutability))
    }

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(Purity)

}

object EagerL0PurityAnalysis
    extends L0PurityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0PurityAnalysis(p)
        val dms = p.get(DeclaredMethodsKey).declaredMethods
        val simpleContexts = p.get(SimpleContextsKey)
        val methodsWithBody = dms.collect {
            case dm if dm.hasSingleDefinedMethod && dm.definedMethod.body.isDefined =>
                simpleContexts(dm)
        }
        ps.scheduleEagerComputationsForEntities(methodsWithBody)(analysis.determinePurity)
        analysis
    }
}

object LazyL0PurityAnalysis
    extends L0PurityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0PurityAnalysis(p)
        ps.registerLazyPropertyComputation(Purity.key, analysis.doDeterminePurity)
        analysis
    }
}
