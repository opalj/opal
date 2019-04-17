/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package test

import scala.collection.immutable.ListSet
import org.opalj.log.LogContext
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.fpcf.seq.PKESequentialPropertyStore
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.domain.l1
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.fpcf.analyses.AbstractIFDSAnalysis.V
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

trait Fact extends AbstractIFDSFact
case class Variable(index: Int) extends Fact
case class ArrayElement(index: Int, element: Int) extends Fact
//case class StaticField(classType: ObjectType, fieldName: String) extends Fact
case class InstanceField(index: Int, classType: ObjectType, fieldName: String) extends Fact
case class FlowFact(flow: ListSet[Method]) extends Fact {
    override val hashCode: Int = {
        // HERE, a foldLeft introduces a lot of overhead due to (un)boxing.
        var r = 1
        flow.foreach(f ⇒ r = (r + f.hashCode()) * 31)
        r
    }
}
case class NullFact() extends Fact with AbstractIFDSNullFact

/**
 * A simple IFDS taint analysis.
 *
 * @author Dominik Helm, Mario Trageser
 */
class TestTaintAnalysis private (
        implicit
        val project: SomeProject
) extends AbstractIFDSAnalysis[Fact] {

    override val propertyKey: IFDSPropertyMetaInformation[Fact] = Taint

    override def createPropertyValue(result: Map[Statement, Set[Fact]]): IFDSProperty[Fact] = {
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
                        // Instead of using an iterator, we are going to use internal iteration
                        // in ++ definedBy.iterator.map(ArrayElement(_, index.get))
                        definedBy.foldLeft(in) { (c, n) ⇒ c + ArrayElement(n, index.get) }
                    else // Taint whole array if index is unknown
                        // Instead of using an iterator, we are going to use internal iteration:
                        // in ++ definedBy.iterator.map(Variable)
                        definedBy.foldLeft(in) { (c, n) ⇒ c + Variable(n) }
                else if (index.isDefined && definedBy.size == 1) // Untaint if possible
                    in - ArrayElement(definedBy.head, index.get)
                else in
            /*case PutStatic.ASTID ⇒
                    val put = stmt.stmt.asPutStatic
                    if (isTainted(put.value, in)) in + StaticField(put.declaringClass, put.name)
                    else in - StaticField(put.declaringClass, put.name)*/
            /*case PutField.ASTID ⇒
                    val put = stmt.stmt.asPutField
                    val definedBy = put.objRef.asVar.definedBy
                    if (isTainted(put.value, in))
                        in ++ definedBy.iterator.map(InstanceField(_, put.declaringClass, put.name))
                    else if (definedBy.size == 1) // Untaint if object is known precisely
                        in - InstanceField(definedBy.head, put.declaringClass, put.name)
                    else in */
            case _ ⇒ in
        }

    /**
     * Returns true if the expression contains a taint.
     */
    def isTainted(expr: Expr[V], in: Set[Fact]): Boolean = {
        expr.isVar && in.exists {
            case Variable(index)            ⇒ expr.asVar.definedBy.contains(index)
            case ArrayElement(index, _)     ⇒ expr.asVar.definedBy.contains(index)
            case InstanceField(index, _, _) ⇒ expr.asVar.definedBy.contains(index)
            case _                          ⇒ false
        }
    }

    /**
     * Returns the constant int value of an expression if it exists, None otherwise.
     */
    def getConstValue(expr: Expr[V], code: Array[Stmt[V]]): Option[Int] = {
        if (expr.isIntConst) Some(expr.asIntConst.value)
        else if (expr.isVar) {
            // TODO The following looks optimizable!
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

    def handleAssignment(stmt: Statement, expr: Expr[V], in: Set[Fact]): Set[Fact] =
        expr.astID match {
            case Var.ASTID ⇒
                val newTaint = in.collect {
                    case Variable(index) if expr.asVar.definedBy.contains(index) ⇒
                        Some(Variable(stmt.index))
                    case ArrayElement(index, taintIndex) if expr.asVar.definedBy.contains(index) ⇒
                        Some(ArrayElement(stmt.index, taintIndex))
                    case _ ⇒ None
                }.flatten
                in ++ newTaint
            case ArrayLoad.ASTID ⇒
                val load = expr.asArrayLoad
                if (in.exists {
                    // The specific array element may be tainted
                    case ArrayElement(index, taintedIndex) ⇒
                        val element = getConstValue(load.index, stmt.code)
                        load.arrayRef.asVar.definedBy.contains(index) &&
                            (element.isEmpty || taintedIndex == element.get)
                    // Or the whole array
                    case Variable(index) ⇒ load.arrayRef.asVar.definedBy.contains(index)
                    case _               ⇒ false
                })
                    in + Variable(stmt.index)
                else
                    in
            /*case GetStatic.ASTID ⇒
                val get = expr.asGetStatic
                if (in.contains(StaticField(get.declaringClass, get.name)))
                    in + Variable(stmt.index)
                else in*/
            /*case GetField.ASTID ⇒
                val get = expr.asGetField
                if (in.exists {
                    // The specific field may be tainted
                    case InstanceField(index, _, taintedField) ⇒
                        taintedField == get.name && get.objRef.asVar.definedBy.contains(index)
                    // Or the whole object
                    case Variable(index) ⇒ get.objRef.asVar.definedBy.contains(index)
                    case _               ⇒ false
                })
                    in + Variable(stmt.index)
                else
                    in*/
            case _ ⇒ in
        }

    override def callFlow(
        stmt:   Statement,
        callee: DeclaredMethod,
        in:     Set[Fact]
    ): Set[Fact] = {
        val allParams = asCall(stmt.stmt).receiverOption ++ asCall(stmt.stmt).params
        if (callee.name == "sink") {
            if (in.exists {
                case Variable(index) ⇒
                    allParams.exists(p ⇒ p.asVar.definedBy.contains(index))
                case _ ⇒ false
            })
                println(s"Found flow: $stmt")
            Set.empty
        } else if (callee.name == "forName" && (callee.declaringClassType eq ObjectType.Class) &&
            callee.descriptor.parameterTypes == RefArray(ObjectType.String)) {
            if (in.exists {
                case Variable(index) ⇒
                    asCall(stmt.stmt).params.exists(p ⇒ p.asVar.definedBy.contains(index))
                case _ ⇒ false
            })
                println(s"Found flow: $stmt")
            Set.empty
        } else if ((callee.descriptor.returnType eq ObjectType.Class) ||
            (callee.descriptor.returnType eq ObjectType.Object)) {
            in.collect {
                case Variable(index) ⇒ // Taint formal parameter if actual parameter is tainted
                    allParams.zipWithIndex.collect {
                        case (param, pIndex) if param.asVar.definedBy.contains(index) ⇒
                            Variable(paramToIndex(pIndex, !callee.definedMethod.isStatic))
                    }

                case ArrayElement(index, taintedIndex) ⇒
                    // Taint element of formal parameter if element of actual parameter is tainted
                    allParams.zipWithIndex.collect {
                        case (param, pIndex) if param.asVar.definedBy.contains(index) ⇒
                            ArrayElement(paramToIndex(pIndex, !callee.definedMethod.isStatic), taintedIndex)
                    }

                /*case InstanceField(index, declClass, taintedField) ⇒
                    // Taint field of formal parameter if field of actual parameter is tainted
                    // Only if the formal parameter is of a type that may have that field!
                    asCall(stmt.stmt).allParams.zipWithIndex.collect {
                        case (param, pIndex) if param.asVar.definedBy.contains(index) &&
                            (paramToIndex(pIndex, !callee.definedMethod.isStatic) != -1 ||
                                classHierarchy.isSubtypeOf(declClass, callee.declaringClassType)) ⇒
                            InstanceField(paramToIndex(pIndex, !callee.definedMethod.isStatic), declClass, taintedField)
                    }*/
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
        def isRefTypeParam(index: Int): Boolean =
            if (index == -1) true
            else {
                callee.descriptor.parameterType(
                    paramToIndex(index, includeThis = false)
                ).isReferenceType
            }

        if (callee.name == "source" && stmt.stmt.astID == Assignment.ASTID)
            Set(Variable(stmt.index))
        else if (callee.name == "sanitize")
            Set.empty
        else {
            val allParams = (asCall(stmt.stmt).receiverOption ++ asCall(stmt.stmt).params).toSeq
            var flows: Set[Fact] = Set.empty
            for (fact ← in) {
                fact match {
                    case Variable(index) if index < 0 && index > -100 && isRefTypeParam(index) ⇒
                        // Taint actual parameter if formal parameter is tainted
                        val param =
                            allParams(paramToIndex(index, !callee.definedMethod.isStatic))
                        flows ++= param.asVar.definedBy.iterator.map(Variable)

                    case ArrayElement(index, taintedIndex) if index < 0 && index > -100 ⇒
                        // Taint element of actual parameter if element of formal parameter is tainted
                        val param =
                            allParams(paramToIndex(index, !callee.definedMethod.isStatic))
                        flows ++= param.asVar.definedBy.iterator.map(ArrayElement(_, taintedIndex))

                    case InstanceField(index, declClass, taintedField) if index < 0 && index > -10 ⇒
                        // Taint field of actual parameter if field of formal parameter is tainted
                        val param =
                            allParams(paramToIndex(index, !callee.definedMethod.isStatic))
                        flows ++= param.asVar.definedBy.iterator.map(InstanceField(_, declClass, taintedField))
                    //case sf: StaticField ⇒ flows += sf
                    case FlowFact(flow) ⇒
                        val newFlow = flow + stmt.method
                        if (entryPoints.contains(declaredMethods(exit.method))) {
                            //println(s"flow: "+newFlow.map(_.toJava).mkString(", "))
                        } else {
                            flows += FlowFact(newFlow)
                        }
                    case _ ⇒
                }
            }

            // Propagate taints of the return value
            if (exit.stmt.astID == ReturnValue.ASTID && stmt.stmt.astID == Assignment.ASTID) {
                val returnValue = exit.stmt.asReturnValue.expr.asVar
                flows ++= in.collect {
                    case Variable(index) if returnValue.definedBy.contains(index) ⇒
                        Variable(stmt.index)
                    case ArrayElement(index, taintedIndex) if returnValue.definedBy.contains(index) ⇒
                        ArrayElement(stmt.index, taintedIndex)
                    case InstanceField(index, declClass, taintedField) if returnValue.definedBy.contains(index) ⇒
                        InstanceField(stmt.index, declClass, taintedField)
                }
            }

            flows
        }
    }

    /**
     * Converts a parameter origin to the index in the parameter seq (and vice-versa).
     */
    def paramToIndex(param: Int, includeThis: Boolean): Int =
        (if (includeThis) -1 else -2) - param

    override def callToReturnFlow(stmt: Statement, succ: Statement, in: Set[Fact]): Set[Fact] = {
        val call = asCall(stmt.stmt)
        if (call.name == "sanitize") {
            in.filter {
                case Variable(index) ⇒
                    !(call.params ++ call.receiverOption).exists { p ⇒
                        val definedBy = p.asVar.definedBy
                        definedBy.size == 1 && definedBy.contains(index)
                    }
                case _ ⇒ true
            }
        } else if (call.name == "forName" && (call.declaringClass eq ObjectType.Class) &&
            call.descriptor.parameterTypes == RefArray(ObjectType.String)) {
            if (in.exists {
                case Variable(index) ⇒
                    asCall(stmt.stmt).params.exists(p ⇒ p.asVar.definedBy.contains(index))
                case _ ⇒ false
            }) {
                /*if (entryPoints.contains(declaredMethods(stmt.method))) {
                    println(s"flow: "+stmt.method.toJava)
                    in
                } else*/
                in ++ Set(FlowFact(ListSet(stmt.method)))
            } else {
                in
            }
        } else {
            in
        }
    }

    val entryPoints: Map[DeclaredMethod, Fact] = (for {
        m ← p.allMethodsWithBody
        if (m.isPublic || m.isProtected) && (m.descriptor.returnType == ObjectType.Object || m.descriptor.returnType == ObjectType.Class)
        index ← m.descriptor.parameterTypes.zipWithIndex.collect { case (pType, index) if pType == ObjectType.String ⇒ index }
    } //yield (declaredMethods(m), null)
    yield declaredMethods(m) → Variable(-2 - index)).toMap

}

object TestTaintAnalysis extends IFDSAnalysis[Fact] {
    override def init(p: SomeProject, ps: PropertyStore) = new TestTaintAnalysis()(p)

    override def property: IFDSPropertyMetaInformation[Fact] = Taint
}

class Taint(val flows: Map[Statement, Set[Fact]]) extends IFDSProperty[Fact] {

    override type Self = Taint

    def key: PropertyKey[Taint] = Taint.key
}

object Taint extends IFDSPropertyMetaInformation[Fact] {
    override type Self = Taint

    val key: PropertyKey[Taint] = PropertyKey.create(
        "TestTaint",
        new Taint(Map.empty)
    )
}

object TestTaintAnalysisRunner {

    def main(args: Array[String]): Unit = {
        if (args.contains("--help")) {
            println("Potential parameters:")
            println(" -seq to use the SequentialPropertyStore")
            println(" -l2 to use the l2 domain instead of the default l1 domain")
            println(" -delay for a three seconds delay before the taint flow analysis is started")
        }

        val p = Project(bytecode.RTJar)
        p.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) ⇒ {
                implicit val lg: LogContext = p.logContext
                PropertyStore.updateDebug(false)
                val ps =
                    if (args.contains("-seq"))
                        PKESequentialPropertyStore.apply(context: _*)
                    else
                        ???
                //PKEFJPoolPropertyStore.apply(context: _*)
                ps
            }
        )
        if (args.contains("-l2")) {
            p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
                case None               ⇒ Set(classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]])
                case Some(requirements) ⇒ requirements + classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
            }
        } else {
            p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
                case None               ⇒ Set(classOf[l1.DefaultDomainWithCFGAndDefUse[_]])
                case Some(requirements) ⇒ requirements + classOf[l1.DefaultDomainWithCFGAndDefUse[_]]
            }
        }

        val ps = p.get(PropertyStoreKey)

        if (args.contains("-delay")) {
            println("Sleeping for three seconds.")
            Thread.sleep(3000)
        }

        val (_, analyses) =
            time(2, 25, 10, {
                val project = p.recreate()
                project.get(FPCFAnalysesManagerKey).runAll(LazyTACAIProvider, TestTaintAnalysis)
            })({ (t, times) ⇒
                val average = times.map(_.toSeconds.timeSpan).sum / times.length
                println(s"average time for taint-flow analysis: ${average}")
            })
        val entryPoints = analyses.collect { case (_, a: TestTaintAnalysis) ⇒ a.entryPoints }.head
        for {
            e ← entryPoints
            flows = ps(e, TestTaintAnalysis.property.key)
            fact ← flows.ub.asInstanceOf[IFDSProperty[Fact]].flows.values.flatten.toSet[Fact]
        } {
            fact match {
                case FlowFact(flow) ⇒ println(s"flow: "+flow.map(_.toJava).mkString(", "))
                case _              ⇒
            }
        }
        println(
            ps.statistics.iterator.map(_.toString()).toList
                .sorted
                .mkString("PropertyStore Statistics:\n\t", "\n\t", "\n")
        )
    }
}
