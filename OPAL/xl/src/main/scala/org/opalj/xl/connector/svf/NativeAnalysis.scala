/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package connector
package svf

import scala.collection.mutable.ListBuffer

import svfjava.SVFAnalysisListener
import svfjava.SVFModule

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
import org.opalj.br.Fields
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisBase
import org.opalj.tac.fpcf.analyses.APIBasedAnalysis
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisState
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.OnlyCallersWithUnknownContext

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
            var svfModuleName:      String                               = ???,
            var connectorDependees: Set[EOptionP[Entity, Property]]      = Set.empty,
            var connectorResults:   Set[ProperPropertyComputationResult] = Set.empty[ProperPropertyComputationResult],
            var javaJNIMapping:     Map[Long, PointsToSet]               = Map.empty,
            var oldEPS:             SomeEPS                              = null
    ) extends BaseAnalysisState with TypeIteratorState

    def runSVF(implicit svfConnectorState: SVFConnectorState): ProperPropertyComputationResult = {

        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
            new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

        println("run SVF")
        val functions = svfConnectorState.svfModule.getFunctions

        val javaDeclaredMethod = svfConnectorState.calleeContext.method

        val fps = formalParameters(javaDeclaredMethod)

        val outerResultListBuffer = new ListBuffer[Array[Long]]

        var basePTS = Set.empty[Long]
        for {
            fp <- fps
            if fp != null
        } {
            val innerResultListBuffer: ListBuffer[Long] = new ListBuffer[Long]

            val pointsToSet = currentPointsTo(fp, fp, PointsToSetLike.noFilter)

            pointsToSet.forNewestNElements(pointsToSet.numElements) { allocation =>
                svfConnectorState.javaJNIMapping += allocation.asInstanceOf[Long] -> pointsToSet
                if (fp.origin == -1) {
                    basePTS += allocation.asInstanceOf[Long]
                } else {
                    innerResultListBuffer.addOne(allocation.asInstanceOf[Long])
                }
            }

            val setPropertyDependeesMap =
                if (pointsToAnalysisState.hasDependees(fp))
                    pointsToAnalysisState.dependeesOf(fp)
                else
                    Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]
            svfConnectorState.connectorDependees ++= setPropertyDependeesMap.valuesIterator.map(_._1)
            if(fp.origin != -1) {
              outerResultListBuffer.addOne(innerResultListBuffer.toArray)
            }
        }

        val parameterPointsToSets = outerResultListBuffer.toArray

        println(s"result:::: $parameterPointsToSets")

        val javaFunctionFullName = ("Java/"+javaDeclaredMethod.declaringClassType.fqn+"/"+javaDeclaredMethod.name).replace("/", "_")

        val functionSelection = functions.filter(_.contains(javaFunctionFullName))
        assert(functionSelection.nonEmpty)
        for (f <- functionSelection) {
            val resultPTS = svfConnectorState.svfModule.processFunction(f, basePTS.toArray, parameterPointsToSets)

            resultPTS.foreach(l => {
                if (svfConnectorState.javaJNIMapping.contains(l)) {
                    val pointsToSet = svfConnectorState.javaJNIMapping(l)
                    pointsToAnalysisState.includeSharedPointsToSet(svfConnectorState.calleeContext, pointsToSet, PointsToSetLike.noFilter)
                }
            })
        }

        Results(createResults ++ svfConnectorState.connectorResults, InterimPartialResult(svfConnectorState.connectorDependees, svfConnectorContinuation))
    }

    def handleNewCaller(
        calleeContext: ContextType,
        callerContext: ContextType,
        pc:            Int,
        isDirect:      Boolean
    ): ProperPropertyComputationResult = {

        implicit val svfConnectorState = SVFConnectorState(calleeContext, pc, project)

        svfjava.SVFJava.init()

        svfConnectorState.svfModule = SVFModule.createSVFModule(svfConnectorState.svfModuleName, new SVFAnalysisListener() {
            override def nativeToJavaCallDetected(basePTS: Array[Long], className: String, methodName: String, methodSignature: String, argsPTSs: Array[Array[Long]]): Array[Long] = {
                val objectType = ObjectType(className.replace(";", "").substring(1))
                var possibleMethods = Iterable.empty[Method]
                if (!project.instanceMethods.contains(objectType))
                    return Array[Long]()
                possibleMethods = project.instanceMethods(objectType).filter(_.name == methodName).map(_.method)

                implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
                    new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

                val resultListBuffer: ListBuffer[Long] = new ListBuffer[Long]
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

                    for (argPTS <- argsPTSs) {
                        val paramType = declaredMethod.descriptor.parameterType(paramIndex)
                        val fp = getFormalParameter(paramIndex + 1, fps, context)
                        val filter = (t: ReferenceType) => classHierarchy.isSubtypeOf(t, paramType.asReferenceType)
                        for (ptElement <- argPTS) {
                            val parameterPointsToSet = svfConnectorState.javaJNIMapping(ptElement)
                            pointsToAnalysisState.includeSharedPointsToSet(
                                fp,
                                parameterPointsToSet,
                                filter
                            )
                        }
                        paramIndex = paramIndex + 1
                    }

                    if (context.method.descriptor.returnType.isReferenceType) {
                        val pointsToSet = currentPointsTo("callFunction", context, PointsToSetLike.noFilter)

                        pointsToSet.forNewestNElements(pointsToSet.numElements) {
                            element => resultListBuffer.addOne(element.asInstanceOf[Long])
                        }
                        val callFunctionDependeesMap = if (pointsToAnalysisState.hasDependees("callFunction"))
                            pointsToAnalysisState.dependeesOf("callFunction")
                        else
                            Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]
                        svfConnectorState.connectorDependees ++= callFunctionDependeesMap.valuesIterator.map(_._1)
                    }
                    svfConnectorState.connectorResults ++= createResults
                })
                resultListBuffer.toArray
            }

            override def jniNewObject(className: String, context: String): Long = {

                val referenceType = ObjectType(className.replace(";", "").substring(1))

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
                svfConnectorState.javaJNIMapping += result -> newPointsToSet
                result
            }

            override def getField(baseLongArray: Array[Long], className: String, fieldName: String): Array[Long] = {

                implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
                    new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

                var result: List[Long] = List.empty

                for (l <- baseLongArray) {
                    val baseObjectPointsToSet = svfConnectorState.javaJNIMapping(l)

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
                                classFile.get.fields.find(_.name == fieldName).toList
                            else
                                List.empty[Fields]
                        }

                        possibleFields.foreach(field => {
                            val fieldEntities = Iterator((baseObjectPointsToSet, field))
                            for (fieldEntity <- fieldEntities) {
                                val fieldPointsToSet =
                                    currentPointsTo("getField", fieldEntity, PointsToSetLike.noFilter)
                                fieldPointsToSet.forNewestNElements(fieldPointsToSet.numElements) {
                                    element => result = element.asInstanceOf[Long] :: result
                                }
                            }
                        })
                    }

                    val readFieldDependeesMap = if (pointsToAnalysisState.hasDependees("getField"))
                        pointsToAnalysisState.dependeesOf("getField")
                    else
                        Map.empty[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)]

                    svfConnectorState.connectorDependees = svfConnectorState.connectorDependees ++
                        readFieldDependeesMap.valuesIterator.map(_._1)
                    svfConnectorState.connectorResults ++= createResults
                }
                result.toArray
            }

            override def setField(baseLongArray: Array[Long], className: String, fieldName: String, rhs: Array[Long]): Unit = {

                implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
                    new PointsToAnalysisState(NoContext.asInstanceOf[ContextType], null)

                var rhsPointsToSet = emptyPointsToSet
                for (l <- rhs) {
                    rhsPointsToSet = rhsPointsToSet.included(svfConnectorState.javaJNIMapping(l))
                }

                for (l <- baseLongArray) {
                    val baseObjectPointsToSet = svfConnectorState.javaJNIMapping(l)

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
                                classFile.get.fields.find(_.name == fieldName).toList
                            else
                                List.empty[Fields]
                        }
                        possibleFields.foreach(field => {
                            baseObjectPointsToSet.forNewestNElements(baseObjectPointsToSet.numElements) { as =>
                                val tpe = getTypeOf(as)
                                if (tpe.isObjectType) {
                                    Iterator((as, field)).foreach(fieldEntity => {
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
                svfConnectorState.connectorResults ++= createResults
            }
        })
        runSVF(svfConnectorState)
    }

    def svfConnectorContinuation(eps: SomeEPS)(implicit svfConnectorState: SVFConnectorState): ProperPropertyComputationResult = {
        svfConnectorState.connectorDependees = svfConnectorState.connectorDependees.filter(dependee => dependee.e != eps.e)
        eps match {
            case UBP(_: PointsToSet @unchecked) =>
                svfConnectorState.connectorDependees += eps
                svfConnectorState.oldEPS = eps
                runSVF(svfConnectorState)

            case ep => Results()
        }
    }
}
