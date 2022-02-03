/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.br.{ClassFile, Method, ObjectType, ObjectVariableInfo, PCAndInstruction}
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.Instruction
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.value.ValueInformation

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import org.opalj.br.fpcf.{BasicFPCFEagerAnalysisScheduler, FPCFAnalysis}
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers

import scala.io.Source
import scala.xml.Elem
import scala.xml.XML

/**
 * An Analysis that can be used to construct call graphs for Android apps.
 * This Analysis generates call graph edges for:
 *      -Android Lifecycle callbacks
 *      -General Android callbacks
 *      -Edges that originate from Intents
 * Might not work for androidx.
 * Some features are using over-approximation in order to produce a sound call graph.
 *
 * @author Tom Nikisch
 */
class AndroidICCAnalysis(val project: SomeProject) extends FPCFAnalysis {

    final val androidURI = "http://schemas.android.com/apk/res/android"
    final val intentFilterString = "android/content/IntentFilter"
    final val intentOT = ObjectType("android/content/Intent")
    final val pendingIntentOT = ObjectType("android/app/PendingIntent")
    final val androidActivityOT = ObjectType("android/app/Activity")
    final val androidServiceOT = ObjectType("android/app/Service")
    final val androidComponentName = "android/content/ComponentName"
    final val intentStart = List("{_ <: android.content.Intent", "_ <: android.content.Intent")
    final val activity = "activity"
    final val service = "service"
    final val receiver = "receiver"
    final val undef = "undefined"

    final val paramRegex = ".*\\(\"|\"\\).*"
    final val valueRegex = ".*<|>.*"
    final val otRegex = ".*\\(|\\).*"

    final val init = "<init>"
    final val setAction = "setAction"
    final val addCategory = "addCategory"
    final val setData = "setData"
    final val setDataAndNormalize = "setDataAndNormalize"
    final val setDataAndType = "setDataAndType"
    final val setDataAndTypeAndNormalize = "setDataAndTypeAndNormalize"
    final val setType = "setType"
    final val setTypeAndNormalize = "setTypeAndNormalize"
    final val setPackage = "setPackage"

    final val senderClasses = List(
        ObjectType("android/content/Context"), pendingIntentOT, ObjectType("android/content/IntentSender"), androidActivityOT
    )
    final val virtualSendingMethods = List(
        "startIntentSender", "startIntentSenderForResult", "startIntentSenderFromChild", "startActivityFromChild",
        "startActivity", "startActivities", "startActivityForResult", "startNextMatchingActivity", "sendIntent", "send",
        "bindIsolatedService", "bindService", "bindServiceAsUser", "sendBroadcast", "sendBroadcastAsUser",
        "sendBroadcastWithMultiplePermissions", "sendOrderedBroadcast", "sendOrderedBroadcastAsUser",
        "sendStickyBroadcast", "sendStickyBroadcastAsUser", "sendStickyOrderedBroadcast", "sendStickyOrderedBroadcastAsUser",
        "startForegroundService", "startService"
    )

    final val staticSendingMethods = List(
        "getActivities", "getActivity", "getBroadcast", "getForegroundService", "getService"
    )

    val activityStartMethods: List[String] = List(
        "startActivity", "startActivityForResult", "startActivityFromChild", "startActivityIfNeeded", "startActivities",
        "getActivity", "getActivities", "startNextMatchingActivity"
    )
    val serviceMethods: List[String] = List(
        "startService", "bindService", "stopService", "unbindService", "getService", "bindIsolatedService",
        "bindServiceAsUser", "startForegroundService"
    )
    val broadcastReceiverStartMethods: List[String] = List(
        "sendBroadcast", "sendOrderedBroadcast", "sendStickyBroadcast", "sendStickyOrderedBroadcast", "getBroadcast",
        "sendBroadcastAsUser", "sendStickyBroadcastAsUser", "sendStickyOrderedBroadcastAsUser"
    )

    final val onStart = "onStart"
    final val onPause = "onPause"
    final val onStop = "onStop"
    final val onDestroy = "onDestroy"
    final val onActivityResult = "onActivityResult"
    final val onResume = "onResume"
    final val onRestart = "onRestart"
    final val onStartCommand = "onStartCommand"
    final val startService = "startService"
    final val onBind = "onBind"
    final val onRebind = "onRebind"
    final val onUnbind = "onUnbind"
    final val bindService = "bindService"
    final val stopService = "stopService"
    final val unbindService = "unbindService"
    final val stopSelf = "stopSelf"
    final val onReceive = "onReceive"
    final val onCreate = "onCreate"
    final val onBroadcastReceive = "onBroadcastReceive"

    //explicit
    final val setClass = "setClass"
    final val setClassName = "setClassName"
    final val setComponent = "setComponent"

    final val intentMethods = List(
        setAction, addCategory, setData, setDataAndNormalize, setDataAndType, setDataAndTypeAndNormalize, setType, setTypeAndNormalize, setPackage
    )
    final val explicitMethods = List(setClass, setClassName, setComponent)

    val intentFilters: ListBuffer[IntentFilter] = ListBuffer.empty[IntentFilter]
    val edges: ListBuffer[((Method, Int), Method)] = ListBuffer.empty[((Method, Int), Method)]
    val explicitIntents: ListBuffer[ExplicitIntent] = ListBuffer.empty[ExplicitIntent]
    val implicitIntents: ListBuffer[ImplicitIntent] = ListBuffer.empty[ImplicitIntent]

    val senderMap: mutable.Map[(ObjectType, String), ListBuffer[(Method, String, Int)]] =
        mutable.Map.empty[(ObjectType, String), ListBuffer[(Method, String, Int)]] //Class and name of the Method that returns the searched intent -> the intent sending methods and the component type
    val returningIntentsMap: mutable.Map[(ObjectType, String), ListBuffer[ImplicitIntent]] =
        mutable.Map.empty[(ObjectType, String), ListBuffer[ImplicitIntent]] //class and method that returns an intent -> intent

    val componentMap: mutable.Map[String, ListBuffer[ClassFile]] = mutable.Map(
        activity -> ListBuffer.empty[ClassFile],
        service -> ListBuffer.empty[ClassFile]
    )

    /**
     * Calls all necessary methods to generate the call graph edges that are Android specific.
     *
     * @param manifest AndroidManifest.xml
     */
    def performAnalysis(
        manifest: Elem
    ): ProperPropertyComputationResult = {
        parseManifest(manifest)
        generateLifecycleCallbacks()
        searchIntentsAndCallbacks()
        intentMatching()

        val decMeths = project.get(DeclaredMethodsKey)
        val contexts = project.get(SimpleContextsKey)
        val resList = ListBuffer.empty[PartialResult[_, _ >: Null <: Property]]

        edges.foreach { e ⇒
            val calls = new IndirectCalls()
            calls.addCall(contexts(decMeths(e._1._1)), e._1._2, contexts(decMeths(e._2)))
            resList ++= calls.partialResults(contexts(decMeths(e._1._1))).seq
        }

        Results(resList)
    }

    /**
     * Analyses the AndroidManifest.xml of the project to find intent filters and relevant components to generate
     * lifecycle callbacks.
     */
    def parseManifest(
        manifest: Elem
    ): Unit = {
        val pack = manifest.attribute("package").get.toString().replaceAll("\\.", "/")
        List(activity, receiver, service).foreach { comp ⇒
            val components = manifest \\ comp
            components.foreach { c ⇒
                var ot = c.attribute(androidURI, "name").head.toString().replaceAll("\\.", "/")
                if (ot.startsWith("/")) { ot = pack + ot }
                val rec = project.classFile(ObjectType(ot))
                if (rec.isDefined) {
                    if (comp == activity || comp == service) componentMap(comp) += rec.get
                    val filters = c \ "intent-filter"
                    if (filters.nonEmpty) {
                        filters.foreach { filter ⇒
                            val intentFilter = new IntentFilter()
                            intentFilter.componentType = comp
                            intentFilter.receiver = rec.get
                            intentFilter.actions = (filter \ "action").map(_.attribute(androidURI, "name").
                                get.head.toString()).to[ListBuffer]
                            intentFilter.categories = (filter \ "category").map(_.attribute(androidURI, "name").
                                get.head.toString()).to[ListBuffer]
                            val data = filter \ "data"
                            if (data.nonEmpty) {
                                data.foreach { d ⇒
                                    val t = d.attribute(androidURI, "mimeType")
                                    if (t.isDefined) {
                                        intentFilter.dataTypes += t.get.head.toString()
                                    }
                                    val s = d.attribute(androidURI, "scheme")
                                    if (s.isDefined) {
                                        intentFilter.dataSchemes += s.get.head.toString()
                                    }
                                    val h = d.attribute(androidURI, "host")
                                    if (h.isDefined) {
                                        var authority = h.get.head.toString()
                                        val port = d.attribute(androidURI, "port")
                                        if (port.isDefined) {
                                            authority = authority + port.get.head.toString()
                                        }
                                        intentFilter.dataAuthorities += authority
                                    }
                                    val p = d.attribute(androidURI, "path")
                                    if (p.isDefined) {
                                        intentFilter.dataPaths += p.get.head.toString()
                                    }
                                    val pp = d.attribute(androidURI, "pathPrefix")
                                    if (pp.isDefined) {
                                        intentFilter.dataPaths += pp.get.head.toString()
                                    }
                                    val pathPattern = d.attribute(androidURI, "pathPattern")
                                    if (pathPattern.isDefined) {
                                        intentFilter.dataPaths += pathPattern.get.head.toString()
                                    }
                                }
                            }
                            intentFilters += intentFilter
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates the lifecycle callbacks for all android components of the project.
     * Uses the results from parseManifest.
     */
    def generateLifecycleCallbacks(): Unit = {
        var fullLifecycle = true //cg is only sound if this is true
        var aLifecycleList = List.empty[Method]
        var sLifecycleList = List.empty[Method]
        //find default super methods
        if (fullLifecycle) {
            val activityCF = project.classFile(androidActivityOT).orNull
            if (activityCF != null) {
                val aOnCreate = activityCF.findMethod(onCreate).head
                val aOnStart = activityCF.findMethod(onStart).head
                val aOnRestart = activityCF.findMethod(onRestart).head
                val aOnResume = activityCF.findMethod(onResume).head
                val aOnPause = activityCF.findMethod(onPause).head
                val aOnStop = activityCF.findMethod(onStop).head
                val aOnDestroy = activityCF.findMethod(onDestroy).head
                val aOnActivityResult = activityCF.findMethod(onActivityResult).head
                aLifecycleList = List(aOnCreate, aOnStart, aOnResume, aOnPause, aOnStop, aOnDestroy, aOnActivityResult, aOnRestart)
            } else fullLifecycle = false

            val serviceCF = project.classFile(androidServiceOT).orNull

            if (serviceCF != null) {
                val sOnCreate = serviceCF.findMethod(onCreate).head
                val sOnStart = serviceCF.findMethod(onStartCommand).head
                val sOnBind = serviceCF.findMethod(onBind).head
                val sOnUnbind = serviceCF.findMethod(onUnbind).head
                val sOnDestroy = serviceCF.findMethod(onDestroy).head
                sLifecycleList = List(sOnCreate, sOnStart, sOnBind, sOnUnbind, sOnDestroy)
            } else fullLifecycle = false
        }
        componentMap(activity).foreach { a ⇒

            val create = a.findMethod(onCreate).headOption
            val start = a.findMethod(onStart).headOption
            var restart = a.findMethod(onRestart).headOption
            val resume = a.findMethod(onResume).headOption
            val pause = a.findMethod(onPause).headOption
            val stop = a.findMethod(onStop).headOption
            val destroy = a.findMethod(onDestroy).headOption
            val activityResult = a.findMethod(onActivityResult).headOption

            val lifecycleListOption = List(create, start, resume, pause, stop, destroy, activityResult)

            if (fullLifecycle) {
                val l = ListBuffer.empty[Method]
                for (index ← lifecycleListOption.indices) {
                    if (lifecycleListOption(index).isEmpty) {
                        l += aLifecycleList(index)
                    } else l += lifecycleListOption(index).get
                }
                if (restart.isEmpty) {
                    restart = Option(aLifecycleList(7))
                }
                val lifecycleList = l.toList
                for (index ← 1 until lifecycleList.size) {
                    edges += (lifecycleList(index - 1), 0) -> lifecycleList(index)
                }
                makeEdge(lifecycleList(3), lifecycleList(2))
                makeEdge(lifecycleList(4), restart.get)
                makeEdge(restart.get, lifecycleList(1))
                makeEdge(lifecycleList(4), lifecycleList.head)

            } else {
                val lifecycleList = lifecycleListOption.flatten
                for (index ← 1 until lifecycleList.size) {
                    edges += (lifecycleList(index - 1), 0) -> lifecycleList(index)
                }
                makeEdge(pause, resume)
                makeEdge(stop, restart)
                makeEdge(restart, start)
                makeEdge(stop, create)
            }

        }
        componentMap(service).foreach { s ⇒
            val create = s.findMethod(onCreate).headOption
            val startComm = s.findMethod(onStartCommand).headOption
            val bind = s.findMethod(onBind).headOption
            val unbind = s.findMethod(onUnbind).headOption
            val destroy = s.findMethod(onDestroy).headOption

            if (fullLifecycle) {
                val lifecycleList1 = List(create, startComm, bind, unbind, destroy)
                val l = ListBuffer.empty[Method]
                for (index ← lifecycleList1.indices) {
                    if (lifecycleList1(index).isEmpty) {
                        l += sLifecycleList(index)
                    } else l += lifecycleList1(index).get
                }
                val lifecycleList = l.toList
                makeEdge(lifecycleList.head, lifecycleList(1))
                makeEdge(lifecycleList.head, lifecycleList(2))
                makeEdge(lifecycleList(1), lifecycleList(4))
                makeEdge(lifecycleList(2), lifecycleList(3))
                makeEdge(lifecycleList(3), lifecycleList(4))
                makeEdge(lifecycleList(1), lifecycleList(4))
            } else {
                makeEdge(create, startComm)
                makeEdge(create, bind)
                makeEdge(startComm, destroy)
                makeEdge(bind, unbind)
                makeEdge(unbind, destroy)
                makeEdge(create, destroy)
            }
        }
    }

    def makeEdge(
        org: Method,
        rec: Method
    ): Unit = {
        edges += ((org, 0) -> rec)
    }

    def makeEdge(
        org: Option[Method],
        rec: Option[Method]
    ): Unit = {
        if (org.isDefined && rec.isDefined) {
            edges += ((org.get, 0) -> rec.get)
        }
    }

    /**
     * Searches all Intents and callbacks in the project.
     */
    def searchIntentsAndCallbacks(): Unit = {

        val source = Source.fromInputStream(this.getClass.getResourceAsStream("/org.opalj.tac.fpcf.analyses.cg/AndroidCallbackList.txt"))
        val callbackList = source.getLines.toList
        source.close
        val dec = project.get(DeclaredMethodsKey)
        project.allMethodsWithBody.filter(dec(_) != null).foreach(m ⇒ {
            val body = m.body.get
            val intentUseSites: mutable.Map[Int, List[Int]] = mutable.Map.empty[Int, List[Int]]
            try {
                val tacCode = propertyStore.get(m, TACAI.key).get.ub.tac.get
                body.collectWithIndex {
                    case PCAndInstruction(pc, in: INVOKESPECIAL) ⇒

                        val superInterfaceClasses = project.classHierarchy.allSupertypes(in.declaringClass.asObjectType, reflexive = true)
                        //find IntentFilters
                        if (superInterfaceClasses.contains(ObjectType(intentFilterString))) {
                            val stmts = tacCode.stmts
                            val index = tacCode.pcToIndex(pc)
                            val c = stmts(index)
                            val useSites = tacCode.stmts(c.asNonVirtualMethodCall.receiver.asVar.definedBy.head).
                                asAssignment.targetVar.usedBy.toChain.toList
                            reconstructIntentfilter(c.asNonVirtualMethodCall, tacCode, useSites, m)
                        }

                        //find Intents
                        if (superInterfaceClasses.contains(intentOT)) {
                            val stmts = tacCode.stmts
                            val index = tacCode.pcToIndex(pc)
                            val c = stmts(index).asNonVirtualMethodCall
                            val useSites = tacCode.stmts(c.receiver.asVar.definedBy.head).
                                asAssignment.targetVar.usedBy.toChain.toList
                            intentUseSites += index -> useSites
                            println("recIntent")
                            reconstructIntent(c, tacCode, useSites, m)
                        }
                }

                //find sending methods (virtual)
                body.collect {
                    case in: Instruction if in.isInstanceOf[INVOKEVIRTUAL] &&
                        senderClasses.contains(in.asInstanceOf[INVOKEVIRTUAL].declaringClass) ⇒ in
                }.foreach { in ⇒
                    if (virtualSendingMethods.contains(in.value.asInstanceOf[INVOKEVIRTUAL].name)) {

                        val index = tacCode.pcToIndex(in.pc)
                        val methodCall = tacCode.stmts(index)

                        if (methodCall.isVirtualMethodCall) {
                            val virtualCall = methodCall.asVirtualMethodCall
                            if (intentUseSites.isEmpty || (!intentUseSites.exists(k ⇒
                                k._2.contains(tacCode.pcToIndex(in.pc))) &&
                                !findOrigin(virtualCall.params, tacCode, intentUseSites))) {
                                //backwards search to find intent
                                val virtualCall = methodCall.asVirtualMethodCall
                                val classMethodTupel = searchParamsVirtual(virtualCall, tacCode)
                                if (classMethodTupel.isDefined) {
                                    val cmt = classMethodTupel.get
                                    //origin of intent found wait for results
                                    if (senderMap.contains(cmt)) {
                                        senderMap(cmt) += ((m, getComponentType(virtualCall.name), virtualCall.pc))
                                    } else senderMap += (cmt -> ListBuffer((m, getComponentType(virtualCall.name), virtualCall.pc)))
                                }
                            }
                        }

                    }
                }

                //find sending methods (static)
                body.collect {
                    case in: Instruction if in.isInstanceOf[INVOKESTATIC] &&
                        in.asInstanceOf[INVOKESTATIC].declaringClass == pendingIntentOT ⇒ in
                }.foreach { in ⇒
                    if (staticSendingMethods.contains(in.value.asInstanceOf[INVOKESTATIC].name)) {
                        val index = tacCode.pcToIndex(in.pc)
                        val methodCall = tacCode.stmts(index)
                        if (methodCall.isAssignment) {
                            val staticCall = methodCall.asAssignment.expr.asStaticFunctionCall
                            if (intentUseSites.isEmpty ||
                                (!intentUseSites.exists(k ⇒ k._2.contains(tacCode.pcToIndex(in.pc))) &&
                                    !findOrigin(staticCall.params, tacCode, intentUseSites))) {
                                //backwards search to find intent
                                val classMethodTupel = searchParamsStatic(staticCall, tacCode)
                                if (classMethodTupel.isDefined) {
                                    val cmt = classMethodTupel.get
                                    //origin of intent found wait for results
                                    if (senderMap.contains(cmt)) {
                                        senderMap(cmt) += ((m, getComponentType(staticCall.name), staticCall.pc))
                                    } else senderMap += (cmt -> ListBuffer((m, getComponentType(staticCall.name), staticCall.pc)))
                                }
                            }
                        }
                    }
                }

                //find callbacks
                val callbackMethods = ListBuffer.empty[Method]
                body.collectWithIndex {
                    case PCAndInstruction(pc, in: INVOKEVIRTUAL) ⇒
                        val index = tacCode.pcToIndex(pc)
                        if (index > -1) {
                            val methodCall = tacCode.stmts(index)
                            if (methodCall.isVirtualMethodCall) {
                                var i = 0
                                methodCall.asVirtualMethodCall.descriptor.parameterTypes.foreach { p ⇒
                                    if (p.isObjectType) {
                                        val ot = p.toString.replaceAll(otRegex, "")
                                        //only interfaces defined in 'callbacklist' are handled as callback interfaces and dynamically searching for callback interfaces
                                        if (callbackList.contains(ot) ||
                                            (ot.startsWith("android") && ot.endsWith("Listener") && ot.substring(ot.lastIndexOf("$") + 1).startsWith("On"))) {
                                            val par = methodCall.asVirtualMethodCall.params(i).asVar.value.verificationTypeInfo
                                            if (par.isObjectVariableInfo) {
                                                val cf = project.classFile(par.asObjectVariableInfo.clazz.asObjectType)
                                                if (cf.isDefined) {
                                                    val methods = cf.get.methods.filter(f ⇒ f.name != init && f.name != "<clinit>")
                                                    methods.foreach { method ⇒
                                                        if (!callbackMethods.contains(method)) callbackMethods += method
                                                    }
                                                }
                                            }
                                        } else i += 1
                                    } else i += 1
                                }
                            }
                        }
                }
                if (callbackMethods.nonEmpty) {
                    generateCallBackEdges(m, callbackMethods)
                }
            } catch {
                case _: Throwable ⇒
            }
        })

    }

    def generateCallBackEdges(
        m:               Method,
        callbackMethods: ListBuffer[Method]
    ): Unit = {

        val cf = m.classFile
        val compType = cf.thisType

        if (project.classHierarchy.isSubtypeOf(compType, androidActivityOT)) {
            val relevantLifecycleMethods = ListBuffer.empty[Method]
            relevantLifecycleMethods ++= cf.findMethod(onStart)
            relevantLifecycleMethods ++= cf.findMethod(onResume)
            relevantLifecycleMethods ++= cf.findMethod(onRestart)
            if (relevantLifecycleMethods.nonEmpty) {
                callbackMethods.foreach { ce ⇒
                    if (ce.classFile != m.classFile) {
                        relevantLifecycleMethods.foreach { lm ⇒
                            edges += ((lm, 0) -> ce)
                        }
                    }
                }
            }
        } else if (project.classHierarchy.isSubtypeOf(compType, androidServiceOT)) {
            val relevantLifecycleMethods = ListBuffer.empty[Method]
            relevantLifecycleMethods ++= cf.findMethod(onStart)
            relevantLifecycleMethods ++= cf.findMethod(onBind)
            relevantLifecycleMethods ++= cf.findMethod(onRebind)
            if (relevantLifecycleMethods.nonEmpty) {
                callbackMethods.foreach { ce ⇒
                    if (ce.classFile != m.classFile) {
                        relevantLifecycleMethods.foreach { lm ⇒
                            edges += ((lm, 0) -> ce)
                        }
                    }
                }
            }
        }
    }

    //Checks if the intent is reachable within the processed method
    def findOrigin(
        params:           Seq[Expr[DUVar[ValueInformation]]],
        tacCode:          TACode[TACMethodParameter, DUVar[ValueInformation]],
        originalUseSites: mutable.Map[Int, List[Int]]
    ): Boolean = {
        params.foreach { p ⇒
            val pString = p.asVar.value.toString
            if (pString.startsWith(intentStart.head) || pString.startsWith(intentStart(1))) {
                //the intent to its source
                val defIndex = p.asVar.definedBy.head
                if (defIndex < 0) {
                    return false; //intent is not reachable
                }
                if (originalUseSites.exists(k ⇒ k._2.contains(defIndex))) {
                    return true;
                }
                val stmt = tacCode.stmts(defIndex)
                if (stmt.isAssignment) {
                    if (stmt.asAssignment.expr.isStaticFunctionCall || stmt.asAssignment.expr.isVirtualFunctionCall) {
                        return findOrigin(stmt.asAssignment.expr.asFunctionCall.params, tacCode, originalUseSites);
                    }
                }
            }
        }
        false //intent is not reachable
    }

    /**
     * Searches the parameters of functionCall to find the origin of the intent and return the class and method
     * that returns the searched intent.
     */
    def searchParamsStatic(
        functionCall: FunctionCall[DUVar[ValueInformation]],
        tacCode:      TACode[TACMethodParameter, DUVar[ValueInformation]]
    ): Option[(ObjectType, String)] = {
        functionCall.params.foreach { p ⇒
            val pString = p.asVar.value.toString
            if (pString.startsWith(intentStart.head) || pString.startsWith(intentStart(1))) {
                val defIndex = p.asVar.definedBy.head
                if (defIndex < 0) {
                    return None;
                }
                val defSiteStmt = tacCode.stmts(defIndex)
                if (defSiteStmt.isAssignment) {
                    val call = defSiteStmt.asAssignment.expr.asFunctionCall
                    if (!intentMethods.contains(call.name)) {
                        return Some(call.declaringClass.asObjectType -> call.name);
                    }
                    return searchParamsStatic(call, tacCode);
                }
            }
        }
        Some[(ObjectType, String)](functionCall.declaringClass.asObjectType -> functionCall.name)
    }

    /**
     * Searches the parameters of functionCall to find the origin of the intent and return the class and method
     * that returns the searched intent.
     */
    def searchParamsVirtual(
        functionCall: MethodCall[DUVar[ValueInformation]],
        tacCode:      TACode[TACMethodParameter, DUVar[ValueInformation]]
    ): Option[(ObjectType, String)] = {
        functionCall.params.foreach { p ⇒
            val pString = p.asVar.value.toString
            if (pString.startsWith(intentStart.head) || pString.startsWith(intentStart(1))) {

                val defIndex = p.asVar.definedBy.head
                if (defIndex < 0) {
                    return None;
                }
                val defSiteStmt = tacCode.stmts(defIndex)
                if (defSiteStmt.isAssignment && (defSiteStmt.asAssignment.expr.isVirtualFunctionCall ||
                    defSiteStmt.asAssignment.expr.isStaticFunctionCall)) {

                    val call = defSiteStmt.asAssignment.expr.asFunctionCall
                    if (!intentMethods.contains(call.name)) {
                        return Some(call.declaringClass.asObjectType -> call.name);
                    }
                    return searchParamsStatic(call, tacCode);
                }
            }
        }
        Some[(ObjectType, String)](functionCall.declaringClass.asObjectType -> functionCall.name)
    }

    def getComponentType(
        name: String
    ): String = {
        if (activityStartMethods.contains(name)) {
            return activity;
        }
        if (serviceMethods.contains(name)) {
            return service;
        }
        if (broadcastReceiverStartMethods.contains(name)) {
            return receiver;
        }
        undef
    }

    def reconstructIntent(
        c:        NonVirtualMethodCall[V],
        tacCode:  TACode[TACMethodParameter, DUVar[ValueInformation]],
        useSites: List[Int],
        m:        Method
    ): Unit = {
        val p = c.params
        println(c.descriptor)
        p.size match {
            case 0 ⇒ //Intent()
                val intent = new ImplicitIntent(m)
                evaluateUseSites(tacCode, useSites, intent)
            case 1 ⇒ //Intent(String action)
                if (p.head.asVar.value.verificationTypeInfo == ObjectVariableInfo(ObjectType.String)) {
                    val intent = new ImplicitIntent(m)
                    intent.action = (p.head.asVar.value.toString.replaceAll(paramRegex, ""))
                    evaluateUseSites(tacCode, useSites, intent)
                } else {
                    //Intent(Intent 0)
                    if (p.head.asVar.definedBy.head > -1) {
                        val stmts = tacCode.stmts
                        val na = stmts(p.head.asVar.definedBy.head).asAssignment
                        val addUseSites = na.targetVar.usedBy.toChain.toList
                        addUseSites.foreach { i ⇒
                            val s = stmts(i)
                            if (s.isNonVirtualMethodCall && s.asNonVirtualMethodCall.name == init && c.pc != s.pc) {
                                reconstructIntent(s.asNonVirtualMethodCall, tacCode, useSites ++ addUseSites, m)
                            }
                        }
                    } else {
                        //intent comes from another method => overapproximation
                        val intent = new ImplicitIntent(m)
                        intent.iData = undef
                        intent.action = undef
                        intent.categories += undef
                        evaluateUseSites(tacCode, useSites, intent)
                    }
                }
            case 2 ⇒ //Intent(String action, Uri uri)
                if (p.head.asVar.value.verificationTypeInfo == ObjectVariableInfo(ObjectType.String)) {

                    val intent = new ImplicitIntent(m)
                    intent.action = p.head.asVar.value.toString.replaceAll(paramRegex, "")
                    intent.iData = (checkData(1, tacCode, p))
                    evaluateUseSites(tacCode, useSites, intent)

                } else { //Intent(Context context, Class cls)
                    val clsFile = project.classFile(ObjectType(p.last.asVar.value.toString.
                        replaceAll(valueRegex, "").replaceAll("\\.", "/")))

                    if (clsFile.isDefined) {
                        val intent = new ExplicitIntent(m, clsFile.get)
                        intent.findStartedMethod(tacCode, useSites)
                        if (intent.calledMethod.nonEmpty) {
                            explicitIntents += intent
                        }
                    }

                }
            case _ ⇒ //Intent(String action, Uri uri, Context packageContext, Class cls)
                val clsFile = project.classFile(ObjectType(p.last.asVar.value.toString.
                    replaceAll(valueRegex, "").replaceAll("\\.", "/")))

                if (clsFile.isDefined) {
                    val intent = new ExplicitIntent(m, clsFile.get)
                    intent.findStartedMethod(tacCode, useSites)
                    if (intent.calledMethod.nonEmpty) {
                        explicitIntents += intent
                    }
                } else {
                    val intent = new ImplicitIntent(m)
                    intent.action = p.head.asVar.value.toString.replaceAll(paramRegex, "")
                    checkData(1, tacCode, p)
                    intent.iData = checkData(1, tacCode, p)
                    evaluateUseSites(tacCode, useSites, intent)
                }
        }
    }

    /**
     * Searches the value of a data parameter.
     */
    def checkData(
        dataPosition: Int,
        tacCode:      TACode[TACMethodParameter, DUVar[ValueInformation]],
        parameter:    Seq[Expr[DUVar[ValueInformation]]]
    ): String = {
        val defSite = parameter(dataPosition).asVar.definedBy.head
        if (defSite > -1) {
            val use = tacCode.stmts(defSite)
            if (use.isAssignment) {
                val expr = use.asAssignment.expr
                if (expr.isStaticFunctionCall) {
                    if (expr.asStaticFunctionCall.name == "parse") {
                        val defSite2 = expr.asStaticFunctionCall.params.head.asVar.definedBy.head
                        if (defSite2 > -1) {
                            val str = tacCode.stmts(defSite2)
                            if (str != null &&
                                str.isAssignment &&
                                str.asAssignment.expr != null) {
                                val strExpr = str.asAssignment.expr
                                if (strExpr.isStringConst) {
                                    return str.asAssignment.targetVar.value.toString.replaceAll(paramRegex, "");
                                }
                            }
                        }
                    }
                }
            }
        }
        if (parameter(dataPosition).asVar.value.toString.startsWith("{_ <:") ||
            parameter(dataPosition).asVar.value.toString.startsWith("null") ||
            defSite < 0) {
            //toDo: Find Data, check defSites until Data string is found
            undef
        } else parameter(dataPosition).toString
    }

    def reconstructIntentfilter(
        c:        NonVirtualMethodCall[V],
        tacCode:  TACode[TACMethodParameter, DUVar[ValueInformation]],
        useSites: List[Int],
        m:        Method
    ): Unit = {
        val p = c.params
        val intentFilter = new IntentFilter()
        val stmts = tacCode.stmts

        p.size match {
            case 1 ⇒
                if (p.head.asVar.value.verificationTypeInfo == ObjectVariableInfo(ObjectType.String)) {
                    intentFilter.actions += p.head.asVar.value.toString.replaceAll(paramRegex, "")
                    intentFilter.evaluateUseSites(tacCode, useSites)
                    if (intentFilter.registeredReceivers.nonEmpty) handleReceivers(intentFilter)
                } else { //copy constructor
                    val addUseSites = stmts(p.head.asVar.definedBy.head).asAssignment.targetVar.usedBy.toChain.toList
                    addUseSites.foreach { i ⇒
                        val s = stmts(i)
                        if (s.isNonVirtualMethodCall && s.asNonVirtualMethodCall.name == init && s.pc != c.pc) {
                            reconstructIntentfilter(s.asNonVirtualMethodCall, tacCode, useSites ++ addUseSites, m)
                        }
                    }
                }
            case 2 ⇒
                intentFilter.actions += p.head.asVar.value.toString.replaceAll(paramRegex, "")
                intentFilter.dataTypes += p(1).asVar.value.toString.replaceAll(paramRegex, "")
                intentFilter.evaluateUseSites(tacCode, useSites)
                if (intentFilter.registeredReceivers.nonEmpty) handleReceivers(intentFilter)
            case 0 ⇒
                intentFilter.evaluateUseSites(tacCode, useSites)
                if (intentFilter.registeredReceivers.nonEmpty) handleReceivers(intentFilter)
        }

    }

    def handleReceivers(
        intentFilter: IntentFilter
    ): Unit = {
        if (intentFilter.registeredReceivers.nonEmpty) {
            intentFilter.registeredReceivers.map(r ⇒ project.classFile(r)).
                filter(cf ⇒ cf.isDefined).map(_.get).foreach { addRec ⇒
                    val addFilter = intentFilter.cloneFilter()
                    addFilter.componentType = receiver
                    addFilter.receiver = addRec
                    intentFilters += addFilter
                }
        }
    }

    def evaluateUseSites(
        tacCode:  TACode[TACMethodParameter, DUVar[ValueInformation]],
        useSites: List[Int],
        intent:   ImplicitIntent
    ): Unit = {
        val statements = tacCode.stmts
        val addUseSites: ListBuffer[Int] = ListBuffer.empty[Int]

        useSites.foreach { use ⇒
            val stmt = statements(use)
            if (stmt.isExprStmt && stmt.asExprStmt.expr.isVirtualFunctionCall) {
                val virtualCall = stmt.asExprStmt.expr.asVirtualFunctionCall
                val someIntent = internalFinder(virtualCall.name, virtualCall.params, intent, tacCode, virtualCall.pc)
                if (someIntent.isDefined) {
                    val expIntent = someIntent.head
                    expIntent.findStartedMethod(tacCode, useSites)
                    if (expIntent.calledMethod.nonEmpty) {
                        explicitIntents += expIntent
                        return ;
                    }
                }
            } else if (stmt.isVirtualMethodCall) {
                val virtualCall = stmt.asVirtualMethodCall
                internalFinder(virtualCall.name, virtualCall.params, intent, tacCode, virtualCall.pc)
            } else if (stmt.isAssignment && stmt.asAssignment.expr.isVirtualFunctionCall) {
                val virtualCall = stmt.asAssignment.expr.asVirtualFunctionCall
                if (virtualCall.declaringClass == intentOT) { addUseSites ++= stmt.asAssignment.targetVar.usedBy.toChain }
                val someIntent = internalFinder(virtualCall.name, virtualCall.params, intent, tacCode, virtualCall.pc)
                if (someIntent.isDefined) {
                    val expIntent = someIntent.head
                    expIntent.findStartedMethod(tacCode, useSites)
                    if (expIntent.calledMethod.nonEmpty) {
                        explicitIntents += expIntent
                        return ;
                    }
                } else if (stmt.asAssignment.expr.isStaticFunctionCall) {
                    val staticCall = stmt.asAssignment.expr.asStaticFunctionCall
                    if (staticCall.declaringClass == intentOT) addUseSites ++= stmt.asAssignment.targetVar.usedBy.toChain
                    internalFinder(staticCall.name, staticCall.params, intent, tacCode, staticCall.pc)
                }
            } else if (stmt.isInstanceOf[ReturnValue[DUVar[ValueInformation]]]) {
                val intentTupel = intent.caller.classFile.thisType -> intent.caller.name
                if (returningIntentsMap.contains(intentTupel)) {
                    returningIntentsMap(intentTupel) += intent
                } else returningIntentsMap += ((intent.caller.classFile.thisType -> intent.caller.name) -> ListBuffer(intent))
            }
        }
        if (addUseSites.nonEmpty) {
            evaluateUseSites(tacCode, addUseSites.toList, intent)
        } else implicitIntents += intent
    }

    def internalFinder(
        name:      String,
        parameter: Seq[Expr[DUVar[ValueInformation]]],
        intent:    ImplicitIntent,
        tacCode:   TACode[TACMethodParameter, DUVar[ValueInformation]],
        pc:        Int
    ): Option[ExplicitIntent] = {

        if (explicitMethods.contains(name)) {
            if (name.equals(setComponent)) {
                tacCode.stmts(parameter.head.asVar.definedBy.head).asAssignment.targetVar.usedBy.foreach { u ⇒
                    val stmt = tacCode.stmts(u)
                    if (stmt.isNonVirtualMethodCall && stmt.asNonVirtualMethodCall.declaringClass ==
                        ObjectType(androidComponentName)) {
                        val param = stmt.asNonVirtualMethodCall.params
                        if (param.last.asVar.value.toString.startsWith("Class")) {
                            val clsFile = project.classFile(ObjectType(param.last.asVar.value.toString.
                                replaceAll(valueRegex, "").replaceAll("\\.", "/")))
                            if (clsFile.isDefined) {
                                val i = new ExplicitIntent(intent.caller, clsFile.get)
                                i.pc = pc
                                return Some(i);
                            }
                        }
                    }
                }
            } else {
                name match {
                    case `setClass` ⇒ {
                        val clsFile = project.classFile(
                            ObjectType(parameter(1).toString.replaceAll(valueRegex, "").replaceAll("\\.", "/"))
                        )
                        if (clsFile.isDefined) {
                            val i = new ExplicitIntent(intent.caller, clsFile.get)
                            i.pc = pc
                            return Some(i);
                        }
                    }
                    case `setClassName` ⇒ {
                        val clsFile = project.classFile(
                            ObjectType(parameter(1).toString.replaceAll(paramRegex, "").replaceAll("\\.", "/"))
                        )
                        if (clsFile.isDefined) {
                            val i = new ExplicitIntent(intent.caller, clsFile.get)
                            i.pc = pc
                            return Some(i);
                        }
                    }
                }
            }
        } else if (intentMethods.contains(name)) {
            val firstParam: String = parameter.head.asVar.value.toString.replaceAll(paramRegex, "")

            name match {
                case `setAction`   ⇒ intent.action = firstParam
                case `addCategory` ⇒ intent.categories += firstParam
                case `setData`     ⇒ intent.iData = checkData(0, tacCode, parameter)
                case `setDataAndType` ⇒
                    intent.iData = checkData(0, tacCode, parameter)
                    intent.iType = parameter.last.asVar.value.toString.replaceAll(paramRegex, "")
                case `setDataAndNormalize` ⇒ intent.iData = checkData(0, tacCode, parameter)
                case `setDataAndTypeAndNormalize` ⇒
                    intent.iData = checkData(0, tacCode, parameter)
                    intent.iType = parameter.last.asVar.value.toString.replaceAll(paramRegex, "")
                case `setType`             ⇒ intent.iType = firstParam
                case `setTypeAndNormalize` ⇒ intent.iType = firstParam
                case `setPackage`          ⇒ intent.iPackage = firstParam
            }
        } else if (activityStartMethods.contains(name)) {
            intent.pc = pc
            intent.componentTypes += activity
            intent.calledMethods += onCreate
        } else if (serviceMethods.contains(name)) {
            intent.pc = pc
            intent.componentTypes += service
            name match {
                case `startService`  ⇒ intent.calledMethods ++= ListBuffer(onCreate, onStartCommand)
                case `bindService`   ⇒ intent.calledMethods ++= ListBuffer(onCreate, onBind, onRebind)
                case `stopService`   ⇒ intent.calledMethods += stopSelf
                case `unbindService` ⇒ intent.calledMethods += onUnbind
            }
        } else if (broadcastReceiverStartMethods.contains(name)) {
            intent.pc = pc
            intent.componentTypes += receiver
            intent.calledMethods ++= ListBuffer(onReceive, onBroadcastReceive)
        }
        None
    }

    /**
     * Matches found intents with intent filters and writes the results to 'edges'.
     * Uses the results from 'searchIntentsAndCallbacks'.
     */
    def intentMatching(): Unit = {
        //ordering Filters in maps for matching
        val filterMap: mutable.Map[String, mutable.Map[String, mutable.Map[ListBuffer[String], ListBuffer[IntentFilter]]]] = mutable.Map.empty
        intentFilters.toList.foreach { f ⇒
            f.actions += ""
            for (action ← f.actions) {
                if (!filterMap.contains(f.componentType)) {
                    filterMap += (f.componentType -> mutable.Map.empty)
                }
                if (filterMap(f.componentType).contains(action)) {
                    if (filterMap(f.componentType)(action).contains(f.categories)) {
                        filterMap(f.componentType)(action)(f.categories).append(f)
                    } else {
                        filterMap(f.componentType)(action) += (f.categories -> ListBuffer(f))
                    }
                } else {
                    filterMap(f.componentType) += (action -> mutable.Map(f.categories -> ListBuffer(f)))
                }
            }
        }

        //match sink and source (intents returned from methods to methods that send the respective intents)
        senderMap.keySet.foreach { k ⇒
            if (returningIntentsMap.contains(k)) {
                val intents = returningIntentsMap(k)
                implicitIntents --= intents
                val sender = senderMap(k)
                intents.foreach { i ⇒
                    sender.foreach { s ⇒
                        val intent = i.clone()
                        intent.caller = s._1
                        intent.componentTypes += s._2
                        intent.pc = s._3
                        implicitIntents += intent
                    }
                }
            } else {
                //overapproximate (no source found)
                senderMap(k).foreach { s ⇒
                    val intent = new ImplicitIntent(s._1)
                    intent.componentTypes += s._2
                    intent.pc = s._3
                    intent.action = undef
                    intent.categories += undef
                    intent.iData = undef
                    implicitIntents += intent
                }
            }
        }

        //Intent matching: testing implicitIntents against comptype, actions and categories of intentFilters
        for (intent ← implicitIntents) {
            intent.componentTypes.foreach { compType ⇒
                if (filterMap.contains(compType)) {
                    if (filterMap(compType).contains(intent.action)) {
                        filterMap(compType)(intent.action).keySet.foreach { categories ⇒
                            if ((intent.categories.isEmpty && categories.isEmpty) || intent.categories.forall(categories.contains)) {
                                if (intent.iData == undef) {
                                    filterMap(compType)(intent.action)(categories).foreach { f ⇒
                                        matchFound(intent, f)
                                    }
                                } else {
                                    testIntentDataAgainstFilters(intent, filterMap(compType)(intent.action)(categories))
                                }
                            }
                        }
                    }
                }
            }
        }
        //adding explicit intent edges
        explicitIntents.foreach { i ⇒
            val caller = i.caller
            i.calledMethod.foreach { m ⇒
                edges += ((caller, i.pc) -> m)
            }
        }
    }

    /**
     * Test whether the data field of an intent matches the data field of any of the [[filters]].
     * If there is a match, an edge is created.
     * @param intent Intent that provides the data that is tested.
     * @param filters ListBuffer of IntentFilters that are matched against the [[intent]].
     */
    def testIntentDataAgainstFilters(
        intent:  ImplicitIntent,
        filters: ListBuffer[IntentFilter]
    ): Unit = {

        for (filter ← filters) {
            val types = filter.dataTypes
            val schemes = filter.dataSchemes
            val data = intent.iData
            if (types.isEmpty && schemes.isEmpty && intent.iType.isEmpty && data.isEmpty) {
                matchFound(intent, filter)
            } else if (data.nonEmpty) {
                val ssi = data.indexOf(":")
                val scheme = data.substring(0, ssi)
                if (schemes.contains(scheme)) {
                    val authorities = filter.dataAuthorities
                    val authorityEnd = findAuthority(data)
                    if (authorityEnd > ssi + 3 && authorities.contains(data.substring(ssi + 3, authorityEnd))) {
                        val paths = filter.dataPaths
                        if (paths.isEmpty) {
                            matchFound(intent, filter)
                        } else if (ssi < authorityEnd && paths.exists(p ⇒ p.startsWith(findPath(data, authorityEnd)))) {
                            val types = filter.dataTypes
                            val iType = intent.iType
                            if (types.isEmpty && iType.isEmpty || types.contains(iType)) {
                                matchFound(intent, filter)
                            }
                        }
                    }
                }
            }
        }
    }

    def matchFound(
        intent: ImplicitIntent,
        filter: IntentFilter
    ): Unit = {
        val caller = intent.caller
        findCalledMethods(filter.receiver, intent.calledMethods, intent.componentTypes).foreach { m ⇒
            edges += ((caller, intent.pc) -> m)
        }
    }

    def findAuthority(
        uri: String
    ): Integer = {
        val ssi = uri.indexOf(":")
        if (uri.substring(ssi).startsWith("//")) {
            for (index ← ssi + 3 to uri.length) {
                uri.charAt(index) match {
                    case '/' | '\\' | '?' | '#' ⇒ return index;
                }
            }
        }
        uri.length
    }
    def findPath(
        uri:   String,
        start: Integer
    ): String = {
        var i = start + 1
        while (i < uri.length) {
            uri.charAt(i) match {
                case '?' | '#' | '*' ⇒ return uri.substring(start + 1, i);
                case _               ⇒ i += 1
            }
        }
        uri.substring(start + 1)
    }

    def findCalledMethods(
        receiverCF:     ClassFile,
        methods:        ListBuffer[String],
        componentTypes: ListBuffer[String]
    ): ListBuffer[Method] = {
        val foundMethods = ListBuffer.empty[Method]
        if (methods.isEmpty) {
            componentTypes.foreach {
                case `activity` ⇒
                    foundMethods ++= receiverCF.findMethod(onCreate)
                case `service` ⇒
                    foundMethods ++= receiverCF.findMethod(onCreate) ++= receiverCF.findMethod(onRebind)
                case `receiver` ⇒
                    foundMethods ++= receiverCF.findMethod(onReceive) ++= receiverCF.findMethod(onBroadcastReceive)
            }
        } else {
            methods.foreach { m ⇒
                foundMethods ++= receiverCF.findMethod(m)
            }
        }
        foundMethods
    }

    /**
     * This class is used to reconstruct explicit intents from the analysed project.
     * @param caller The method that sends the explicit intent.
     * @param callee The method the explicit intent is send to.
     */
    class ExplicitIntent(val caller: Method, val callee: ClassFile) {

        val activityStartMethods: List[String] = List(
            "startActivity", "startActivityForResult", "startActivityFromChild", "startActivityIfNeeded", "getActivity", "getActivities"
        )
        val serviceMethods: List[String] = List(
            "startService", "bindService", "stopService", "unbindService", "getService"
        )
        val broadcastReceiverStartMethods: List[String] = List(
            "sendBroadcast", "sendOrderedBroadcast", "sendStickyBroadcast", "sendStickyOrderedBroadcast", "getBroadcast"
        )

        final val onCreate = "onCreate"
        final val onStartCommand = "onStartCommand"
        final val startService = "startService"
        final val onBind = "onBind"
        final val onRebind = "onRebind"
        final val onUnbind = "onUnbind"
        final val bindService = "bindService"
        final val stopService = "stopService"
        final val unbindService = "unbindService"
        final val stopSelf = "stopSelf"
        final val onReceive = "onReceive"

        var pc: Int = 0
        var calledMethod: List[Method] = List.empty[Method]

        def findStartedMethod(
            tacCode: TACode[TACMethodParameter, DUVar[ValueInformation]],
            useSite: List[Int]
        ): Unit = {
            val calledMethods = new ListBuffer[Method]

            useSite.foreach { i ⇒
                val stmt = tacCode.stmts(i)
                if (stmt.isVirtualMethodCall) {
                    calledMethods ++= internalSearchMethod(stmt.asVirtualMethodCall.name, stmt.asVirtualMethodCall.pc)

                } else if (stmt.isExprStmt) {
                    if (stmt.asExprStmt.expr.isVirtualFunctionCall) {
                        calledMethods ++= internalSearchMethod(stmt.asExprStmt.expr.asVirtualFunctionCall.name, stmt.asExprStmt.pc)
                    }
                } else if (stmt.isAssignment) {
                    if (stmt.asAssignment.expr.isStaticFunctionCall) {
                        calledMethods ++= internalSearchMethod(stmt.asAssignment.expr.asStaticFunctionCall.name, stmt.asAssignment.pc)
                    }
                }
            }
            calledMethod = calledMethods.toList
        }

        def internalSearchMethod(
            nme: String,
            pc:  Int
        ): List[Method] = {
            var calledMethods = List.empty[Method]
            if (activityStartMethods.contains(nme)) {
                this.pc = pc
                calledMethods ++= callee.findMethod(onCreate)
            } else if (broadcastReceiverStartMethods.contains(nme)) {
                this.pc = pc
                calledMethods ++= callee.findMethod(onReceive)
            } else if (serviceMethods.contains(nme)) {
                this.pc = pc
                nme match {
                    case `startService` ⇒ calledMethods ++= callee.findMethod(onCreate) ++ callee.findMethod(onStartCommand)
                    case `bindService` ⇒ calledMethods ++= callee.findMethod(onCreate) ++ callee.findMethod(onBind) ++
                        callee.findMethod(onRebind)
                    case `stopService`   ⇒ calledMethods ++= callee.findMethod(stopSelf)
                    case `unbindService` ⇒ calledMethods ++= callee.findMethod(onUnbind)
                }
            }
            calledMethods
        }
    }

    /**
     * This class is used to reconstruct intent filters from the analysed project. It holds all relevant information to
     * simulate intent matching.
     */
    class IntentFilter() {
        final val addDataAuthority = "addDataAuthority"
        final val addDataPath = "addDataPath"
        final val addDataScheme = "addDataScheme"
        final val addDataSSP = "addDataSchemeSpecificPart"
        final val addDataType = "addDataType"
        final val paramRegex = ".*\\(\"|\"\\).*"
        final val addAction = "addAction"
        final val addCategory = "addCategory"
        final val registerReceiver = "registerReceiver"
        final val receiverRegex = ".*\\: |\\[.*"

        final val filterDataMethods: List[String] = List(
            addDataAuthority, addDataPath, addDataScheme, addDataSSP, addDataType
        )

        var receiver: ClassFile = _
        var componentType: String = _
        var registeredReceivers: ListBuffer[ObjectType] = ListBuffer.empty[ObjectType]

        var actions: ListBuffer[String] = ListBuffer.empty[String]
        var categories: ListBuffer[String] = ListBuffer.empty[String]

        var dataTypes: ListBuffer[String] = ListBuffer.empty[String]
        var dataAuthorities: ListBuffer[String] = ListBuffer.empty[String]
        var dataPaths: ListBuffer[String] = ListBuffer.empty[String]
        var dataSchemes: ListBuffer[String] = ListBuffer.empty[String]
        var dataSSPs: ListBuffer[String] = ListBuffer.empty[String]

        def cloneFilter(): IntentFilter = {
            val clone = new IntentFilter()
            clone.actions = actions
            clone.categories = categories
            clone.dataAuthorities = dataAuthorities
            clone.dataTypes = dataTypes
            clone.dataPaths = dataPaths
            clone.dataSchemes = dataSchemes
            clone.componentType = componentType
            clone.receiver = receiver
            clone
        }

        def evaluateUseSites(
            tacCode:  TACode[TACMethodParameter, DUVar[ValueInformation]],
            useSites: List[Int]
        ): Unit = {
            val statements = tacCode.stmts
            useSites.foreach { use ⇒
                val stmt = statements(use)
                if (stmt.isVirtualMethodCall) {
                    val name = stmt.asVirtualMethodCall.name

                    val parameter = stmt.asVirtualMethodCall.params
                    if (name == addAction) {
                        actions += parameter.head.asVar.value.toString.replaceAll(paramRegex, "")
                    }
                    if (name == addCategory) {
                        categories += parameter.head.asVar.value.toString.replaceAll(paramRegex, "")

                    }
                    if (filterDataMethods.contains(name)) {
                        name match {
                            case `addDataAuthority` ⇒ dataAuthorities += (parameter.head.asVar.value.toString.
                                replaceAll(paramRegex, "") + parameter.last.asVar.value.toString.
                                replaceAll(paramRegex, ""))
                            case `addDataPath` ⇒ dataPaths += (parameter.head.asVar.value.toString.
                                replaceAll(paramRegex, "") + parameter.last.asVar.value.toString.
                                replaceAll(paramRegex, ""))
                            case `addDataScheme` ⇒ dataSchemes += parameter.head.asVar.value.toString.
                                replaceAll(paramRegex, "")
                            case `addDataSSP` ⇒ dataSSPs += parameter.head.asVar.value.toString.
                                replaceAll(paramRegex, "")
                            case `addDataType` ⇒ dataTypes += parameter.head.asVar.value.toString.
                                replaceAll(paramRegex, "")
                        }
                    }
                }
                if (stmt.isAssignment) {
                    if (stmt.asAssignment.expr.isVirtualFunctionCall && stmt.asAssignment.expr.asVirtualFunctionCall.name == registerReceiver) {
                        val virtualCall = stmt.asAssignment.expr.asVirtualFunctionCall
                        if (virtualCall.receiver.asVar.definedBy.head > -1)
                            registeredReceivers += ObjectType(statements(virtualCall.
                                receiver.asVar.definedBy.head).asAssignment.expr.asGetField.objRef.asVar.value.toString.
                                replaceAll(receiverRegex, "").replaceAll("\\.", "/"))
                    }
                }
            }
        }
    }

    /**
     * This class is used to reconstruct the implicit intents, that are sent in the analysed project.
     * @param caller The method that sends the implicit intent. It is the caller of all icc calls that originate from this intent.
     */
    class ImplicitIntent(var caller: Method) {

        var action: String = ""
        var categories: ListBuffer[String] = ListBuffer.empty[String]
        var iType: String = ""
        var iPackage: String = ""
        var iData: String = ""
        var componentTypes: ListBuffer[String] = ListBuffer.empty[String]
        var calledMethods: ListBuffer[String] = ListBuffer.empty[String]
        var pc: Int = 0

        override def clone(): ImplicitIntent = {
            val clone = new ImplicitIntent(caller)
            clone.action = action
            clone.categories = categories
            clone.iType = iType
            clone.iPackage = iPackage
            clone.iData = iData
            clone.componentTypes = componentTypes
            clone.calledMethods = calledMethods
            clone
        }
    }
}

/**
 * Schedules the execution of an AndroidICCAnalysis.
 * In order for the analysis to work the path to the projects AndroidManifest must be set via setManifest.
 */
object eagerAndroidICCAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {
    var manifestPath: String = _
    def setManifest(manifestPath: String): Unit = {
        this.manifestPath = manifestPath
    }
    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey, TypeProviderKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callers, Callees, TACAI)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(Callers, Callees)

    override def start(p: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new AndroidICCAnalysis(p)
        propertyStore.scheduleEagerComputationForEntity(XML.loadFile(manifestPath))(analysis.performAnalysis)
        analysis
    }
}
