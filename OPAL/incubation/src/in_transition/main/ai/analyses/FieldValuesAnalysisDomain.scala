/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses

import org.opalj.ai.Computation
import org.opalj.ai.Domain
import org.opalj.ai.NoUpdate
import org.opalj.ai.SomeUpdate
import org.opalj.br.ClassFile
import org.opalj.br.Code
import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.domain.la.RefinedTypeLevelFieldAccessInstructions
import org.opalj.ai.domain.la.RefinedTypeLevelInvokeInstructions
import org.opalj.ai.analyses.cg.CallGraphCache
import org.opalj.br.MethodSignature

/**
 * A very basic domain that we use for analyzing the real type of the values stored in a
 * field.
 *
 * ==Usage==
 * One instance of this domain has to be used to analyze all methods of the respective
 * class. Only after the analysis of all methods, the information returned by
 * [[fieldsWithRefinedValues]] is guaranteed to be correct.
 *
 * ==Thread Safety==
 * This domain is not thread-safe. The methods of a class have to be analyzed
 * sequentially. The order in which the methods are analyzed is not relevant. However,
 * before the analysis of a [[org.opalj.br.Method]], the method [[setMethodContext]]
 * has to be called.
 *
 * @author Michael Eichberg
 */
class BaseFieldValuesAnalysisDomain(
        val project:   SomeProject,
        val classFile: ClassFile
) extends Domain
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

    import scala.collection.mutable.{Map ⇒ MutableMap}

    val thisClassType: ObjectType = classFile.thisType

    // Map of fieldNames (that are relevant) and the (refined) type information
    private[this] val fieldInformation: MutableMap[String /*FieldName*/ , Option[DomainValue]] = {
        val relevantFields: Iterable[String] =
            for {
                field ← classFile.fields
                if field.fieldType.isObjectType
                fieldType = field.fieldType.asObjectType

                // test that there is some potential for specialization
                // e.g., an analysis for a field of type string doesn't make sense
                // since there will be no subtype
                if !project.classFile(fieldType).map(_.isFinal).getOrElse(false)
                if classHierarchy.hasSubtypes(fieldType).isYes

                // test that the initialization can be made by the declaring class only:
                if field.isFinal || field.isPrivate
            } yield { field.name }
        MutableMap.empty ++ relevantFields.map(_ → None)
    }

    def hasCandidateFields: Boolean = fieldInformation.nonEmpty

    def candidateFields: Iterable[String] = fieldInformation.keys

    private[this] var currentCode: Code = null

    /**
     * Sets the method that is currently analyzed. This method '''must not be called'''
     * during the abstract interpretation of a method. It is must be called
     * before this domain is used for the first time and immediately before the
     * interpretation of the next method (code block) starts.
     */
    def setMethodContext(method: Method): Unit = {
        currentCode = method.body.get
    }

    def code: Code = currentCode

    def fieldsWithRefinedValues: Seq[(Field, DomainValue)] = {
        val refinedFields =
            for {
                field ← classFile.fields
                Some(DomainReferenceValue(fieldValue)) ← fieldInformation.get(field.name)
                upperTypeBound = fieldValue.upperTypeBound
                // we filter those fields that are known to be "null" (the upper
                // type bound is empty), because some of them
                // are actually not null; they are initialized using native code
                if upperTypeBound.nonEmpty
                if (upperTypeBound.size != 1) || (upperTypeBound.head ne field.fieldType)
            } yield {
                (field, fieldValue)
            }
        refinedFields
    }

    private def updateFieldInformation(
        value:              DomainValue,
        declaringClassType: ObjectType,
        name:               String
    ): Unit = {
        if ((declaringClassType eq thisClassType) &&
            fieldInformation.contains(name)) {
            fieldInformation(name) match {
                case Some(previousValue) ⇒
                    if (previousValue ne value) {
                        previousValue.join(Int.MinValue, value) match {
                            case SomeUpdate(newValue) ⇒
                                fieldInformation.update(name, Some(newValue))
                            case NoUpdate ⇒ /*nothing to do*/
                        }
                    }
                case None ⇒
                    fieldInformation.update(name, Some(value))
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

        updateFieldInformation(value, declaringClassType, name)

        super.putfield(pc, objectref, value, declaringClassType, name, fieldType)
    }

    override def putstatic(
        pc:                 PC,
        value:              DomainValue,
        declaringClassType: ObjectType,
        name:               String,
        fieldType:          FieldType
    ): Computation[Nothing, Nothing] = {

        updateFieldInformation(value, declaringClassType, name)

        super.putstatic(pc, value, declaringClassType, name, fieldType)
    }

}

class FPFieldValuesAnalysisDomain(
        project:                          SomeProject,
        val fieldValueInformation:        FieldValueInformation,
        val methodReturnValueInformation: MethodReturnValueInformation,
        val cache:                        CallGraphCache[MethodSignature, scala.collection.Set[Method]],
        classFile:                        ClassFile
) extends BaseFieldValuesAnalysisDomain(project, classFile)
    with RefinedTypeLevelFieldAccessInstructions
    with RefinedTypeLevelInvokeInstructions
