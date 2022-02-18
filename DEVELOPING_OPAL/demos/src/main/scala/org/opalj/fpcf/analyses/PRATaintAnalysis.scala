/* BSD 2-Clause License - see OPAL/LICENSE for details. */
/*package org.opalj
package fpcf
package analyses

import java.io.File

import scala.collection.immutable.ListSet
import scala.io.Source

import org.opalj.log.LogContext
import org.opalj.fpcf.seq.PKESequentialPropertyStore
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.Project.JavaClassFileReader
import org.opalj.ai.domain.l1
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.Var
import org.opalj.tac.ReturnValue
import org.opalj.tac.Call
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.AbstractIFDSAnalysis.V
import org.opalj.tac.fpcf.analyses.AbstractIFDSAnalysis
import org.opalj.tac.fpcf.analyses.Statement
import org.opalj.tac.fpcf.analyses.IFDSAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer

trait Fact
case object NullFact extends Fact
case class Variable(index: Int) extends Fact
case class FlowFact(flow: ListSet[Method]) extends Fact {
    override val hashCode: Int = {
        // HERE, a foldLeft introduces a lot of overhead due to (un)boxing.
        var r = 1
        flow.foreach(f ⇒ r = (r + f.hashCode()) * 31)
        r
    }
}
class FinalFact(flow: ListSet[Method]) extends FlowFact(flow)
object FinalFact {
    def unapply(arg: FinalFact): Some[ListSet[Method]] = Some(arg.flow)
}

/**
 * A simple IFDS taint analysis.
 *
 * @author Dominik Helm
 */
class PRATaintAnalysis private (
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
            case _ ⇒ in
        }

    /**
     * Returns true if the expression contains a taint.
     */
    def isTainted(expr: Expr[V], in: Set[Fact]): Boolean = {
        expr.isVar && in.exists {
            case Variable(index) ⇒ expr.asVar.definedBy.contains(index)
            case _               ⇒ false
        }
    }

    def handleAssignment(stmt: Statement, expr: Expr[V], in: Set[Fact]): Set[Fact] =
        expr.astID match {
            case Var.ASTID ⇒
                val newTaint = in.collect {
                    case Variable(index) if expr.asVar.definedBy.contains(index) ⇒
                        Some(Variable(stmt.index))
                    case _ ⇒ None
                }.flatten
                in ++ newTaint
            case _ ⇒ in
        }

    override def callFlow(
        stmt:   Statement,
        callee: DeclaredMethod,
        in:     Set[Fact]
    ): Set[Fact] = {
        val call = asCall(stmt.stmt)
        val allParams = call.receiverOption ++ asCall(stmt.stmt).params
        if (isSink(call)) {
            Set.empty
        } else {
            in.collect {
                case Variable(index) ⇒ // Taint formal parameter if actual parameter is tainted
                    allParams.zipWithIndex.collect {
                        case (param, pIndex) if param.asVar.definedBy.contains(index) ⇒
                            Variable(paramToIndex(pIndex, !callee.definedMethod.isStatic))
                    }
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

        val call = asCall(stmt.stmt)
        if (isSource(call) && stmt.stmt.astID == Assignment.ASTID)
            Set(Variable(stmt.index))
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

                    case FlowFact(flow) ⇒
                        val newFlow = flow + stmt.method
                        flows += FlowFact(newFlow)
                    case _ ⇒
                }
            }

            // Propagate taints of the return value
            if (exit.stmt.astID == ReturnValue.ASTID && stmt.stmt.astID == Assignment.ASTID) {
                val returnValue = exit.stmt.asReturnValue.expr.asVar
                flows ++= in.collect {
                    case Variable(index) if returnValue.definedBy.contains(index) ⇒
                        Variable(stmt.index)
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
        if (isSink(call)) {
            if (in.exists {
                case Variable(index) ⇒
                    asCall(stmt.stmt).params.exists(p ⇒ p.asVar.definedBy.contains(index))
                case _ ⇒ false
            }) {
                in ++ Set(FlowFact(ListSet(stmt.method)))
            } else {
                in
            }
        } else {
            in
        }
    }

    def isSource(call: Call[V]): Boolean = {
        PRATaintAnalysis.sources.get((call.name, call.descriptor)).exists(p.classHierarchy.isSubtypeOf(_, call.declaringClass))
    }

    def isSink(call: Call[V]): Boolean = {
        PRATaintAnalysis.sinks.get((call.name, call.descriptor)).exists(p.classHierarchy.isSubtypeOf(_, call.declaringClass))
    }

    val entryPoints: Map[DeclaredMethod, Fact] = (for {
        m ← p.allMethodsWithBody
    } yield declaredMethods(m) → NullFact).toMap

}

object PRATaintAnalysis extends IFDSAnalysis[Fact] {
    override def init(p: SomeProject, ps: PropertyStore) = new PRATaintAnalysis()(p)

    override def property: IFDSPropertyMetaInformation[Fact] = Taint

    var sources: Map[(String, MethodDescriptor), ObjectType] = _
    var sinks: Map[(String, MethodDescriptor), ObjectType] = _
}

class Taint(val flows: Map[Statement, Set[Fact]]) extends IFDSProperty[Fact] {

    override type Self = Taint

    def key: PropertyKey[Taint] = Taint.key
}

object Taint extends IFDSPropertyMetaInformation[Fact] {
    override type Self = Taint

    val key: PropertyKey[Taint] = PropertyKey.create(
        "PRATaint",
        new Taint(Map.empty)
    )
}

object PRATaintAnalysisRunner {

    def main(args: Array[String]): Unit = {

        val cp = new File(args(0)+"S")
        val sources = new File(args(1))
        val sinks = new File(args(2))

        val p = Project(
            JavaClassFileReader().ClassFiles(cp),
            JavaClassFileReader().ClassFiles(new File("/home/dominik/Desktop/android.jar")),
            libraryClassFilesAreInterfacesOnly = false,
            Traversable.empty
        )
        p.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) ⇒ {
                implicit val lg: LogContext = p.logContext
                val ps = PKESequentialPropertyStore.apply(context: _*)
                PropertyStore.updateDebug(false)
                ps
            }
        )
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey)(
            (i: Option[Set[Class[_ <: AnyRef]]]) ⇒ (i match {
                case None               ⇒ Set(classOf[l1.DefaultDomainWithCFGAndDefUse[_]])
                case Some(requirements) ⇒ requirements + classOf[l1.DefaultDomainWithCFGAndDefUse[_]]
            }): Set[Class[_ <: AnyRef]]
        )

        PRATaintAnalysis.sources = getList(sources)
        PRATaintAnalysis.sinks = getList(sinks)

        val ps = p.get(PropertyStoreKey)
        val manager = p.get(FPCFAnalysesManagerKey)
        val (_, analyses) =
            manager.runAll(LazyL0BaseAIAnalysis, TACAITransformer, PRATaintAnalysis)

        val entryPoints = analyses.collect { case (_, a: PRATaintAnalysis) ⇒ a.entryPoints }.head
        for {
            e ← entryPoints
            flows = ps(e, PRATaintAnalysis.property.key)
            fact ← flows.ub.asInstanceOf[IFDSProperty[Fact]].flows.values.flatten.toSet[Fact]
        } {
            fact match {
                case FlowFact(flow) ⇒ println(s"flow: "+flow.map(_.toJava).mkString(", "))
                case _              ⇒
            }
        }

    }

    def getList(file: File): Map[(String, MethodDescriptor), ObjectType] = {
        (
            for {
                line ← Source.fromFile(file).getLines()
                Array(declClass, returnType, signature, _) = line.split(' ')
                index = signature.indexOf("(")
                name = signature.substring(0, index)
                parameters = signature.substring(index + 1, signature.length - 1)
                jvmSignature = parameters.split(',').map(toJVMType).mkString("(", "", ")"+toJVMType(returnType))
                descriptor = MethodDescriptor(jvmSignature)
            } yield (name, descriptor) → ObjectType(declClass.replace('.', '/'))
        ).toMap
    }

    def toJVMType(javaType: String): String = {
        val trimmedType = javaType.trim
        if (trimmedType.endsWith("[]")) "["+toJVMType(trimmedType.substring(0, trimmedType.length - 2))
        else trimmedType match {
            case "void"    ⇒ "V"
            case "byte"    ⇒ "B"
            case "char"    ⇒ "C"
            case "double"  ⇒ "D"
            case "float"   ⇒ "F"
            case "int"     ⇒ "I"
            case "long"    ⇒ "J"
            case "short"   ⇒ "S"
            case "boolean" ⇒ "Z"
            case _         ⇒ "L"+trimmedType.replace('.', '/')+";"
        }
    }
}
*/ 