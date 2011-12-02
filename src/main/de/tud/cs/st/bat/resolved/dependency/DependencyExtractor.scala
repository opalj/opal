/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.resolved
package dependency

import DependencyType._
import de.tud.cs.st.bat.AccessFlagsContexts._
import de.tud.cs.st.bat.ACC_STATIC
import de.tud.cs.st.bat.AccessFlagsIterator

/**
 * DependencyExtractor can process a ClassFile and extract all dependencies between
 * class, interfaces, fields and methods. The dependencies are passed to
 * the addDep-method provided by the given DependencyBuilder.
 *
 * @author Thomas Schlosser
 */
class DependencyExtractor(val builder: DependencyBuilder) extends InstructionDependencyExtractor {

    import builder._

    def process(clazz: ClassFile) {
        val thisClassID = getID(clazz)
        val ClassFile(_, _, _, thisClass, superClass, interfaces, fields, methods, attributes) = clazz

        // process super class
        addDependency(thisClassID, getID(superClass), EXTENDS)

        // process interfaces
        interfaces foreach { i ⇒ addDependency(thisClassID, getID(i), IMPLEMENTS) }

        //process attributes
        attributes foreach {
            _ match {
                case AnnotationsAttribute(_, annotations) ⇒
                    annotations foreach { process(_)(thisClassID) }
                case ema: EnclosingMethodAttribute ⇒ {
                    val EnclosingMethodAttribute(enclosingClazz, enclosingMethodName, enclosingMethodDescriptor) = ema
                    addDependency(
                        thisClassID,
                        {
                            if (isEnclosedByMethod(ema))
                                getID(enclosingClazz, enclosingMethodName, enclosingMethodDescriptor)
                            else
                                getID(enclosingClazz)
                        },
                        IS_INNER_CLASS_OF)
                }
                case InnerClassesAttribute(innerClasses) ⇒
                    for (
                        InnerClassesEntry(innerClassType, outerClassType, _, _) ← innerClasses if outerClassType != null if outerClassType == thisClass
                    ) {
                        addDependency(getID(innerClassType), thisClassID, IS_INNER_CLASS_OF)
                    }
                case sa: Signature ⇒
                    processSignature(sa, thisClassID)
                case _ ⇒
            }
        }

        //process fields
        fields foreach { process(_)(thisClass, thisClassID) }

        //process methods
        methods foreach { process(_)(thisClass, thisClassID) }
    }

    private def process(field: Field)(implicit thisClass: ObjectType, thisClassID: Int) {
        val fieldID = getID(thisClass, field)
        val Field(accessFlags, _, fieldType, attributes) = field

        addDependency(fieldID, thisClassID, if (ACC_STATIC ∈ accessFlags) IS_CLASS_MEMBER_OF else IS_INSTANCE_MEMBER_OF)
        addDependency(fieldID, getID(fieldType), IS_OF_TYPE)

        //process attributes
        attributes foreach {
            _ match {
                case AnnotationsAttribute(_, annotations) ⇒
                    annotations foreach { process(_)(fieldID) }
                case cv: ConstantValue[_] ⇒
                    addDependency(fieldID, getID(cv.valueType), USES_CONSTANT_VALUE_OF_TYPE)
                case sa: Signature ⇒
                    processSignature(sa, fieldID)
                case _ ⇒
            }
        }
    }

    private def process(method: Method)(implicit thisClass: ObjectType, thisClassID: Int) {
        implicit val methodID = getID(thisClass, method)
        val Method(accessFlags, _, MethodDescriptor(parameterTypes, returnType), attributes) = method

        addDependency(methodID, thisClassID, if (ACC_STATIC ∈ accessFlags) IS_CLASS_MEMBER_OF else IS_INSTANCE_MEMBER_OF)
        addDependency(methodID, getID(returnType), RETURNS)
        parameterTypes foreach { pt ⇒ addDependency(methodID, getID(pt), HAS_PARAMETER_OF_TYPE) }

        //process attributes
        attributes foreach {
            _ match {
                case AnnotationsAttribute(_, annotations) ⇒
                    annotations foreach { process(_)(methodID) }
                case ParameterAnnotationsAttribute(_, parameterAnnotations) ⇒
                    parameterAnnotations foreach { _ foreach { process(_)(methodID, PARAMETER_ANNOTATED_WITH) } }
                case ExceptionsAttribute(exceptionTable) ⇒
                    exceptionTable foreach { e ⇒ addDependency(methodID, getID(e), THROWS) }
                case elementValue: ElementValue ⇒
                    processElementValue(elementValue)(methodID)
                case CodeAttribute(_, _, code, exceptionTable, attributes) ⇒
                    process(methodID, code)
                    for (
                        ExceptionTableEntry(_, _, _, catchType) ← exceptionTable if !isFinallyBlock(catchType)
                    ) {
                        addDependency(methodID, getID(catchType), CATCHES)
                    }
                    attributes foreach {
                        _ match {
                            case LocalVariableTableAttribute(localVariableTable) ⇒
                                for (LocalVariableTableEntry(_, _, _, fieldType, _) ← localVariableTable) {
                                    addDependency(methodID, getID(fieldType), HAS_LOCAL_VARIABLE_OF_TYPE)
                                }
                            case LocalVariableTypeTableAttribute(localVariableTypeTable) ⇒
                                for (LocalVariableTypeTableEntry(_, _, _, signature, _) ← localVariableTypeTable) {
                                    processSignature(signature, methodID)
                                }
                            case _ ⇒
                        }
                    }
                case sa: Signature ⇒
                    processSignature(sa, methodID)
                case _ ⇒
            }
        }
    }

    private def processSignature(signature: SignatureElement, srcID: Int, isInTypeParameters: Boolean = false) {
        def processFormalTypeParameters(formalTypeParameters: Option[List[FormalTypeParameter]]) {
            formalTypeParameters match {
                case Some(list) ⇒
                    for (FormalTypeParameter(_, classBound, interfaceBound) ← list) {
                        if (classBound.isDefined)
                            processSignature(classBound.get, srcID, true)
                        if (interfaceBound.isDefined)
                            processSignature(interfaceBound.get, srcID, true)
                    }
                case None ⇒
            }
        }

        def processSimpleClassTypeSignature(simpleClassTypeSignatures: SimpleClassTypeSignature) {
            val SimpleClassTypeSignature(_, typeArguments) = simpleClassTypeSignatures
            processTypeArguments(typeArguments)
        }

        def processTypeArguments(typeArguments: Option[List[TypeArgument]]) {
            typeArguments match {
                case Some(args) ⇒
                    args foreach {
                        _ match {
                            case ProperTypeArgument(_, fieldTypeSignature) ⇒
                                processSignature(fieldTypeSignature, srcID, true)
                            // case Wildcard ⇒
                            case _ ⇒
                        }
                    }
                case None ⇒
            }
        }

        signature match {
            case ClassSignature(formalTypeParameters, superClassSignature, superInterfacesSignature) ⇒
                processFormalTypeParameters(formalTypeParameters)
                processSignature(superClassSignature, srcID)
                superInterfacesSignature foreach { cts ⇒ processSignature(cts, srcID) }
            case MethodTypeSignature(formalTypeParameters, parametersTypeSignatures, returnTypeSignature, throwsSignature) ⇒
                processFormalTypeParameters(formalTypeParameters)
                parametersTypeSignatures foreach { pts ⇒ processSignature(pts, srcID) }
                processSignature(returnTypeSignature, srcID)
                throwsSignature foreach { ts ⇒ processSignature(ts, srcID) }
            case ArrayTypeSignature(typeSignature) ⇒
                processSignature(typeSignature, srcID)
            case cts: ClassTypeSignature ⇒
                if (isInTypeParameters)
                    addDependency(srcID, getID(cts.objectType), USES_TYPE_IN_TYPE_PARAMETERS)
                processSimpleClassTypeSignature(cts.simpleClassTypeSignature)
            case ProperTypeArgument(_, fieldTypeSignature) ⇒
                processSignature(fieldTypeSignature, srcID)
            // case TypeVariableSignature(identifier) ⇒
            // case VoidType ⇒
            case _ ⇒
        }
    }

    private def processElementValue(elementValue: ElementValue)(implicit srcID: Int, annotationDependencyType: DependencyType = ANNOTATED_WITH) {
        elementValue match {
            case ClassValue(returnType) ⇒
                addDependency(srcID, getID(returnType), USES_DEFAULT_CLASS_VALUE_TYPE)
            case EnumValue(enumType, constName) ⇒
                addDependency(srcID, getID(enumType), USES_DEFAULT_ENUM_VALUE_TYPE)
                addDependency(srcID, getID(enumType, constName), USES_ENUM_VALUE)
            case ArrayValue(values) ⇒
                values foreach { processElementValue(_) }
            case AnnotationValue(annotation) ⇒
                process(annotation)(srcID, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
            case _ ⇒
        }
    }

    private def process(annotation: Annotation)(implicit srcID: Int, dependencyType: DependencyType = ANNOTATED_WITH) {
        val Annotation(annotationType, elementValuePairs) = annotation

        addDependency(srcID, getID(annotationType), dependencyType)
        for (ElementValuePair(_, elementValue) ← elementValuePairs) {
            processElementValue(elementValue)
        }
    }

    private def isFinallyBlock(catchType: ObjectType): Boolean =
        catchType == null

    private def isEnclosedByMethod(ema: EnclosingMethodAttribute): Boolean =
        ema.name != null && ema.descriptor != null // otherwise the inner class is assigned to a field
}
