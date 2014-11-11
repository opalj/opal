/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package ai
package analyses

import scala.collection.mutable.{ Map ⇒ MutableMap }

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
import org.opalj.br.UpperTypeBound
import org.opalj.br.analyses.SomeProject

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
 * before the analysis of a method, the method [[setMethodContext]] has to be called.
 *
 * @author Michael Eichberg
 */
class FieldValuesAnalysisDomain(
    override val project: SomeProject,
    val classFile: ClassFile)
        extends Domain
        with domain.TheProject
        with domain.ProjectBasedClassHierarchy
        with domain.TheClassFile
        with domain.TheCode
        with domain.DefaultDomainValueBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelIntegerValues
        with domain.l0.DefaultTypeLevelLongValues
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.DefaultPrimitiveValuesConversions
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l0.DefaultReferenceValuesBinding
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization {

    import scala.collection.mutable.{ Map ⇒ MutableMap }

    val thisClassType: ObjectType = classFile.thisType

    // Map of fieldNames (that are relevant) and the (refined) type information
    private[this] val fieldInformation: MutableMap[String /*FieldName*/ , Option[DomainValue]] = {
        val relevantFields: Iterable[String] =
            for {
                field ← classFile.fields
                if field.fieldType.isObjectType
                fieldType = field.fieldType.asObjectType

                // test that there is some potential for specialization
                if !project.classFile(fieldType).map(_.isFinal).getOrElse(false)
                if classHierarchy.hasSubtypes(fieldType).isYes

                // test that the initialization can be made by the declaring class only:
                if field.isFinal || field.isPrivate
            } yield { field.name }
        MutableMap.empty ++ relevantFields.map(_ -> None)
    }

    def hasCandidateFields: Boolean = fieldInformation.nonEmpty

    def candidateFields: Iterable[String] = fieldInformation.keys

    private[this] var currentCode: Code = null

    /**
     * Sets the method that is currently analyzed. This method '''must not be called'''
     * during the abstract interpretation of a method. It is allowed to be called
     * before this domain is used for the first time and immediately after the
     * abstract interpretation of the method has completed/before the next interpreation
     * starts.
     */
    def setMethodContext(method: Method): Unit = {
        currentCode = method.body.get
    }

    def code: Code = currentCode

    def fieldsWithRefinedValues: Seq[(Field, DomainValue)] = {
        val refinedFields =
            for {
                field ← classFile.fields
                Some(ReferenceValue(fieldValue)) ← fieldInformation.get(field.name)
                upperTypeBound = fieldValue.upperTypeBound
                // we filter those fields that are known to be "null" (the upper 
                // type bound is empty), because some of them
                // are actually not null; they are initialized using native code
                if upperTypeBound.nonEmpty
                if (upperTypeBound.size != 1) || (upperTypeBound.first ne field.fieldType)
            } yield {
                (field, fieldValue)
            }
        refinedFields
    }

    private def updateFieldInformation(
        value: DomainValue,
        declaringClassType: ObjectType,
        name: String): Unit = {
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
        pc: PC,
        objectref: DomainValue,
        value: DomainValue,
        declaringClassType: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, ExceptionValue] = {

        updateFieldInformation(value, declaringClassType, name)

        super.putfield(pc, objectref, value, declaringClassType, name, fieldType)
    }

    override def putstatic(
        pc: PC,
        value: DomainValue,
        declaringClassType: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, Nothing] = {

        updateFieldInformation(value, declaringClassType, name)

        super.putstatic(pc, value, declaringClassType, name, fieldType)
    }

}

