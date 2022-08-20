/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.apk.analyses

import org.opalj.apk.ApkContextRegisteredReceiver
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.tac.LazyDetachedTACAIKey

import scala.collection.mutable.ListBuffer

/**
 * Analyzes code of an APK for dynamically registered Intents for Broadcast Receivers.
 *
 * @author Nicolas Gross
 */
object ContextRegisteredReceiversAnalysis {
    val RegisterReceiverName = "registerReceiver"
    val ContextClass = "android.content.Context"
    val LocalBroadcastManagerClass = "androidx.localbroadcastmanager.content.LocalBroadcastManager"
    val ActivityClass = "android.app.Activity"

    private def classMatches(clazz: ObjectType): Boolean = {
        clazz.toJava.equals(ContextClass) || clazz.toJava.equals(LocalBroadcastManagerClass) ||
            clazz.toJava.equals(ActivityClass)
    }

    private def classHierarchyMatches(project: Project[_], clazz: ObjectType): Boolean = {
        var tmpClazz = clazz
        while (!classMatches(tmpClazz)) {
            if (tmpClazz.toJava.equals("java.lang.Object")) {
                return false
            }
            tmpClazz = project.classFile(tmpClazz) match {
                case Some(c) => c.superclassType.get
                case _ => return false
            }
        }
        true
    }

    def analyze(project: Project[_]): Seq[ApkContextRegisteredReceiver] = {
        val foundReceivers: ListBuffer[ApkContextRegisteredReceiver] = ListBuffer.empty

        // calls from java code
        val tacProvider = project.get(LazyDetachedTACAIKey)
        project.allClassFiles.foreach(_.methodsWithBody.foreach(m => {
            var alreadyFoundCall = false
            m.body.get.instructionIterator.foreach {
                case i: MethodInvocationInstruction =>
                    if (!alreadyFoundCall && i.name.equals(RegisterReceiverName) &&
                        classHierarchyMatches(project, i.declaringClass.mostPreciseObjectType)) {
                        // potential further calls are found in TAC, remaining bytecode instructions must not be analyzed
                        alreadyFoundCall = true

                        // only create TAC for methods where 'registerReceiver' was found in bytecode
                        val tacMethod = tacProvider.apply(m)
                        tacMethod.stmts.foreach(s => {
                            val call = if (s.isAssignment && s.asAssignment.expr.isVirtualFunctionCall) {
                                s.asAssignment.expr.asVirtualFunctionCall
                            } else if (s.isExprStmt && s.asExprStmt.expr.isVirtualFunctionCall) {
                                s.asExprStmt.expr.asVirtualFunctionCall
                            } else if (s.isVirtualMethodCall) {
                                s.asVirtualMethodCall
                            } else {
                                null
                            }

                            if (call != null && call.name.equals(RegisterReceiverName) &&
                                classHierarchyMatches(project, call.declaringClass.mostPreciseObjectType)) {
                                val receiverClass = call.params.head.asVar.value.asReferenceValue.upperTypeBound.head.toJava
                                foundReceivers.append(new ApkContextRegisteredReceiver(receiverClass, Seq.empty, m, s.pc))
                            }
                        })
                    }
                case _ =>
            }
        }))

        // TODO calls from native code

        foundReceivers.toSeq
    }
}