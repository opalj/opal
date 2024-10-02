/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package ifds

import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.ll.llvm.StructType
import org.opalj.ll.llvm.value.Argument
import org.opalj.ll.llvm.value.Call
import org.opalj.ll.llvm.value.GetElementPtr
import org.opalj.ll.llvm.value.GlobalVariable
import org.opalj.ll.llvm.value.Load
import org.opalj.ll.llvm.value.Store
import org.opalj.ll.llvm.value.Value
import org.opalj.ll.llvm.value.constant.ConstantDataArray

/**
 * Collection of utility methods to handle JNI Methods
 *
 * @author Nicolas Gross
 * @author Marc Clement
 */
object JNICallUtil {

    /**
     * Checks whether the call is a call to the JNI interface.
     * This is done by the assumption that the first parameter of such a call is a struct of type "struct.JNINativeInterface_"
     */
    def isJNICall(call: Call): Boolean = call.calledValue match {
        case load: Load => load.src match {
            case gep: GetElementPtr => gep.sourceElementType match {
                case struct: StructType if struct.name == "struct.JNINativeInterface_" => true
                case _ => false
            }
            case _ => false
        }
        case _ => false
    }

    /**
     * Converts a Java parameter index (param 0 == this if non-static) to the respective index
     * in a JNICall of this method.
     *
     * @param index the Java parameter index.
     * @param isStatic whether the Java method is static.
     * @return the respective native parameter index in the JNICall.
     */
    def javaParamIndexToNative(index: Int, isStatic: Boolean): Int = {
        // JNI call args if static: JNIEnv, class, method, arg 0, arg 1, ...
        // JNI call args if non-static: JNIEnv, this, method, arg 0, arg 1, ...
        if (isStatic) {
            // static call, tainted arg
            index + 3
        } else if (index == 0) {
            // non-static call, tainted this
            1
        } else {
            // non-static call, tainted arg
            index + 2
        }
    }

    /**
     * Converts a native JNI function parameter index to a Java parameter index (arg 0 == this if non-static).
     *
     * @param index the native JNICall parameter index.
     * @param isStatic whether the Java method is static.
     * @return the respective Java parameter index.
     */
    def nativeParamIndexToJava(index: Int, isStatic: Boolean): Int = {
        // JNI function args if static: JNIEnv, arg 0, arg 1, ...
        // JNI function args if non-static: JNIEnv, this, arg 0, arg 1, ...
        index - 1
    }

    def resolve(call: Call)(implicit declaredMethods: DeclaredMethods): Set[_ <: NativeFunction] = resolveJNIFunction(call) match {
        case Symbol("CallTypeMethod") => resolveMethodId(call.operand(2)) // methodID is the third parameter
        case _                        => Set()
    }

    /**
     * Resolves a Java native method name to the actual function name in the .so file.
     *
     * https://docs.oracle.com/en/java/javase/13/docs/specs/jni/design.html#resolving-native-method-names
     *
     * @param nativeMethod the 'native' tagged Java method.
     * @return the resolved function name.
     */
    def resolveNativeFunctionName(nativeMethod: Method): String = {
        val calleeName = nativeMethod.name.map {
            case c if isAlphaNumeric(c) => c
            case '_'                    => "_1"
            case ';'                    => "_2"
            case '['                    => "_3"
            case c                      => s"_${c.toInt.toHexString.reverse.padTo(4, '0').reverse}"
        }.mkString
        val classFile = nativeMethod.classFile.fqn
            .replace("_", "_1")
            .replace('/', '_')
        "Java_" + classFile + "_" + calleeName
    }

    /**
     * Resolves a native function to the class and method name of the corresponding Java companion method.
     *
     * @param nativeFunction the native function from the .so file.
     * @return A tuple (fqn, method name) if function has native Java companion, otherwise None.
     */
    def resolveNativeMethodName(nativeFunction: LLVMFunction): Option[(String, String)] = {
        if (!nativeFunction.name.startsWith("Java_")) return None
        val nameWithoutJava = nativeFunction.name.substring(5)

        // split class name and method name
        var lastCharWasDigit = true
        var methodNameIndex = -1
        val iterator = nameWithoutJava.zipWithIndex.reverseIterator
        while (methodNameIndex == -1 && iterator.hasNext) {
            val (c, i) = iterator.next()
            if (!lastCharWasDigit && c == '_') methodNameIndex = i
            if (c >= '0' && c <= '9') lastCharWasDigit = true
            else lastCharWasDigit = false
        }
        val parts = nameWithoutJava.splitAt(methodNameIndex)

        // decode class name
        val fqnDecoded = "_([^0-9])".r
            .replaceAllIn(parts._1, m => "/" + m.group(1))
            .replace("_1", "_")

        // decode method name
        val tmpMethodDecoded = parts._2.substring(1)
            .replace("_1", "_")
            .replace("_2", ";")
            .replace("_3", "[")
        val regexHexEnc = "_([0-9A-Fa-f]{4})".r
        val methodDecoded = regexHexEnc
            .replaceAllIn(tmpMethodDecoded, m => Integer.parseInt(m.group(1), 16).toChar.toString)

        Some((fqnDecoded, methodDecoded))
    }

    private def isAlphaNumeric(char: Char): Boolean = {
        char >= 'a' && char <= 'z' || char >= 'A' && char <= 'Z' || char >= '0' && char <= '9'
    }

    private def resolveJNIFunction(call: Call): Symbol = call.calledValue match {
        case load: Load =>
            load.src match {
                // https://docs.oracle.com/en/java/javase/13/docs/specs/jni/functions.html has the indices
                case gep: GetElementPtr if gep.isConstant => gep.constants.tail.head match {
                    case 31      => Symbol("GetObjectClass")
                    case 33      => Symbol("GetMethodId")
                    case 49 | 61 => Symbol("CallTypeMethod") // CallIntMethod | CallVoidMethod
                    case index   => throw new IllegalArgumentException(s"unknown JNI function index ${index}")
                }
                case _ => throw new IllegalArgumentException("unknown JNI load src")
            }
        case _ => throw new IllegalArgumentException("unknown JNI call argument")
    }

    private def resolveMethodId(methodId: Value)(implicit declaredMethods: DeclaredMethods): Set[JNIMethod] = {
        val sources = methodId.asInstanceOf[Load].src.users.collect { case store: Store => store.src }
        sources.flatMap {
            case call: Call => {
                if (resolveJNIFunction(call) != Symbol("GetMethodId")) throw new IllegalArgumentException("unexpected call")
                if (!resolveClassIsThis(call.operand(1))) // class is the second parameter
                    throw new IllegalArgumentException("unexpected class argument")

                val functionName = call.function.name
                if (!functionName.startsWith("Java_"))
                    throw new IllegalArgumentException("unexpected function name")

                val className = resolveNativeMethodName(LLVMFunction(call.function)).get._1
                val name = resolveString(call.operand(2)) // name is the third parameter
                val signature = resolveString(call.operand(3)) // signature is the third parameter
                findJavaMethods(className, name, signature)
            }
            case _ => Set.empty
        }.toSet
    }

    private def resolveString(name: Value): String = name match {
        case global: GlobalVariable => global.initializer match {
            case stringData: ConstantDataArray => stringData.asString
        }
    }

    private def resolveClassIsThis(clazz: Value): Boolean = {
        clazz.asInstanceOf[Load].src.users.forall {
            case store: Store => store.src match {
                case call: Call =>
                    if (resolveJNIFunction(call) != Symbol("GetObjectClass")) {
                        throw new IllegalArgumentException("unexpected call")
                    }
                    resolveObjectIsThis(call.operand(1)) // object is the second parameter
                case _ => false
            }
            case _ => true // We do not care if clazz is not a Store
        }
    }

    private def resolveObjectIsThis(obj: Value): Boolean = {
        obj.asInstanceOf[Load].src.users.forall {
            case store: Store => store.src match {
                case argument: Argument => argument.index == 1
                case _                  => false
            }
            case _ => true // We do not care if obj is not a Store
        }
    }

    private def findJavaMethods(
        classFqn:   String,
        methodName: String,
        signature:  String
    )(implicit declaredMethods: DeclaredMethods): Set[JNIMethod] = {
        declaredMethods.declaredMethods.filter(declaredMethod => {
            val classType = declaredMethod.declaringClassType
            (classType.fqn == classFqn &&
                declaredMethod.name == methodName &&
                declaredMethod.descriptor.toJVMDescriptor == signature)
        }).map(_.definedMethod).map(JNIMethod).toSet
    }
}
