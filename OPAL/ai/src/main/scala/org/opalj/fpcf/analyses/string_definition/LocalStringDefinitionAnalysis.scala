/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.properties.StringConstancyLevel._
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.ComputationSpecification
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst
import org.opalj.tac.VirtualFunctionCall

import scala.collection.mutable.ArrayBuffer

class StringTrackingAnalysisContext(
    val stmts: Array[Stmt[V]]
)

/**
 * LocalStringDefinitionAnalysis processes a read operation of a local string variable at a program
 * position, ''pp'', in a way that it finds the set of possible strings that can be read at ''pp''.
 *
 * "Local" as this analysis takes into account only the enclosing function as a context. Values
 * coming from other functions are regarded as dynamic values even if the function returns a
 * constant string value. [[StringConstancyProperty]] models this by inserting "*" into the set of
 * possible strings.
 *
 * StringConstancyProperty might contain more than one possible string, e.g., if the source of the
 * value is an array.
 *
 * @author Patrick Mell
 */
class LocalStringDefinitionAnalysis(
        val project: SomeProject
) extends FPCFAnalysis {

    def analyze(
        data: P
    ): PropertyComputationResult = {
        // TODO: What is a better way to test if the given DUVar is of a certain type?
        val simpleClassName = data._1.value.getClass.getSimpleName
        simpleClassName match {
            case "StringValue"             ⇒ processStringValue(data)
            case "MultipleReferenceValues" ⇒ processMultipleDefSites(data)
            case "SObjectValue"            ⇒ processSObjectValue(data)
            case _ ⇒ throw new IllegalArgumentException(
                s"cannot process given UVar type ($simpleClassName)"
            )
        }
    }

    /**
     * Processes the case that the UVar is a string value.
     */
    private def processStringValue(data: P): PropertyComputationResult = {
        val tacProvider = p.get(SimpleTACAIKey)
        val methodStmts = tacProvider(data._2).stmts

        val assignedValues = ArrayBuffer[String]()
        val level = CONSTANT

        val defSites = data._1.definedBy
        defSites.filter(_ >= 0).foreach(defSite ⇒ {
            val Assignment(_, _, expr) = methodStmts(defSite)
            expr match {
                case s: StringConst ⇒
                    assignedValues += s.value
                // TODO: Non-constant strings are not taken into consideration; problem?
                case _ ⇒
            }
        })

        Result(data, StringConstancyProperty(level, assignedValues))
    }

    /**
     * Processes the case that a UVar has multiple definition sites.
     */
    private def processMultipleDefSites(data: P): PropertyComputationResult = {
        // TODO: To be implemented
        NoResult
    }

    /**
     * Processes the case that the UVar is of type `SObjectValue`.
     */
    private def processSObjectValue(data: P): PropertyComputationResult = {
        val tacProvider = p.get(SimpleTACAIKey)
        val stmts = tacProvider(data._2).stmts

        val defSite = data._1.definedBy.filter(_ >= 0)
        // TODO: Consider case for more than one defSite? Example for that?
        val expr = stmts(defSite.head).asAssignment.expr
        expr match {
            case _: NonVirtualFunctionCall[V] ⇒
                // Local analysis => no processing
                Result(data, StringConstancyProperty(DYNAMIC, ArrayBuffer("*")))

            case VirtualFunctionCall(_, _, _, _, _, receiver, _) ⇒
                val intermResult = processVirtualFuncCall(stmts, receiver)
                Result(data, StringConstancyProperty(intermResult._1, intermResult._2))

            case ArrayLoad(_, _, arrRef) ⇒
                // For assignments which use arrays, determine all possible values
                val arrDecl = stmts(arrRef.asVar.definedBy.head)
                val arrValues = arrDecl.asAssignment.targetVar.usedBy.filter {
                    stmts(_).isInstanceOf[ArrayStore[V]]
                } map { f: Int ⇒
                    val defSite = stmts(f).asArrayStore.value.asVar.definedBy.head
                    stmts(defSite).asAssignment.expr.asStringConst.value
                }
                Result(data, StringConstancyProperty(CONSTANT, arrValues.to[ArrayBuffer]))
        }
    }

    /**
     * Processes the case that a function call is involved, e.g., to StringBuilder#append.
     *
     * @param stmts    The surrounding context. For this analysis, the surrounding method.
     * @param receiver Receiving object of the VirtualFunctionCall.
     * @return Returns a tuple with the constancy level and the string value after the function
     *         call.
     */
    private def processVirtualFuncCall(
        stmts: Array[Stmt[V]], receiver: Expr[V]
    ): (StringConstancyLevel, ArrayBuffer[String]) = {
        var level = CONSTANT

        // TODO: Are these long concatenations the best / most robust way?
        val appendCall =
            stmts(receiver.asVar.definedBy.head).asAssignment.expr.asVirtualFunctionCall

        // Get previous value of string builder
        val baseAssignment = stmts(appendCall.receiver.asVar.definedBy.head).asAssignment
        val baseStr = valueOfAppendCall(baseAssignment.expr.asVirtualFunctionCall, stmts)
        var assignedStr = baseStr._1
        // Get appended value and build the new string value
        val appendData = valueOfAppendCall(appendCall, stmts)
        if (appendData._2 == CONSTANT) {
            assignedStr += appendData._1
        } else {
            assignedStr += "*"
            level = PARTIALLY_CONSTANT
        }
        Tuple2(level, ArrayBuffer(assignedStr))
    }

    /**
     * Determines the string value that was passed to a `StringBuilder#append` method. This function
     * can process string constants as well as function calls as argument to append.
     *
     * @param call  A function call of `StringBuilder#append`. Note that for all other methods an
     *              [[IllegalArgumentException]] will be thrown.
     * @param stmts The surrounding context. For this analysis, the surrounding method.
     * @return For constants strings as arguments, this function returns the string value and the
     *         level [[CONSTANT]]. For function calls "*" (to indicate ''any
     *         value'') and [[DYNAMIC]].
     */
    private def valueOfAppendCall(
        call: VirtualFunctionCall[V], stmts: Array[Stmt[V]]
    ): (String, Value) = {
        // TODO: Check the base object as well
        if (call.name != "append") {
            throw new IllegalArgumentException("can only process StringBuilder#append calls")
        }

        val defAssignment = call.params.head.asVar.definedBy.head
        val assignExpr = stmts(defAssignment).asAssignment.expr
        assignExpr match {
            case _: NonVirtualFunctionCall[V] ⇒ Tuple2("*", DYNAMIC)
            case StringConst(_, value)        ⇒ (value, CONSTANT)
        }
    }

}

sealed trait LocalStringDefinitionAnalysisScheduler extends ComputationSpecification {

    final override def derives: Set[PropertyKind] = Set(StringConstancyProperty)

    final override def uses: Set[PropertyKind] = { Set() }

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

/**
 * Executor for the lazy analysis.
 */
object LazyStringDefinitionAnalysis
        extends LocalStringDefinitionAnalysisScheduler
        with FPCFLazyAnalysisScheduler {

    final override def startLazily(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): FPCFAnalysis = {
        val analysis = new LocalStringDefinitionAnalysis(p)
        ps.registerLazyPropertyComputation(StringConstancyProperty.key, analysis.analyze)
        analysis
    }

}
