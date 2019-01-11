/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package analyses

import org.opalj.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.MultiResult
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.value.IsReferenceValue
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile
import org.opalj.br.Code
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.Field
import org.opalj.ai.fpcf.analyses.FieldValuesAnalysis.ignoredFields
import org.opalj.ai.fpcf.properties.FieldValue
import org.opalj.ai.fpcf.properties.ValueBasedFieldValueInformation

/**
 * Computes for each private field an approximation of the type of values stored in the field.
 *
 * Basically, we perform an abstract interpretation of all methods that assign a value
 * to the field to compute the type of the values stored in the field.
 *
 * This analysis is deliberately optimized for performance and, given that we don't have precise
 * call-graph information and also don't track the origin of values, we can't track
 * the overall nullness property of the values stored in a field. E.g., even if a we always
 * see that a field is only written in the constructor we still don't know if that happens w.r.t.
 * `this` or some other instance.
 *
 * @author Michael Eichberg
 */
class LBFieldValuesAnalysis private[analyses] (
        val project: SomeProject
) extends FPCFAnalysis { analysis ⇒

    final val fieldAccessInformation = project.get(FieldAccessInformationKey)

    /**
     *  A very basic domain that we use for analyzing the values stored in a field.
     *
     * One instance of this domain is used to analyze all methods of the respective
     * class. Only after the analysis of all methods, the information returned by
     * [[fieldInformation]] is guaranteed to be correct.
     *
     * @author Michael Eichberg
     */
    class InitialFieldValuesAnalysisDomain(
            val classFile: ClassFile
    ) extends CorrelationalDomain
        with domain.TheProject
        with domain.TheCode
        with domain.DefaultSpecialDomainValuesBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelIntegerValues
        with domain.l0.DefaultTypeLevelLongValues
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelPrimitiveValuesConversions
        with domain.l0.TypeLevelLongValuesShiftOperators
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l0.DefaultReferenceValuesBinding
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization {

        override implicit val project: SomeProject = analysis.project

        val thisClassType: ObjectType = classFile.thisType

        // Map of fieldNames (that are potentially relevant) and the (refined) value information
        var fieldInformation: Map[Field, Option[DomainValue]] = {
            val relevantFieldsIterable: Iterable[(Field, Option[DomainValue])] =
                for {
                    field ← classFile.fields
                    if field.fieldType.isReferenceType

                    // test that the initialization can be made by the declaring class only:
                    if field.isFinal || field.isPrivate // || TODO <the field is package visible and the package is closed ... requires additional support below to find all relevant methods> || ...

                    // we don't want to ignore the field
                    if ignoredFields.get(thisClassType).forall(!_.contains(field.name))

                    // TODO XXX FIXME We don't think that the field is accessed via unsafe or reflection

                    // If the type is final, then the type is already necessarily precise...
                    if !classHierarchy.isKnownToBeFinal(field.fieldType.asReferenceType)

                } yield {
                    field → None
                }

            relevantFieldsIterable.toMap
        }

        def hasCandidateFields: Boolean = fieldInformation.nonEmpty

        def candidateFields: Iterable[Field] = fieldInformation.keys

        private[this] var currentCode: Code = null

        /**
         * Sets the method that is currently analyzed. This method '''must not be called'''
         * during the abstract interpretation of a method. It is must be called
         * before this domain is used for the first time and immediately before the
         * interpretation of the next method (code block) starts.
         */
        def setMethodContext(method: Method): Unit = currentCode = method.body.get

        def code: Code = currentCode

        private def updateFieldInformation(
            value:              DomainValue,
            declaringClassType: ObjectType,
            name:               String,
            fieldType:          FieldType
        ): Unit = {
            if (declaringClassType ne thisClassType)
                return ;

            classFile.findField(name, fieldType).foreach { field ⇒
                if (fieldInformation.contains(field)) {
                    fieldInformation(field) match {
                        case Some(previousValue) ⇒
                            if (previousValue ne value) {
                                previousValue.join(Int.MinValue, value) match {
                                    case SomeUpdate(newValue) ⇒
                                        fieldInformation += (field → Some(newValue))
                                    case NoUpdate ⇒ /*nothing to do*/
                                }
                            }
                        case None ⇒
                            fieldInformation += (field → Some(value))
                    }
                }
            }
        }

        override def putfield(
            pc:                 PC,
            objectref:          DomainValue,
            value:              DomainValue,
            declaringClassType: ObjectType,
            name:               String,
            fieldType:          FieldType
        ): Computation[Nothing, ExceptionValue] = {
            updateFieldInformation(value, declaringClassType, name, fieldType)
            super.putfield(pc, objectref, value, declaringClassType, name, fieldType)
        }

        override def putstatic(
            pc:                 PC,
            value:              DomainValue,
            declaringClassType: ObjectType,
            name:               String,
            fieldType:          FieldType
        ): Computation[Nothing, Nothing] = {
            updateFieldInformation(value, declaringClassType, name, fieldType)
            super.putstatic(pc, value, declaringClassType, name, fieldType)
        }
    }

    private[analyses] def analyze(classFile: ClassFile): PropertyComputationResult = {
        val domain = new InitialFieldValuesAnalysisDomain(classFile)
        if (domain.hasCandidateFields) {
            val relevantMethods =
                domain.fieldInformation.keys.foldLeft(Set.empty[Method]) { (ms, field) ⇒
                    ms ++ fieldAccessInformation.writeAccesses(field).map(_._1)
                }
            relevantMethods.foreach { method ⇒
                domain.setMethodContext(method)
                BaseAI(method, domain) // the state is implicitly accumulated in the domain
            }

            var results: List[FinalEP[Field, FieldValue]] = Nil
            domain.fieldInformation foreach { e ⇒
                val (field, domainValueOption: Option[IsReferenceValue @unchecked]) = e
                domainValueOption.foreach { domainValue ⇒
                    if (domainValue.isPrecise || domainValue.isNull.isYesOrNo ||
                        // when we reach this point:
                        //      value.isNull == Unknown &&
                        //      the type is not precise
                        domainValue.leastUpperType.get != field.fieldType) {
                        val vi = ValueBasedFieldValueInformation(domainValue.toCanonicalForm)
                        results ::= FinalEP(field, vi)
                    }
                }
            }
            MultiResult(results)
        } else {
            NoResult
        }
    }

}

object FieldValuesAnalysis {

    /**
     * The following (final) fields are directly initialized by the JVM or some native code.
     * I.e., the initialization is not visible and if we don't ignore the fields, we would derive
     * wrong information.
     */
    // IMPROVE Move to configuration file.
    final val ignoredFields: Map[ObjectType, Set[String]] = Map(
        ObjectType.System → Set("in", "out", "err"),
        ObjectType("java/net/InterfaceAddress") → Set("address"),
        ObjectType("java/util/concurrent/FutureTask") → Set("runner"),
        ObjectType("java/nio/channels/SelectionKey") → Set("attachment")
    )

}

object EagerLBFieldValuesAnalysis extends BasicFPCFEagerAnalysisScheduler {

    override def uses: Set[PropertyBounds] = Set()

    def derivedProperty: PropertyBounds = PropertyBounds.lb(FieldValue.key)

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new LBFieldValuesAnalysis(p)
        val classFiles = p.allClassFiles
        ps.scheduleEagerComputationsForEntities(classFiles)(analysis.analyze)
        analysis
    }
}
