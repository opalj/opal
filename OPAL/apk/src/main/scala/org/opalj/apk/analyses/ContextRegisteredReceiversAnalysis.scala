/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.apk.analyses

import org.opalj.apk.ApkContextRegisteredReceiver
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.tac.{AITACode, DUVar, LazyDetachedTACAIKey, Stmt, TACMethodParameter}
import org.opalj.value.{TheStringValue, ValueInformation}

import scala.collection.mutable.ListBuffer

/**
 * Analyzes code of an APK for dynamically registered Intents for Broadcast Receivers.
 *
 * @author Nicolas Gross
 */
object ContextRegisteredReceiversAnalysis {
    val RegisterReceiverMethod = "registerReceiver"
    val ContextClass = "android.content.Context"
    val LocalBroadcastManagerClass = "androidx.localbroadcastmanager.content.LocalBroadcastManager"
    val ActivityClass = "android.app.Activity"
    val IntentFilterClass = "android.content.IntentFilter"

    def analyze(project: Project[_]): Seq[ApkContextRegisteredReceiver] = {
        val foundReceivers: ListBuffer[ApkContextRegisteredReceiver] = ListBuffer.empty

        // calls from java code
        val tacProvider = project.get(LazyDetachedTACAIKey)
        project.allClassFiles.foreach(_.methodsWithBody.foreach(m => {
            var alreadyFoundCall = false
            m.body.get.instructionIterator.foreach {
                case i: MethodInvocationInstruction =>
                    if (!alreadyFoundCall && i.name.equals(RegisterReceiverMethod) &&
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

                            if (call != null && call.name.equals(RegisterReceiverMethod) &&
                                classHierarchyMatches(project, call.declaringClass.mostPreciseObjectType)) {
                                val receiverType = call.params.head.asVar.value.asReferenceValue.upperTypeBound
                                // check if broadcast receiver param is null
                                // if yes: sticky intent, result is cached, no code executed -> ignore
                                if (receiverType.nonEmpty) {
                                    // get broadcast receiver class, might be imprecise
                                    val receiverClass = receiverType.head.toJava

                                    // try to find intents, might be incomplete
                                    val intentDef = tacMethod.stmts(call.params(1).asVar.definedBy.head)
                                    val (actions, categories) = assembleIntentFilter(tacMethod, intentDef)

                                    foundReceivers.append(new ApkContextRegisteredReceiver(
                                        receiverClass, actions, categories, m, s.pc))
                                }
                            }
                        })
                    }
                case _ =>
            }
        }))

        // TODO calls from native code

        foundReceivers.toSeq
    }

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

    /**
     * Tries to rebuild the IntentFilter for a registerReceiver() call. Only works with IntentFilters that are
     * created and its actions and categories are added in the same method as where registerReceiver() is called.
     */
    private def assembleIntentFilter(tacMethod: AITACode[TACMethodParameter, ValueInformation],
                                     intentDef: Stmt[DUVar[ValueInformation]]
                                    ): (Seq[String], Seq[String]) = {
        val foundActions: ListBuffer[String] = ListBuffer.empty
        val foundCategories: ListBuffer[String] = ListBuffer.empty
        if (intentDef.isAssignment && intentDef.asAssignment.expr.isNew &&
            intentDef.asAssignment.expr.asNew.tpe.toJava.equals(IntentFilterClass)) {
            intentDef.asAssignment.targetVar.usedBy.map(tacMethod.stmts(_)).foreach(s => {
                var isAction = true
                val call = if (s.isNonVirtualMethodCall &&
                    s.asNonVirtualMethodCall.declaringClass.mostPreciseObjectType.toJava.equals(IntentFilterClass) &&
                    s.asNonVirtualMethodCall.name.equals("<init>")) {
                    s.asNonVirtualMethodCall
                } else if (s.isVirtualMethodCall &&
                    s.asVirtualMethodCall.declaringClass.mostPreciseObjectType.toJava.equals(IntentFilterClass) &&
                    s.asVirtualMethodCall.name.equals("addAction")) {
                    s.asVirtualMethodCall
                } else if (s.isVirtualMethodCall &&
                    s.asVirtualMethodCall.declaringClass.mostPreciseObjectType.toJava.equals(IntentFilterClass) &&
                    s.asVirtualMethodCall.name.equals("addCategory")) {
                    isAction = false
                    s.asVirtualMethodCall
                } else {
                    null
                }

                if (call != null) {
                    val actionOrCategory = call.params.head.asVar.value.asReferenceValue.toCanonicalForm.
                        asInstanceOf[TheStringValue].value
                    if (isAction) {
                        foundActions.append(actionOrCategory)
                    } else {
                        foundCategories.append(actionOrCategory)
                    }
                }
            })
        }
        (foundActions.toSeq, foundCategories.toSeq)
    }
}