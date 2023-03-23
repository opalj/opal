/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package ifds

import org.opalj.br.analyses.DeclaredMethods
import org.opalj.ll.llvm.value.constant.ConstantDataArray
import org.opalj.ll.llvm.value.constant.GetElementPtrConst
import org.opalj.ll.llvm.value.Argument
import org.opalj.ll.llvm.value.Call
import org.opalj.ll.llvm.value.GetElementPtr
import org.opalj.ll.llvm.value.GlobalVariable
import org.opalj.ll.llvm.value.Load
import org.opalj.ll.llvm.value.Store
import org.opalj.ll.llvm.value.Value
import org.opalj.ll.llvm.PointerType
import org.opalj.ll.llvm.StructType

object JNICallUtil {

    /**
     * Checks whether the call is a call to the JNI interface.
     * This is done by the assumption that the first parameter of such a call is a struct of type "struct.JNINativeInterface_"
     */
    def isJNICall(call: Call): Boolean = call.calledFunctionType.params.headOption match {
        case Some(firstParam) =>
            firstParam match {
                case p1: PointerType =>
                    p1.element match {
                        case p2: PointerType =>
                            p2.element match {
                                case struct: StructType if struct.name == "struct.JNINativeInterface_" =>
                                    true
                                case other => false
                            }
                        case _ => false
                    }
                case _ => false
            }
        case _ => false
    }

    def resolve(call: Call)(implicit declaredMethods: DeclaredMethods): Set[_ <: NativeFunction] =
        resolveJNIFunction(call) match {
            case Symbol("CallTypeMethod") => resolveMethodId(call.operand(2)) // methodID is the third parameter
            case _                        => Set()
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
                if (!functionName.startsWith("Java_")) {
                    throw new IllegalArgumentException("unexpected function name")
                }
                val className = call.function.name.substring(5).split("_").head
                val name = resolveString(call.operand(2)) // name is the third parameter
                val signature = resolveString(call.operand(3)) // signature is the third parameter
                findJavaMethods(className, name, signature)
            }
            case _ => Set.empty
        }.toSet
    }

    private def resolveString(name: Value): String = name match {
        case gep: GetElementPtrConst => gep.base match {
            case global: GlobalVariable => global.initializer match {
                case stringData: ConstantDataArray => stringData.asString
            }
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
        className: String,
        name:      String,
        signature: String
    )(implicit declaredMethods: DeclaredMethods): Set[JNIMethod] = {
        declaredMethods.declaredMethods.filter(declaredMethod => {
            val classType = declaredMethod.declaringClassType
            (classType.simpleName == className &&
                declaredMethod.name == name &&
                declaredMethod.descriptor.toJVMDescriptor == signature)
        }).map(_.definedMethod).map(JNIMethod).toSet
    }
}
