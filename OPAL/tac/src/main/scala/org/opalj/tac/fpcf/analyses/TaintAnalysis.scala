/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses

import java.io.File

import org.opalj.ai.domain.l1
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.{DeclaredMethod, Method, ObjectType}
import org.opalj.br.analyses.{Project, SomeProject}
import org.opalj.br.fpcf.{FPCFAnalysesManagerKey, PropertyStoreKey}
import org.opalj.fpcf.seq.PKESequentialPropertyStore
import org.opalj.fpcf.{PropertyKey, PropertyStore, PropertyStoreContext}
import org.opalj.log.LogContext
import org.opalj.tac.fpcf.analyses.AbstractIFDSAnalysis.V
import org.opalj.tac._
import org.opalj.tac.fpcf.properties.{IFDSProperty, IFDSPropertyMetaInformation}
import org.opalj.util.PerformanceEvaluation.time

import scala.collection.immutable.ListSet

trait Fact
case class NullFact() extends Fact
case class Variable(index: Int) extends Fact
case class ArrayElement(index: Int, element: Int) extends Fact
case class InstanceField(index: Int, classType: ObjectType, fieldName: String) extends Fact
case class FlowFact(flow: ListSet[Method]) extends Fact {
    override val hashCode: Int = {
        var r = 1
        flow.foreach(f ⇒ r = (r + f.hashCode()) * 31)
        r
    }
}

class TaintAnalysis private (implicit val project: SomeProject) extends AbstractIFDSAnalysis[Fact] {

    override val propertyKey: IFDSPropertyMetaInformation[Fact] = Taint

    val entryPoints: Map[DeclaredMethod, Fact] = Map(
        p.allProjectClassFiles
            .filter(classFile ⇒
                classFile.thisType.fqn == "org/opalj/fpcf/fixtures/taint/TaintAnalysisTestClass")
            .flatMap(classFile ⇒ classFile.methods)
            .filter(method ⇒ method.name == "run")
            .map(method ⇒ declaredMethods(method))
            .head -> null
    )

    override def createPropertyValue(result: Map[Statement, Set[Fact]]): IFDSProperty[Fact] = {
        new Taint(result)
    }

    override def normalFlow(statement: Statement, succ: Statement, in: Set[Fact]): Set[Fact] =
        statement.stmt.astID match {
            case Assignment.ASTID ⇒
                handleAssignment(statement, in)
            case ArrayStore.ASTID ⇒
                val store = statement.stmt.asArrayStore
                val definedBy = store.arrayRef.asVar.definedBy
                val index = getIntConstant(store.index, statement.code)
                if (isTainted(store.value, in)) {
                    if (index.isDefined)
                        // Taint a known array index
                        definedBy.foldLeft(in) { (c, n) ⇒
                            c + ArrayElement(n, index.get)
                        }
                    else
                        // Taint the whole array if the index is unknown
                        definedBy.foldLeft(in) { (c, n) ⇒
                            c + Variable(n)
                        }
                } else if (index.isDefined && definedBy.size == 1)
                    // Untaint if possible
                    in - ArrayElement(definedBy.head, index.get)
                else in
            case _ ⇒ in
        }

    /**
     * Handles assignment statements. Propagates all incoming facts.
     * A new fact for the assigned variable will be created, if:
     * - The source expression is a tainted variable
     * - The source expression is a load operation of a tainted array
     * - The source expression is a load operation with a constant index from an array, in which this index is tainted
     *
     * @param statement The assignment
     * @param in The incoming facts
     * @return The incoming and the new facts.
     *
     * TODO Why don't we untaint the assigned variable?
     */
    def handleAssignment(statement: Statement, in: Set[Fact]): Set[Fact] = {
        val sourceExpr = statement.stmt.asAssignment.expr
        sourceExpr.astID match {
            case Var.ASTID ⇒
                val sourceDefinedBy = sourceExpr.asVar.definedBy
                in ++ in.collect {
                    case Variable(index) if sourceDefinedBy.contains(index) ⇒
                        Some(Variable(statement.index))
                    case ArrayElement(index, taintedElement) if sourceDefinedBy.contains(index) ⇒
                        Some(ArrayElement(statement.index, taintedElement))
                    case _ ⇒ None
                }.flatten
            case ArrayLoad.ASTID ⇒
                val loadExpr = sourceExpr.asArrayLoad
                val arrayDefinedBy = loadExpr.arrayRef.asVar.definedBy
                if (in.exists {
                    // One specific array element may be tainted
                    case ArrayElement(index, taintedElement) ⇒
                        val loadedIndex = getIntConstant(loadExpr.index, statement.code)
                        arrayDefinedBy.contains(index) &&
                            (loadedIndex.isEmpty || taintedElement == loadedIndex.get)
                    // Or the whole array
                    case Variable(index) ⇒ arrayDefinedBy.contains(index)
                    case _               ⇒ false
                })
                    in + Variable(statement.index)
                else
                    in
            case _ ⇒ in
        }
    }

    /**
     * Checks, if some expression always evaluates to the same int constant.
     *
     * @param expr The expression.
     * @param code The TAC code, which contains the expression.
     * @return Some int, if this analysis is sure that `expr` always evaluates to the same int constant, None otherwise.
     */
    def getIntConstant(expr: Expr[V], code: Array[Stmt[V]]): Option[Int] = {
        if (expr.isIntConst) Some(expr.asIntConst.value)
        else if (expr.isVar) {
            // TODO The following looks optimizable!
            val constVals = expr.asVar.definedBy.iterator
                .map[Option[Int]] { idx ⇒
                    if (idx >= 0) {
                        val stmt = code(idx)
                        if (stmt.astID == Assignment.ASTID && stmt.asAssignment.expr.isIntConst)
                            Some(stmt.asAssignment.expr.asIntConst.value)
                        else
                            None
                    } else None
                }
                .toIterable
            if (constVals.forall(option ⇒ option.isDefined && option.get == constVals.head.get))
                constVals.head
            else None
        } else None
    }

    /**
     * Checks, if the result of some variable expression could be tainted.
     *
     * @param expr The variable expression.
     * @param in The current data flow facts.
     * @return
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
     * Propagates tainted parameters to the callee. If a call to the sink method with a tainted parameter is detected, no
     * call-to-start edges will be created.
     */
    override def callFlow(statement: Statement, callee: DeclaredMethod, in: Set[Fact]): Set[Fact] = {
        val allParams = asCall(statement.stmt).receiverOption ++ asCall(statement.stmt).params
        if (callee.name == "sink") {
            if (in.exists {
                case Variable(index) ⇒ allParams.exists(p ⇒ p.asVar.definedBy.contains(index))
                case _               ⇒ false
            })
                println(s"Found flow to sink: $statement")
            Set.empty
            //TODO Why this condition?
        } else if ((callee.descriptor.returnType eq ObjectType.Class) ||
            (callee.descriptor.returnType eq ObjectType.Object)) {
            in.collect {

                // Taint formal parameter if actual parameter is tainted
                case Variable(index) ⇒
                    allParams.zipWithIndex.collect {
                        case (param, paramIndex) if param.asVar.definedBy.contains(index) ⇒
                            Variable(switchParamAndVariableIndex(paramIndex, !callee.definedMethod.isStatic))
                    }

                // Taint element of formal parameter if element of actual parameter is tainted
                case ArrayElement(index, taintedIndex) ⇒
                    allParams.zipWithIndex.collect {
                        case (param, paramIndex) if param.asVar.definedBy.contains(index) ⇒
                            ArrayElement(
                                switchParamAndVariableIndex(paramIndex, !callee.definedMethod.isStatic),
                                taintedIndex
                            )
                    }
            }.flatten
        } else Set.empty
    }

    /**
     * Propagates the taints. If the sink method was called with a tainted parameter, a FlowFact will be created to track
     * the call chain back.
     */
    override def callToReturnFlow(statement: Statement, succ: Statement, in: Set[Fact]): Set[Fact] = {
        val call = asCall(statement.stmt)
        if (call.name == "sink") {
            if (in.exists {
                case Variable(index) ⇒
                    asCall(statement.stmt).params.exists(p ⇒ p.asVar.definedBy.contains(index))
                case _ ⇒ false
            }) {
                in ++ Set(FlowFact(ListSet(statement.method)))
            } else {
                in
            }
        } else {
            in
        }
    }

    override def returnFlow(
        statement: Statement,
        callee:    DeclaredMethod,
        exit:      Statement,
        succ:      Statement,
        in:        Set[Fact]
    ): Set[Fact] = {

        /**
         * Checks whether the cllee's formal parameter is of a reference type.
         */
        def isRefTypeParam(index: Int): Boolean =
            if (index == -1) true
            else {
                callee.descriptor.parameterType(switchParamAndVariableIndex(index, isStaticMethod = false)).isReferenceType
            }

        if (callee.name == "source" && statement.stmt.astID == Assignment.ASTID)
            Set(Variable(statement.index))
        else {
            val allParams = (asCall(statement.stmt).receiverOption ++ asCall(statement.stmt).params).toSeq
            var flows: Set[Fact] = Set.empty
            for (fact ← in) {
                fact match {

                    // Taint actual parameter if formal parameter is tainted
                    case Variable(index) if index < 0 && index > -100 && isRefTypeParam(index) ⇒
                        val param =
                            allParams(switchParamAndVariableIndex(index, !callee.definedMethod.isStatic))
                        flows ++= param.asVar.definedBy.iterator.map(Variable)

                    // Taint element of actual parameter if element of formal parameter is tainted
                    case ArrayElement(index, taintedIndex) if index < 0 && index > -100 ⇒
                        val param =
                            allParams(switchParamAndVariableIndex(index, !callee.definedMethod.isStatic))
                        flows ++= param.asVar.definedBy.iterator.map(ArrayElement(_, taintedIndex))

                    // Taint field of actual parameter if field of formal parameter is tainted
                    case InstanceField(index, declClass, taintedField) if index < 0 && index > -10 ⇒
                        val param =
                            allParams(switchParamAndVariableIndex(index, !callee.definedMethod.isStatic))
                        flows ++= param.asVar.definedBy.iterator.map(InstanceField(_, declClass, taintedField))

                    // Track the call chain to the sink back
                    case FlowFact(flow) ⇒
                        flows += FlowFact(flow + statement.method)
                    case _ ⇒
                }
            }

            // Propagate taints of the return value
            if (exit.stmt.astID == ReturnValue.ASTID && statement.stmt.astID == Assignment.ASTID) {
                val returnValue = exit.stmt.asReturnValue.expr.asVar
                flows ++= in.collect {
                    case Variable(index) if returnValue.definedBy.contains(index) ⇒
                        Variable(statement.index)
                    case ArrayElement(index, taintedIndex) if returnValue.definedBy.contains(index) ⇒
                        ArrayElement(statement.index, taintedIndex)
                    case InstanceField(index, declClass, taintedField) if returnValue.definedBy.contains(index) ⇒
                        InstanceField(statement.index, declClass, taintedField)
                }
            }

            flows
        }
    }

    /**
     * Converts the index of a method's formal parameter to its variable index in the method's scope and vice versa.
     *
     * @param index The index of a formal parameter in the parameter list or of a variable.
     * @param isStaticMethod States, whether the method is static
     * @return A variable index if a parameter index was passed or a parameter index if a variable index was passed.
     */
    def switchParamAndVariableIndex(index: Int, isStaticMethod: Boolean): Int =
        (if (isStaticMethod) -1 else -2) - index
}

class Taint(val flows: Map[Statement, Set[Fact]]) extends IFDSProperty[Fact] {

    override type Self = Taint

    def key: PropertyKey[Taint] = Taint.key
}

object Taint extends IFDSPropertyMetaInformation[Fact] {

    override type Self = Taint

    val key: PropertyKey[Taint] = PropertyKey.create("Taint", new Taint(Map.empty))
}

object TaintAnalysis extends IFDSAnalysis[Fact] {

    override def init(p: SomeProject, ps: PropertyStore) = new TaintAnalysis()(p)

    override def property: IFDSPropertyMetaInformation[Fact] = Taint
}

object TaintAnalysisRunner {

    def main(args: Array[String]): Unit = {
        if (args.contains("--help")) {
            println("Potential parameters:")
            println(" -seq to use the SequentialPropertyStore")
            println(" -l2 to use the l2 domain instead of the default l1 domain")
            println(" -delay for a three seconds delay before the taint flow analysis is started")
        }
        val p = Project(
            new File(
                "DEVELOPING_OPAL/validate/target/scala-2.12/test-classes/org/opalj/fpcf/fixtures/taint/TaintAnalysisTest.class"
            )
        )
        p.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) ⇒ {
                implicit val lg: LogContext = p.logContext
                PropertyStore.updateDebug(false)
                if (args.contains("-seq"))
                    PKESequentialPropertyStore.apply(context: _*)
                else
                    ???
            }
        )
        val requirement =
            if (args.contains("-l2")) classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
            else classOf[l1.DefaultDomainWithCFGAndDefUse[_]]
        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey,
            (i: Option[Set[Class[_ <: AnyRef]]]) ⇒
                (i match {
                    case None               ⇒ Set(requirement)
                    case Some(requirements) ⇒ requirements + requirement
                }): Set[Class[_ <: AnyRef]]
        )
        val ps = p.get(PropertyStoreKey)
        val manager = p.get(FPCFAnalysesManagerKey)
        if (args.contains("-delay")) {
            Thread.sleep(3000)
        }
        val (_, analyses) =
            time {
                manager.runAll(LazyTACAIProvider, TaintAnalysis)
            } { t ⇒
                println(s"Time for taint-flow analysis: ${t.toSeconds}")
            }
        for {
            e ← analyses.collect { case (_, a: TaintAnalysis) ⇒ a.entryPoints }.head
            fact ← ps(e, TaintAnalysis.property.key).ub
                .asInstanceOf[IFDSProperty[Fact]]
                .flows
                .values
                .flatten
                .toSet[Fact]
        } {
            fact match {
                case FlowFact(flow) ⇒ println(s"flow: "+flow.map(_.toJava).mkString(", "))
                case _              ⇒
            }
        }
    }
}
