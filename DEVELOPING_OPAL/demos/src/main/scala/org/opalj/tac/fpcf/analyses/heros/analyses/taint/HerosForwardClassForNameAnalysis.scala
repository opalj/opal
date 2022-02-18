/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.heros.analyses.taint

import java.io.File
import java.util
import java.util.Collections

import scala.collection.JavaConverters._

import heros.FlowFunction
import heros.FlowFunctions
import heros.TwoElementSet
import heros.flowfunc.Identity
import heros.flowfunc.KillAll

import org.opalj.util.Milliseconds
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.br.ClassHierarchy
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.DeclaredMethod
import org.opalj.tac.fpcf.analyses.heros.cfg.OpalForwardICFG
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.GetField
import org.opalj.tac.GetStatic
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.ReturnValue
import org.opalj.tac.Var
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.fpcf.analyses.ifds.taint.ArrayElement
import org.opalj.tac.fpcf.analyses.ifds.taint.Fact
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.InstanceField
import org.opalj.tac.fpcf.analyses.ifds.taint.StaticField
import org.opalj.tac.fpcf.analyses.ifds.taint.Variable
import org.opalj.tac.ArrayLength
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.BinaryExpr
import org.opalj.tac.Compare
import org.opalj.tac.NewArray
import org.opalj.tac.PrefixExpr
import org.opalj.tac.PrimitiveTypecastExpr
import org.opalj.tac.fpcf.analyses.heros.analyses.HerosAnalysis
import org.opalj.tac.fpcf.analyses.heros.analyses.HerosAnalysisRunner
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintAnalysis
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V

/**
 * An implementation of the ForwardClassForNameAnalysis in the Heros framework.
 * For documentation, see ForwardClassForNameAnalysis.
 *
 * @author Mario Trageser
 */
class HerosForwardClassForNameAnalysis(p: SomeProject, icfg: OpalForwardICFG, initialMethods: Map[Method, util.Set[Fact]]) extends HerosTaintAnalysis(p, icfg) {

    override val initialSeeds: util.Map[Statement, util.Set[Fact]] = {
        var result: Map[Statement, util.Set[Fact]] = Map.empty
        for ((m, facts) ← initialMethods) {
            result += icfg.getStartPointsOf(m).iterator().next() → facts
        }
        result.asJava
    }

    val classHierarchy: ClassHierarchy = p.classHierarchy

    var flowFacts = Map.empty[Method, Set[FlowFact]]

    override def createFlowFunctionsFactory(): FlowFunctions[Statement, Fact, Method] = {

        new FlowFunctions[Statement, Fact, Method]() {

            override def getNormalFlowFunction(stmt: Statement, succ: Statement): FlowFunction[Fact] = {
                stmt.stmt.astID match {
                    case Assignment.ASTID ⇒
                        handleAssignment(stmt, stmt.stmt.asAssignment.expr)
                    case ArrayStore.ASTID ⇒
                        val store = stmt.stmt.asArrayStore
                        val definedBy = store.arrayRef.asVar.definedBy
                        val index = TaintAnalysis.getIntConstant(store.index, stmt.code)
                        (source: Fact) ⇒ {
                            if (isTainted(store.value, source)) {
                                if (index.isDefined) {
                                    (definedBy.iterator.map[Fact](ArrayElement(_, index.get)).toSet + source).asJava
                                } else {
                                    (definedBy.iterator.map[Fact](Variable).toSet + source).asJava
                                }
                            } else {
                                if (index.isDefined && definedBy.size == 1) {
                                    val idx = index.get
                                    val arrayDefinedBy = definedBy.head
                                    source match {
                                        case ArrayElement(`arrayDefinedBy`, `idx`) ⇒ Collections.emptySet()
                                        case _                                     ⇒ Collections.singleton(source)
                                    }
                                } else Collections.singleton(source)
                            }
                        }
                    case PutStatic.ASTID ⇒
                        val put = stmt.stmt.asPutStatic
                        (source: Fact) ⇒ {
                            if (isTainted(put.value, source))
                                TwoElementSet.twoElementSet(source, StaticField(put.declaringClass, put.name))
                            else Collections.singleton(source)
                        }
                    case PutField.ASTID ⇒
                        val put = stmt.stmt.asPutField
                        val definedBy = put.objRef.asVar.definedBy
                        (source: Fact) ⇒ {
                            if (isTainted(put.value, source)) {
                                (definedBy.iterator.map(InstanceField(_, put.declaringClass, put.name)).toSet[Fact] + source).asJava
                            } else {
                                Collections.singleton(source)
                            }
                        }
                    case _ ⇒ Identity.v()
                }
            }

            def handleAssignment(stmt: Statement, expr: Expr[V]): FlowFunction[Fact] =
                expr.astID match {
                    case Var.ASTID ⇒
                        (source: Fact) ⇒ {
                            source match {
                                case Variable(index) if expr.asVar.definedBy.contains(index) ⇒
                                    TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                case _ ⇒ Collections.singleton(source)
                            }
                        }
                    case ArrayLoad.ASTID ⇒
                        val load = expr.asArrayLoad
                        val arrayDefinedBy = load.arrayRef.asVar.definedBy
                        (source: Fact) ⇒ {
                            source match {
                                // The specific array element may be tainted
                                case ArrayElement(index, taintedIndex) ⇒
                                    val arrIndex = TaintAnalysis.getIntConstant(load.index, stmt.code)
                                    if (arrayDefinedBy.contains(index) &&
                                        (arrIndex.isEmpty || taintedIndex == arrIndex.get))
                                        TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                    else
                                        Collections.singleton(source)
                                // Or the whole array
                                case Variable(index) if arrayDefinedBy.contains(index) ⇒
                                    TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                case _ ⇒ Collections.singleton(source)
                            }
                        }
                    case GetStatic.ASTID ⇒
                        val get = expr.asGetStatic
                        (source: Fact) ⇒ {
                            if (source == StaticField(get.declaringClass, get.name))
                                TwoElementSet.twoElementSet(source, Variable(stmt.index))
                            else Collections.singleton(source)
                        }
                    case GetField.ASTID ⇒
                        val get = expr.asGetField
                        val objectDefinedBy = get.objRef.asVar.definedBy
                        (source: Fact) ⇒ {
                            source match {
                                // The specific field may be tainted
                                case InstanceField(index, _, taintedField) if taintedField == get.name && objectDefinedBy.contains(index) ⇒
                                    TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                // Or the whole object
                                case Variable(index) if objectDefinedBy.contains(index) ⇒
                                    TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                case _ ⇒ Collections.singleton(source)
                            }
                        }
                    case BinaryExpr.ASTID | PrefixExpr.ASTID | Compare.ASTID | PrimitiveTypecastExpr.ASTID |
                        NewArray.ASTID | ArrayLength.ASTID ⇒
                        (source: Fact) ⇒ {
                            val result = new util.HashSet[Fact]
                            (0 until expr.subExprCount)
                                .foreach(subExpression ⇒
                                    result.addAll(
                                        handleAssignment(stmt, expr.subExpr(subExpression))
                                            .computeTargets(source)
                                    ))
                            result
                        }
                    case _ ⇒ Identity.v()
                }

            override def getCallFlowFunction(stmt: Statement, callee: Method): FlowFunction[Fact] = {
                val callObject = asCall(stmt.stmt)
                val allParams = callObject.allParams
                if (relevantCallee(callee)) {
                    val allParamsWithIndices = allParams.zipWithIndex
                    source: Fact ⇒
                        (source match {
                            case Variable(index) ⇒ // Taint formal parameter if actual parameter is tainted
                                allParamsWithIndices.collect {
                                    case (param, paramIdx) if param.asVar.definedBy.contains(index) ⇒
                                        Variable(AbstractIFDSAnalysis.switchParamAndVariableIndex(paramIdx, callee.isStatic))
                                }.toSet[Fact]

                            case ArrayElement(index, taintedIndex) ⇒
                                // Taint element of formal parameter if element of actual parameter is tainted
                                allParamsWithIndices.collect {
                                    case (param, paramIdx) if param.asVar.definedBy.contains(index) ⇒
                                        ArrayElement(AbstractIFDSAnalysis.switchParamAndVariableIndex(paramIdx, callee.isStatic), taintedIndex)
                                }.toSet[Fact]

                            case InstanceField(index, declClass, taintedField) ⇒
                                // Taint field of formal parameter if field of actual parameter is tainted
                                // Only if the formal parameter is of a type that may have that field!
                                allParamsWithIndices.collect {
                                    case (param, paramIdx) if param.asVar.definedBy.contains(index) &&
                                        (AbstractIFDSAnalysis.switchParamAndVariableIndex(paramIdx, callee.isStatic) != -1 ||
                                            classHierarchy.isSubtypeOf(declClass, callee.classFile.thisType)) ⇒
                                        InstanceField(AbstractIFDSAnalysis.switchParamAndVariableIndex(paramIdx, callee.isStatic), declClass, taintedField)
                                }.toSet[Fact]
                            case sf: StaticField ⇒ Set(sf).asInstanceOf[Set[Fact]]
                            case _               ⇒ Set.empty[Fact]
                        }).asJava
                } else KillAll.v()
            }

            override def getReturnFlowFunction(stmt: Statement, callee: Method, exit: Statement, succ: Statement): FlowFunction[Fact] = {

                def isRefTypeParam(index: Int): Boolean =
                    if (index == -1) true
                    else {
                        val parameterOffset = if (callee.isStatic) 0 else 1
                        callee.descriptor.parameterType(
                            AbstractIFDSAnalysis.switchParamAndVariableIndex(index, callee.isStatic)
                                - parameterOffset
                        ).isReferenceType
                    }

                val callStatement = asCall(stmt.stmt)
                val allParams = callStatement.allParams

                source: Fact ⇒ {
                    val paramFacts = source match {

                        case Variable(index) if index < 0 && index > -100 && isRefTypeParam(index) ⇒
                            val params = allParams(
                                AbstractIFDSAnalysis.switchParamAndVariableIndex(index, callee.isStatic)
                            )
                            params.asVar.definedBy.iterator.map(Variable).toSet

                        case ArrayElement(index, taintedIndex) if index < 0 && index > -100 ⇒
                            // Taint element of actual parameter if element of formal parameter is tainted
                            val params =
                                asCall(stmt.stmt).allParams(AbstractIFDSAnalysis.switchParamAndVariableIndex(index, callee.isStatic))
                            params.asVar.definedBy.iterator.map(ArrayElement(_, taintedIndex)).asInstanceOf[Iterator[Fact]].toSet

                        case InstanceField(index, declClass, taintedField) if index < 0 && index > -10 ⇒
                            // Taint field of actual parameter if field of formal parameter is tainted
                            val params =
                                allParams(AbstractIFDSAnalysis.switchParamAndVariableIndex(index, callee.isStatic))
                            params.asVar.definedBy.iterator.map(InstanceField(_, declClass, taintedField)).asInstanceOf[Iterator[Fact]].toSet

                        case sf: StaticField ⇒ Set(sf)
                        case FlowFact(flow) if !flow.contains(stmt.method) ⇒
                            val flowFact = FlowFact(stmt.method +: flow)
                            if (initialMethods.contains(stmt.method))
                                flowFacts = flowFacts.updated(
                                    stmt.method,
                                    flowFacts.getOrElse(stmt.method, Set.empty[FlowFact]) + flowFact
                                )
                            Set(flowFact)
                        case _ ⇒
                            Set.empty
                    }

                    val returnFact =
                        if (exit.stmt.astID == ReturnValue.ASTID && stmt.stmt.astID == Assignment.ASTID) {
                            val returnValueDefinedBy = exit.stmt.asReturnValue.expr.asVar.definedBy
                            source match {
                                case Variable(index) if returnValueDefinedBy.contains(index) ⇒
                                    Some(Variable(stmt.index))
                                case ArrayElement(index, taintedIndex) if returnValueDefinedBy.contains(index) ⇒
                                    Some(ArrayElement(stmt.index, taintedIndex))
                                case InstanceField(index, declClass, taintedField) if returnValueDefinedBy.contains(index) ⇒
                                    Some(InstanceField(stmt.index, declClass, taintedField))
                                case _ ⇒ None
                            }
                        } else None

                    val flowFact =
                        if (isClassForName(callee) && source == Variable(-2)) {
                            val flowFact = FlowFact(Seq(stmt.method))
                            if (initialMethods.contains(stmt.method))
                                flowFacts = flowFacts.updated(
                                    stmt.method,
                                    flowFacts.getOrElse(stmt.method, Set.empty[FlowFact]) + flowFact
                                )
                            Some(flowFact)
                        } else None

                    val allFacts = paramFacts ++ returnFact.asInstanceOf[Option[Fact]] ++
                        flowFact.asInstanceOf[Option[Fact]]

                    allFacts.asJava
                }
            }

            override def getCallToReturnFlowFunction(stmt: Statement, succ: Statement): FlowFunction[Fact] =
                Identity.v()
        }
    }

    /**
     * Returns true if the expression contains a taint.
     */
    private def isTainted(expr: Expr[V], in: Fact): Boolean = {
        expr.isVar && (in match {
            case Variable(source)            ⇒ expr.asVar.definedBy.contains(source)
            case ArrayElement(source, _)     ⇒ expr.asVar.definedBy.contains(source)
            case InstanceField(source, _, _) ⇒ expr.asVar.definedBy.contains(source)
            case _                           ⇒ false
        })
    }

    private def relevantCallee(callee: Method): Boolean =
        callee.descriptor.parameterTypes.exists {
            case ObjectType.Object ⇒ true
            case ObjectType.String ⇒ true
            case _                 ⇒ false
        } && (!HerosAnalysis.canBeCalledFromOutside(callee) || isClassForName(callee))

    private def isClassForName(method: Method): Boolean =
        declaredMethods(method).declaringClassType == ObjectType.Class && method.name == "forName"
}

class HerosForwardClassForNameAnalysisRunner extends HerosAnalysisRunner[Fact, HerosForwardClassForNameAnalysis] {

    override protected def createAnalysis(p: SomeProject): HerosForwardClassForNameAnalysis = {
        val declaredMethods = p.get(DeclaredMethodsKey)
        val propertyStore = p.get(PropertyStoreKey)
        val initialMethods = scala.collection.mutable.Map.empty[Method, util.Set[Fact]]
        for {
            method ← declaredMethods.declaredMethods.filter(canBeCalledFromOutside(_, propertyStore))
                .map(_.definedMethod)
            index ← method.descriptor.parameterTypes.zipWithIndex.collect {
                case (pType, index) if pType == ObjectType.String ⇒
                    index
            }
        } {
            val fact = Variable(-2 - index)
            if (initialMethods.contains(method)) initialMethods(method).add(fact)
            else initialMethods += (method → new util.HashSet[Fact](Collections.singleton(fact)))
        }
        new HerosForwardClassForNameAnalysis(p, new OpalForwardICFG(p), initialMethods.toMap)
    }

    override protected def printResultsToConsole(analysis: HerosForwardClassForNameAnalysis, analysisTime: Milliseconds): Unit = {
        for {
            method ← analysis.flowFacts.keys
            fact ← analysis.flowFacts(method)
        } println(s"flow: "+fact.flow.map(_.toJava).mkString(", "))
        println(s"Time: $analysisTime")
    }

    private def canBeCalledFromOutside(method: DeclaredMethod, propertyStore: PropertyStore): Boolean = {
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