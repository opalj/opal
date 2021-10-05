/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.scalatest.funsuite.AnyFunSuite

/**
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class MethodDescriptorTest extends AnyFunSuite {

    test("Parsing: ()V") {
        val md = MethodDescriptor("()V")
        assert(md.parameterTypes.size.toInt == 0)
        assert(md.returnType.isVoidType)
    }

    test("Parsing: (III)I") {
        val md = MethodDescriptor("(III)I")
        assert(md.parameterTypes.size.toInt == 3)
        assert(md.parameterTypes(0).isIntegerType)
        assert(md.parameterTypes(1).isIntegerType)
        assert(md.parameterTypes(2).isIntegerType)
        assert(md.returnType.isIntegerType)
    }

    test("Parsing: ([I)[I") {
        val md = MethodDescriptor("([I)[I")
        assert(md.parameterTypes.size.toInt == 1)
        assert(md.parameterTypes(0).isArrayType)
        assert(md.returnType.isArrayType)
    }

    test("Parsing: ([[[III)[I") {
        val md = MethodDescriptor("([[[III)[I")
        assert(md.parameterTypes.size.toInt == 3)
        assert(md.parameterTypes(0).isArrayType)
        assert(md.parameterTypes(1).isIntegerType)
        assert(md.parameterTypes(2).isIntegerType)
        assert(md.returnType.isArrayType)
    }

    test("Parsing: (IDLjava/lang/Thread;)Ljava/lang/Object;") {
        val md = MethodDescriptor("(IDLjava/lang/Thread;)Ljava/lang/Object;")
        assert(md.parameterTypes.size.toInt == 3)
        assert(md.parameterTypes(0).isIntegerType)
        assert(md.parameterTypes(1).isDoubleType)
        assert(md.parameterTypes(2).isObjectType)
        assert(md.returnType.isObjectType)
    }

    test("Parsing: (IDLjava/lang/Thread;[J)[Ljava/lang/Object;") {
        val md = MethodDescriptor("(IDLjava/lang/Thread;[J)[Ljava/lang/Object;")
        assert(md.parameterTypes.size.toInt == 4)
        assert(md.parameterTypes(0).isIntegerType)
        assert(md.parameterTypes(1).isDoubleType)
        assert(md.parameterTypes(2).isObjectType)
        assert(md.parameterTypes(3).isArrayType)
        assert(md.returnType.isArrayType)
        assert(md match {
            case MethodDescriptor(
                Seq(
                    _: BaseType,
                    DoubleType,
                    ObjectType(_),
                    ArrayType(LongType)
                    ),
                ArrayType(ObjectType("java/lang/Object"))) => true
            case _ => false
        })
    }

    {
        val jvmDescriptors = Seq(
            "()V",
            "([[[III)[I",
            "([I)[I",
            "(JZSCIFD)V",
            "(IDLjava/lang/Thread;)Ljava/lang/Object;",
            "(IDLjava/lang/Thread;[J)[Ljava/lang/Object;"
        )
        jvmDescriptors.foreach { jvmDescriptor =>
            test(s"recreating JVM descriptor $jvmDescriptor") {
                assert(MethodDescriptor(jvmDescriptor).toJVMDescriptor === jvmDescriptor)
            }
        }
    }

}
