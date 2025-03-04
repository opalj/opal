/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package connector
package svf

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import svfjava.SVFAnalysisListener
import svfjava.SVFModule
import org.opalj.xl.logger.PointsToInteractionLogger

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.analyses.pointsto.{ArrayEntity, PointsToAnalysisBase, PointsToAnalysisState}
import org.opalj.tac.fpcf.analyses.APIBasedAnalysis
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.OnlyCallersWithUnknownContext
import org.opalj.br.DeclaredField
import org.opalj.br.fpcf.properties.fieldaccess.IndirectFieldAccesses

abstract class NativeAnalysis(
                               final val project:            SomeProject,
                               final override val apiMethod: DeclaredMethod
                             ) extends PointsToAnalysisBase with APIBasedAnalysis {

  case class SVFConnectorState(
                                calleeContext:          ContextType,
                                pc:                     Integer,
                                project:                SomeProject,
                                var svfModule:          SVFModule                            = null,
                                var n:                  Integer                              = -1000,
                                var svfModuleName:      String                               = System.getenv("LLVM_LIB_PATH"),
                                var connectorDependees: Set[EOptionP[Entity, Property]]      = Set.empty,
                                var connectorResults:   Set[ProperPropertyComputationResult] = Set.empty[ProperPropertyComputationResult],
                                var mapping: mutable.Map[Long, PointsToSet] = mutable.Map.empty[Long, PointsToSet],
                                var indirectFieldAccesses: IndirectFieldAccesses = new IndirectFieldAccesses()
                              ) extends BaseAnalysisState with TypeIteratorState


  def runSVF(implicit svfConnectorState: SVFConnectorState): ProperPropertyComputationResult = this.synchronized{
    if (svfConnectorState.svfModuleName == null || svfConnectorState.svfModuleName.isEmpty) {
      throw new RuntimeException("LLVM_LIB_PATH not set. Please specify the .bc module to analyze. ")
    }
    implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
      new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

      val javaJNITranslator = new SVFTranslator[PointsToSet](svfConnectorState.mapping, emptyPointsToSet)

    val listener = new SVFAnalysisListener() {

        override def getNativeFunctionArgument(methodName: String, argumentIndex: Int) : Array[Long] = {
            val result = new ListBuffer[Long]
            val allNativeMethods = project.allProjectClassFiles.
                flatMap(_.methods).filter(_.isNative).map(method=>
                    ("Java/" + method.classFile.thisType.fqn + "/" + method.name) -> method).toMap
            val method = allNativeMethods(methodName.replace("_", "/"))
            val formalparamater = formalParameters(declaredMethods(method))(argumentIndex+1)
            val pointsToSet = currentPointsTo("formalParameter", formalparamater,  PointsToSetLike.noFilter)

            pointsToSet.forNewestNElements(pointsToSet.numElements) { allocation =>
                javaJNITranslator.storePointsToSet(allocation.asInstanceOf[Long], pointsToSet)
                result.addOne(allocation.asInstanceOf[Long])
            }

           val formalParameterDependeesMap =
                if (pointsToAnalysisState.hasDependees("formalParameter"))
                    pointsToAnalysisState.dependeesOf("formalParameter")
                else
                    Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]
            svfConnectorState.connectorDependees = svfConnectorState.connectorDependees ++
                formalParameterDependeesMap.valuesIterator.map(_._1)

            result.toArray
        }

      override def nativeToJavaCallDetected(basePTS: Array[Long], className: String, methodName: String, methodSignature: String, argsPTSs: Array[Array[Long]]): Array[Long] = {
          var possibleMethods = Iterable.empty[Method]
          var objectTypeOptional: Option[ObjectType] = None

          for (ptElement <- basePTS) {
          val parameterPointsToSet = javaJNITranslator.getPTS(ptElement)
          parameterPointsToSet.forNewestNTypes(parameterPointsToSet.numElements) {
            tpe =>
              if (tpe.isObjectType) {
                val objectType = tpe.asObjectType
                  objectTypeOptional = Some(objectType)
                if (project.instanceMethods.contains(objectType)) {
                  possibleMethods ++= project.instanceMethods(objectType).
                      filter(_.name == methodName).map(_.method)
                }
              }
          }
        }
          val resultListBuffer: ListBuffer[Long] = new ListBuffer[Long]
        objectTypeOptional.foreach(objectType => {
        if (possibleMethods.isEmpty) {
          possibleMethods ++= project.allMethods.filter(
              method => method.name.equals(methodName) &&
                  method.signature.descriptor.toJVMDescriptor.equals(methodSignature)
          )
        }

        if (!project.instanceMethods.contains(objectType))
          return Array()

        possibleMethods = project.instanceMethods(objectType).filter(_.name == methodName).map(_.method)

        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
          new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)


        possibleMethods.foreach(method => {
          val declaredMethod = declaredMethods(method)
          val context = typeIterator.newContext(declaredMethod)

          //Call Graph
          svfConnectorState.connectorResults += PartialResult[DeclaredMethod, Callers](declaredMethod, Callers.key, {
            case InterimUBP(ub) if !ub.hasCallersWithUnknownContext =>
              Some(InterimEUBP(declaredMethod, ub.updatedWithUnknownContext()))

            case _: InterimEP[_, _] => None

            case _: EPK[_, _] =>
              Some(InterimEUBP(declaredMethod, OnlyCallersWithUnknownContext))

            case r => throw new IllegalStateException(s"unexpected previous result $r")
          })

          // function parameters
          val fps = formalParameters(declaredMethod)
          var paramIndex = 0
          val baseFP = getFormalParameter(0, fps, context)
          val baseFilter = (t: ReferenceType) => classHierarchy.isSubtypeOf(t, objectType.asReferenceType)
          for (pointsToElement <- basePTS) {
            val parameterPointsToSet = javaJNITranslator.getPTS(pointsToElement)
            pointsToAnalysisState.includeSharedPointsToSet(
              baseFP,
              parameterPointsToSet,
              baseFilter
            )
          }

          for (argumentPointsToSet <- argsPTSs) {
            val paramType = declaredMethod.descriptor.parameterType(paramIndex)
            val formalParameter = getFormalParameter(paramIndex + 1, fps, context)
            val filter =
                (t: ReferenceType) => classHierarchy.isSubtypeOf(t, paramType.asReferenceType)

            for (pointsToElement <- argumentPointsToSet) {
              val parameterPointsToSet = javaJNITranslator.getPTS(pointsToElement)
              pointsToAnalysisState.includeSharedPointsToSet(
                  formalParameter,
                parameterPointsToSet,
                filter
              )
            }
            paramIndex = paramIndex + 1
          }

          if (context.method.descriptor.returnType.isReferenceType) {
            val pointsToSet = currentPointsTo("callFunction", context, PointsToSetLike.noFilter)

            pointsToSet.forNewestNElements(pointsToSet.numElements) {
              element =>
              {
                resultListBuffer.addOne(element.asInstanceOf[Long])
                javaJNITranslator.storePointsToSet(element.asInstanceOf[Long], pointsToSet)
              }
            }

            val callFunctionDependeesMap = if (pointsToAnalysisState.hasDependees("callFunction"))
              pointsToAnalysisState.dependeesOf("callFunction")
            else
              Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]
            svfConnectorState.connectorDependees ++= callFunctionDependeesMap.valuesIterator.map(_._1)
          }
          svfConnectorState.connectorResults ++= createResults
        })
      })
          PointsToInteractionLogger.nativeToJavaCalls.put(methodName, resultListBuffer.toArray)
        resultListBuffer.toArray
      }

      override def jniNewObject(className: String, context: String): Long = {

        val referenceType = ObjectType(
            if (className.startsWith("L") && className.endsWith(";"))
                className.replace(";", "").substring(1)
            else
                className
        )

        val newPointsToSet = createPointsToSet(
          svfConnectorState.n,
          NoContext.asInstanceOf[ContextType],
          referenceType,
          false,
          false
        )
        svfConnectorState.n = svfConnectorState.n - 1
        var result: Long = -1
        newPointsToSet.forNewestNElements(1) {
          element =>
          {
            result = element.asInstanceOf[Long]
          }
        }
        javaJNITranslator.storePointsToSet(result, newPointsToSet)
          assert(result>0)
        result
      }

      override def getField(baseLongArray: Array[Long], className: String, fieldName: String): Array[Long] = {

        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
          new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

        var result: List[Long] = List.empty

        for (l <- baseLongArray) {
          val baseObjectPointsToSet = javaJNITranslator.getPTS(l)

          var tpe: Option[ObjectType] = None

          baseObjectPointsToSet.forNewestNTypes(baseObjectPointsToSet.numElements) { tmpTpe =>
            if (tpe.isEmpty) {
              tpe = Some(tmpTpe.asObjectType)
              //TODO Supertype
            }
          }
          if (tpe.isDefined) {
            val objectType = tpe.get
            val classFile = project.classFile(objectType)
            val possibleFields = {
              if (classFile.isDefined)
                classFile.get.fields.filter(_.name == fieldName).map(declaredFields(_)).toList
              else
                List.empty[DeclaredField]
            }

            possibleFields.foreach(field => {

              svfConnectorState.
                  indirectFieldAccesses.addFieldRead(svfConnectorState.calleeContext, 5, field, None)

              baseObjectPointsToSet.forNewestNElements(baseObjectPointsToSet.numElements) { as =>
              {
                val fieldEntity = (as, field)

                val fieldPointsToSet =
                  currentPointsTo("getField", fieldEntity, PointsToSetLike.noFilter)

                fieldPointsToSet.forNewestNElements(fieldPointsToSet.numElements) {
                  element =>
                  {
                    javaJNITranslator.storePointsToSet(element.asInstanceOf[Long], fieldPointsToSet)
                    result = element.asInstanceOf[Long] :: result
                  }
                }
                val readFieldDependeesMap = if (pointsToAnalysisState.hasDependees("getField"))
                  pointsToAnalysisState.dependeesOf("getField")
                else
                  Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

                svfConnectorState.connectorDependees = svfConnectorState.connectorDependees ++
                  readFieldDependeesMap.valuesIterator.map(_._1)
              }
              }
            })
          }

          svfConnectorState.connectorResults ++= createResults ++ svfConnectorState.indirectFieldAccesses.partialResults(svfConnectorState.calleeContext)
        }
          result.foreach(id => assert(id!=0))
          //result = 3 :: result
        result.toArray
      }

      override def setField(baseLongArray: Array[Long], className: String, fieldName: String, rhs: Array[Long]): Unit = {

        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
          new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

        var rhsPointsToSet = emptyPointsToSet
        for (l <- rhs) {
          rhsPointsToSet = rhsPointsToSet.included(javaJNITranslator.getPTS(l))
        }

        for (l <- baseLongArray) {
          val baseObjectPointsToSet = javaJNITranslator.getPTS(l)

          var tpe: Option[ObjectType] = None

          baseObjectPointsToSet.forNewestNTypes(baseObjectPointsToSet.numElements) { tmpTpe =>
            if (tpe.isEmpty) {
              tpe = Some(tmpTpe.asObjectType)
              //TODO Supertype
            }
          }
          if (tpe.isDefined) {
            val objectType = tpe.get
            val classFile = project.classFile(objectType)
            val possibleDeclardFields = {
              if (classFile.isDefined)
                classFile.get.fields.filter(_.name == fieldName).map(declaredFields(_)).toList
              else
                List.empty[DeclaredField]
            }
              possibleDeclardFields.foreach(declaredField => {

               svfConnectorState.
                   indirectFieldAccesses.addFieldWrite(svfConnectorState.calleeContext, 5, declaredField, None, None)

              baseObjectPointsToSet.forNewestNElements(baseObjectPointsToSet.numElements) { as =>
                val tpe = getTypeOf(as)
                if (tpe.isObjectType) {
                  Iterator((as, declaredField)).foreach(fieldEntity => {
                    pointsToAnalysisState.includeSharedPointsToSet(
                      fieldEntity,
                      rhsPointsToSet,
                      PointsToSetLike.noFilter
                    )
                  })
                }
              }
            })
          }
        }

        val setPropertyDependeesMap = if (pointsToAnalysisState.hasDependees("writeField"))
          pointsToAnalysisState.dependeesOf("writeField")
        else
          Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

        svfConnectorState.connectorDependees ++= setPropertyDependeesMap.valuesIterator.map(_._1)
        svfConnectorState.connectorResults ++= createResults ++ svfConnectorState.indirectFieldAccesses.partialResults(svfConnectorState.calleeContext)
      }

      override def getArrayElement(baseLongArray: Array[Long]): Array[Long] = {
        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
          new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

        var result: List[Long] = List.empty

        for (l <- baseLongArray) {
          val baseObjectPointsToSet = javaJNITranslator.getPTS(l)

          baseObjectPointsToSet.forNewestNElements(baseObjectPointsToSet.numElements) { as =>
          {
            val arrayEntity = ArrayEntity(as)
            val arrayPTS =
              currentPointsTo("getArrayElement", arrayEntity, PointsToSetLike.noFilter)

            arrayPTS.forNewestNElements(arrayPTS.numElements) {
              element =>
              {
                javaJNITranslator.storePointsToSet(element.asInstanceOf[Long], arrayPTS)
                result = element.asInstanceOf[Long] :: result
              }
            }
            val readFieldDependeesMap =
                if (pointsToAnalysisState.hasDependees("getArrayElement"))
                    pointsToAnalysisState.dependeesOf("getArrayElement")
                else
                    Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

            svfConnectorState.connectorDependees = svfConnectorState.connectorDependees ++
              readFieldDependeesMap.valuesIterator.map(_._1)
          }
          }

          svfConnectorState.connectorResults ++= createResults
        }
        result.toArray
      }

      override def setArrayElement(longs: Array[Long], longs1: Array[Long]): Unit = {
          //TODO
      }
    }



    val functions = svfConnectorState.svfModule.getFunctions

    val javaDeclaredMethod = svfConnectorState.calleeContext.method

    val outerResultListBuffer = new ListBuffer[Array[Long]]

    var basePTS = Set.empty[Long]
    for {
        formalParameter <- formalParameters(javaDeclaredMethod)
        if formalParameter != null
    } {
      val innerResultListBuffer: ListBuffer[Long] = new ListBuffer[Long]

      val pointsToSet = currentPointsTo("formalParameter", formalParameter,  PointsToSetLike.noFilter)
        val formalParameterDependeesMap =
            if (pointsToAnalysisState.hasDependees("formalParameter"))
                pointsToAnalysisState.dependeesOf("formalParameter")
            else
                Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

        svfConnectorState.connectorDependees = svfConnectorState.connectorDependees ++
            formalParameterDependeesMap.valuesIterator.map(_._1)

      pointsToSet.forNewestNElements(pointsToSet.numElements) { allocation =>
        javaJNITranslator.storePointsToSet(allocation.asInstanceOf[Long], pointsToSet)
        if (formalParameter.origin == -1) {
          basePTS += allocation.asInstanceOf[Long]
        } else {
          innerResultListBuffer.addOne(allocation.asInstanceOf[Long])
        }
      }

      val setPropertyDependeesMap =
        if (pointsToAnalysisState.hasDependees(formalParameter))
          pointsToAnalysisState.dependeesOf(formalParameter)
        else
          Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

      svfConnectorState.connectorDependees ++= setPropertyDependeesMap.valuesIterator.map(_._1)

      if (formalParameter.origin != -1)
        outerResultListBuffer.addOne(innerResultListBuffer.toArray)
    }

    val parameterPointsToSets = outerResultListBuffer.toArray

    val javaMethodFullName =
        ("Java/"+javaDeclaredMethod.declaringClassType.fqn.replace("_", "_1")+
            "/"+javaDeclaredMethod.name).replace("/", "_")

    var functionSelection = functions.filter(_.equals(javaMethodFullName))
    if (functionSelection.isEmpty) {
      // collect overloaded functions
      functionSelection = functions.filter(_.startsWith(javaMethodFullName+"__"))
    }
    if (functionSelection.isEmpty) {
      //throw new RuntimeException("native method not found :"+javaMethodFullName)
      //println(s"native method not found : $javaMethodFullName")
    }
    for (f <- functionSelection) {
      val resultPTS = svfConnectorState.svfModule.processFunction(f, basePTS.toArray, parameterPointsToSets, listener)
        PointsToInteractionLogger.javaToNativeCalls.put(f, resultPTS)
      if (resultPTS.isEmpty) {
        pointsToAnalysisState.includeSharedPointsToSet(svfConnectorState.calleeContext, emptyPointsToSet, PointsToSetLike.noFilter)
      } else {
        resultPTS.foreach(l => {
          val pointsToSet = javaJNITranslator.getPTS(l)
          pointsToAnalysisState.includeSharedPointsToSet(svfConnectorState.calleeContext, pointsToSet, PointsToSetLike.noFilter)
        })
      }
    }
    Results(createResults ++ svfConnectorState.connectorResults, InterimPartialResult(svfConnectorState.connectorDependees, svfConnectorContinuation))
  }

  def handleNewCaller(
                       calleeContext: ContextType,
                       callerContext: ContextType,
                       pc:            Int,
                       isDirect:      Boolean
                     ): ProperPropertyComputationResult = this.synchronized{
    implicit val svfConnectorState = SVFConnectorState(calleeContext, pc, project)
    svfjava.SVFJava.init()
    svfConnectorState.svfModule = SVFModule.createSVFModule(svfConnectorState.svfModuleName)
    if(calleeContext.method.name.contains("setOut")){
      println("set out--------->")
      println(calleeContext.method.name)
      runSVF(svfConnectorState)
    }

    else Results()
  }

  def svfConnectorContinuation(eps: SomeEPS)(implicit svfConnectorState: SVFConnectorState): ProperPropertyComputationResult = this.synchronized{
    svfConnectorState.connectorDependees = svfConnectorState.connectorDependees.filter(dependee => dependee.e != eps.e)
    eps match {
      case UBP(_: PointsToSet @unchecked) =>
        svfConnectorState.connectorDependees += eps
          svfjava.SVFJava.init()
          svfConnectorState.svfModule = SVFModule.createSVFModule(svfConnectorState.svfModuleName)
        runSVF(svfConnectorState)

      case _ => throw new Exception(s"message: ${eps.toString}")
    }
  }
}
