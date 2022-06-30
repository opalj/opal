/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.heros.analyses.taint

import heros.{FlowFunction, FlowFunctions, TwoElementSet}
import heros.flowfunc.{Identity, KillAll}
import org.opalj.br.{ClassHierarchy, DeclaredMethod, Method, ObjectType}
import org.opalj.br.analyses.{DeclaredMethodsKey, SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{FinalEP, PropertyStore}
import org.opalj.tac._
import org.opalj.tac.fpcf.analyses.heros.analyses.{HerosAnalysis, HerosAnalysisRunner}
import org.opalj.tac.fpcf.analyses.heros.cfg.OpalForwardICFG
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V
import org.opalj.tac.fpcf.analyses.ifds.taint._
import org.opalj.tac.fpcf.analyses.ifds.{JavaIFDSProblem, JavaMethod, JavaStatement}
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.util.Milliseconds

import java.io.File
import java.util
import java.util.Collections
import scala.collection.JavaConverters._

/**
 * An implementation of the ForwardClassForNameAnalysis in the Heros framework.
 * For documentation, see ForwardClassForNameAnalysis.
 *
 * @author Mario Trageser
 */
class HerosForwardClassForNameAnalysis(
        p:              SomeProject,
        icfg:           OpalForwardICFG,
        initialMethods: Map[Method, util.Set[TaintFact]]
) extends HerosTaintAnalysis(p, icfg) {

    override val initialSeeds: util.Map[JavaStatement, util.Set[TaintFact]] = {
        var result: Map[JavaStatement, util.Set[TaintFact]] = Map.empty
        for ((m, facts) <- initialMethods) {
            result += icfg.getStartPointsOf(m).iterator().next() -> facts
        }
        result.asJava
    }

    val classHierarchy: ClassHierarchy = p.classHierarchy

    var flowFacts = Map.empty[Method, Set[FlowFact]]

    override def createFlowFunctionsFactory(): FlowFunctions[JavaStatement, TaintFact, Method] = {

        new FlowFunctions[JavaStatement, TaintFact, Method]() {

            override def getNormalFlowFunction(stmt: JavaStatement, succ: JavaStatement): FlowFunction[TaintFact] = {
                stmt.stmt.astID match {
                    case Assignment.ASTID =>
                        handleAssignment(stmt, stmt.stmt.asAssignment.expr)
                    case ArrayStore.ASTID =>
                        val store = stmt.stmt.asArrayStore
                        val definedBy = store.arrayRef.asVar.definedBy
                        val index = TaintProblem.getIntConstant(store.index, stmt.code)
                        (source: TaintFact) => {
                            if (isTainted(store.value, source)) {
                                if (index.isDefined) {
                                    (definedBy.iterator.map[TaintFact](ArrayElement(_, index.get)).toSet + source).asJava
                                } else {
                                    (definedBy.iterator.map[TaintFact](Variable).toSet + source).asJava
                                }
                            } else {
                                if (index.isDefined && definedBy.size == 1) {
                                    val idx = index.get
                                    val arrayDefinedBy = definedBy.head
                                    source match {
                                        case ArrayElement(`arrayDefinedBy`, `idx`) => Collections.emptySet()
                                        case _                                     => Collections.singleton(source)
                                    }
                                } else Collections.singleton(source)
                            }
                        }
                    case PutStatic.ASTID =>
                        val put = stmt.stmt.asPutStatic
                        (source: TaintFact) => {
                            if (isTainted(put.value, source))
                                TwoElementSet.twoElementSet(source, StaticField(put.declaringClass, put.name))
                            else Collections.singleton(source)
                        }
                    case PutField.ASTID =>
                        val put = stmt.stmt.asPutField
                        val definedBy = put.objRef.asVar.definedBy
                        (source: TaintFact) => {
                            if (isTainted(put.value, source)) {
                                (definedBy.iterator
                                    .map(InstanceField(_, put.declaringClass, put.name))
                                    .toSet[TaintFact] + source).asJava
                            } else {
                                Collections.singleton(source)
                            }
                        }
                    case _ => Identity.v()
                }
            }

            def handleAssignment(stmt: JavaStatement, expr: Expr[V]): FlowFunction[TaintFact] =
                expr.astID match {
                    case Var.ASTID =>
                        (source: TaintFact) => {
                            source match {
                                case Variable(index) if expr.asVar.definedBy.contains(index) =>
                                    TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                case _ => Collections.singleton(source)
                            }
                        }
                    case ArrayLoad.ASTID =>
                        val load = expr.asArrayLoad
                        val arrayDefinedBy = load.arrayRef.asVar.definedBy
                        (source: TaintFact) => {
                            source match {
                                // The specific array element may be tainted
                                case ArrayElement(index, taintedIndex) =>
                                    val arrIndex = TaintProblem.getIntConstant(load.index, stmt.code)
                                    if (arrayDefinedBy.contains(index) &&
                                        (arrIndex.isEmpty || taintedIndex == arrIndex.get))
                                        TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                    else
                                        Collections.singleton(source)
                                // Or the whole array
                                case Variable(index) if arrayDefinedBy.contains(index) =>
                                    TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                case _ => Collections.singleton(source)
                            }
                        }
                    case GetStatic.ASTID =>
                        val get = expr.asGetStatic
                        (source: TaintFact) => {
                            if (source == StaticField(get.declaringClass, get.name))
                                TwoElementSet.twoElementSet(source, Variable(stmt.index))
                            else Collections.singleton(source)
                        }
                    case GetField.ASTID =>
                        val get = expr.asGetField
                        val objectDefinedBy = get.objRef.asVar.definedBy
                        (source: TaintFact) => {
                            source match {
                                // The specific field may be tainted
                                case InstanceField(index, _, taintedField) if taintedField == get.name && objectDefinedBy.contains(index) =>
                                    TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                // Or the whole object
                                case Variable(index) if objectDefinedBy.contains(index) =>
                                    TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                case _ => Collections.singleton(source)
                            }
                        }
                    case BinaryExpr.ASTID | PrefixExpr.ASTID | Compare.ASTID | PrimitiveTypecastExpr.ASTID |
                        NewArray.ASTID | ArrayLength.ASTID =>
                        (source: TaintFact) => {
                            val result = new util.HashSet[TaintFact]
                            (0 until expr.subExprCount)
                                .foreach(
                                    subExpression =>
                                        result.addAll(
                                            handleAssignment(stmt, expr.subExpr(subExpression))
                                                .computeTargets(source)
                                        )
                                )
                            result
                        }
                    case _ => Identity.v()
                }

            override def getCallFlowFunction(stmt: JavaStatement, callee: Method): FlowFunction[TaintFact] = {
                val callObject = asCall(stmt.stmt)
                val allParams = callObject.allParams
                if (relevantCallee(callee)) {
                    val allParamsWithIndices = allParams.zipWithIndex
                    source: TaintFact =>
                        (source match {
                            case Variable(index) => // Taint formal parameter if actual parameter is tainted
                                allParamsWithIndices
                                    .collect {
                                        case (param, paramIdx) if param.asVar.definedBy.contains(index) =>
                                            Variable(
                                                JavaIFDSProblem.switchParamAndVariableIndex(paramIdx, callee.isStatic)
                                            )
                                    }
                                    .toSet[TaintFact]

                            case ArrayElement(index, taintedIndex) =>
                                // Taint element of formal parameter if element of actual parameter is tainted
                                allParamsWithIndices
                                    .collect {
                                        case (param, paramIdx) if param.asVar.definedBy.contains(index) =>
                                            ArrayElement(
                                                JavaIFDSProblem.switchParamAndVariableIndex(paramIdx, callee.isStatic),
                                                taintedIndex
                                            )
                                    }
                                    .toSet[TaintFact]

                            case InstanceField(index, declClass, taintedField) =>
                                // Taint field of formal parameter if field of actual parameter is tainted
                                // Only if the formal parameter is of a type that may have that field!
                                allParamsWithIndices
                                    .collect {
                                        case (param, paramIdx) if param.asVar.definedBy.contains(index) &&
                                            (JavaIFDSProblem.switchParamAndVariableIndex(
                                                paramIdx,
                                                callee.isStatic
                                            ) != -1 ||
                                                classHierarchy.isSubtypeOf(declClass, callee.classFile.thisType)) =>
                                            InstanceField(
                                                JavaIFDSProblem.switchParamAndVariableIndex(paramIdx, callee.isStatic),
                                                declClass,
                                                taintedField
                                            )
                                    }
                                    .toSet[TaintFact]
                            case sf: StaticField => Set(sf).asInstanceOf[Set[TaintFact]]
                            case _               => Set.empty[TaintFact]
                        }).asJava
                } else KillAll.v()
            }

            override def getReturnFlowFunction(
                stmt:   JavaStatement,
                callee: Method,
                exit:   JavaStatement,
                succ:   JavaStatement
            ): FlowFunction[TaintFact] = {

                def isRefTypeParam(index: Int): Boolean =
                    if (index == -1) true
                    else {
                        val parameterOffset = if (callee.isStatic) 0 else 1
                        callee.descriptor
                            .parameterType(
                                JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic)
                                    - parameterOffset
                            )
                            .isReferenceType
                    }

                val callStatement = asCall(stmt.stmt)
                val allParams = callStatement.allParams

                source: TaintFact => {
                    val paramFacts = source match {

                        case Variable(index) if index < 0 && index > -100 && isRefTypeParam(index) =>
                            val params = allParams(
                                JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic)
                            )
                            params.asVar.definedBy.iterator.map(Variable).toSet

                        case ArrayElement(index, taintedIndex) if index < 0 && index > -100 =>
                            // Taint element of actual parameter if element of formal parameter is tainted
                            val params =
                                asCall(stmt.stmt).allParams(
                                    JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic)
                                )
                            params.asVar.definedBy.iterator
                                .map(ArrayElement(_, taintedIndex))
                                .asInstanceOf[Iterator[TaintFact]]
                                .toSet

                        case InstanceField(index, declClass, taintedField) if index < 0 && index > -10 =>
                            // Taint field of actual parameter if field of formal parameter is tainted
                            val params =
                                allParams(JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic))
                            params.asVar.definedBy.iterator
                                .map(InstanceField(_, declClass, taintedField))
                                .asInstanceOf[Iterator[TaintFact]]
                                .toSet

                        case sf: StaticField => Set(sf)
                        case FlowFact(flow) if !flow.contains(stmt.method) =>
                            val flowFact = FlowFact(JavaMethod(stmt.method) +: flow)
                            if (initialMethods.contains(stmt.method))
                                flowFacts = flowFacts.updated(
                                    stmt.method,
                                    flowFacts.getOrElse(stmt.method, Set.empty[FlowFact]) + flowFact
                                )
                            Set(flowFact)
                        case _ =>
                            Set.empty
                    }

                    val returnFact =
                        if (exit.stmt.astID == ReturnValue.ASTID && stmt.stmt.astID == Assignment.ASTID) {
                            val returnValueDefinedBy = exit.stmt.asReturnValue.expr.asVar.definedBy
                            source match {
                                case Variable(index) if returnValueDefinedBy.contains(index) =>
                                    Some(Variable(stmt.index))
                                case ArrayElement(index, taintedIndex) if returnValueDefinedBy.contains(index) =>
                                    Some(ArrayElement(stmt.index, taintedIndex))
                                case InstanceField(index, declClass, taintedField) if returnValueDefinedBy.contains(index) =>
                                    Some(InstanceField(stmt.index, declClass, taintedField))
                                case _ => None
                            }
                        } else None

                    val flowFact =
                        if (isClassForName(callee) && source == Variable(-2)) {
                            val flowFact = FlowFact(Seq(JavaMethod(stmt.method)))
                            if (initialMethods.contains(stmt.method))
                                flowFacts = flowFacts.updated(
                                    stmt.method,
                                    flowFacts.getOrElse(stmt.method, Set.empty[FlowFact]) + flowFact
                                )
                            Some(flowFact)
                        } else None

                    val allFacts = paramFacts ++ returnFact.asInstanceOf[Option[TaintFact]] ++
                        flowFact.asInstanceOf[Option[TaintFact]]

                    allFacts.asJava
                }
            }

            override def getCallToReturnFlowFunction(
                stmt: JavaStatement,
                succ: JavaStatement
            ): FlowFunction[TaintFact] =
                Identity.v()
        }
    }

    /**
     * Returns true if the expression contains a taint.
     */
    private def isTainted(expr: Expr[V], in: TaintFact): Boolean = {
        expr.isVar && (in match {
            case Variable(source)            => expr.asVar.definedBy.contains(source)
            case ArrayElement(source, _)     => expr.asVar.definedBy.contains(source)
            case InstanceField(source, _, _) => expr.asVar.definedBy.contains(source)
            case _                           => false
        })
    }

    private def relevantCallee(callee: Method): Boolean =
        callee.descriptor.parameterTypes.exists {
            case ObjectType.Object => true
            case ObjectType.String => true
            case _                 => false
        } && (!HerosAnalysis.canBeCalledFromOutside(callee) || isClassForName(callee))

    private def isClassForName(method: Method): Boolean =
        declaredMethods(method).declaringClassType == ObjectType.Class && method.name == "forName"
}

class HerosForwardClassForNameAnalysisRunner
    extends HerosAnalysisRunner[TaintFact, HerosForwardClassForNameAnalysis] {

    override protected def createAnalysis(p: SomeProject): HerosForwardClassForNameAnalysis = {
        val declaredMethods = p.get(DeclaredMethodsKey)
        val propertyStore = p.get(PropertyStoreKey)
        val initialMethods = scala.collection.mutable.Map.empty[Method, util.Set[TaintFact]]
        for {
            method <- declaredMethods.declaredMethods
                .filter(canBeCalledFromOutside(_, propertyStore))
                .map(_.definedMethod)
            index <- method.descriptor.parameterTypes.zipWithIndex.collect {
                case (pType, index) if pType == ObjectType.String =>
                    index
            }
        } {
            val fact = Variable(-2 - index)
            if (initialMethods.contains(method)) initialMethods(method).add(fact)
            else initialMethods += (method -> new util.HashSet[TaintFact](Collections.singleton(fact)))
        }
        new HerosForwardClassForNameAnalysis(p, new OpalForwardICFG(p), initialMethods.toMap)
    }

    override protected def printResultsToConsole(
        analysis:     HerosForwardClassForNameAnalysis,
        analysisTime: Milliseconds
    ): Unit = {
        for {
            method <- analysis.flowFacts.keys
            fact <- analysis.flowFacts(method)
        } println(s"flow: "+fact.flow.map(_.signature).mkString(", "))
        println(s"Time: $analysisTime")
    }

    private def canBeCalledFromOutside(
        method:        DeclaredMethod,
        propertyStore: PropertyStore
    ): Boolean = {
        val FinalEP(_, callers) = propertyStore(method, Callers.key)
        callers.hasCallersWithUnknownContext
    }
}

object HerosForwardClassForNameAnalysisRunner {

    def main(args: Array[String]): Unit = {
        val fileIndex = args.indexOf("-f")
        new HerosForwardClassForNameAnalysisRunner().run(
            if (fileIndex >= 0) Some(new File(args(fileIndex + 1))) else None
        )
    }
}
