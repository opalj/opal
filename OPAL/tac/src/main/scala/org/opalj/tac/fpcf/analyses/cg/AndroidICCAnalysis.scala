/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.br.{ClassFile, DeclaredMethod, Method, ObjectType, ObjectVariableInfo}
import org.opalj.br.analyses.{DeclaredMethodsKey, ProjectInformationKeys, SomeProject}
import org.opalj.br.instructions.{INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL, Instruction}
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.{OrderedProperty, PartialResult, ProperPropertyComputationResult, Property, PropertyBounds, PropertyKey, PropertyMetaInformation, PropertyStore, Results}
import org.opalj.value.ValueInformation

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import org.opalj.br.fpcf.properties.cg.{Callees, Callers}
import org.opalj.br.fpcf.{BasicFPCFEagerAnalysisScheduler, FPCFAnalysis}

import scala.io.Source
import scala.xml.{Elem, XML}

sealed trait AndroidICCPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = AndroidICC
}
sealed trait AndroidICC extends OrderedProperty with AndroidICCPropertyMetaInformation {
    final def key: PropertyKey[AndroidICC] = AndroidICC.key
}

object AndroidICC extends AndroidICCPropertyMetaInformation {
    final val key: PropertyKey[AndroidICC] = PropertyKey.create("AndroidICC")
}

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
    final val intentString = "android/content/Intent"
    final val stringString = "java/lang/String"
    final val pendingIntent = "android/app/PendingIntent"
    final val androidActivity = "android/app/Activity"
    final val androidService = "android/app/Service"
    final val androidReceiver = "android/content/BroadcastReceiver"
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

    final val senderClasses = List(ObjectType("android/content/Context"), ObjectType("android/app/PendingIntent"),
        ObjectType("android/content/IntentSender"), ObjectType(androidActivity))
    final val virtualSendingMethods = List("startIntentSender", "startIntentSenderForResult", "startIntentSenderFromChild",
        "startActivityFromChild", "startActivity", "startActivities", "startActivityForResult", "startNextMatchingActivity",
        "sendIntent", "send", "bindIsolatedService", "bindService", "bindServiceAsUser", "sendBroadcast",
        "sendBroadcastAsUser", "sendBroadcastWithMultiplePermissions", "sendOrderedBroadcast", "sendOrderedBroadcastAsUser",
        "sendStickyBroadcast", "sendStickyBroadcastAsUser", "sendStickyOrderedBroadcast", "sendStickyOrderedBroadcastAsUser",
        "startForegroundService", "startService")

    final val staticSendingMethods = List("getActivities", "getActivity", "getBroadcast", "getForegroundService", "getService")

    val activityStartMethods: List[String] = List("startActivity", "startActivityForResult", "startActivityFromChild",
        "startActivityIfNeeded", "startActivities", "getActivity", "getActivities", "startNextMatchingActivity")
    val serviceMethods: List[String] = List("startService", "bindService", "stopService", "unbindService", "getService",
        "bindIsolatedService", "bindServiceAsUser", "startForegroundService")
    val broadcastReceiverStartMethods: List[String] = List("sendBroadcast", "sendOrderedBroadcast", "sendStickyBroadcast",
        "sendStickyOrderedBroadcast", "getBroadcast", "sendBroadcastAsUser", "sendStickyBroadcastAsUser",
        "sendStickyOrderedBroadcastAsUser")

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

    final val intentMethods = List(setAction, addCategory, setData, setDataAndNormalize, setDataAndType,
        setDataAndTypeAndNormalize, setType, setTypeAndNormalize, setPackage)
    final val explicitMethods = List(setClass, setClassName, setComponent)

    val intentFilters: ListBuffer[intentFilter] = ListBuffer.empty[intentFilter]
    val edges: ListBuffer[((Method, Int), Method)] = ListBuffer.empty[((Method, Int), Method)]
    val explicitIntents: ListBuffer[explicitIntent] = ListBuffer.empty[explicitIntent]
    val implicitIntents: ListBuffer[implicitIntent] = ListBuffer.empty[implicitIntent]

    val senderMap: mutable.Map[(ObjectType, String), ListBuffer[(Method, String, Int)]] =
        mutable.Map.empty[(ObjectType, String), ListBuffer[(Method, String, Int)]] //Class and name of the Method that returns the searched intent -> the intent sending methods and the component type
    val returningIntentsMap: mutable.Map[(ObjectType, String), ListBuffer[implicitIntent]] =
        mutable.Map.empty[(ObjectType, String), ListBuffer[implicitIntent]] //class and method that returns an intent -> intent

    val componentMap: mutable.Map[String, ListBuffer[ClassFile]] = mutable.Map(
        activity -> ListBuffer.empty[ClassFile],
        service -> ListBuffer.empty[ClassFile]
    )

    /**
     * Calls all necessary methods to generate the call graph edges that are Android specific.
     *
     * @param manifest AndroidManifest.xml
     *
     */
    def performAnalysis(
                         manifest: Elem
    ): ProperPropertyComputationResult = {
        parseManifest(manifest)
        generateLifecycleCallbacks()
        searchIntentsAndCallbacks()
        intentMatching()

        val decMeths = project.get(DeclaredMethodsKey)
        val resList = ListBuffer.empty[PartialResult[DeclaredMethod, _ >: Null <: Property]]

        edges.foreach { e ⇒
            val calls = new IndirectCalls()
            calls.addCall(new SimpleContext(decMeths(e._1._1)), e._1._2, decMeths(e._2))
            resList ++= calls.partialResults(decMeths(e._1._1)).seq
        }

        Results(resList)
    }

    /**
     * Analyses the AndroidManifest.xml of the project to find intent filters and relevant components to generate
     * lifecycle callbacks
     *
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
                            val intentFilter = new intentFilter()
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
     * Uses the Results from parseManifest.
     */
    def generateLifecycleCallbacks(): Unit = {
        var fullLifecycle = true //cg is only sound if this is true
        var aLifecycleList = List.empty[Method]
        var sLifecycleList = List.empty[Method]
        //find default super methods
        if(fullLifecycle) {
            val activityCF = project.classFile(ObjectType(androidActivity)).orNull
            if (activityCF != null) {
                val AonCreate = activityCF.findMethod(onCreate).head
                val AonStart = activityCF.findMethod(onStart).head
                val AonRestart = activityCF.findMethod(onRestart).head
                val AonResume = activityCF.findMethod(onResume).head
                val AonPause = activityCF.findMethod(onPause).head
                val AonStop = activityCF.findMethod(onStop).head
                val AonDestroy = activityCF.findMethod(onDestroy).head
                val AonActivityResult = activityCF.findMethod(onActivityResult).head
                aLifecycleList = List(AonCreate, AonStart, AonResume, AonPause, AonStop, AonDestroy, AonActivityResult, AonRestart)
            }
            else fullLifecycle = false

            val serviceCF = project.classFile(ObjectType(androidService)).orNull

            if(serviceCF != null){
                val SonCreate = serviceCF.findMethod(onCreate).head
                val SonStart = serviceCF.findMethod(onStartCommand).head
                val SonBind = serviceCF.findMethod(onBind).head
                val SonUnbind = serviceCF.findMethod(onUnbind).head
                val SonDestroy = serviceCF.findMethod(onDestroy).head
                sLifecycleList = List(SonCreate, SonStart, SonBind, SonUnbind, SonDestroy)
            }
            else fullLifecycle = false
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

            if(fullLifecycle){
                val l = ListBuffer.empty[Method]
                for (index ← lifecycleListOption.indices){
                    if(lifecycleListOption(index).isEmpty){
                        l += aLifecycleList(index)
                    }
                    else l += lifecycleListOption(index).get
                }
                if(restart.isEmpty){
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

            }
            else {
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

            if(fullLifecycle){
                val lifecycleList1 = List (create, startComm, bind, unbind, destroy)
                val l = ListBuffer.empty[Method]
                for (index ← lifecycleList1.indices){
                    if(lifecycleList1(index).isEmpty){
                        l += sLifecycleList(index)
                    }
                    else l += lifecycleList1(index).get
                }
                val lifecycleList = l.toList
                makeEdge(lifecycleList.head, lifecycleList(1))
                makeEdge(lifecycleList.head, lifecycleList(2))
                makeEdge(lifecycleList(1), lifecycleList(4))
                makeEdge(lifecycleList(2), lifecycleList(3))
                makeEdge(lifecycleList(3), lifecycleList(4))
                makeEdge(lifecycleList(1), lifecycleList(4))
            }
            else {
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
     * searches all Intents and callbacks in the project.
     *
     */
    def searchIntentsAndCallbacks(): Unit = {
        val tacKey = project.get(ComputeTACAIKey)
        val source = Source.fromFile("OPAL\\tac\\src\\main\\resources\\callbackList.txt")
        val callbackList = source.getLines.toList
        source.close
        val dec = project.get(DeclaredMethodsKey)
        project.allMethodsWithBody.filter(dec(_) != null).foreach(m ⇒ {
            val body = m.body.get
            val intentUseSites: mutable.Map[Int, List[Int]] = mutable.Map.empty[Int, List[Int]]
            try{
            val tacCode = tacKey(m)

            body.collect {
                case in: Instruction if in.isInstanceOf[INVOKESPECIAL] ⇒ in
            }.foreach(in ⇒ {

                val inSpecial = in.value.asInstanceOf[INVOKESPECIAL]
                val superInterfaceClasses = project.classHierarchy.allSuperinterfacetypes(inSpecial.declaringClass.asObjectType, true)

                //find IntentFilters
                if (superInterfaceClasses.contains(ObjectType(intentFilterString))) {
                        val stms = tacCode.stmts
                        val index = tacCode.pcToIndex(in.pc)
                        val c = stms(index)
                        val useSites = tacCode.stmts(c.asNonVirtualMethodCall.receiver.asVar.definedBy.head).
                            asAssignment.targetVar.usedBy.toChain.toList
                        reconstructIntentfilter(c.asNonVirtualMethodCall, tacCode, useSites,m)
                }

                //find Intents
                if (superInterfaceClasses.contains(ObjectType(intentString))) {
                    val stms = tacCode.stmts
                    val index = tacCode.pcToIndex(in.pc)
                    val c = stms(index).asNonVirtualMethodCall
                    val useSites = tacCode.stmts(c.receiver.asVar.definedBy.head).
                      asAssignment.targetVar.usedBy.toChain.toList
                    intentUseSites += index -> useSites
                    reconstructIntent(c, tacCode, useSites, m)
                }
            })

            //find sending methods (virtual)
            body.collect {
                case in: Instruction if in.isInstanceOf[INVOKEVIRTUAL] &&
                    senderClasses.contains(in.asInstanceOf[INVOKEVIRTUAL].declaringClass) ⇒ in
            }.foreach{ in ⇒
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
                    in.asInstanceOf[INVOKESTATIC].declaringClass == ObjectType(pendingIntent) ⇒ in
            }.foreach{in ⇒
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
            body.collect{ case in: Instruction if in.isInstanceOf[INVOKEVIRTUAL] ⇒ in }.foreach { in ⇒
                val index = tacCode.pcToIndex(in.pc)
                if (index > -1) {
                    val methodCall = tacCode.stmts(index)
                    if (methodCall.isVirtualMethodCall) {
                        var i = 0
                        methodCall.asVirtualMethodCall.descriptor.parameterTypes.foreach { p ⇒
                            if (p.isObjectType) {
                                val ot = p.toString.replaceAll(otRegex, "")
                                //only interfaces defined in 'callbacklist' are handled as callback interfaces and dynamically searching for callback interfaces
                                if(callbackList.contains(ot) ||
                                  (ot.startsWith("android") && ot.endsWith("Listener") && ot.substring(ot.lastIndexOf("$") + 1).startsWith("On"))){
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
            }}
          catch{
              case _: Throwable =>
        }
        })


    }

    def generateCallBackEdges(
                               m:               Method,
                               callbackMethods: ListBuffer[Method]
                             ): Unit = {

        val cf = m.classFile
        val compType = cf.thisType

        if (project.classHierarchy.isSubtypeOf(compType, ObjectType(androidActivity))) {
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
                return
            }

        } else if (project.classHierarchy.isSubtypeOf(compType, ObjectType(androidService))) {
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
                return
            }
        }
    }

    //Checks if the intent is reachable within the processed method
    def findOrigin(
                    params:           Seq[Expr[DUVar[ValueInformation]]],
                    tacCode:          AITACode[TACMethodParameter, ValueInformation],
                    originalUseSites: mutable.Map[Int, List[Int]]
                  ): Boolean = {
        params.foreach { p ⇒
            if (p.asVar.value.toString.startsWith(intentStart.head) || p.asVar.value.toString.startsWith(intentStart(1))) {
                //the intent to its source
                val defIndex = p.asVar.definedBy.head
                if (defIndex < 0) {
                    return false //intent is not reachable
                }
                if (originalUseSites.exists(k ⇒ k._2.contains(defIndex))) {
                    return true
                }
                val stm = tacCode.stmts(defIndex)
                if (stm.isAssignment) {
                    if (stm.asAssignment.expr.isStaticFunctionCall || stm.asAssignment.expr.isVirtualFunctionCall) {
                        return findOrigin(stm.asAssignment.expr.asFunctionCall.params, tacCode, originalUseSites)
                    }
                }
            }
        }
        false //intent is not reachable
    }

    /**
     * searches the parameters of functionCall to find the origin of the intent and return the class and method
     * that returns the searched intent
     *
     */
    def searchParamsStatic(
                            functionCall: FunctionCall[DUVar[ValueInformation]],
                            tacCode: AITACode[TACMethodParameter, ValueInformation]
                          ): Option[(ObjectType, String)] = {
        functionCall.params.foreach { p ⇒
            if (p.asVar.value.toString.startsWith(intentStart.head) || p.asVar.value.toString.startsWith(intentStart(1))) {
                val defIndex = p.asVar.definedBy.head
                if (defIndex < 0) {
                    return None
                }
                val defSiteStm = tacCode.stmts(defIndex)
                if (defSiteStm.isAssignment) {
                    val call = defSiteStm.asAssignment.expr.asFunctionCall
                    if (!intentMethods.contains(call.name)) {
                        return Some[(ObjectType, String)](call.declaringClass.asObjectType -> call.name)
                    }
                    return searchParamsStatic(call, tacCode)
                }
            }
        }
        Some[(ObjectType, String)](functionCall.declaringClass.asObjectType -> functionCall.name)
    }

    /**
     * searches the parameters of functionCall to find the origin of the intent and return the class and method
     * that returns the searched intent
     *
     */
    def searchParamsVirtual(
                             functionCall: MethodCall[DUVar[ValueInformation]],
                             tacCode: AITACode[TACMethodParameter, ValueInformation]
                           ): Option[(ObjectType, String)] = {
        functionCall.params.foreach { p ⇒
            if (p.asVar.value.toString.startsWith(intentStart.head) || p.asVar.value.toString.startsWith(intentStart(1))) {

                val defIndex = p.asVar.definedBy.head
                if (defIndex < 0) {
                    return None
                }
                val defSiteStm = tacCode.stmts(defIndex)
                if (defSiteStm.isAssignment && (defSiteStm.asAssignment.expr.isVirtualFunctionCall ||
                  defSiteStm.asAssignment.expr.isStaticFunctionCall)) {

                    val call = defSiteStm.asAssignment.expr.asFunctionCall
                    if (!intentMethods.contains(call.name)) {
                        return Some[(ObjectType, String)](call.declaringClass.asObjectType -> call.name)
                    }
                    return searchParamsStatic(call, tacCode)
                }
            }
        }
        Some[(ObjectType, String)](functionCall.declaringClass.asObjectType -> functionCall.name)
    }

    def getComponentType(
                          name: String
                        ): String = {
        if (activityStartMethods.contains(name)) {
            return activity
        }
        if (serviceMethods.contains(name)) {
            return service
        }
        if (broadcastReceiverStartMethods.contains(name)) {
            return receiver
        }
        undef
    }

    def reconstructIntent(
                           c:           NonVirtualMethodCall[V],
                           tacCode:     AITACode[TACMethodParameter, ValueInformation],
                           useSites:    List[Int],
                           m:           Method
    ): Unit = {
        val p = c.params

        p.size match {
            case 0 ⇒ //Intent()
                val intent = new implicitIntent(m)
                evaluateUseSites(tacCode, useSites, intent)
            case 1 ⇒ //Intent(String action)
                if (p.head.asVar.value.verificationTypeInfo == ObjectVariableInfo(ObjectType(stringString))) {
                    val intent = new implicitIntent(m)
                    intent.setAction(p.head.asVar.value.toString.replaceAll(paramRegex, ""))
                    evaluateUseSites(tacCode, useSites, intent)
                } else {
                    //Intent(Intent 0)
                    if(p.head.asVar.definedBy.head > -1){
                        val stmts = tacCode.stmts
                        val na = stmts(p.head.asVar.definedBy.head).asAssignment
                        val addUseSites = na.targetVar.usedBy.toChain.toList
                        addUseSites.foreach{i =>
                          val s = stmts(i)
                          if(s.isNonVirtualMethodCall && s.asNonVirtualMethodCall.name == init && c.pc != s.pc){
                              reconstructIntent(s.asNonVirtualMethodCall, tacCode, useSites ++ addUseSites, m)
                          }
                        }
                    }
                    else{
                        //intent comes from another method => overapproximation
                        val intent = new implicitIntent(m)
                        intent.setData(undef)
                        intent.setAction(undef)
                        intent.addCategory(undef)
                        evaluateUseSites(tacCode, useSites, intent)
                    }
                }
            case 2 ⇒ //Intent(String action, Uri uri)
                if (p.head.asVar.value.verificationTypeInfo == ObjectVariableInfo(ObjectType(stringString))) {

                    val intent = new implicitIntent(m)
                    intent.setAction(p.head.asVar.value.toString.replaceAll(paramRegex, ""))
                    intent.setData(checkData(1, tacCode, p))
                    evaluateUseSites(tacCode, useSites, intent)

                } else { //Intent(Context context, Class cls)
                    val clsFile = project.classFile(ObjectType(p.last.asVar.value.toString.
                      replaceAll(valueRegex, "").replaceAll("\\.", "/")))

                    if (clsFile.isDefined) {
                        val intent = new explicitIntent(m, clsFile.get)
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
                    val intent = new explicitIntent(m, clsFile.get)
                    intent.findStartedMethod(tacCode, useSites)
                    if (intent.calledMethod.nonEmpty) {
                        explicitIntents += intent
                    }
                }
                else {
                    val intent = new implicitIntent(m)
                    intent.setAction(p.head.asVar.value.toString.replaceAll(paramRegex, ""))
                    checkData(1, tacCode, p)
                    intent.setData(checkData(1, tacCode, p))
                    evaluateUseSites(tacCode, useSites, intent)
                }
        }
    }

    //Searches the value of a data parameter
    def checkData(
                   dataPosition: Int,
                   tacCode:      AITACode[TACMethodParameter, ValueInformation],
                   parameter:    Seq[Expr[DUVar[ValueInformation]]]
                 ): String = {
        val defSite = parameter(dataPosition).asVar.definedBy.head
        if(defSite > -1){
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
                                    return str.asAssignment.targetVar.value.toString.replaceAll(paramRegex, "")
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
                                 c:         NonVirtualMethodCall[V],
                                 tacCode:   AITACode[TACMethodParameter, ValueInformation],
                                 useSites:  List[Int],
                                 m:         Method
    ): Unit = {
        val p = c.params
        val intentFilter = new intentFilter()
        val stmts = tacCode.stmts

        p.size match {
            case 1 ⇒
                if (p.head.asVar.value.verificationTypeInfo == ObjectVariableInfo(ObjectType(stringString))) {
                    intentFilter.addAction(p.head.asVar.value.toString.replaceAll(paramRegex, ""))
                    intentFilter.evaluateUseSites(tacCode, useSites)
                    if(intentFilter.registeredReceivers.nonEmpty) handleReceivers(intentFilter)
                } else { //copy constructor
                    val addUseSites = stmts(p.head.asVar.definedBy.head).asAssignment.targetVar.usedBy.toChain.toList
                    addUseSites.foreach{i =>
                        val s = stmts(i)
                        if(s.isNonVirtualMethodCall && s.asNonVirtualMethodCall.name == init && s.pc != c.pc){
                            reconstructIntentfilter(s.asNonVirtualMethodCall, tacCode, useSites ++ addUseSites, m)
                        }
                    }
                }
            case 2 ⇒
                intentFilter.addAction(p.head.asVar.value.toString.replaceAll(paramRegex, ""))
                intentFilter.addDataType(p(1).asVar.value.toString.replaceAll(paramRegex, ""))
                intentFilter.evaluateUseSites(tacCode, useSites)
                if(intentFilter.registeredReceivers.nonEmpty) handleReceivers(intentFilter)
            case 0 ⇒
                intentFilter.evaluateUseSites(tacCode, useSites)
                if(intentFilter.registeredReceivers.nonEmpty) handleReceivers(intentFilter)
        }

    }

    def handleReceivers(
                           intentFilter: intentFilter
    ): Unit= {
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
                          tacCode:  AITACode[TACMethodParameter, ValueInformation],
                          useSites: List[Int],
                          intent:   implicitIntent
                        ): Unit = {
        val statements = tacCode.stmts
        val addUseSites: ListBuffer[Int] = ListBuffer.empty[Int]

        useSites.foreach { ln ⇒
            val stm = statements(ln)

            if (stm.isExprStmt && stm.asExprStmt.expr.isVirtualFunctionCall) {
                    val virtualCall = stm.asExprStmt.expr.asVirtualFunctionCall
                    val someIntent = internalFinder(virtualCall.name, virtualCall.params, intent, tacCode, virtualCall.pc)
                    if (someIntent.isDefined) {
                        val expIntent = someIntent.head
                        expIntent.findStartedMethod(tacCode, useSites)
                        if (expIntent.calledMethod.nonEmpty) {
                            explicitIntents += expIntent
                            return
                        }
                    }
            } else if (stm.isVirtualMethodCall) {
                val virtualCall = stm.asVirtualMethodCall
                internalFinder(virtualCall.name, virtualCall.params, intent, tacCode, virtualCall.pc)
            } else if (stm.isAssignment && stm.asAssignment.expr.isVirtualFunctionCall) {
                    val virtualCall = stm.asAssignment.expr.asVirtualFunctionCall
                    if (virtualCall.declaringClass == ObjectType(intentString)) { addUseSites ++= stm.asAssignment.targetVar.usedBy.toChain }
                    val someIntent = internalFinder(virtualCall.name, virtualCall.params, intent, tacCode, virtualCall.pc)
                    if (someIntent.isDefined) {
                        val expIntent = someIntent.head
                        expIntent.findStartedMethod(tacCode, useSites)
                        if (expIntent.calledMethod.nonEmpty) {
                            explicitIntents += expIntent
                            return
                        }
                    } else if (stm.asAssignment.expr.isStaticFunctionCall) {
                        val staticCall = stm.asAssignment.expr.asStaticFunctionCall
                        if (staticCall.declaringClass == ObjectType(intentString)) addUseSites ++= stm.asAssignment.targetVar.usedBy.toChain
                        internalFinder(staticCall.name, staticCall.params, intent, tacCode, staticCall.pc)
                    }
            } else if (stm.isInstanceOf[ReturnValue[DUVar[ValueInformation]]]) {
                val intentTupel = intent.caller.classFile.thisType -> intent.caller.name
                if (returningIntentsMap.contains(intentTupel)) {
                    returningIntentsMap(intentTupel) += intent
                } else returningIntentsMap += ((intent.caller.classFile.thisType -> intent.caller.name) -> ListBuffer(intent))
            }
        }
        if (addUseSites.nonEmpty) {
            evaluateUseSites(tacCode, addUseSites.toList, intent)
        }
        else implicitIntents += intent
    }

    def internalFinder(
                        nme:       String,
                        parameter: Seq[Expr[DUVar[ValueInformation]]],
                        intent:    implicitIntent,
                        tacCode:   AITACode[TACMethodParameter, ValueInformation],
                        pc:        Int
                      ): Option[explicitIntent] = {

        if (explicitMethods.contains(nme)) {
            if (nme.equals(setComponent)) {
                tacCode.stmts(parameter.head.asVar.definedBy.head).asAssignment.targetVar.usedBy.foreach { u ⇒
                    val stm = tacCode.stmts(u)
                    if (stm.isNonVirtualMethodCall && stm.asNonVirtualMethodCall.declaringClass ==
                      ObjectType(androidComponentName)) {
                        val param = stm.asNonVirtualMethodCall.params
                        if (param.last.asVar.value.toString.startsWith("Class")) {
                            val clsFile = project.classFile(ObjectType(param.last.asVar.value.toString.
                              replaceAll(valueRegex, "").replaceAll("\\.", "/")))
                            if (clsFile.isDefined) {
                                val i = new explicitIntent(intent.caller, clsFile.get)
                                i.pc = pc
                                return Some(i)
                            }
                        }
                    }
                }
            }
            else{
                nme match{
                    case `setClass` => {
                        val clsFile = project.classFile(
                            ObjectType(parameter(1).toString.replaceAll(valueRegex, "").replaceAll("\\.", "/")))
                        if(clsFile.isDefined){
                            val i = new explicitIntent(intent.caller, clsFile.get)
                            i.pc = pc
                            return Some(i)
                        }
                    }
                    case `setClassName` => {
                        val clsFile = project.classFile(
                            ObjectType(parameter(1).toString.replaceAll(paramRegex, "").replaceAll("\\.", "/")))
                        if(clsFile.isDefined){
                            val i = new explicitIntent(intent.caller, clsFile.get)
                            i.pc = pc
                            return Some(i)
                        }
                    }
                }
            }
        } else if (intentMethods.contains(nme)) {
            val firstParam: String = parameter.head.asVar.value.toString.replaceAll(paramRegex, "")

            nme match {
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
        } else if (activityStartMethods.contains(nme)) {
            intent.pc = pc
            intent.componentTypes += activity
            intent.calledMethods += onCreate
        } else if (serviceMethods.contains(nme)) {
            intent.pc = pc
            intent.componentTypes += service
            nme match {
                case `startService`  ⇒ intent.calledMethods ++= ListBuffer(onCreate, onStartCommand)
                case `bindService`   ⇒ intent.calledMethods ++= ListBuffer(onCreate, onBind, onRebind)
                case `stopService`   ⇒ intent.calledMethods += stopSelf
                case `unbindService` ⇒ intent.calledMethods += onUnbind
            }
        } else if (broadcastReceiverStartMethods.contains(nme)) {
            intent.pc = pc
            intent.componentTypes += receiver
            intent.calledMethods ++= ListBuffer(onReceive, onBroadcastReceive)
        }
        None
    }

    def setComponentType(
                          superTypes: UIDSet[ObjectType],
                          filter: intentFilter
    ): Unit = {
        if (superTypes.contains(ObjectType(androidActivity))) {
            filter.componentType = activity
        } else if (superTypes.contains(ObjectType(androidService))) {
            filter.componentType = service
        } else if (superTypes.contains(ObjectType(androidReceiver))) {
            filter.componentType = receiver
        } else filter.componentType = undef

    }

    /**
     * Matches found intents with intent filters and writes the results to 'edges'.
     * Uses the results from 'searchIntentsAndCallbacks'.
     */
    def intentMatching(): Unit = {
        //ordering Filters in maps for matching
        val filterMap: mutable.Map[String, mutable.Map[String, mutable.Map[ListBuffer[String], ListBuffer[intentFilter]]]] = mutable.Map.empty
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
                senderMap(k).foreach{s =>
                    val intent = new implicitIntent(s._1)
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
                                    overApproximateEdges(intent, filterMap(compType)(intent.action)(categories))
                                } else {
                                    testData(intent, filterMap(compType)(intent.action)(categories))
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

    def testData(
                  intent: implicitIntent,
                  filters: ListBuffer[intentFilter]
    ): Unit = {

        for (filter ← filters) {
            val types = filter.dataTypes
            val schemes = filter.dataSchemes
            val data = intent.iData
            if (types.isEmpty && schemes.isEmpty) {
                if (intent.iType.isEmpty && data.isEmpty) {
                    matchFound(intent, filter)
                }
            } else {
                if (data.nonEmpty) {
                    val ssi = data.indexOf(":")
                    val scheme = data.substring(0, ssi)
                    if (schemes.contains(scheme)) {
                        val authorities = filter.dataAuthorities
                        val authorityEnd = findAuthority(data)
                        if (authorityEnd > ssi + 3) {
                            val authority = data.substring(ssi + 3, authorityEnd)

                            if (authorities.contains(authority)) {
                                val paths = filter.dataPaths
                                if (paths.isEmpty) {
                                    matchFound(intent, filter)
                                } else if (ssi < authorityEnd) {
                                    val path = findPath(data, authorityEnd)

                                    if (paths.exists(p ⇒ p.startsWith(path))) {
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
            }
        }
    }

    def overApproximateEdges(
                              intent:  implicitIntent,
                              filters: ListBuffer[intentFilter]
                            ): Unit = {
        filters.foreach { f ⇒
            matchFound(intent, f)
        }
    }

    def matchFound(
                    intent: implicitIntent,
                    filter: intentFilter
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
                    case '/' | '\\' | '?' | '#' ⇒ return index
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
                case '?' | '#' | '*' ⇒ return uri.substring(start + 1, i)
                case _               ⇒ i += 1
            }
        }
        uri.substring(start + 1)
    }

    def findCalledMethods(
                           receiverCF:       ClassFile,
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

}

class explicitIntent(val caller: Method, val callee: ClassFile) {

    val activityStartMethods: List[String] = List("startActivity", "startActivityForResult", "startActivityFromChild",
        "startActivityIfNeeded", "getActivity", "getActivities")
    val serviceMethods: List[String] = List("startService", "bindService", "stopService", "unbindService", "getService")
    val broadcastReceiverStartMethods: List[String] = List("sendBroadcast", "sendOrderedBroadcast",
        "sendStickyBroadcast", "sendStickyOrderedBroadcast", "getBroadcast")

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
                           tacCode: AITACode[TACMethodParameter, ValueInformation],
                           useSite: List[Int]
    ): Unit = {
        val calledMethods = new ListBuffer[Method]

        useSite.foreach { i ⇒
            val stm = tacCode.stmts(i)
            if (stm.isVirtualMethodCall) {
                calledMethods ++= internalSearchMethod(stm.asVirtualMethodCall.name, stm.asVirtualMethodCall.pc)

            } else if (stm.isExprStmt) {
                if (stm.asExprStmt.expr.isVirtualFunctionCall) {
                    calledMethods ++= internalSearchMethod(stm.asExprStmt.expr.asVirtualFunctionCall.name, stm.asExprStmt.pc)
                }
            } else if (stm.isAssignment) {
                if (stm.asAssignment.expr.isStaticFunctionCall) {
                    calledMethods ++= internalSearchMethod(stm.asAssignment.expr.asStaticFunctionCall.name, stm.asAssignment.pc)
                }
            }
        }
        calledMethod = calledMethods.toList
    }

    def internalSearchMethod(
                              nme: String,
                              pc: Int
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

class intentFilter() {
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

    final val filterDataMethods: List[String] = List(addDataAuthority, addDataPath,
        addDataScheme, addDataSSP, addDataType)

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

    def addAction(action: String): Unit = {
        actions += action

    }
    def addCategory(category: String): Unit = {
        categories += category
    }
    def addDataType(dataType: String): Unit = {
        dataTypes += dataType
    }

    def setReceiver(rec: ClassFile): Unit = {
        receiver = rec
    }

    def cloneFilter(): intentFilter = {
        val clone = new intentFilter()
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
                          tacCode: AITACode[TACMethodParameter, ValueInformation],
                          useSites: List[Int]
    ): Unit = {
        val statements = tacCode.stmts
        useSites.foreach { ln ⇒
            val stm = statements(ln)
            if (stm.isVirtualMethodCall) {
                val name = stm.asVirtualMethodCall.name

                val parameter = stm.asVirtualMethodCall.params
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
            if (stm.isAssignment) {
                if (stm.asAssignment.expr.asVirtualFunctionCall.name == registerReceiver) {
                    if (stm.asAssignment.expr.asVirtualFunctionCall.receiver.asVar.definedBy.head > -1)
                        registeredReceivers += ObjectType(statements(stm.asAssignment.expr.asVirtualFunctionCall.
                            receiver.asVar.definedBy.head).asAssignment.expr.asGetField.objRef.asVar.value.toString.
                            replaceAll(receiverRegex, "").replaceAll("\\.", "/"))
                }
            }
        }
    }
}

class implicitIntent(var caller: Method) {

    var action: String = ""
    var categories: ListBuffer[String] = ListBuffer.empty[String]
    var iType: String = ""
    var iPackage: String = ""
    var iData: String = ""
    var componentTypes: ListBuffer[String] = ListBuffer.empty[String]
    var calledMethods: ListBuffer[String] = ListBuffer.empty[String]
    var pc: Int = 0

    def setAction(action: String): Unit = {
        this.action = action
    }

    def addCategory(category: String): Unit = {
        categories += category
    }
    def setData(uri: String): Unit = {
        iData = uri
    }

    override def clone(): implicitIntent = {
        val clone = new implicitIntent(caller)
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

/**
 * Schedules the execution of an AndroidICCAnalysis.
 * In order for the analysis to work the path to the projects AndroidManifest must be set via setManifest
 */
object eagerAndroidICCAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {
    var manifestPath: String = _
    def setManifest(manifestPath: String): Unit = {
        this.manifestPath = manifestPath
    }
    override def requiredProjectInformation: ProjectInformationKeys = Seq(ComputeTACAIKey)

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.lb(Callers), PropertyBounds.lb(Callees))

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = Set(PropertyBounds.lb(Callers), PropertyBounds.lb(Callees))

    override def start(p: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new AndroidICCAnalysis(p)
        propertyStore.scheduleEagerComputationForEntity(XML.loadFile(manifestPath))(analysis.performAnalysis)
        analysis
    }
}
