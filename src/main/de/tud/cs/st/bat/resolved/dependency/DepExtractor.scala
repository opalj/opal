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

import java.lang.Integer
import DependencyType._
import de.tud.cs.st.bat.AccessFlagsContexts._
import de.tud.cs.st.bat.ACC_STATIC
import de.tud.cs.st.bat.AccessFlagsIterator

/**
 * DepExtractor can process a ClassFile and extract all dependencies between
 * class, interfaces, fields and methods. The dependencies are passed to
 * the addDep-method provided by the given DepBuilder.
 *
 * @author Thomas Schlosser
 */
class DepExtractor(val builder: DepBuilder) extends InstructionDepExtractor {

  import builder._

  def process(clazz: ClassFile) {
    val thisClassID = getID(clazz)
    val ClassFile(_, _, _, thisClass, superClass, interfaces, fields, methods, attributes) = clazz

    // process super class
    addDep(thisClassID, getID(superClass), EXTENDS)

    // process interfaces
    for (interface <- interfaces) {
      addDep(thisClassID, getID(interface), IMPLEMENTS)
    }

    //process attributes
    for (attribute <- attributes) {
      attribute match {
        case Annotations_Attribute(_, annotations) =>
          for (annotation <- annotations) {
            process(annotation)(thisClassID)
          }
        case ema: EnclosingMethod_attribute => {
          val EnclosingMethod_attribute(enclosingClazz, enclosingMethodName, enclosingMethodDescriptor) = ema
          if (isEnclosedByMethod(ema)) {
            addDep(thisClassID, getID(enclosingClazz, enclosingMethodName, enclosingMethodDescriptor), IS_INNER_CLASS_OF)
          } else {
            addDep(thisClassID, getID(enclosingClazz), IS_INNER_CLASS_OF)
          }
        }
        case InnerClasses_attribute(innerClasses) =>
          for (InnerClassesEntry(innerClassType, outerClassType, _, _) <- innerClasses) {
            if (outerClassType != null && outerClassType == thisClass) {
              addDep(getID(innerClassType), thisClassID, IS_INNER_CLASS_OF)
            }
          }
        case sa: Signature =>
        //TODO: impl.
        case _ =>
      }
    }

    //process fields
    for (field <- fields) {
      process(field)(thisClass, thisClassID)
    }

    //process methods
    for (method <- methods) {
      process(method)(thisClass, thisClassID)
    }
  }

  private def process(field: Field_Info)(implicit thisClass: ObjectType, thisClassID: Int) {
    val fieldID = getID(thisClass, field)
    val Field_Info(accessFlags, _, FieldDescriptor(fieldType), attributes) = field

    addDep(fieldID, thisClassID, if (ACC_STATIC ∈ accessFlags) IS_CLASS_MEMBER_OF else IS_INSTANCE_MEMBER_OF)
    addDep(fieldID, getID(fieldType), IS_OF_TYPE)

    //process attributes
    for (attribute <- attributes) {
      attribute match {
        case Annotations_Attribute(_, annotations) =>
          for (annotation <- annotations) {
            process(annotation)(fieldID)
          }
        case cv: ConstantValue[_] =>
          addDep(fieldID, getID(cv.valueType), USES_CONSTANT_VALUE_OF_TYPE)
        case sa: Signature =>
        //TODO: impl.
        case _ =>
      }
    }
  }

  private def process(method: Method_Info)(implicit thisClass: ObjectType, thisClassID: Int) {
    implicit val methodID = getID(thisClass, method)
    val Method_Info(accessFlags, _, MethodDescriptor(parameterTypes, returnType), attributes) = method

    addDep(methodID, thisClassID, if (ACC_STATIC ∈ accessFlags) IS_CLASS_MEMBER_OF else IS_INSTANCE_MEMBER_OF)
    addDep(methodID, getID(returnType), RETURNS)
    for (paramType <- parameterTypes) {
      addDep(methodID, getID(paramType), HAS_PARAMETER_OF_TYPE)
    }

    //process attributes
    for (attribute <- attributes) {
      attribute match {
        case Annotations_Attribute(_, annotations) =>
          for (annotation <- annotations) {
            process(annotation)(methodID)
          }
        case ParameterAnnotations_attribute(_, parameterAnnotations) =>
          for (annotations <- parameterAnnotations) {
            for (annotation <- annotations) {
              process(annotation)(methodID, PARAMETER_ANNOTATED_WITH)
            }
          }
        case Exceptions_attribute(exceptionTable) =>
          for (exception <- exceptionTable) {
            addDep(methodID, getID(exception), THROWS)
          }
        case AnnotationDefault_attribute(elementValue) => {
          processElementValue(elementValue)(methodID)
        }
        case Code_attribute(_, _, code, exceptionTable, attributes) =>
          process(methodID, code)
          for (ExceptionTableEntry(_, _, _, catchType) <- exceptionTable) {
            if (!isFinallyBlock(catchType)) {
              addDep(methodID, getID(catchType), CATCHES)
            }
          }
          for (attr <- attributes) {
            attr match {
              case LocalVariableTable_attribute(localVariableTable) =>
                for (LocalVariableTableEntry(_, _, _, fieldType, _) <- localVariableTable) {
                  addDep(methodID, getID(fieldType), HAS_LOCAL_VARIABLE_OF_TYPE)
                }
              case LocalVariableTypeTable_attribute(localVariableTypeTable) =>
                for (LocalVariableTypeTableEntry(_, _, _, signature, _) <- localVariableTypeTable) {
                  //TODO: impl.
                }
              case _ =>
            }
          }
        case sa: Signature =>
        //TODO: impl.
        case _ =>
      }
    }
  }

  private def processElementValue(elementValue: ElementValue)(implicit srcID: Int, annotationDepType: DependencyType = ANNOTATED_WITH) {
    elementValue match {
      case ClassValue(returnType) =>
        addDep(srcID, getID(returnType), USES_DEFAULT_CLASS_VALUE_TYPE)
      case EnumValue(enumType, constName) =>
        addDep(srcID, getID(enumType), USES_DEFAULT_ENUM_VALUE_TYPE)
        addDep(srcID, getID(enumType, constName), USES_ENUM_VALUE)
      case ArrayValue(values) =>
        for (value <- values) {
          processElementValue(value)
        }
      case AnnotationValue(annotation) =>
        process(annotation)(srcID, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
      case _ =>
    }
  }

  private def process(annotation: Annotation)(implicit srcID: Int, depType: DependencyType = ANNOTATED_WITH) {
    val Annotation(annotationType, elementValuePairs) = annotation

    addDep(srcID, getID(annotationType), depType)
    for (ElementValuePair(_, elementValue) <- elementValuePairs) {
      processElementValue(elementValue)
    }
  }

  private def isFinallyBlock(catchType: ObjectType): Boolean =
    catchType == null

  private def isEnclosedByMethod(ema: EnclosingMethod_attribute): Boolean =
    ema.name != null && ema.descriptor != null // otherwise the inner class is assigned to a field
}
