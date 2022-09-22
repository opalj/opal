/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint.summaries

import org.opalj.br.{ArrayType, BooleanType, ByteType, CharType, DoubleType, FieldType, FloatType, IntegerType, LongType, Method, ObjectType, ShortType, Type, VoidType}
import org.opalj.tac.Call
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V
import org.opalj.tac.fpcf.analyses.ifds.{JavaIFDSProblem, JavaStatement}
import org.opalj.tac.fpcf.analyses.ifds.taint.{ArrayElement, InstanceField, TaintFact, TaintNullFact, Variable}
import org.opalj.tac.fpcf.analyses.ifds.taint.summaries.Flow.nodeToTaint
import org.opalj.tac.fpcf.analyses.ifds.taint.summaries.ClassSummary.{signaturePattern, stringToFieldType, stringToType}

import java.io.File
import scala.util.matching.Regex
import scala.xml.{Node, XML}

/**
 * This file implements a subset of the StubDroid summary specification.
 *
 * Not implemented (yet):
 * - Summaries for inner classes
 * - Type Checking
 * - Metadata/Exclusiveness
 * - Aliasing (depends on the IFDS analysis)
 * - Access Paths with k > 1 (again, depends on the IFS analysis)
 */

/**
 * Holds summaries for all classes.
 *
 * @param files summary XML files
 */
case class TaintSummaries(files: List[File]) {
    val summaries: Map[String, ClassSummary] =
        (files.map(f => f.getName.replace(".xml", "").replace(".", "/"))
            zip
            files.map(f => new ClassSummary(XML.loadFile(f)))).toMap

    private def isSummarized(objType: ObjectType): Boolean =
        summaries.contains(objType.fqn)

    /**
     * Return true if the method is summarized.
     *
     * @param callStmt call site in TACAI
     * @return whether the method is summarized.
     */
    def isSummarized(callStmt: Call[JavaIFDSProblem.V]): Boolean =
        isSummarized(callStmt.declaringClass.mostPreciseObjectType)

    /**
     * Return true if the method is summarized.
     *
     * @param method callee
     * @return whether the method is summarized.
     */
    def isSummarized(method: Method): Boolean =
        isSummarized(method.classFile.thisType)

    /**
     * Applies the summary if available, else does nothing.
     * Precondition: callee class is summarized.
     *
     * @param call     call java statement
     * @param callStmt call TAC statement
     * @param in       fact
     * @return out set of facts
     */
    def compute(call: JavaStatement, callStmt: Call[V], in: TaintFact): Set[TaintFact] = {
        val classFqn = callStmt.declaringClass.mostPreciseObjectType.fqn
        assert(summaries.contains(classFqn))

        summaries(classFqn).compute(call, callStmt, in)
    }
}

/**
 * Holds the methods summaries for a single class.
 *
 * @param summaryNode 'summary' XML node
 */
case class ClassSummary(summaryNode: Node) {
    /* Method summaries. */
    def methods: Seq[MethodSummary] = (summaryNode \\ "methods" \\ "method")
        .map(methodNode => {
            val sig = (methodNode \@ "id")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
            signaturePattern.findFirstMatchIn(sig) match {
                case Some(m) =>
                    val paramTypes: List[FieldType] =
                        if (m.group(3) == "") List()
                        else m.group(3)
                            .split(",")
                            .map(s => stringToFieldType(s.strip()))
                            .toList
                    MethodSummary(
                        stringToType(m.group(1)),
                        m.group(2),
                        paramTypes,
                        methodNode
                    )
                case None =>
                    throw new RuntimeException("couldn't parse the signature of "+sig);
            }
        }).toList

    /**
     * Applies the summary if available, else does perform the identity.
     *
     * @param call call java statement
     * @param callStmt call TAC statement
     * @param in fact
     * @return out set of facts
     */
    def compute(call: JavaStatement, callStmt: Call[V], in: TaintFact): Set[TaintFact] = {
        in match {
            /* We do not have any summaries for null facts. */
            case TaintNullFact => Set(in)
            case _ =>
                /* Find the right method, even when overloaded. */
                val methodSummary = methods.find(m => m.methodName == callStmt.name
                    && m.paramTypes
                    .zip(callStmt.descriptor.parameterTypes)
                    .forall(t => t._1 == t._2))
                methodSummary match {
                    case Some(methodSummary) => methodSummary.compute(call, callStmt, in) + in
                    case None                => Set(in)
                }
        }
    }
}

object ClassSummary {
    /* group 1 = return type, group 2 = method name, group 3 = param list */
    val signaturePattern: Regex = """([a-zA-Z.\[\]]*?) ([a-zA-Z0-9_<>]*?)\(([a-zA-Z.\[\],\s]*?)\)""".r

    /**
     * Converts the type string in Soot's format to the OPAL ObjectType, excluding void.
     *
     * @param str type string
     * @return ObjectType
     */
    def stringToFieldType(str: String): FieldType = {
        str match {
            case "boolean" => BooleanType
            case "byte"    => ByteType
            case "char"    => CharType
            case "short"   => ShortType
            case "int"     => IntegerType
            case "long"    => LongType
            case "float"   => FloatType
            case "double"  => DoubleType
            case "String"  => ObjectType.String
            case _ =>
                if (str.endsWith("[]"))
                    ArrayType(stringToFieldType(str.substring(0, str.length - 2)))
                else
                    ObjectType(str.replace(".", "/"))
        }
    }

    /**
     * Converts the type string in Soot's format to the OPAL ObjectType, including void.
     *
     * @param str type string
     * @return ObjectType
     */
    def stringToType(str: String): Type = {
        str match {
            case "void" => VoidType
            case _      => stringToFieldType(str)
        }
    }
}

/**
 * Represents the summary of a single method.
 *
 * @param returnType return type
 * @param methodName method name
 * @param paramTypes parameter types
 * @param methodNode 'method' XML node
 */
case class MethodSummary(returnType: Type, methodName: String, paramTypes: List[FieldType], methodNode: Node) {
    def flows: Seq[Flow] = methodNode.head.map(flowNode => Flow(flowNode))

    def compute(call: JavaStatement, callStmt: Call[V], in: TaintFact): Set[TaintFact] = {
        flows.flatMap(f =>
            if (f.matchesFrom(callStmt, in))
                f.createTaint(call, callStmt, in)
            else
                Set.empty).toSet
    }

    override def toString: String = s"${returnType.toString} ${methodName}(${paramTypes.mkString(", ")})"
}

/**
 * Represents one summarized flow.
 *
 * @param flowNode 'flow' XML node
 */
case class Flow(flowNode: Node) {
    // only needed if we'd use a on-demand alias analysis
    val isAlias: Boolean = (flowNode \@ "isAlias").equals("true")
    // TODO: how important is type checking?
    val typeChecking: Boolean = (flowNode \@ "typeChecking").equals("true")
    val from: SummaryTaint = nodeToTaint((flowNode \\ "from").head)
    val to: SummaryTaint = nodeToTaint((flowNode \\ "to").head)

    /**
     * Return true if the taint matches the 'from' rule.
     *
     * @param callStmt call TAC statement
     * @param in fact
     * @return Boolean
     */
    def matchesFrom(callStmt: Call[V], in: TaintFact): Boolean = {
        val isStatic = callStmt.receiverOption.isEmpty
        val allParamsWithIndex = callStmt.allParams.zipWithIndex

        from match {
            case ParameterSummaryTaint(summaryIndex) =>
                in match {
                    case Variable(index) =>
                        JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index, isStatic) == summaryIndex
                    /* Summaries do not know array elements. */
                    case ArrayElement(index, _) =>
                        JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index, isStatic) == summaryIndex
                    case InstanceField(index, _, fieldName) =>
                        JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index, isStatic) == summaryIndex
                    case _ => false
                }
            case ParameterSummaryTaintWithField(summaryIndex, summaryFieldName) =>
                in match {
                    case Variable(index) =>
                        JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index, isStatic) == summaryIndex
                    /* Summaries do not know array elements. */
                    case ArrayElement(index, _) =>
                        JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index, isStatic) == summaryIndex
                    case InstanceField(index, _, fieldName) =>
                        (JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index, isStatic) == summaryIndex
                            && summaryFieldName == fieldName)
                    case _ => false
                }
            case BaseObjectSummaryTaint(summaryFieldName) =>
                in match {
                    case InstanceField(index, _, fieldName) =>
                        (JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index, isStatic) == -1
                            && summaryFieldName == fieldName)
                    /* Over-approximation: apply the rule even though if no field is known. */
                    case Variable(index) =>
                        JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index, isStatic) == -1
                    case _ => false
                }
            case _ => false
        }
    }

    /**
     * Return the resulting set of facts after applying the summary.
     *
     * @param call call java statement
     * @param callStmt call TAC statement
     * @param in fact
     * @return Out set of facts
     */
    def createTaint(call: JavaStatement, callStmt: Call[V], in: TaintFact): Set[TaintFact] = {
        to match {
            case ParameterSummaryTaint(summaryIndex) =>
                callStmt.params(summaryIndex).asVar.definedBy.map(i => Variable(i))
            case ParameterSummaryTaintWithField(summaryIndex, fieldName) =>
                callStmt.params(summaryIndex).asVar.definedBy.map(i =>
                    InstanceField(i, callStmt.declaringClass.mostPreciseObjectType, fieldName))
            case BaseObjectSummaryTaint(fieldName) if callStmt.receiverOption.isDefined =>
                callStmt.receiverOption.get.asVar.definedBy.map(i =>
                    InstanceField(i, callStmt.declaringClass.mostPreciseObjectType, fieldName))
            case ReturnSummaryTaint() if call.stmt.isAssignment =>
                Set(Variable(call.index))
            case ReturnSummaryTaintWithField(fieldName) if call.stmt.isAssignment =>
                Set(InstanceField(call.index, callStmt.declaringClass.mostPreciseObjectType, fieldName))
            case _ => Set.empty
        }
    }
}

object Flow {
    val firstFieldPattern: Regex = """\[[A-Za-z\.\[\]]*?: [A-Za-z\.\[\]]*? ([a-zA-z]*?)[,\]]""".r

    private def getFieldNameFromAttribute(attr: String): String = {
        firstFieldPattern.findFirstMatchIn(attr) match {
            case Some(m) => m.group(1)
            case None    => throw new RuntimeException("Failed to parse Access Path: "+attr)
        }
    }

    /**
     * Maps a 'from' or 'to' XML node to an SummaryTaint
     *
     * @param node 'from' or 'to' node
     * @return SummaryTaint
     */
    def nodeToTaint(node: Node): SummaryTaint = {
        (node \@ "sourceSinkType", node \@ "AccessPath") match {
            case ("Parameter", "") =>
                ParameterSummaryTaint((node \@ "ParameterIndex").toInt - 2)
            case ("Parameter", attr) =>
                ParameterSummaryTaintWithField(
                    (node \@ "ParameterIndex").toInt - 2,
                    getFieldNameFromAttribute(attr)
                )
            case ("Field", attr)  => BaseObjectSummaryTaint(getFieldNameFromAttribute(attr))
            case ("Return", "")   => ReturnSummaryTaint()
            case ("Return", attr) => ReturnSummaryTaintWithField(getFieldNameFromAttribute(attr))
        }
    }
}

trait SummaryTaint
case class ParameterSummaryTaint(index: Int) extends SummaryTaint
case class ParameterSummaryTaintWithField(index: Int, fieldName: String) extends SummaryTaint
case class BaseObjectSummaryTaint(fieldName: String) extends SummaryTaint
case class ReturnSummaryTaint() extends SummaryTaint
case class ReturnSummaryTaintWithField(fieldName: String) extends SummaryTaint
