package de.tud.cs.st.bat.resolved
package dependency

import java.lang.Integer
import DependencyType._

class DepExtractor(val builder: DepBuilder) extends InstructionDepExtractor {

  val FIELD_AND_METHOD_SEPARATOR = "."

  def process(clazz: ClassFile) {
    implicit val thisClassName = getName(clazz.thisClass)
    implicit val thisClassID = getID(clazz.thisClass)

    // process super class
    val superClassID = getID(clazz.superClass)
    builder.addDep(thisClassID, superClassID, EXTENDS)

    // process interfaces
    for (interface <- clazz.interfaces) {
      val interfaceID = getID(interface)
      builder.addDep(thisClassID, interfaceID, IMPLEMENTS)
    }

    //process attributes
    for (attribute <- clazz.attributes) {
      attribute match {
        case aa: Annotations_Attribute =>
          for (annotation <- aa.annotations) {
            process(annotation)
          }
        case ema: EnclosingMethod_attribute => {
          if (isEnclosedByMethod(ema)) {
            builder.addDep(thisClassID, getID(ema.clazz, ema.name, ema.descriptor), IS_DEFINED_IN)
          }
          //TODO: where should the IS_DEFINED_IN dependency of the inner class refer to -- in the else case?
        }
        case ica: InnerClasses_attribute =>
          for (c <- ica.classes) {
            if (c.outerClassType != null && c.outerClassType.eq(clazz.thisClass)) {
              builder.addDep(getID(c.innerClassType), thisClassID, IS_DEFINED_IN)
            }
          }
        case _ => Nil
      }
    }

    //process fields
    for (field <- clazz.fields) {
      process(field)
    }

    //process methods
    for (method <- clazz.methods) {
      process(method)
    }
  }

  private def process(field: Field_Info)(implicit thisClassName: String, thisClassID: Option[Int]) {
    implicit val fieldID = getID(thisClassName, field)
    builder.addDep(fieldID, thisClassID, IS_DEFINED_IN)
    builder.addDep(fieldID, getID(field.descriptor), FIELD_TYPE)
    //process attributes
    for (attribute <- field.attributes) {
      attribute match {
        case aa: Annotations_Attribute =>
          for (annotation <- aa.annotations) {
            process(annotation)(fieldID)
          }
        case cva: ConstantValue_attribute =>
          builder.addDep(fieldID, getID(cva.constantValue.valueType), CONSTANT_VALUE_TYPE)
        case _ => Nil
      }
    }
  }

  private def process(method: Method_Info)(implicit thisClassName: String, thisClassID: Option[Int]) {
    implicit val methodID = getID(thisClassName, method)
    builder.addDep(methodID, thisClassID, IS_DEFINED_IN)
    builder.addDep(methodID, getID(method.descriptor.returnType), RETURN_TYPE)
    for (paramType <- method.descriptor.parameterTypes) {
      builder.addDep(methodID, getID(paramType), PARAMETER_TYPE)
    }

    //process attributes
    for (attribute <- method.attributes) {
      attribute match {
        case aa: Annotations_Attribute =>
          for (annotation <- aa.annotations) {
            process(annotation)(methodID)
          }
        case paa: ParameterAnnotations_attribute =>
          for (annotations <- paa.parameterAnnotations) {
            for (annotation <- annotations) {
              process(annotation)(methodID, PARAMETER_ANNOTATION_TYPE)
            }
          }
        case ea: Exceptions_attribute =>
          for (exception <- ea.exceptionTable) {
            builder.addDep(methodID, getID(exception), THROWS_EXCEPTION_TYPE)
          }
        case ada: AnnotationDefault_attribute => {
          processElementValue(ada.elementValue)(methodID)
        }
        case ca: Code_attribute =>
          process(methodID, ca.code)
          for (exception <- ca.exceptionTable) {
            if (!isFinallyBlock(exception.catchType)) {
              builder.addDep(methodID, getID(exception.catchType), CATCHED_EXCEPTION_TYPE)
            }
          }
        //TODO: add tests for this attribute (first, the issue that states that this attribute is not transformed into the BAT model has to be solved)
        case lvta: LocalVariableTable_attribute =>
          for (lvte <- lvta.localVariableTable) {
            builder.addDep(methodID, getID(lvte.fieldType), LOCAL_VARIABLE_TYPE)
          }
        case _ => Nil
      }
    }
  }

  private def processElementValue(elementValue: ElementValue)(implicit srcID: Option[Int], annotationDepType: DependencyType = ANNOTATION_TYPE) {
    elementValue match {
      case ClassValue(returnType) =>
        builder.addDep(srcID, getID(returnType), USED_DEFAULT_CLASS_VALUE_TYPE)
      case EnumValue(enumType, constName) =>
        builder.addDep(srcID, getID(enumType), USED_DEFAULT_ENUM_VALUE_TYPE)
        builder.addDep(srcID, getID(enumType, constName), USED_ENUM_VALUE)
      case ArrayValue(values) =>
        for (value <- values) {
          processElementValue(value)
        }
      case AnnotationValue(annotation) =>
        process(annotation)(srcID, USED_DEFAULT_ANNOTATION_VALUE_TYPE)
      case _ => Nil
    }
  }

  private def process(annotation: Annotation)(implicit srcID: Option[Int], depType: DependencyType = ANNOTATION_TYPE) {
    builder.addDep(srcID, getID(annotation.annotationType), depType)
    for (elemValuePair <- annotation.elementValuePairs) {
      processElementValue(elemValuePair.elementValue)
    }
  }

  private def isFinallyBlock(catchType: ObjectType): Boolean =
    catchType == null

  private def isEnclosedByMethod(ema: EnclosingMethod_attribute): Boolean =
    ema.name != null && ema.descriptor != null // otherwise the inner class is assigned to a field

  protected def filter(name: String): Boolean = {
    for (swFilter <- Array("byte", "short", "int", "long", "float", "double", "char", "boolean", "void")) {
      if (name.startsWith(swFilter)) {
        return true
      }
    }
    false
  }
}
