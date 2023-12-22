/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package apk
package analyses

import scala.collection.mutable.ListBuffer

import org.opalj.apk.ApkContextRegisteredReceiver
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.tac.AITACode
import org.opalj.tac.DUVar
import org.opalj.tac.LazyDetachedTACAIKey
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.VirtualFunctionCallStatement
import org.opalj.value.TheStringValue
import org.opalj.value.ValueInformation

/**
 * Analyzes code of an APK for dynamically registered Intents for Broadcast Receivers.
 *
 * @author Nicolas Gross
 */
object ContextRegisteredReceiversAnalysis {
    private val RegisterReceiverMethod = "registerReceiver"
    private val ContextClass = ObjectType("android/content/Context")
    private val LocalBroadcastManagerClass = ObjectType("androidx/localbroadcastmanager/content/LocalBroadcastManager")
    private val ActivityClass = ObjectType("android/app/Activity")
    private val IntentFilterClass = ObjectType("android/content/IntentFilter")

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
                        val tacMethod = tacProvider(m)
                        tacMethod.stmts.foreach {
                            case s @ VirtualFunctionCallStatement(call) =>
                                if (call.name.equals(RegisterReceiverMethod) &&
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

                                        foundReceivers.append(
                                            new ApkContextRegisteredReceiver(
                                                receiverClass,
                                                actions,
                                                categories,
                                                m,
                                                s.pc
                                            )(project.config)
                                        )
                                    }
                                }
                            case _ =>
                        }
                    }
                case _ =>
            }
        }))

        // TODO calls from native code

        foundReceivers.toSeq
    }

    private def classMatches(clazz: ObjectType): Boolean = {
        clazz == ContextClass || clazz == LocalBroadcastManagerClass ||
            clazz == ActivityClass
    }

    private def classHierarchyMatches(project: Project[_], clazz: ObjectType): Boolean = {
        var tmpClazz = clazz
        while (!classMatches(tmpClazz)) {
            if (tmpClazz.toJava.equals("java.lang.Object")) {
                return false
            }
            tmpClazz = project.classFile(tmpClazz) match {
                case Some(c) => c.superclassType.get
                case _       => return false;
            }
        }
        true
    }

    /**
     * Tries to rebuild the IntentFilter for a registerReceiver() call. Only works with IntentFilters that are
     * created and its actions and categories are added in the same method as where registerReceiver() is called.
     */
    private def assembleIntentFilter(
        tacMethod: AITACode[TACMethodParameter, ValueInformation],
        intentDef: Stmt[DUVar[ValueInformation]]
    ): (Seq[String], Seq[String]) = {
        val foundActions: ListBuffer[String] = ListBuffer.empty
        val foundCategories: ListBuffer[String] = ListBuffer.empty
        if (intentDef.isAssignment && intentDef.asAssignment.expr.isNew &&
            intentDef.asAssignment.expr.asNew.tpe == IntentFilterClass) {
            intentDef.asAssignment.targetVar.usedBy
                .foreach(tacMethod.stmts(_) match {
                    case VirtualFunctionCallStatement(call) if call.declaringClass.mostPreciseObjectType == IntentFilterClass =>
                        val actionOrCategory = call.params.head.asVar.value.asReferenceValue.toCanonicalForm
                            .asInstanceOf[TheStringValue]
                            .value
                        call.name match {
                            case "<init>" | "addAction" => foundActions.append(actionOrCategory)
                            case "addCategory"          => foundCategories.append(actionOrCategory)
                        }
                    case _ =>
                })
        }
        (foundActions.toSeq, foundCategories.toSeq)
    }
}
