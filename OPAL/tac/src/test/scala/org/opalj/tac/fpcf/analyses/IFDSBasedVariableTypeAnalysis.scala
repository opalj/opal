/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.New
import org.opalj.tac.Return
import org.opalj.tac.ReturnValue
import org.opalj.tac.Var

trait VTAFact extends AbstractIFDSFact
case object VTANullFact extends VTAFact with AbstractIFDSNullFact

/**
 * A posible run time type of a variable.
 *
 * @param definedBy The variable's definition site.
 * @param t The variable's type.
 */
case class VariableType(definedBy: Int, t: ObjectType) extends VTAFact

//TODO Felder
//TODO Subsuming
/**
 * A variable type analysis implemented as an IFDS analysis.
 *
 * @param project The analyzed project.
 * @author Mario Trageser
 */
class IFDSBasedVariableTypeAnalysis private (implicit val project: SomeProject) extends AbstractIFDSAnalysis[VTAFact] {

    override val propertyKey: IFDSPropertyMetaInformation[VTAFact] = VTAResult

    /**
     * The analysis starts with all public methods in VTATestClass.
     */
    override val entryPoints: Map[DeclaredMethod, VTAFact] = p.allProjectClassFiles.filter(classFile ⇒
        classFile.thisType.fqn == "org/opalj/fpcf/fixtures/vta/VTATestClass")
        .flatMap(classFile ⇒ classFile.methods)
        .filter(method ⇒ method.isPublic)
        .map(method ⇒ declaredMethods(method) → VTANullFact).toMap

    override def createPropertyValue(result: Map[Statement, Set[VTAFact]]): IFDSProperty[VTAFact] = {
        new VTAResult(result)
    }

    /**
     * If a variable or an array element is assigned a new value,
     * a new ValueType will be created for this variable with the type of the source expression.
     */
    override def normalFlow(statement: Statement, successor: Statement, in: Set[VTAFact]): Set[VTAFact] = statement.stmt.astID match {
        case Assignment.ASTID ⇒
            val expression = statement.stmt.asAssignment.expr
            expression.astID match {
                case Var.ASTID       ⇒ handleAssignment(statement.index, expression.asVar.definedBy, in)
                case ArrayLoad.ASTID ⇒ handleAssignment(statement.index, expression.asArrayLoad.arrayRef.asVar.definedBy, in)
                case New.ASTID       ⇒ in + VariableType(statement.index, expression.asNew.tpe)
            }
        case ArrayStore.ASTID ⇒ {
            val arrayStore = statement.stmt.asArrayStore
            val value = arrayStore.value
            value.astID match {
                case Var.ASTID ⇒
                    val valueDefinedBy = value.asVar.definedBy
                    arrayStore.arrayRef.asVar.definedBy.map((index: Int) ⇒ handleAssignment(index, valueDefinedBy, in)).flatten
            }
        }
        case Return.ASTID | ReturnValue.ASTID ⇒ in
    }

    /**
     * Called, when normalFlow finds an assignment, where a target variable get assigned the value of a source variable.
     * If `in` contains a VariableType for an index in `sourceIndices`,
     * a new VariableType for `targetIndex` will be added to `in` with the type of the source VariableType.
     *
     * @param targetIndex The definition site of the target variable.
     * @param sourceIndices The possible definition sites of the source variable.
     * @param in The data flow facts valid before the assignment.
     * @return in plus the newly created VariableTypes.
     */
    def handleAssignment(targetIndex: Int, sourceIndices: IntTrieSet, in: Set[VTAFact]) =
        in ++ in.collect {
            case VariableType(index, c) if sourceIndices.contains(index) ⇒ VariableType(targetIndex, c)
        }

    override def callFlow(call: Statement, callee: DeclaredMethod, in: Set[VTAFact]): Set[VTAFact] = {
        Set.empty
    }

    override def callToReturnFlow(call: Statement, successor: Statement, in: Set[VTAFact]): Set[VTAFact] = {
        in
    }

    override def returnFlow(call: Statement, callee: DeclaredMethod, exit: Statement, successor: Statement, in: Set[VTAFact]): Set[VTAFact] = {
        Set.empty
    }

    override def nativeCall(statement: Statement, callee: DeclaredMethod, successor: Statement, in: Set[VTAFact]): Set[VTAFact] = {
        Set.empty
    }
}

object IFDSBasedVariableTypeAnalysis extends IFDSAnalysis[VTAFact] {

    override def init(p: SomeProject, ps: PropertyStore) = new IFDSBasedVariableTypeAnalysis()(p)

    override def property: IFDSPropertyMetaInformation[VTAFact] = VTAResult
}

/**
 * The IFDSProperty for this analysis.
 */
class VTAResult(val flows: Map[Statement, Set[VTAFact]]) extends IFDSProperty[VTAFact] {

    override type Self = VTAResult

    override def key: PropertyKey[VTAResult] = VTAResult.key
}

object VTAResult extends IFDSPropertyMetaInformation[VTAFact] {

    override type Self = VTAResult

    val key: PropertyKey[VTAResult] = PropertyKey.create("VTA", new VTAResult(Map.empty))
}