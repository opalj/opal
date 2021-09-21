/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

/**
 * Tests instantiation of [[Instruction]]s using the convenience factory methods.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class FactoryMethodsOfInstructionTest extends AnyFlatSpec {

    behavior of "factory methods of Instructions"

    val declaringClass = "my/invoke/Class"

    val methodName = "myMythod"
    val methodDescriptor = "()V"

    val fieldName = "myField"
    val fieldTypeObject = "Ljava/lang/Object"
    val fieldTypeBoolean = "Z"
    val fieldTypeBooleanArray = "[Z"

    "INVOKEINTERFACE's factory method" should "return an INVOKEINTERFACE instruction" in {
        val invoke = INVOKEINTERFACE(declaringClass, methodName, methodDescriptor)

        assert(invoke.getClass.getName == "org.opalj.br.instructions.INVOKEINTERFACE")
        assert(invoke.declaringClass.fqn == declaringClass)
        assert(invoke.name == methodName)
        assert(invoke.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

    "INVOKEVIRTUAL's factory method" should "return an INVOKEVIRTUAL instruction" in {
        val invoke = INVOKEVIRTUAL(declaringClass, methodName, methodDescriptor)

        assert(invoke.getClass.getName == "org.opalj.br.instructions.INVOKEVIRTUAL")
        assert(invoke.declaringClass.asObjectType.fqn == declaringClass)
        assert(invoke.name == methodName)
        assert(invoke.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

    "INVOKESPECIAL's factory method" should "return an INVOKESPECIAL instruction" in {
        val invoke = INVOKESPECIAL(declaringClass, false, methodName, methodDescriptor)

        assert(invoke.getClass.getName == "org.opalj.br.instructions.INVOKESPECIAL")
        assert(invoke.declaringClass.fqn == declaringClass)
        assert(!invoke.isInterface)
        assert(invoke.name == methodName)
        assert(invoke.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

    "INVOKESTATIC's factory method" should "return an INVOKESTATIC instruction" in {
        val invoke = INVOKESTATIC(declaringClass, false, methodName, methodDescriptor)

        assert(invoke.getClass.getName == "org.opalj.br.instructions.INVOKESTATIC")
        assert(invoke.declaringClass.fqn == declaringClass)
        assert(!invoke.isInterface)
        assert(invoke.name == methodName)
        assert(invoke.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

    "GETSTATIC's factory method" should "return an GETSTATIC instruction" in {
        val getStaticObject = GETSTATIC(declaringClass, fieldName, fieldTypeObject)

        assert(getStaticObject.getClass.getName == "org.opalj.br.instructions.GETSTATIC")
        assert(getStaticObject.declaringClass.fqn == declaringClass)
        assert(getStaticObject.fieldType.asObjectType.fqn ==
            fieldTypeObject.substring(1, fieldTypeObject.length - 1))

        val getStaticBoolean = GETSTATIC(declaringClass, fieldName, fieldTypeBoolean)
        assert(getStaticBoolean.getClass.getName == "org.opalj.br.instructions.GETSTATIC")
        assert(getStaticBoolean.declaringClass.fqn == declaringClass)
        assert(getStaticBoolean.fieldType.asBaseType.isBooleanType)
    }

    "PUTSTATIC's factory method" should "return an PUTSTATIC instruction" in {
        val getStaticObject = PUTSTATIC(declaringClass, fieldName, fieldTypeObject)

        assert(getStaticObject.getClass.getName == "org.opalj.br.instructions.PUTSTATIC")
        assert(getStaticObject.declaringClass.fqn == declaringClass)
        assert(getStaticObject.fieldType.asObjectType.fqn ==
            fieldTypeObject.substring(1, fieldTypeObject.length - 1))

        val getStaticBoolean = PUTSTATIC(declaringClass, fieldName, fieldTypeBoolean)
        assert(getStaticBoolean.getClass.getName == "org.opalj.br.instructions.PUTSTATIC")
        assert(getStaticBoolean.declaringClass.fqn == declaringClass)
        assert(getStaticBoolean.fieldType.asBaseType.isBooleanType)
    }

    "ANEWARRAY's factory method" should "return an ANEWARRAY instruction" in {
        val anewarrayObject = ANEWARRAY(fieldTypeObject)

        assert(anewarrayObject.getClass.getName == "org.opalj.br.instructions.ANEWARRAY")
        assert(anewarrayObject.componentType.isObjectType)
    }

    "MULTIANEWARRAY's factory method" should "return an MULTIANEWARRAY instruction" in {
        val multianewarrayObject = MULTIANEWARRAY(fieldTypeBooleanArray, 1)

        assert(multianewarrayObject.getClass.getName == "org.opalj.br.instructions.MULTIANEWARRAY")
        assert(multianewarrayObject.arrayType.componentType.isBooleanType)
        assert(multianewarrayObject.dimensions == 1)
    }

    "MULTIANEWARRAY's factory method" should "catch the error if the dimensions don't fit" in {
        assertThrows[IllegalArgumentException] { MULTIANEWARRAY(fieldTypeBooleanArray, 2) }
    }

    "INSTANCEOF's factory method" should "return an INSTANCEOF instruction" in {
        val instanceOfObject = INSTANCEOF(fieldTypeObject)

        assert(instanceOfObject.getClass.getName == "org.opalj.br.instructions.INSTANCEOF")
        assert(instanceOfObject.referenceType.isObjectType)

        val instanceOfArray = INSTANCEOF(fieldTypeBooleanArray)

        assert(instanceOfArray.getClass.getName == "org.opalj.br.instructions.INSTANCEOF")
        assert(instanceOfArray.referenceType.asArrayType.componentType.isBooleanType)
    }

    "CHECKCAST's factory method" should "return an CHECKCAST instruction" in {
        val checkcastObject = CHECKCAST(fieldTypeObject)

        assert(checkcastObject.getClass.getName == "org.opalj.br.instructions.CHECKCAST")
        assert(checkcastObject.referenceType.isObjectType)
    }
}
