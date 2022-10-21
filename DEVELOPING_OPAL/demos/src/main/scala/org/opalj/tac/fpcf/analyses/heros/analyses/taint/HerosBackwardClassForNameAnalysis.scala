/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.heros.analyses.taint

import java.io.File
import java.util
import scala.jdk.CollectionConverters._
import heros.FlowFunction
import heros.FlowFunctions
import heros.flowfunc.Identity
import org.opalj.util.Milliseconds
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.analyses.heros.cfg.OpalBackwardICFG
import org.opalj.tac.fpcf.analyses.ifds.{JavaMethod, JavaStatement}
import org.opalj.tac.Assignment
import org.opalj.tac.BinaryExpr
import org.opalj.tac.Compare
import org.opalj.tac.PrefixExpr
import org.opalj.tac.ArrayLength
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Expr
import org.opalj.tac.GetField
import org.opalj.tac.NewArray
import org.opalj.tac.PrimitiveTypecastExpr
import org.opalj.tac.Var
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V
import org.opalj.tac.ArrayStore
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.ReturnValue
import org.opalj.tac.fpcf.analyses.heros.analyses.HerosAnalysis
import org.opalj.tac.fpcf.analyses.heros.analyses.HerosAnalysisRunner
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintProblem
import org.opalj.tac.fpcf.properties._

/**
 * An implementation of the BackwardClassForNameAnalysis in the Heros framework.
 * For a documentation of the data flow functions, see BackwardClassForNameAnalysis.
 *
 * @author Mario Trageser
 */
class HerosBackwardClassForNameAnalysis(p: SomeProject, icfg: OpalBackwardICFG) extends HerosTaintAnalysis(p, icfg) {

    override val initialSeeds: util.Map[JavaStatement, util.Set[TaintFact]] =
        p.allProjectClassFiles.filter(classFile =>
            classFile.thisType.fqn == "java/lang/Class")
            .flatMap(classFile => classFile.methods)
            .filter(_.name == "forName")
            .map(method => icfg.getExitStmt(method) -> Set[TaintFact](Variable(-2)).asJava).toMap.asJava

    var flowFacts = Map.empty[Method, Set[FlowFact]]

    override def followReturnsPastSeeds(): Boolean = true

    override def createFlowFunctionsFactory(): FlowFunctions[JavaStatement, TaintFact, Method] = {

        new FlowFunctions[JavaStatement, TaintFact, Method]() {

            override def getNormalFlowFunction(statement: JavaStatement, succ: JavaStatement): FlowFunction[TaintFact] = {
                val method = statement.method
                val stmt = statement.stmt
                source: TaintFact => {
                    var result = stmt.astID match {
                        case Assignment.ASTID =>
                            if (isTainted(statement.index, source))
                                createNewTaints(stmt.asAssignment.expr, statement) + source
                            else Set(source)
                        case ArrayStore.ASTID =>
                            val arrayStore = stmt.asArrayStore
                            val arrayIndex = TaintProblem.getIntConstant(arrayStore.index, statement.code)
                            val arrayDefinedBy = arrayStore.arrayRef.asVar.definedBy
                            var facts = (source match {
                                // In this case, we taint the whole array.
                                case Variable(index) if arrayDefinedBy.contains(index) =>
                                    createNewTaints(arrayStore.value, statement)
                                // In this case, we taint exactly the stored element.
                                case ArrayElement(index, taintedElement) if arrayDefinedBy.contains(index) &&
                                    (arrayIndex.isEmpty || arrayIndex.get == taintedElement) =>
                                    createNewTaints(arrayStore.value, statement)
                                case _ => Set.empty[TaintFact]
                            }) + source
                            if (arrayDefinedBy.size == 1 && arrayIndex.isDefined)
                                facts -= ArrayElement(arrayDefinedBy.head, arrayIndex.get)
                            facts
                        case PutField.ASTID =>
                            val putField = stmt.asPutField
                            val objectDefinedBy = putField.objRef.asVar.definedBy
                            if (source match {
                                case InstanceField(index, declaringClass, name) if objectDefinedBy.contains(index) &&
                                    putField.declaringClass == declaringClass && putField.name == name =>
                                    true
                                case _ => false
                            }) createNewTaints(putField.value, statement) + source
                            else Set(source)
                        case PutStatic.ASTID =>
                            val putStatic = stmt.asPutStatic
                            if (source match {
                                case StaticField(declaringClass, name) if putStatic.declaringClass == declaringClass && putStatic.name == name =>
                                    true
                                case _ => false
                            }) createNewTaints(putStatic.value, statement) + source
                            else Set(source)
                        case _ => Set(source)
                    }
                    if (icfg.isExitStmt(succ) && HerosAnalysis.canBeCalledFromOutside(method) && (source match {
                        case Variable(index) if index < 0            => true
                        case ArrayElement(index, _) if index < 0     => true
                        case InstanceField(index, _, _) if index < 0 => true
                        case _                                       => false
                    })) {
                        val fact = FlowFact(Seq(JavaMethod(method)))
                        result += fact
                        flowFacts = flowFacts.updated(method, flowFacts.getOrElse(method, Set.empty[FlowFact]) + fact)
                    }
                    result.asJava
                }

            }

            override def getCallFlowFunction(stmt: JavaStatement, callee: Method): FlowFunction[TaintFact] = {
                val callObject = asCall(stmt.stmt)
                val staticCall = callee.isStatic
                source: TaintFact => {
                    val returnValueFacts =
                        if (stmt.stmt.astID == Assignment.ASTID)
                            source match {
                                case Variable(index) if index == stmt.index =>
                                    createNewTaintsForCallee(callee)
                                case ArrayElement(index, taintedElement) if index == stmt.index =>
                                    toArrayElement(createNewTaintsForCallee(callee), taintedElement)
                                case InstanceField(index, declaringClass, name) if index == stmt.index =>
                                    toInstanceField(createNewTaintsForCallee(callee), declaringClass, name)
                                case _ => Set.empty[TaintFact]
                            }
                        else Set.empty
                    val thisOffset = if (callee.isStatic) 0 else 1
                    val parameterFacts = callObject.allParams.zipWithIndex
                        .filter(pair => (pair._2 == 0 && !staticCall) || callObject.descriptor.parameterTypes(pair._2 - thisOffset).isReferenceType)
                        .flatMap { pair =>
                            val param = pair._1.asVar
                            val paramIndex = pair._2
                            source match {
                                case Variable(index) if param.definedBy.contains(index) =>
                                    Some(Variable(JavaIFDSProblem.switchParamAndVariableIndex(paramIndex, staticCall)))
                                case ArrayElement(index, taintedElement) if param.definedBy.contains(index) =>
                                    Some(ArrayElement(
                                        JavaIFDSProblem.switchParamAndVariableIndex(paramIndex, staticCall), taintedElement
                                    ))
                                case InstanceField(index, declaringClass, name) if param.definedBy.contains(index) =>
                                    Some(InstanceField(
                                        JavaIFDSProblem.switchParamAndVariableIndex(paramIndex, staticCall),
                                        declaringClass, name
                                    ))
                                case staticField: StaticField => Some(staticField)
                                case _                        => None
                            }
                        }
                    (returnValueFacts ++ parameterFacts).asJava
                }
            }

            override def getReturnFlowFunction(statement: JavaStatement, callee: Method, exit: JavaStatement, succ: JavaStatement): FlowFunction[TaintFact] = {
                // If a method has no caller, returnFlow will be called with a null statement.
                if (statement == null) return Identity.v()
                val stmt = statement.stmt
                val callStatement = asCall(stmt)
                val staticCall = callee.isStatic
                val thisOffset = if (staticCall) 0 else 1
                val formalParameterIndices = (0 until callStatement.descriptor.parametersCount)
                    .map(index => JavaIFDSProblem.switchParamAndVariableIndex(index + thisOffset, staticCall))
                source: TaintFact =>
                    (source match {
                        case Variable(index) if formalParameterIndices.contains(index) =>
                            createNewTaints(
                                callStatement.allParams(JavaIFDSProblem.switchParamAndVariableIndex(index, staticCall)), statement
                            )
                        case ArrayElement(index, taintedElement) if formalParameterIndices.contains(index) =>
                            toArrayElement(createNewTaints(
                                callStatement.allParams(JavaIFDSProblem.switchParamAndVariableIndex(index, staticCall)),
                                statement
                            ), taintedElement)
                        case InstanceField(index, declaringClass, name) if formalParameterIndices.contains(index) =>
                            toInstanceField(createNewTaints(
                                callStatement.allParams(JavaIFDSProblem.switchParamAndVariableIndex(index, staticCall)),
                                statement
                            ), declaringClass, name)
                        case staticField: StaticField => Set[TaintFact](staticField)
                        case _                        => Set.empty[TaintFact]
                    }).asJava
            }

            override def getCallToReturnFlowFunction(stmt: JavaStatement, succ: JavaStatement): FlowFunction[TaintFact] =
                Identity.v()
        }
    }

    private def isTainted(index: Int, source: TaintFact, taintedElement: Option[Int] = None): Boolean = source match {
        case Variable(variableIndex) => variableIndex == index
        case ArrayElement(variableIndex, element) =>
            variableIndex == index && (taintedElement.isEmpty || taintedElement.get == element)
        case _ => false
    }

    private def createNewTaintsForCallee(callee: Method): Set[TaintFact] = {
        icfg.getStartPointsOf(callee).asScala.flatMap { statement =>
            val stmt = statement.stmt
            stmt.astID match {
                case ReturnValue.ASTID => createNewTaints(stmt.asReturnValue.expr, statement)
                case _                 => Set.empty[TaintFact]
            }
        }.toSet
    }

    private def createNewTaints(expression: Expr[V], statement: JavaStatement): Set[TaintFact] =
        expression.astID match {
            case Var.ASTID => expression.asVar.definedBy.map(Variable)
            case ArrayLoad.ASTID =>
                val arrayLoad = expression.asArrayLoad
                val arrayIndex = TaintProblem.getIntConstant(expression.asArrayLoad.index, statement.code)
                val arrayDefinedBy = arrayLoad.arrayRef.asVar.definedBy
                if (arrayIndex.isDefined) arrayDefinedBy.map(ArrayElement(_, arrayIndex.get))
                else arrayDefinedBy.map(Variable)
            case BinaryExpr.ASTID | PrefixExpr.ASTID | Compare.ASTID |
                PrimitiveTypecastExpr.ASTID | NewArray.ASTID | ArrayLength.ASTID =>
                (0 until expression.subExprCount).foldLeft(Set.empty[TaintFact])((acc, subExpr) =>
                    acc ++ createNewTaints(expression.subExpr(subExpr), statement))
            case GetField.ASTID =>
                val getField = expression.asGetField
                getField.objRef.asVar.definedBy
                    .map(InstanceField(_, getField.declaringClass, getField.name))
            /*case GetStatic.ASTID =>
                val getStatic = expression.asGetStatic
                Set(StaticField(getStatic.declaringClass, getStatic.name))*/
            case _ => Set.empty
        }

    private def toArrayElement(facts: Set[TaintFact], taintedElement: Int): Set[TaintFact] =
        facts.map {
            case Variable(variableIndex)            => ArrayElement(variableIndex, taintedElement)
            case ArrayElement(variableIndex, _)     => ArrayElement(variableIndex, taintedElement)
            case InstanceField(variableIndex, _, _) => ArrayElement(variableIndex, taintedElement)
        }

    private def toInstanceField(facts: Set[TaintFact], declaringClass: ObjectType, name: String): Set[TaintFact] =
        facts.map {
            case Variable(variableIndex)        => InstanceField(variableIndex, declaringClass, name)
            case ArrayElement(variableIndex, _) => InstanceField(variableIndex, declaringClass, name)
            case InstanceField(variableIndex, _, _) =>
                InstanceField(variableIndex, declaringClass, name)
        }

}

class HerosBackwardClassForNameAnalysisRunner extends HerosAnalysisRunner[TaintFact, HerosBackwardClassForNameAnalysis] {

    override protected def createAnalysis(p: SomeProject): HerosBackwardClassForNameAnalysis =
        new HerosBackwardClassForNameAnalysis(p, new OpalBackwardICFG(p))

    override protected def printResultsToConsole(analysis: HerosBackwardClassForNameAnalysis, analysisTime: Milliseconds): Unit = {
        for {
            method <- analysis.flowFacts.keys
            fact <- analysis.flowFacts(method)
        } println(s"flow: "+fact.flow.map(_.signature).mkString(", "))
        println(s"Time: $analysisTime")
    }
}

object HerosBackwardClassForNameAnalysisRunner {
    def main(args: Array[String]): Unit = {
        val fileIndex = args.indexOf("-f")
        new HerosBackwardClassForNameAnalysisRunner().run(
            if (fileIndex >= 0) Some(new File(args(fileIndex + 1))) else None
        )
    }
}