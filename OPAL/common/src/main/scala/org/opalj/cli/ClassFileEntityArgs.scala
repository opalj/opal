/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.stringListConverter

object ClassNameArg extends PlainArg[List[String]] {
    override val name: String = "class"
    override val description: String = "Fully-qualified class name of class to analyze, e.g., java.util.HashMap"
}

object FieldNameArg extends ParsedArg[List[String], List[(String, String)]] {
    override val name: String = "field"
    override val description: String =
        "Fully-qualified class name and field name to analyze, e.g., java.util.HashMap.entrySet"

    override def parse(arg: List[String]): List[(String, String)] = {
        arg.map { fieldName =>
            val fieldIndex = fieldName.lastIndexOf('.')
            (fieldName.take(fieldIndex), fieldName.drop(fieldIndex + 1))
        }
    }
}

object MethodNameArg extends ParsedArg[List[String], List[(String, String)]] {
    override val name: String = "method"
    override val description: String =
        "Fully-qualified class name and method name to analyze, e.g., java.util.HashMap.get"

    override def parse(arg: List[String]): List[(String, String)] = {
        arg.map { methodName =>
            val methodIndex = methodName.lastIndexOf('.')
            (methodName.take(methodIndex), methodName.drop(methodIndex + 1))
        }
    }
}

object PartialSignatureArg extends ParsedArg[List[String], List[(String, String, String)]] {
    override val name: String = "method"
    override val description: String =
        "Fully-qualified class name and (partial) method signature to analyze, e.g., java.util.HashMap.get(java.lang.Object"

    override def parse(arg: List[String]): List[(String, String, String)] = {
        arg.map { methodSignature =>
            val (classAndMethod, descriptorString) = methodSignature.splitAt(methodSignature.indexOf('('))
            val methodNameIndex = classAndMethod.lastIndexOf('.')
            val className = classAndMethod.take(methodNameIndex).replace('.', '/')
            val methodName = className.drop(methodNameIndex + 1)

            (className, methodName, descriptorString)
        }
    }

}
