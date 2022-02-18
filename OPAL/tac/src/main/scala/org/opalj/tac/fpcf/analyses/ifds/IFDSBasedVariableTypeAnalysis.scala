/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import scala.annotation.tailrec

import java.io.File
import java.io.PrintWriter

import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyBounds
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.ReferenceType
import org.opalj.br.ArrayType
import org.opalj.br.ClassFile
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.Expr
import org.opalj.tac.GetField
import org.opalj.tac.GetStatic
import org.opalj.tac.New
import org.opalj.tac.ReturnValue
import org.opalj.tac.Var
import org.opalj.tac.fpcf.properties.TACAI

trait VTAFact extends SubsumableFact
case object VTANullFact extends VTAFact with SubsumableNullFact

/**
 * A possible run time type of a variable.
 *
 * @param definedBy The variable's definition site.
 * @param t The variable's type.
 * @param upperBound True, if the variable's type could also be every subtype of `t`.
 */
case class VariableType(definedBy: Int, t: ReferenceType, upperBound: Boolean) extends VTAFact {

    /**
     * If this VariableType is an upper bound, it subsumes every subtype.
     */
    override def subsumes(other: AbstractIFDSFact, project: SomeProject): Boolean = {
        if (upperBound) other match {
            case VariableType(definedByOther, tOther, _) if definedBy == definedByOther && t.isObjectType && tOther.isObjectType ⇒
                project.classHierarchy.isSubtypeOf(tOther.asObjectType, t.asObjectType)
            case _ ⇒ false
        }
        else false
    }
}

/**
 * A possible run time type of the receiver of a call.
 *
 * @param line The line of the call.
 * @param t The callee's type.
 * @param upperBound True, if the callee's type could also be every subtype of `t`.
 */
case class CalleeType(line: Int, t: ReferenceType, upperBound: Boolean) extends VTAFact {

    /**
     * If this CalleeType is an upper bound, it subsumes every subtype.
     */
    override def subsumes(other: AbstractIFDSFact, project: SomeProject): Boolean = {
        if (upperBound) other match {
            case CalleeType(lineOther, tOther, _) if line == lineOther && t.isObjectType && tOther.isObjectType ⇒
                tOther.asObjectType.isSubtypeOf(t.asObjectType)(project.classHierarchy)
            case _ ⇒ false
        }
        else false
    }
}

/**
 * A variable type analysis implemented as an IFDS analysis.
 * In contrast to an ordinary variable type analysis, which also determines types of fields,
 * this analysis only determines the types of local variables.
 * The subsuming traint can be mixed in to enable subsuming.
 *
 * @param project The analyzed project.
 * @author Mario Trageser
 */
class IFDSBasedVariableTypeAnalysis private (implicit val project: SomeProject)
    extends ForwardIFDSAnalysis[VTAFact] {

    override val propertyKey: IFDSPropertyMetaInformation[VTAFact] = VTAResult

    /**
     * The analysis starts with all public methods in java.lang or org.opalj.
     */
    override def entryPoints: Seq[(DeclaredMethod, VTAFact)] = {
        p.allProjectClassFiles.filter(classInsideAnalysisContext)
            .flatMap(classFile ⇒ classFile.methods)
            .filter(isEntryPoint).map(method ⇒ declaredMethods(method))
            .flatMap(entryPointsForMethod)
    }

    override protected def nullFact: VTAFact = VTANullFact

    override protected def createPropertyValue(result: Map[Statement, Set[VTAFact]]): IFDSProperty[VTAFact] =
        new VTAResult(result)

    /**
     * If a new object is instantiated and assigned to a variable or array, a new ValueType will be
     * created for the assignment's target.
     * If there is an assignment of a variable or array element, a new VariableType will be
     * created for the assignment's target with the source's type.
     * If there is a field read, a new VariableType will be created with the field's declared type.
     */
    override protected def normalFlow(statement: Statement, successor: Statement,
                                      in: Set[VTAFact]): Set[VTAFact] = {
        val stmt = statement.stmt
        stmt.astID match {
            case Assignment.ASTID ⇒
                // Add facts for the assigned variable.
                in ++ newFacts(statement.method, statement.stmt.asAssignment.expr,
                    statement.index, in)
            case ArrayStore.ASTID ⇒
                /*
                 * Add facts for the array store, like it was a variable assignment.
                 * By doing so, we only want to get the variable's type.
                 * Then, we change the definedBy-index to the one of the array and wrap the variable's
                 * type with an array type.
                 * Note, that an array type may have at most 255 dimensions.
                 */
                val flow = scala.collection.mutable.Set.empty[VTAFact]
                flow ++= in
                newFacts(statement.method, stmt.asArrayStore.value, statement.index,
                    in).foreach {
                    case VariableType(_, t, upperBound) if !(t.isArrayType && t.asArrayType.dimensions <= 254) ⇒
                        stmt.asArrayStore.arrayRef.asVar.definedBy
                            .foreach(flow += VariableType(_, ArrayType(t), upperBound))
                    case _ ⇒ // Nothing to do
                }
                flow.toSet
            // If the statement is neither an assignment, nor an array store, we just propagate our facts.
            case _ ⇒ in
        }
    }

    /**
     * For each variable, which can be passed as an argument to the call, a new VariableType is
     * created for the callee context.
     */
    override protected def callFlow(call: Statement, callee: DeclaredMethod, in: Set[VTAFact],
                                    source: (DeclaredMethod, VTAFact)): Set[VTAFact] = {
        val callObject = asCall(call.stmt)
        val allParams = callObject.allParams
        // Iterate over all input facts and over all parameters of the call.
        val flow = scala.collection.mutable.Set.empty[VTAFact]
        in.foreach {
            case VariableType(definedBy, t, upperBound) ⇒
                allParams.iterator.zipWithIndex.foreach {
                    /*
                     * We are only interested in a pair of a variable type and a parameter, if the
                     * variable and the parameter can refer to the same object.
                     */
                    case (parameter, parameterIndex) if parameter.asVar.definedBy.contains(definedBy) ⇒
                        // If this is the case, create a new fact for the method's formal parameter.
                        flow += VariableType(
                            AbstractIFDSAnalysis.switchParamAndVariableIndex(parameterIndex, callee.definedMethod.isStatic),
                            t, upperBound
                        )
                    case _ ⇒ // Nothing to do
                }
            case _ ⇒ // Nothing to do
        }
        flow.toSet
    }

    /**
     * If the call is an instance call, new CalleeTypes will be created for the call, one for each
     * VariableType, which could be the call's target.
     */
    override protected def callToReturnFlow(call: Statement, successor: Statement,
                                            in:     Set[VTAFact],
                                            source: (DeclaredMethod, VTAFact)): Set[VTAFact] = {
        // Check, to which variables the callee may refer
        val calleeDefinitionSites = asCall(call.stmt).receiverOption
            .map(callee ⇒ callee.asVar.definedBy).getOrElse(EmptyIntTrieSet)
        val calleeTypeFacts = in.collect {
            // If we know the variable's type, we also know on which type the call is performed.
            case VariableType(index, t, upperBound) if calleeDefinitionSites.contains(index) ⇒
                CalleeType(call.index, t, upperBound)
        }
        if (in.size >= calleeTypeFacts.size) in ++ calleeTypeFacts
        else calleeTypeFacts ++ in
    }

    /**
     * If the call returns a value which is assigned to a variable, a new VariableType will be
     * created in the caller context with the returned variable's type.
     */
    override protected def returnFlow(call: Statement, callee: DeclaredMethod, exit: Statement,
                                      successor: Statement, in: Set[VTAFact]): Set[VTAFact] =
        // We only create a new fact, if the call returns a value, which is assigned to a variable.
        if (exit.stmt.astID == ReturnValue.ASTID && call.stmt.astID == Assignment.ASTID) {
            val returnValue = exit.stmt.asReturnValue.expr.asVar
            in.collect {
                // If we know the type of the return value, we create a fact for the assigned variable.
                case VariableType(definedBy, t, upperBound) if returnValue.definedBy.contains(definedBy) ⇒
                    VariableType(call.index, t, upperBound)
            }
        } else Set.empty

    /**
     * If a method outside of the analysis context is called, we assume that it returns every
     * possible runtime type matching the compile time type.
     */
    override protected def callOutsideOfAnalysisContext(statement: Statement, callee: DeclaredMethod,
                                                        successor: Statement,
                                                        in:        Set[VTAFact]): Set[VTAFact] = {
        val returnType = callee.descriptor.returnType
        if (statement.stmt.astID == Assignment.ASTID && returnType.isReferenceType) {
            Set(VariableType(statement.index, returnType.asReferenceType, upperBound = true))
        } else Set.empty
    }

    /**
     * Only methods in java.lang and org.opalj are inside the analysis context.
     *
     * @param callee The callee.
     * @return True, if the callee is inside the analysis context.
     */
    override protected def insideAnalysisContext(callee: DeclaredMethod): Boolean =
        classInsideAnalysisContext(callee.definedMethod.classFile) &&
            super.insideAnalysisContext(callee)

    /**
     * Checks, if a class is inside the analysis context.
     * By default, that are the packages java.lang and org.opalj.
     *
     * @param classFile The class, which is checked.
     * @return True, if the class is inside the analysis context.
     */
    private def classInsideAnalysisContext(classFile: ClassFile): Boolean = {
        val fqn = classFile.fqn
        fqn.startsWith("java/lang") || fqn.startsWith("org/opalj/fpcf/fixtures/vta")
    }

    /**
     * Checks, if a method is an entry point of this analysis.
     *
     * @param method The method to be checked.
     * @return True, if this method is an entry point of the analysis.
     */
    private def isEntryPoint(method: Method): Boolean = {
        val declaredMethod = declaredMethods(method)
        method.body.isDefined && canBeCalledFromOutside(declaredMethod)
    }

    /**
     * For an entry point method, this method computes all pairs (`method`, inputFact) where
     * inputFact is a VariableType for one of the method's parameter with its compile time type as
     * an upper bound.
     *
     * @param method The entry point method.
     *
     * @return All pairs (`method`, inputFact) where inputFact is a VariableType for one of the
     *         method's parameter with its compile time type as an upper bound.
     */
    private def entryPointsForMethod(method: DeclaredMethod): Seq[(DeclaredMethod, VTAFact)] = {
        // Iterate over all parameters, which have a reference type.
        (method.descriptor.parameterTypes.zipWithIndex.collect {
            case (t, index) if t.isReferenceType ⇒
                /*
                 * Create a fact for the parameter, which says, that the parameter may have any
                 * subtype of its compile time type.
                 */
                VariableType(
                    AbstractIFDSAnalysis.switchParamAndVariableIndex(index, method.definedMethod.isStatic),
                    t.asReferenceType, upperBound = true
                )
            /*
         * In IFDS problems, we must always also analyze the null fact, because it creates the facts,
         * which hold independently of other source facts.
         * Map the input facts, in which we are interested, to a pair of the method and the fact.
         */
        } :+ VTANullFact).map(fact ⇒ (method, fact))
    }

    /**
     * When `normalFlow` reaches an assignment or array store, this method computes the new facts
     * created by the statement.
     *
     * @param expression The source expression of the assignment or array store.
     * @param statementIndex The statement's index.
     * @param in The facts, which hold before the statement.
     * @return The new facts created by the statement.
     */
    private def newFacts(method: Method, expression: Expr[DUVar[ValueInformation]],
                         statementIndex: Int,
                         in:             Set[VTAFact]): Iterator[VariableType] = expression.astID match {
        case New.ASTID ⇒ in.iterator.collect {
            // When a constructor is called, we always know the exact type.
            case VTANullFact ⇒
                VariableType(statementIndex, expression.asNew.tpe, upperBound = false)
        }
        case Var.ASTID ⇒ in.iterator.collect {
            // When we know the source type, we also know the type of the assigned variable.
            case VariableType(index, t, upperBound) if expression.asVar.definedBy.contains(index) ⇒
                VariableType(statementIndex, t, upperBound)
        }
        case ArrayLoad.ASTID ⇒ in.iterator.collect {
            // When we know the array's type, we also know the type of the loaded element.
            case VariableType(index, t, upperBound) if isArrayOfObjectType(t) &&
                expression.asArrayLoad.arrayRef.asVar.definedBy.contains(index) ⇒
                VariableType(statementIndex, t.asArrayType.elementType.asReferenceType, upperBound)
        }
        case GetField.ASTID | GetStatic.ASTID ⇒
            val t = expression.asFieldRead.declaredFieldType
            /*
             * We do not track field types. So we must assume, that it contains any subtype of its
             * compile time type.
             */
            if (t.isReferenceType)
                Iterator(VariableType(statementIndex, t.asReferenceType, upperBound = true))
            else Iterator.empty
        case _ ⇒ Iterator.empty
    }

    /**
     * Checks, if some type is an array type containing an object type.
     *
     * @param t The type to be checked.
     * @param includeObjectType If true, this method also returns true if `t` is an object type
     *                          itself.
     *
     * @return True, if `t` is an array type of an object type.
     */
    @tailrec private def isArrayOfObjectType(t: FieldType, includeObjectType: Boolean = false): Boolean = {
        if (t.isArrayType) isArrayOfObjectType(t.asArrayType.elementType, includeObjectType = true)
        else if (t.isObjectType && includeObjectType) true
        else false
    }
}

object IFDSBasedVariableTypeAnalysis extends IFDSAnalysis[VTAFact] {

    var SUBSUMING: Boolean = true

    override val uses: Set[PropertyBounds] =
        Set(PropertyBounds.finalP(TACAI), PropertyBounds.finalP(Callers))

    override def init(p: SomeProject, ps: PropertyStore): IFDSBasedVariableTypeAnalysis = {
        if (SUBSUMING) new IFDSBasedVariableTypeAnalysis()(p) with Subsuming[VTAFact]
        else new IFDSBasedVariableTypeAnalysis()(p)
    }

    override def property: IFDSPropertyMetaInformation[VTAFact] = VTAResult
}

/**
 * The IFDSProperty for this analysis.
 */
case class VTAResult(flows: Map[Statement, Set[VTAFact]]) extends IFDSProperty[VTAFact] {

    override type Self = VTAResult

    override def key: PropertyKey[VTAResult] = VTAResult.key
}

object VTAResult extends IFDSPropertyMetaInformation[VTAFact] {

    override type Self = VTAResult

    val key: PropertyKey[VTAResult] = PropertyKey.create("VTA", new VTAResult(Map.empty))
}

class IFDSBasedVariableTypeAnalysisRunner extends AbsractIFDSAnalysisRunner {

    override def analysisClass: IFDSBasedVariableTypeAnalysis.type = IFDSBasedVariableTypeAnalysis

    override def printAnalysisResults(analysis: AbstractIFDSAnalysis[_], ps: PropertyStore): Unit = {}

    override protected def additionalEvaluationResult(analysis: AbstractIFDSAnalysis[_]): Option[Object] =
        analysis match {
            case subsuming: Subsuming[_] ⇒ Some(subsuming.numberOfSubsumptions)
            case _                       ⇒ None
        }

    override protected def writeAdditionalEvaluationResultsToFile(
        writer:                      PrintWriter,
        additionalEvaluationResults: Seq[Object]
    ): Unit = {
        val numberOfSubsumptions = additionalEvaluationResults.map(_.asInstanceOf[NumberOfSubsumptions])
        val length = additionalEvaluationResults.length
        val tries = numberOfSubsumptions.map(_.triesToSubsume).sum / length
        val successes = numberOfSubsumptions.map(_.successfulSubsumes).sum / length
        writer.println(s"Average tries to subsume: $tries")
        writer.println(s"Average successful subsumes: $successes")
    }
}

object IFDSBasedVariableTypeAnalysisRunner {
    def main(args: Array[String]): Unit = {
        if (args.contains("--help")) {
            println("Potential parameters:")
            println(" -seq (to use the SequentialPropertyStore)")
            println(" -l2 (to use the l2 domain instead of the default l1 domain)")
            println(" -delay (for a three seconds delay before the taint flow analysis is started)")
            println(" -debug (for debugging mode in the property store)")
            println(" -evalSchedulingStrategies (evaluates all available scheduling strategies)")
            println(" -f <file> (Stores the average runtime to this file)")
        } else {
            val fileIndex = args.indexOf("-f")
            new IFDSBasedVariableTypeAnalysisRunner().run(
                args.contains("-debug"),
                args.contains("-l2"),
                args.contains("-delay"),
                args.contains("-evalSchedulingStrategies"),
                if (fileIndex >= 0) Some(new File(args(fileIndex + 1))) else None
            )
        }
    }
}
