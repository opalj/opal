package de.tud.cs.st.bat.dependency
import java.lang.Integer
import de.tud.cs.st.bat.resolved.ClassFile
import de.tud.cs.st.bat.resolved.ObjectType
import de.tud.cs.st.bat.resolved.Field_Info
import de.tud.cs.st.bat.resolved.Method_Info
import de.tud.cs.st.bat.resolved.SourceFile_attribute
import de.tud.cs.st.bat.resolved.EnclosingMethod_attribute
import de.tud.cs.st.bat.resolved.Annotations_Attribute
import de.tud.cs.st.bat.resolved.ParameterAnnotations_attribute
import de.tud.cs.st.bat.resolved.Exceptions_attribute
import de.tud.cs.st.bat.resolved.AnnotationDefault_attribute
import de.tud.cs.st.bat.resolved.Code_attribute
import de.tud.cs.st.bat.resolved.MethodDescriptor

class DepGraphExtractor(gBuilder: DepGraphBuilder) extends ClassFileProcessor {

  private val FIELD_AND_METHOD_SEPARATOR = "."

  def process(clazz: ClassFile) {
    val thisClassName = getName(clazz.thisClass)
    val thisClassID = getID(clazz.thisClass)

    // process super class
    val superClassID = getID(clazz.superClass)
    gBuilder.addEdge(thisClassID, superClassID, EXTENDS)

    // process interfaces
    for (interface <- clazz.interfaces) {
      val interfaceID = getID(interface)
      gBuilder.addEdge(thisClassID, interfaceID, IMPLEMENTS)
    }

    //process attributes
    for (attribute <- clazz.attributes) {
      attribute match {
        case sfa: SourceFile_attribute => Nil
        case aa: Annotations_Attribute =>
          for (annotation <- aa.annotations) {
            gBuilder.addEdge(thisClassID, getID(annotation.annotationType), ANNOTATION_TYPE)
          }
        case ema: EnclosingMethod_attribute => Nil
        case _ => Nil
      }
    }

    //process fields
    for (field <- clazz.fields) {
      val fieldID = getID(thisClassName, field)
      gBuilder.addEdge(fieldID, thisClassID, IS_DEFINED_IN)
      gBuilder.addEdge(fieldID, getID(field.descriptor), FIELD_TYPE)
    }

    //process methods
    for (method <- clazz.methods) {
      val methodID = getID(thisClassName, method)
      gBuilder.addEdge(methodID, thisClassID, IS_DEFINED_IN)
      gBuilder.addEdge(methodID, getID(method.descriptor.returnType), RETURN_TYPE)
      for (paramType <- method.descriptor.parameterTypes) {
        gBuilder.addEdge(methodID, getID(paramType), PARAMETER_TYPE)
      }
      for (attribute <- method.attributes) {
        attribute match {
          case aa: Annotations_Attribute =>
            for (annotation <- aa.annotations) {
              gBuilder.addEdge(methodID, getID(annotation.annotationType), ANNOTATION_TYPE)
            }
          case paa: ParameterAnnotations_attribute => Nil
          case ea: Exceptions_attribute =>
            for (exception <- ea.exceptionTable) {
              gBuilder.addEdge(methodID, getID(exception), EXCEPTION_TYPE)
            }
          case ada: AnnotationDefault_attribute => Nil
          case ca: Code_attribute =>
            for (instr <- ca.code) {
              if (instr != null) {
                for ((cType, name, methodDescriptor, eType) <- instr.dependencies) {
                  gBuilder.addEdge(methodID, getID(cType, name, methodDescriptor), eType)
                  if (methodDescriptor != null) {
                    gBuilder.addEdge(methodID, getID(methodDescriptor.returnType), USED_TYPE)
                    for (paramType <- methodDescriptor.parameterTypes) {
                      gBuilder.addEdge(methodID, getID(paramType), USED_TYPE)
                    }
                  }
                }
              }
            }
          case _ => Nil
        }
      }
    }
  }

  private def getName(obj: { def toJava: String }): String = obj.toJava
  private def getID(name: String): Integer = if (filter(name)) null else gBuilder.getID(name)
  private def getID(obj: { def toJava: String }): Integer = getID(getName(obj))
  private def getID(className: String, name: String, methodDescriptor: MethodDescriptor): Integer = getID(className + { if (name != null) FIELD_AND_METHOD_SEPARATOR + name + { if (methodDescriptor != null) "(" + methodDescriptor.parameterTypes.map(pT => getName(pT)).mkString(", ") + ")" else "" } else "" })
  private def getID(className: String, field: Field_Info): Integer = getID(className, field.name, null)
  private def getID(className: String, method: Method_Info): Integer = getID(className, method.name, method.descriptor)
  private def getID(obj: { def toJava: String }, name: String, methodDescriptor: MethodDescriptor): Integer = getID(if (obj != null) getName(obj) else "", name, methodDescriptor)

  private def filter(name: String): Boolean = {
    name match {
      case "byte" | "short" | "int" | "long" | "float" | "double" | "char" | "boolean" | "void" => true
      case _ => false
    }
  }
}