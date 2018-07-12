/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package ifds

import java.io.File

import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.fpcf.properties.IFDSProperty
import org.opalj.fpcf.properties.IFDSPropertyMetaInformation
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.Var
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Stmt
import org.opalj.tac.ReturnValue
import org.opalj.tac.PutStatic
import org.opalj.tac.GetStatic
import org.opalj.tac.PutField
import org.opalj.tac.GetField

trait Fact

case class Variable(source: Int) extends Fact
case class ArrayElement(source: Int, element: Int) extends Fact
case class StaticField(classType: ObjectType, fieldName: String) extends Fact
case class InstanceField(source: Int, fieldName: String) extends Fact

class TestTaintAnalysis private[ifds] (
        implicit
        val project: SomeProject
) extends AbstractIFDSAnalysis[Fact] {

    override val property = Taint

    override def createProperty(result: Map[Statement, Set[Fact]]): IFDSProperty[Fact] = {
        new Taint(result)
    }

    override def normalFlow(stmt: Statement, succ: Statement, in: Set[Fact]): Set[Fact] =
        stmt.stmt.astID match {
            case Assignment.ASTID ⇒
                handleAssignment(stmt, stmt.stmt.asAssignment.expr, in)
            case ArrayStore.ASTID ⇒
                val store = stmt.stmt.asArrayStore
                val definedBy = store.arrayRef.asVar.definedBy
                val index = getConstValue(store.index, stmt.code)
                if (isTainted(store.value, in))
                    if (index.isDefined)
                        in ++ definedBy.iterator.map(ArrayElement(_, index.get))
                    else
                        in ++ definedBy.iterator.map(Variable(_))
                else if (index.isDefined && definedBy.size == 1)
                    in - ArrayElement(definedBy.head, index.get)
                else in
            case PutStatic.ASTID ⇒
                val put = stmt.stmt.asPutStatic
                if (isTainted(put.value, in)) in + StaticField(put.declaringClass, put.name)
                else in - StaticField(put.declaringClass, put.name)
            case PutField.ASTID ⇒
                val put = stmt.stmt.asPutField
                val definedBy = put.objRef.asVar.definedBy
                if (isTainted(put.value, in))
                    in ++ definedBy.iterator.map(InstanceField(_, put.name))
                else if (definedBy.size == 1)
                    in - InstanceField(definedBy.head, put.name)
                else in
            case _ ⇒ in
        }

    def isTainted(expr: Expr[V], in: Set[Fact]): Boolean = {
        expr.isVar && in.exists {
            case Variable(source)         ⇒ expr.asVar.definedBy.contains(source)
            case ArrayElement(source, _)  ⇒ expr.asVar.definedBy.contains(source)
            case InstanceField(source, _) ⇒ expr.asVar.definedBy.contains(source)
            case _                        ⇒ false
        }
    }

    def getConstValue(expr: Expr[V], code: Array[Stmt[V]]): Option[Int] = {
        if (expr.isIntConst) Some(expr.asIntConst.value)
        else if (expr.isVar) {
            val constVals = expr.asVar.definedBy.iterator.map { idx ⇒
                val stmt = code(idx)
                if (stmt.astID == Assignment.ASTID && stmt.asAssignment.expr.isIntConst)
                    Some(stmt.asAssignment.expr.asIntConst.value)
                else
                    None
            }.toIterable
            if (constVals.forall(option ⇒ option.isDefined && option.get == constVals.head.get))
                constVals.head
            else None
        } else None
    }

    def handleAssignment(stmt: Statement, expr: Expr[V], in: Set[Fact]): Set[Fact] =
        expr.astID match {
            case Var.ASTID ⇒
                val newTaint = in.collect {
                    case Variable(source) if expr.asVar.definedBy.contains(source) ⇒
                        Some(Variable(stmt.index))
                    case ArrayElement(source, taintIndex) if expr.asVar.definedBy.contains(source) ⇒
                        Some(ArrayElement(stmt.index, taintIndex))
                    case _ ⇒ None
                }.flatten
                in ++ newTaint
            case ArrayLoad.ASTID ⇒
                val load = expr.asArrayLoad
                if (in.exists {
                    case ArrayElement(source, taintedIndex) ⇒
                        val index = getConstValue(load.index, stmt.code)
                        load.arrayRef.asVar.definedBy.contains(source) &&
                            (index.isEmpty || taintedIndex == index.get)
                    case Variable(source) ⇒ load.arrayRef.asVar.definedBy.contains(source)
                    case _                ⇒ false
                })
                    in + Variable(stmt.index)
                else
                    in
            case GetStatic.ASTID ⇒
                val get = expr.asGetStatic
                if (in.contains(StaticField(get.declaringClass, get.name)))
                    in + Variable(stmt.index)
                else in
            case GetField.ASTID ⇒
                val get = expr.asGetField
                if (in.exists {
                    case InstanceField(source, taintedField) ⇒
                        taintedField == get.name && get.objRef.asVar.definedBy.contains(source)
                    case Variable(source) ⇒ get.objRef.asVar.definedBy.contains(source)
                    case _                ⇒ false
                })
                    in + Variable(stmt.index)
                else
                    in
            case _ ⇒ in
        }

    override def callFlow(
        stmt:   Statement,
        params: Seq[Expr[V]],
        callee: DeclaredMethod,
        in:     Set[Fact]
    ): Set[Fact] = {
        if (callee.name == "sink") {
            if (in.exists {
                case Variable(source) ⇒
                    asCall(stmt.stmt).allParams.exists(p ⇒ p.asVar.definedBy.contains(source))
                case _ ⇒ false
            })
                println(s"Found flow: $stmt")
            Set.empty
        } else {
            in.collect {
                case Variable(source) ⇒
                    params.zipWithIndex.collect {
                        case (param, index) if (param.asVar.definedBy.contains(source)) ⇒
                            Variable(paramToIndex(index, callee.definedMethod))
                    }
                case ArrayElement(source, taintedIndex) ⇒ params.zipWithIndex.collect {
                    case (param, index) if (param.asVar.definedBy.contains(source)) ⇒
                        ArrayElement(paramToIndex(index, callee.definedMethod), taintedIndex)
                }
                case InstanceField(source, taintedField) ⇒ params.zipWithIndex.collect {
                    case (param, index) if (param.asVar.definedBy.contains(source)) ⇒
                        InstanceField(paramToIndex(index, callee.definedMethod), taintedField)
                }
                case sf: StaticField ⇒ Set(sf)
            }.flatten
        }
    }

    override def returnFlow(
        stmt:   Statement,
        callee: DeclaredMethod,
        exit:   Statement,
        succ:   Statement,
        in:     Set[Fact]
    ): Set[Fact] = {
        if (callee.name == "source" && stmt.stmt.astID == Assignment.ASTID)
            Set(Variable(stmt.index))
        else if (callee.name == "sanitize")
            Set.empty
        else {
            var flows: Set[Fact] = Set.empty
            for (fact ← in) {
                fact match {
                    case Variable(source) if (source < 0) ⇒
                        val param =
                            asCall(stmt.stmt).allParams(paramToIndex(source, callee.definedMethod))
                        flows ++= param.asVar.definedBy.iterator.map(Variable(_))
                    case ArrayElement(source, taintedIndex) if (source < 0) ⇒
                        val param =
                            asCall(stmt.stmt).allParams(paramToIndex(source, callee.definedMethod))
                        flows ++= param.asVar.definedBy.iterator.map(ArrayElement(_, taintedIndex))
                    case InstanceField(source, taintedField) if (source < 0) ⇒
                        val param =
                            asCall(stmt.stmt).allParams(paramToIndex(source, callee.definedMethod))
                        flows ++= param.asVar.definedBy.iterator.map(InstanceField(_, taintedField))
                    case sf: StaticField ⇒ flows += sf
                    case _               ⇒
                }
            }

            if (exit.stmt.astID == ReturnValue.ASTID && stmt.stmt.astID == Assignment.ASTID) {
                val returnValue = exit.stmt.asReturnValue.expr.asVar
                flows ++= in.collect {
                    case Variable(source) if returnValue.definedBy.contains(source) ⇒
                        Variable(stmt.index)
                    case ArrayElement(source, taintedIndex) if returnValue.definedBy.contains(source) ⇒
                        ArrayElement(stmt.index, taintedIndex)
                    case InstanceField(source, taintedField) if returnValue.definedBy.contains(source) ⇒
                        InstanceField(stmt.index, taintedField)
                }
            }

            flows
        }
    }

    def paramToIndex(param: Int, callee: Method): Int =
        -1 - param - (if (callee.isStatic) 1 else 0)

    override def callToReturnFlow(stmt: Statement, succ: Statement, in: Set[Fact]): Set[Fact] = {
        val call = asCall(stmt.stmt)
        if (call.name == "sanitize") {
            val a = in.filter {
                case Variable(source) ⇒
                    !call.allParams.exists { p ⇒
                        val definedBy = p.asVar.definedBy
                        definedBy.size == 1 && definedBy.contains(source)
                    }
                case _ ⇒ true
            }
            a
        } else
            in
    }
}

object TestTaintAnalysis extends LazyIFDSAnalysis[Fact] {
    override def init(p: SomeProject, ps: PropertyStore) = new TestTaintAnalysis()(p)

    override def property: IFDSPropertyMetaInformation[Fact] = Taint

    override val uses: Set[PropertyKind] = Set.empty
}

class Taint(val flows: Map[Statement, Set[Fact]]) extends IFDSProperty[Fact] {

    override type Self = Taint

    def key = Taint.key

    def noFlowInformation = Taint.noFlowInformation
}

object Taint extends IFDSPropertyMetaInformation[Fact] {
    override type Self = Taint

    val noFlowInformation = new Taint(null)

    val key = PropertyKey.create[DeclaredMethod, Taint](
        "TestTaint",
        noFlowInformation /* TODO fallback */
    )
}

object TestTaintAnalysisRunner {
    def main(args: Array[String]): Unit = {
        val p = Project(new File("/home/dominik/Desktop/test"))
        val ps = p.get(PropertyStoreKey)
        ps.setupPhase(Set(TestTaintAnalysis.property.key))
        TestTaintAnalysis.startLazily(p, ps, TestTaintAnalysis.init(p, ps))
        val declaredMethods = p.get(DeclaredMethodsKey)
        for (m ← p.allMethodsWithBody) {
            val e = (declaredMethods(m), null)
            ps.force(e, TestTaintAnalysis.property.key)
        }
        ps.waitOnPhaseCompletion()
    }
}