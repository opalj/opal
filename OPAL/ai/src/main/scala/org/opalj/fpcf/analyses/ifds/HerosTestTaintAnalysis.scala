/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package ifds

import scala.collection.JavaConverters._
import java.util
import java.util.Collections

import heros.FlowFunctions
import heros.FlowFunction
import heros.TwoElementSet
import heros.flowfunc.Identity
import heros.flowfunc.KillAll
import heros.flowfunc.Gen
import heros.flowfunc.Union
import heros.flowfunc.Kill
import heros.solver.IFDSSolver
import heros.template.DefaultIFDSTabulationProblem

import org.opalj.collection.immutable.RefArray
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ClassHierarchy
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.tac.ExprStmt
import org.opalj.tac.Call
import org.opalj.tac.Stmt
import org.opalj.tac.Assignment
import org.opalj.tac.ReturnValue
import org.opalj.tac.TACode
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.DUVar
import org.opalj.value.ValueInformation
//import org.opalj.tac.PutField
import org.opalj.tac.ArrayStore
import org.opalj.tac.Expr
import org.opalj.tac.ArrayLoad
//import org.opalj.tac.GetField
import org.opalj.tac.Var

import scala.collection.immutable.ListSet
import org.opalj.util.PerformanceEvaluation.time

case object NullFact extends Fact

class HerosTestTaintAnalysis(p: SomeProject, icfg: OpalICFG)
    extends DefaultIFDSTabulationProblem[Statement, Fact, Method, OpalICFG](icfg) {

    override def numThreads(): Int = 4

    val classHierarchy: ClassHierarchy = p.classHierarchy

    override def createFlowFunctionsFactory(): FlowFunctions[Statement, Fact, Method] = {

        new FlowFunctions[Statement, Fact, Method]() {
            def getNormalFlowFunction(stmt: Statement, succ: Statement): FlowFunction[Fact] = {
                stmt.stmt.astID match {
                    case Assignment.ASTID ⇒
                        handleAssignment(stmt, stmt.stmt.asAssignment.expr)
                    case ArrayStore.ASTID ⇒
                        val store = stmt.stmt.asArrayStore
                        val definedBy = store.arrayRef.asVar.definedBy
                        val index = getConstValue(store.index, stmt.code)
                        (source: Fact) ⇒ {
                            if (isTainted(store.value, source)) {
                                if (index.isDefined) {
                                    (definedBy.iterator.map[Fact](ArrayElement(_, index.get)).toSet + source).asJava
                                } else {
                                    (definedBy.iterator.map[Fact](Variable).toSet + source).asJava
                                }
                            } else {
                                if (index.isDefined) {
                                    val idx = index.get
                                    source match {
                                        case ArrayElement(stmt.index, `idx`) ⇒ Collections.emptySet()
                                        case _                               ⇒ Collections.singleton(source)
                                    }
                                } else Collections.singleton((source))
                            }
                        }
                    /*case PutStatic.ASTID ⇒
                        val put = stmt.stmt.asPutStatic
                        if (isTainted(put.value, in)) in + StaticField(put.declaringClass, put.name)
                        else in - StaticField(put.declaringClass, put.name)*/
                    /*case PutField.ASTID ⇒
                        val put = stmt.stmt.asPutField
                        val definedBy = put.objRef.asVar.definedBy
                        (source: Fact) ⇒ {
                            if (isTainted(put.value, source)) {
                                (definedBy.iterator.map(InstanceField(_, put.declaringClass, put.name)).toSet + source).asJava
                            } else {
                                source match {
                                    case InstanceField(stmt.index, put.declaringClass, put.name) ⇒ Collections.emptySet()
                                    case _ ⇒ Collections.singleton(source)
                                }
                            }
                        }*/
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
                                case ArrayElement(index, taintIndex) if expr.asVar.definedBy.contains(index) ⇒
                                    TwoElementSet.twoElementSet(source, ArrayElement(stmt.index, taintIndex))
                                case _ ⇒ Collections.singleton(source)
                            }
                        }
                    case ArrayLoad.ASTID ⇒
                        val load = expr.asArrayLoad
                        (source: Fact) ⇒ {
                            source match {
                                // The specific array element may be tainted
                                case ArrayElement(index, taintedIndex) ⇒
                                    val arrIndex = getConstValue(load.index, stmt.code)
                                    if (load.arrayRef.asVar.definedBy.contains(index) &&
                                        (arrIndex.isEmpty || taintedIndex == arrIndex.get))
                                        TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                    else
                                        Collections.singleton(source)
                                // Or the whole array
                                case Variable(index) if load.arrayRef.asVar.definedBy.contains(index) ⇒
                                    TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                case _ ⇒ Collections.singleton(source)
                            }
                        }
                    /*case GetStatic.ASTID ⇒
                        val get = expr.asGetStatic
                        if (in.contains(StaticField(get.declaringClass, get.name)))
                            in + Variable(stmt.index)
                        else in*/
                    /*case GetField.ASTID ⇒
                        val get = expr.asGetField
                        (source: Fact) ⇒ {
                            source match {
                                // The specific field may be tainted
                                case InstanceField(index, _, taintedField) if taintedField == get.name && get.objRef.asVar.definedBy.contains(index) ⇒
                                    TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                // Or the whole object
                                case Variable(index) if get.objRef.asVar.definedBy.contains(index) ⇒
                                    TwoElementSet.twoElementSet(source, Variable(stmt.index))
                                case _ ⇒ Collections.singleton(source)
                            }
                        }*/
                    case _ ⇒ Identity.v()
                }

            override def getCallFlowFunction(stmt: Statement, callee: Method): FlowFunction[Fact] = {
                if (callee.name == "sink") {
                    source: Fact ⇒
                        {
                            source match {
                                case Variable(index) if asCall(stmt.stmt).allParams.exists(p ⇒ p.asVar.definedBy.contains(index)) ⇒
                                    println(s"Found flow: $stmt")
                                case _ ⇒
                            }
                            Collections.emptySet()
                        }
                } else if (callee.name == "forName" && (callee.classFile.thisType eq ObjectType.Class) &&
                    callee.descriptor.parameterTypes == RefArray(ObjectType.String)) {
                    source: Fact ⇒
                        {
                            source match {
                                case Variable(index) if asCall(stmt.stmt).allParams.exists(p ⇒ p.asVar.definedBy.contains(index)) ⇒
                                    println(s"Found flow: $stmt")
                                case _ ⇒
                            }
                            Collections.emptySet()
                        }
                } else if ((callee.descriptor.returnType eq ObjectType.Class) ||
                    (callee.descriptor.returnType eq ObjectType.Object)) {
                    source: Fact ⇒
                        (source match {
                            case Variable(index) ⇒ // Taint formal parameter if actual parameter is tainted
                                asCall(stmt.stmt).allParams.zipWithIndex.collect {
                                    case (param, paramIdx) if param.asVar.definedBy.contains(index) ⇒
                                        Variable(paramToIndex(paramIdx, !callee.isStatic))
                                }.asInstanceOf[Seq[Fact]]

                            case ArrayElement(index, taintedIndex) ⇒
                                // Taint element of formal parameter if element of actual parameter is tainted
                                asCall(stmt.stmt).allParams.zipWithIndex.collect {
                                    case (param, paramIdx) if param.asVar.definedBy.contains(index) ⇒
                                        ArrayElement(paramToIndex(paramIdx, !callee.isStatic), taintedIndex)
                                }.asInstanceOf[Seq[Fact]]

                            /*case InstanceField(index, declClass, taintedField) if classHierarchy.isSubtypeOf(declClass, callee.classFile.thisType).isYesOrUnknown || classHierarchy.isSubtypeOf(callee.classFile.thisType, declClass).isYesOrUnknown ⇒
                                // Taint field of formal parameter if field of actual parameter is tainted
                                // Only if the formal parameter is of a type that may have that field!
                                asCall(stmt.stmt).allParams.zipWithIndex.collect {
                                    case (param, paramIdx) if param.asVar.definedBy.contains(index) ⇒
                                        InstanceField(paramToIndex(paramIdx, !callee.isStatic), declClass, taintedField)
                                }.asInstanceOf[Seq[Fact]]*/
                            //case sf: StaticField ⇒ Set(sf)
                            case _ ⇒ Seq.empty
                        }).toSet.asJava
                } else KillAll.v()
            }

            override def getReturnFlowFunction(stmt: Statement, callee: Method, exit: Statement, succ: Statement): FlowFunction[Fact] = {

                /**
                 * Checks whether the formal parameter is of a reference type, as primitive types are
                 * call-by-value.
                 */
                def isRefTypeParam(source: Int): Boolean =
                    if (source == -1) true
                    else {
                        callee.descriptor.parameterType(
                            paramToIndex(source, includeThis = false)
                        ).isReferenceType
                    }

                if (callee.name == "source" && stmt.stmt.astID == Assignment.ASTID)
                    new Gen(Variable(stmt.index), NullFact)
                else if (callee.name == "sanitize")
                    KillAll.v()
                else {
                    val returnValue =
                        if (exit.stmt.astID == ReturnValue.ASTID &&
                            stmt.stmt.astID == Assignment.ASTID) {
                            Some(exit.stmt.asReturnValue.expr.asVar)
                        } else None

                    source: Fact ⇒ {
                        val paramFacts = source match {
                            case Variable(index) if index < 0 && index > -100 && isRefTypeParam(index) ⇒
                                // Taint actual parameter if formal parameter is tainted
                                val params =
                                    asCall(stmt.stmt).allParams(paramToIndex(index, !callee.isStatic))
                                params.asVar.definedBy.iterator.map(Variable).asInstanceOf[Iterator[Fact]].toSet

                            case ArrayElement(index, taintedIndex) if index < 0 && index > -100 ⇒
                                // Taint element of actual parameter if element of formal parameter is tainted
                                val params =
                                    asCall(stmt.stmt).allParams(paramToIndex(index, !callee.isStatic))
                                params.asVar.definedBy.iterator.map(ArrayElement(_, taintedIndex)).asInstanceOf[Iterator[Fact]].toSet

                            case InstanceField(index, declClass, taintedField) if index < 0 && index > -10 ⇒
                                // Taint field of actual parameter if field of formal parameter is tainted
                                val params =
                                    asCall(stmt.stmt).allParams(paramToIndex(index, !callee.isStatic))
                                params.asVar.definedBy.iterator.map(InstanceField(_, declClass, taintedField)).asInstanceOf[Iterator[Fact]].toSet

                            //case sf: StaticField ⇒ flows += sf
                            case FlowFact(flow) ⇒
                                val newFlow = flow + stmt.method
                                if (HerosTestTaintAnalysis.initialMethods.contains(stmt.method)) {
                                    println(s"flow: "+newFlow.map(_.toJava).mkString(", "))
                                    Set.empty[Fact]
                                } else {
                                    Set(FlowFact(newFlow))
                                }
                            case _ ⇒
                                Set.empty
                        }

                        val returnFact = source match {
                            case Variable(index) if returnValue.exists(_.definedBy.contains(index)) ⇒
                                Some(Variable(stmt.index))
                            case ArrayElement(index, taintedIndex) if returnValue.exists(_.definedBy.contains(index)) ⇒
                                Some(ArrayElement(stmt.index, taintedIndex))
                            case InstanceField(index, declClass, taintedField) if returnValue.exists(_.definedBy.contains(index)) ⇒
                                Some(InstanceField(stmt.index, declClass, taintedField))
                            case _ ⇒ None
                        }

                        val allFacts = paramFacts ++ returnFact.asInstanceOf[Option[Fact]]

                        allFacts.asJava
                    }
                }
            }

            override def getCallToReturnFlowFunction(stmt: Statement, succ: Statement): FlowFunction[Fact] = {
                val call = asCall(stmt.stmt)
                if (call.name == "sanitize") {
                    val kill = call.allParams.collect {
                        case param if param.asVar.definedBy.size == 1 ⇒
                            new Kill[Fact](Variable(param.asVar.definedBy.head))
                    }
                    Union.union(kill: _*)
                } else if (call.name == "forName" && (call.declaringClass eq ObjectType.Class) &&
                    call.descriptor.parameterTypes == RefArray(ObjectType.String)) {
                    source: Fact ⇒
                        {
                            source match {
                                case Variable(index) if call.allParams.exists(p ⇒ p.asVar.definedBy.contains(index)) ⇒
                                    if (HerosTestTaintAnalysis.initialMethods.contains(stmt.method)) {
                                        println(s"flow: "+stmt.method.toJava)
                                        Collections.singleton(source)
                                    } else {
                                        TwoElementSet.twoElementSet(source, FlowFact(ListSet(stmt.method)))
                                    }
                                case _ ⇒
                                    Collections.singleton(source)
                            }
                        }
                } else {
                    Identity.v()
                }
            }
        }
    }

    override def createZeroValue(): Fact = NullFact

    val propertyStore = p.get(PropertyStoreKey)

    override val initialSeeds: util.Map[Statement, util.Set[Fact]] = {
        var result: Map[Statement, util.Set[Fact]] = Map.empty
        for ((m, facts) ← HerosTestTaintAnalysis.initialMethods) {
            result += icfg.getStartPointsOf(m).iterator().next() → facts
        }
        result.asJava
    }

    /**
     * Converts a parameter origin to the index in the parameter seq (and vice-versa).
     */
    def paramToIndex(param: Int, includeThis: Boolean): Int =
        (if (includeThis) -1 else -2) - param

    /**
     * Gets the Call for a statement that contains a call (MethodCall Stmt or ExprStmt/Assigment
     * with FunctionCall)
     */
    def asCall(stmt: Stmt[V]): Call[V] = stmt.astID match {
        case Assignment.ASTID ⇒ stmt.asAssignment.expr.asFunctionCall
        case ExprStmt.ASTID   ⇒ stmt.asExprStmt.expr.asFunctionCall
        case _                ⇒ stmt.asMethodCall
    }

    /**
     * Returns the constant int value of an expression if it exists, None otherwise.
     */
    def getConstValue(expr: Expr[V], code: Array[Stmt[V]]): Option[Int] = {
        if (expr.isIntConst) Some(expr.asIntConst.value)
        else if (expr.isVar) {
            val constVals = expr.asVar.definedBy.iterator.map[Option[Int]] { idx ⇒
                if (idx >= 0) {
                    val stmt = code(idx)
                    if (stmt.astID == Assignment.ASTID && stmt.asAssignment.expr.isIntConst)
                        Some(stmt.asAssignment.expr.asIntConst.value)
                    else
                        None
                } else None
            }.toIterable
            if (constVals.forall(option ⇒ option.isDefined && option.get == constVals.head.get))
                constVals.head
            else None
        } else None
    }

    /**
     * Returns true if the expression contains a taint.
     */
    def isTainted(expr: Expr[V], in: Fact): Boolean = {
        expr.isVar && (in match {
            case Variable(source)            ⇒ expr.asVar.definedBy.contains(source)
            case ArrayElement(source, _)     ⇒ expr.asVar.definedBy.contains(source)
            case InstanceField(source, _, _) ⇒ expr.asVar.definedBy.contains(source)
            case _                           ⇒ false
        })
    }
}

object HerosTestTaintAnalysis {

    val p = Project(bytecode.RTJar)
    //val p = Project(new File("/home/dominik/Desktop/test"))
    val tacai: Method ⇒ TACode[TACMethodParameter, DUVar[ValueInformation]] = p.get(DefaultTACAIKey)

    val initialMethods: Map[Method, util.Set[Fact]] = {
        var result: Map[Method, util.Set[Fact]] = Map.empty
        for (m ← p.allMethodsWithBody) {
            //result = result.updated(m, Collections.singleton(NullFact))
            if ((m.isPublic || m.isProtected) && (m.descriptor.returnType == ObjectType.Object ||
                m.descriptor.returnType == ObjectType.Class)) {
                m.descriptor.parameterTypes.zipWithIndex.collect {
                    case (pType, index) if pType == ObjectType.String ⇒ index
                } foreach { index ⇒
                    val facts = result.getOrElse(m, new util.HashSet[Fact]())
                    facts.add(Variable(paramToIndex(index, false)))
                    result = result.updated(m, facts)
                }
            }
        }
        result
    }

    /**
     * Converts a parameter origin to the index in the parameter seq (and vice-versa).
     */
    def paramToIndex(param: Int, includeThis: Boolean): Int =
        (if (includeThis) -1 else -2) - param

    def main(args: Array[String]): Unit = {
        time {
            val solver = new IFDSSolver(new HerosTestTaintAnalysis(p, new OpalICFG(p)))
            solver.solve()
        } { t ⇒ println(s"Time: ${t.toSeconds}") }
    }
}