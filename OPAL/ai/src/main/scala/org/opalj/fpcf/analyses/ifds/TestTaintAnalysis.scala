/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package ifds

import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.fpcf.par.RecordAllPropertyStoreTracer
import org.opalj.fpcf.par.PKEParallelTasksPropertyStore
import org.opalj.fpcf.properties.IFDSProperty
import org.opalj.fpcf.properties.IFDSPropertyMetaInformation
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.Var
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Stmt
import org.opalj.tac.ReturnValue
//import org.opalj.tac.PutStatic
//import org.opalj.tac.GetStatic
import org.opalj.tac.PutField
import org.opalj.tac.GetField

trait Fact

case class Variable(source: Int) extends Fact
case class ArrayElement(source: Int, element: Int) extends Fact
//case class StaticField(classType: ObjectType, fieldName: String) extends Fact
case class InstanceField(source: Int, classType: ObjectType, fieldName: String) extends Fact

/**
 * A simple IFDS taint analysis.
 *
 * @author Dominik Helm
 */
class TestTaintAnalysis private[ifds] (
        implicit
        val project: SomeProject
) extends AbstractIFDSAnalysis[Fact] {

    override val property: IFDSPropertyMetaInformation[Fact] = Taint

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
                    if (index.isDefined) // Taint known array index
                        in ++ definedBy.iterator.map(ArrayElement(_, index.get))
                    else // Taint whole array if index is unknown
                        in ++ definedBy.iterator.map(Variable)
                else if (index.isDefined && definedBy.size == 1) // Untaint if possible
                    in - ArrayElement(definedBy.head, index.get)
                else in
            /*case PutStatic.ASTID ⇒
                val put = stmt.stmt.asPutStatic
                if (isTainted(put.value, in)) in + StaticField(put.declaringClass, put.name)
                else in - StaticField(put.declaringClass, put.name)*/
            case PutField.ASTID ⇒
                val put = stmt.stmt.asPutField
                val definedBy = put.objRef.asVar.definedBy
                if (isTainted(put.value, in))
                    in ++ definedBy.iterator.map(InstanceField(_, put.declaringClass, put.name))
                else if (definedBy.size == 1) // Untaint if object is known precisely
                    in - InstanceField(definedBy.head, put.declaringClass, put.name)
                else in
            case _ ⇒ in
        }

    /**
     * Returns true if the expression contains a taint.
     */
    def isTainted(expr: Expr[V], in: Set[Fact]): Boolean = {
        expr.isVar && in.exists {
            case Variable(source)            ⇒ expr.asVar.definedBy.contains(source)
            case ArrayElement(source, _)     ⇒ expr.asVar.definedBy.contains(source)
            case InstanceField(source, _, _) ⇒ expr.asVar.definedBy.contains(source)
            case _                           ⇒ false
        }
    }

    /**
     * Returns the constant int value of an expression if it exists, None otherwise.
     */
    def getConstValue(expr: Expr[V], code: Array[Stmt[V]]): Option[Int] = {
        if (expr.isIntConst) Some(expr.asIntConst.value)
        else if (expr.isVar) {
            val constVals = expr.asVar.definedBy.iterator.map { idx ⇒
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
                    // The specific array element may be tainted
                    case ArrayElement(source, taintedIndex) ⇒
                        val index = getConstValue(load.index, stmt.code)
                        load.arrayRef.asVar.definedBy.contains(source) &&
                            (index.isEmpty || taintedIndex == index.get)
                    // Or the whole array
                    case Variable(source) ⇒ load.arrayRef.asVar.definedBy.contains(source)
                    case _                ⇒ false
                })
                    in + Variable(stmt.index)
                else
                    in
            /*case GetStatic.ASTID ⇒
                val get = expr.asGetStatic
                if (in.contains(StaticField(get.declaringClass, get.name)))
                    in + Variable(stmt.index)
                else in*/
            case GetField.ASTID ⇒
                val get = expr.asGetField
                if (in.exists {
                    // The specific field may be tainted
                    case InstanceField(source, _, taintedField) ⇒
                        taintedField == get.name && get.objRef.asVar.definedBy.contains(source)
                    // Or the whole object
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
        } else if (callee.name == "forName" && (callee.declaringClassType eq ObjectType.Class) &&
            callee.descriptor.parameterTypes == Seq(ObjectType.String)) {
            if (in.exists {
                case Variable(source) ⇒
                    asCall(stmt.stmt).params.exists(p ⇒ p.asVar.definedBy.contains(source))
                case _ ⇒ false
            })
                println(s"Found flow: $stmt")
            Set.empty
        } else if ((callee.descriptor.returnType eq ObjectType.Class) ||
            (callee.descriptor.returnType eq ObjectType.Object)) {
            in.collect {
                case Variable(source) ⇒ // Taint formal parameter if actual parameter is tainted
                    asCall(stmt.stmt).allParams.zipWithIndex.collect {
                        case (param, index) if param.asVar.definedBy.contains(source) ⇒
                            Variable(paramToIndex(index, callee.definedMethod))
                    }

                case ArrayElement(source, taintedIndex) ⇒
                    // Taint element of formal parameter if element of actual parameter is tainted
                    asCall(stmt.stmt).allParams.zipWithIndex.collect {
                        case (param, index) if param.asVar.definedBy.contains(source) ⇒
                            ArrayElement(paramToIndex(index, callee.definedMethod), taintedIndex)
                    }

                case InstanceField(source, declClass, taintedField) if classHierarchy.isSubtypeOf(declClass, callee.declaringClassType).isYesOrUnknown ||
                    classHierarchy.isSubtypeOf(callee.declaringClassType, declClass).isYesOrUnknown ⇒
                    // Taint field of formal parameter if field of actual parameter is tainted
                    // Only if the formal parameter is of a type that may have that field!
                    asCall(stmt.stmt).allParams.zipWithIndex.collect {
                        case (param, index) if param.asVar.definedBy.contains(source) ⇒
                            InstanceField(paramToIndex(index, callee.definedMethod), declClass, taintedField)
                    }
                //case sf: StaticField ⇒ Set(sf)
            }.flatten
        } else Set.empty
    }

    override def returnFlow(
        stmt:   Statement,
        callee: DeclaredMethod,
        exit:   Statement,
        succ:   Statement,
        in:     Set[Fact]
    ): Set[Fact] = {

        /**
         * Checks whether the formal parameter is of a reference type, as primitive types are
         * call-by-value.
         */
        def isRefTypeParam(source: Int): Boolean =
            callee.descriptor.parameterType(
                paramToIndex(source, callee.definedMethod)
            ).isReferenceType


        if (callee.name == "source" && stmt.stmt.astID == Assignment.ASTID)
            Set(Variable(stmt.index))
        else if (callee.name == "sanitize")
            Set.empty
        else {
            var flows: Set[Fact] = Set.empty
            for (fact ← in) {
                fact match {
                    case Variable(source) if source < 0 && source > -100 && isRefTypeParam(source) ⇒
                        // Taint actual parameter if formal parameter is tainted
                        val param =
                            asCall(stmt.stmt).allParams(paramToIndex(source, callee.definedMethod))
                        flows ++= param.asVar.definedBy.iterator.map(Variable)

                    case ArrayElement(source, taintedIndex) if source < 0 && source > -100 ⇒
                        // Taint element of actual parameter if element of formal parameter is tainted
                        val param =
                            asCall(stmt.stmt).allParams(paramToIndex(source, callee.definedMethod))
                        flows ++= param.asVar.definedBy.iterator.map(ArrayElement(_, taintedIndex))

                    case InstanceField(source, declClass, taintedField) if source < 0 && source > -10 ⇒
                        // Taint field of actual parameter if field of formal parameter is tainted
                        val param =
                            asCall(stmt.stmt).allParams(paramToIndex(source, callee.definedMethod))
                        flows ++= param.asVar.definedBy.iterator.map(InstanceField(_, declClass, taintedField))
                    //case sf: StaticField ⇒ flows += sf
                    case _ ⇒
                }
            }

            // Propagate taints of the return value
            if (exit.stmt.astID == ReturnValue.ASTID && stmt.stmt.astID == Assignment.ASTID) {
                val returnValue = exit.stmt.asReturnValue.expr.asVar
                flows ++= in.collect {
                    case Variable(source) if returnValue.definedBy.contains(source) ⇒
                        Variable(stmt.index)
                    case ArrayElement(source, taintedIndex) if returnValue.definedBy.contains(source) ⇒
                        ArrayElement(stmt.index, taintedIndex)
                    case InstanceField(source, declClass, taintedField) if returnValue.definedBy.contains(source) ⇒
                        InstanceField(stmt.index, declClass, taintedField)
                }
            }

            flows
        }
    }

    /**
     * Converts a parameter origin to the index in the parameter seq (and vice-versa).
     */
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

    def key: PropertyKey[Taint] = Taint.key

    def noFlowInformation: Taint = Taint.noFlowInformation
}

object Taint extends IFDSPropertyMetaInformation[Fact] {
    override type Self = Taint

    val noFlowInformation = new Taint(null)

    val key: PropertyKey[Taint] = PropertyKey.create[DeclaredMethod, Taint](
        "TestTaint",
        noFlowInformation
    )
}

object TestTaintAnalysisRunner {

    def paramToIndex(param: Int, callee: Method): Int =
        -1 - param - (if (callee.isStatic) 1 else 0)

    def main(args: Array[String]): Unit = {
        //val p = Project(new File("/home/dominik/Desktop/test"))
        val p = Project(bytecode.RTJar)
        p.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) ⇒ {
                val ps = PKEParallelTasksPropertyStore.create(
                    new RecordAllPropertyStoreTracer,
                    context.iterator.map(_.asTuple).toMap
                )(p.logContext)
                PropertyStore.updateDebug(true)
                ps
            }
        )
        val ps = p.get(PropertyStoreKey)
        ps.setupPhase(Set(TestTaintAnalysis.property.key))
        TestTaintAnalysis.startLazily(p, ps, TestTaintAnalysis.init(p, ps))
        val declaredMethods = p.get(DeclaredMethodsKey)
        var entryPoints = 0
        for (m ← p.allMethodsWithBody) {
            //val e = (declaredMethods(m), null)
            //ps.force(e, TestTaintAnalysis.property.key)
            if (m.isPublic && (m.descriptor.returnType == ObjectType.Object ||
                m.descriptor.returnType == ObjectType.Class)) {
                m.descriptor.parameterTypes.zipWithIndex.collect {
                    case (pType, index) if pType == ObjectType.String ⇒ index
                } foreach { index ⇒
                    val e = (declaredMethods(m), Variable(paramToIndex(index, m)))
                    entryPoints += 1
                    ps.force(e, TestTaintAnalysis.property.key)
                }
            }
        }
        println(entryPoints)
        ps.waitOnPhaseCompletion()
    }
}