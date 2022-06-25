/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.ll.llvm.value.constant.{ConstantDataArray, GetElementPtrConst}
import org.opalj.ll.llvm.value.{Argument, Call, GetElementPtr, GlobalVariable, Load, Store, Value}
import org.opalj.ll.llvm.{PointerType, StructType}

object JNICallUtil {

    /**
     * Checks whether the call is a call to the JNI interface.
     * This is done by the assumption that every such calls first parameter is a struct of type "struct.JNINativeInterface_"
     */
    def isJNICall(call: Call): Boolean = call.calledFunctionType.params.headOption match {
        case Some(firstParam) ⇒
            firstParam match {
                case p1: PointerType ⇒
                    p1.element match {
                        case p2: PointerType ⇒
                            p2.element match {
                                case struct: StructType if struct.name == "struct.JNINativeInterface_" ⇒
                                    true
                                case other ⇒ false
                            }
                        case _ ⇒ false
                    }
                case _ ⇒ false
            }
        case _ ⇒ false
    }

    def resolve(call: Call): Set[NativeFunction] = resolveJNIFunction(call) match {
        case 'CallTypeMethod ⇒ {
            resolveMethodId(call.operand(2))
            Set() // TODO
        } // methodID is the third parameter
        case _ ⇒ Set()
    }

    private def resolveJNIFunction(call: Call): Symbol = call.calledValue match {
        case load: Load ⇒
            load.src match {
                // https://docs.oracle.com/en/java/javase/13/docs/specs/jni/functions.html has the indices
                case gep: GetElementPtr if gep.isConstant ⇒ gep.constants.tail.head match {
                    case 31      ⇒ 'GetObjectClass
                    case 33      ⇒ 'GetMethodId
                    case 49 | 61 ⇒ 'CallTypeMethod // CallIntMethod | CallVoidMethod
                    case index   ⇒ throw new IllegalArgumentException(s"unknown JNI function index ${index}")
                }
                case _ ⇒ throw new IllegalArgumentException("unknown JNI load src")
            }
        case _ ⇒ throw new IllegalArgumentException("unknown JNI call argument")
    }

    private def resolveMethodId(methodId: Value): Unit = {
        val sources = methodId.asInstanceOf[Load].src.users.toSeq.filter(_.isInstanceOf[Store]).map(_.asInstanceOf[Store].src)
        for (call ← sources.filter(_.isInstanceOf[Call]).map(_.asInstanceOf[Call])) {
            if (resolveJNIFunction(call) != 'GetMethodId) throw new IllegalArgumentException("unexpected call")
            val name = resolveMethodName(call.operand(2)) // name is the third parameter
            println(name)
            if (!resolveClassIsThis(call.operand(1))) { // class is the second parameter
                throw new IllegalArgumentException("unexpected class argument")
            }
        }
    }

    private def resolveMethodName(name: Value): String = name match {
        case gep: GetElementPtrConst ⇒ gep.base match {
            case global: GlobalVariable ⇒ global.initializer match {
                case stringData: ConstantDataArray ⇒ stringData.asString
            }
        }
    }

    private def resolveClassIsThis(clazz: Value): Boolean = {
        val sources = clazz.asInstanceOf[Load].src.users.toSeq.filter(_.isInstanceOf[Store]).map(_.asInstanceOf[Store].src)
        sources.filter(_.isInstanceOf[Call]).map(_.asInstanceOf[Call]).forall(call ⇒ {
            if (resolveJNIFunction(call) != 'GetObjectClass) throw new IllegalArgumentException("unexpected call")
            resolveObjectIsThis(call.operand(1)) // object is the second parameter
        })
    }

    private def resolveObjectIsThis(obj: Value): Boolean = {
        val sources = obj.asInstanceOf[Load].src.users.toSeq.filter(_.isInstanceOf[Store]).map(_.asInstanceOf[Store].src)
        sources.forall(_.isInstanceOf[Argument]) && sources.forall(_.asInstanceOf[Argument].index == 1)
    }
}
