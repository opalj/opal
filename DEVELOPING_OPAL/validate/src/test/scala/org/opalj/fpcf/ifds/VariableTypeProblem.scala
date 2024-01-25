/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ifds

import scala.annotation.tailrec

import org.opalj.br.ArrayType
import org.opalj.br.ClassFile
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.AbstractIFDSFact
import org.opalj.ifds.AbstractIFDSNullFact
import org.opalj.ifds.Callable
import org.opalj.ifds.Dependees.Getter
import org.opalj.ifds.IFDSFact
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
import org.opalj.tac.fpcf.analyses.ifds.JavaForwardIFDSProblem
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.value.ValueInformation

trait VTAFact extends AbstractIFDSFact
case object VTANullFact extends VTAFact with AbstractIFDSNullFact

/**
 * A possible run time type of a variable.
 *
 * @param definedBy The variable's definition site.
 * @param t The variable's type.
 * @param upperBound True, if the variable's type could also be every subtype of `t`.
 *
 * @author Marc Clement
 */
case class VariableType(definedBy: Int, t: ReferenceType, upperBound: Boolean) extends VTAFact {

    /**
     * If this VariableType is an upper bound, it subsumes every subtype.
     */
    override def subsumes(other: AbstractIFDSFact, project: SomeProject): Boolean = {
        if (upperBound) other match {
            case VariableType(definedByOther, tOther, _) if definedBy == definedByOther =>
                project.classHierarchy.isSubtypeOf(tOther, t)
            case _ => false
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
            case CalleeType(lineOther, tOther, _) if line == lineOther =>
                project.classHierarchy.isSubtypeOf(tOther, t)
            case _ => false
        }
        else false
    }
}

/**
 * IFDS Problem that performs a Variable Type Analysis
 *
 * @param project the analyzed project
 * @param subsumeFacts Whether to try to subsume new facts under existing facts and save graph edges
 *
 * @author Marc Clement
 */
class VariableTypeProblem(project: SomeProject, override val subsumeFacts: Boolean = false)
    extends JavaForwardIFDSProblem[VTAFact](project) {
    val propertyStore: PropertyStore = project.get(PropertyStoreKey)
    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    override def nullFact: VTAFact = VTANullFact

    override def entryPoints: Seq[(Method, IFDSFact[VTAFact, JavaStatement])] = {
        project.allProjectClassFiles.flatMap(cf =>
            if (classInsideAnalysisContext(cf)) {
                cf.methods.flatMap { m =>
                    if (isEntryPoint(m)) {
                        entryPointsForMethod(m)
                    } else {
                        Seq.empty
                    }
                }
            } else {
                Seq.empty
            }
        )
    }

    /**
     * If a new object is instantiated and assigned to a variable or array, a new ValueType will be
     * created for the assignment's target.
     * If there is an assignment of a variable or array element, a new VariableType will be
     * created for the assignment's target with the source's type.
     * If there is a field read, a new VariableType will be created with the field's declared type.
     */
    override def normalFlow(
        statement:   JavaStatement,
        in:          VTAFact,
        predecessor: Option[JavaStatement]
    ): Set[VTAFact] = {
        val inSet = Set(in)
        val stmt = statement.stmt
        stmt.astID match {
            case Assignment.ASTID =>
                // Add facts for the assigned variable.
                inSet ++ newFacts(statement.method, statement.stmt.asAssignment.expr, statement.index, in)
            case ArrayStore.ASTID =>
                /*
                 * Add facts for the array store, like it was a variable assignment.
                 * By doing so, we only want to get the variable's type.
                 * Then, we change the definedBy-index to the one of the array and wrap the variable's
                 * type with an array type.
                 * Note, that an array type may have at most 255 dimensions.
                 */
                val flow = scala.collection.mutable.Set.empty[VTAFact]
                flow ++= inSet
                newFacts(statement.method, stmt.asArrayStore.value, statement.index, in).foreach {
                    case VariableType(_, t, upperBound) if !(t.isArrayType && t.asArrayType.dimensions <= 254) =>
                        stmt.asArrayStore.arrayRef.asVar.definedBy
                            .foreach(flow += VariableType(_, ArrayType(t), upperBound))
                    case _ => // Nothing to do
                }
                flow.toSet
            // If the statement is neither an assignment, nor an array store, we just propagate our facts.
            case _ => inSet
        }
    }

    /**
     * For each variable, which can be passed as an argument to the call, a new VariableType is
     * created for the callee context.
     */
    override def callFlow(start: JavaStatement, in: VTAFact, call: JavaStatement, callee: Method): Set[VTAFact] = {
        val inSet = Set(in)
        val callObject = JavaIFDSProblem.asCall(call.stmt)
        val allParams = callObject.allParams
        // Iterate over all input facts and over all parameters of the call.
        val flow = scala.collection.mutable.Set.empty[VTAFact]
        inSet.foreach {
            case VariableType(definedBy, t, upperBound) =>
                allParams.iterator.zipWithIndex.foreach {
                    /*
                     * We are only interested in a pair of a variable type and a parameter, if the
                     * variable and the parameter can refer to the same object.
                     */
                    case (parameter, parameterIndex) if parameter.asVar.definedBy.contains(definedBy) =>
                        // If this is the case, create a new fact for the method's formal parameter.
                        flow += VariableType(
                            JavaIFDSProblem
                                .remapParamAndVariableIndex(parameterIndex, callee.isStatic),
                            t,
                            upperBound
                        )
                    case _ => // Nothing to do
                }
            case _ => // Nothing to do
        }
        flow.toSet
    }

    /**
     * If the call is an instance call, new CalleeTypes will be created for the call, one for each
     * VariableType, which could be the call's target.
     */
    override def callToReturnFlow(
        call:         JavaStatement,
        in:           VTAFact,
        successor:    Option[JavaStatement],
        unbCallChain: Seq[Callable]
    ): Set[VTAFact] = {
        val inSet = Set(in)
        // Check, to which variables the callee may refer
        val calleeDefinitionSites = JavaIFDSProblem.asCall(call.stmt).receiverOption
            .map(callee => callee.asVar.definedBy)
            .getOrElse(EmptyIntTrieSet)
        val calleeTypeFacts = inSet.collect {
            // If we know the variable's type, we also know on which type the call is performed.
            case VariableType(index, t, upperBound) if calleeDefinitionSites.contains(index) =>
                CalleeType(call.index, t, upperBound)
        }
        if (inSet.size >= calleeTypeFacts.size) inSet ++ calleeTypeFacts
        else calleeTypeFacts ++ inSet
    }

    /**
     * If the call returns a value which is assigned to a variable, a new VariableType will be
     * created in the caller context with the returned variable's type.
     */
    override def returnFlow(
        exit:         JavaStatement,
        in:           VTAFact,
        call:         JavaStatement,
        successor:    Option[JavaStatement],
        unbCallChain: Seq[Callable]
    ): Set[VTAFact] =
        // We only create a new fact, if the call returns a value, which is assigned to a variable.
        if (exit.stmt.astID == ReturnValue.ASTID && call.stmt.astID == Assignment.ASTID) {
            val inSet = Set(in)
            val returnValue = exit.stmt.asReturnValue.expr.asVar
            inSet.collect {
                // If we know the type of the return value, we create a fact for the assigned variable.
                case VariableType(definedBy, t, upperBound) if returnValue.definedBy.contains(definedBy) =>
                    VariableType(call.index, t, upperBound)
            }
        } else Set.empty

    override def createFlowFactAtExit(callee: Method, in: VTAFact, unbCallChain: Seq[Callable]): Option[VTAFact] = None

    /**
     * Only methods in java.lang and org.opalj are inside the analysis context.
     *
     * @param callee The callee.
     * @return True, if the callee is inside the analysis context.
     */
    override def outsideAnalysisContextCall(callee: Method): Option[OutsideAnalysisContextCallHandler] =
        if (classInsideAnalysisContext(callee.classFile) &&
            super.outsideAnalysisContextCall(callee).isEmpty
        )
            None
        else {
            Some((
                (
                    call:         JavaStatement,
                    successor:    Option[JavaStatement],
                    in:           VTAFact,
                    unbCallChain: Seq[Callable],
                    getter:       Getter
                ) => {
                    val returnType = callee.descriptor.returnType
                    if (call.stmt.astID == Assignment.ASTID && returnType.isReferenceType) {
                        Set(VariableType(call.index, returnType.asReferenceType, upperBound = true))
                    } else Set.empty[VTAFact]
                }
            ): OutsideAnalysisContextCallHandler)
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
    private def newFacts(
        method:         Method,
        expression:     Expr[DUVar[ValueInformation]],
        statementIndex: Int,
        in:             VTAFact
    ): Iterator[VariableType] = {
        val inSet = Set(in)
        expression.astID match {
            case New.ASTID =>
                inSet.iterator.collect {
                    // When a constructor is called, we always know the exact type.
                    case VTANullFact =>
                        VariableType(statementIndex, expression.asNew.tpe, upperBound = false)
                }
            case Var.ASTID =>
                inSet.iterator.collect {
                    // When we know the source type, we also know the type of the assigned variable.
                    case VariableType(index, t, upperBound) if expression.asVar.definedBy.contains(index) =>
                        VariableType(statementIndex, t, upperBound)
                }
            case ArrayLoad.ASTID =>
                inSet.iterator.collect {
                    // When we know the array's type, we also know the type of the loaded element.
                    case VariableType(index, t, upperBound)
                        if isArrayOfObjectType(t) &&
                            expression.asArrayLoad.arrayRef.asVar.definedBy.contains(index) =>
                        VariableType(statementIndex, t.asArrayType.elementType.asReferenceType, upperBound)
                }
            case GetField.ASTID | GetStatic.ASTID =>
                val t = expression.asFieldRead.declaredFieldType
                /*
                 * We do not track field types. So we must assume, that it contains any subtype of its
                 * compile time type.
                 */
                if (t.isReferenceType)
                    Iterator(VariableType(statementIndex, t.asReferenceType, upperBound = true))
                else Iterator.empty
            case _ => Iterator.empty
        }
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
    @tailrec private def isArrayOfObjectType(
        t:                 FieldType,
        includeObjectType: Boolean = false
    ): Boolean = {
        if (t.isArrayType) isArrayOfObjectType(t.asArrayType.elementType, includeObjectType = true)
        else if (t.isObjectType && includeObjectType) true
        else false
    }

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
        method.body.isDefined && canBeCalledFromOutside(method)
    }

    /**
     * Checks, if some `method` can be called from outside the library.
     * The call graph must be computed, before this method may be invoked.
     *
     * @param method The method, which may be callable from outside.
     * @return True, if `method` can be called from outside the library.
     */
    private def canBeCalledFromOutside(method: Method): Boolean = {
        val FinalEP(_, callers) = propertyStore(declaredMethods(method), Callers.key)
        callers.hasCallersWithUnknownContext
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
    private def entryPointsForMethod(method: Method): Seq[(Method, IFDSFact[VTAFact, JavaStatement])] = {
        // Iterate over all parameters, which have a reference type.
        (method.descriptor.parameterTypes.zipWithIndex.collect {
            case (t, index) if t.isReferenceType =>
                /*
                 * Create a fact for the parameter, which says, that the parameter may have any
                 * subtype of its compile time type.
                 */
                VariableType(
                    JavaIFDSProblem.remapParamAndVariableIndex(index, method.isStatic),
                    t.asReferenceType,
                    upperBound = true
                )
            /*
             * In IFDS problems, we must always also analyze the null fact, because it creates the facts,
             * which hold independently of other source facts.
             * Map the input facts, in which we are interested, to a pair of the method and the fact.
             */
        } :+ VTANullFact).map(fact => (method, new IFDSFact(fact)))
    }
}
