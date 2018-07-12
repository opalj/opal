/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses

import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.Type
import org.opalj.ai.domain._
import org.opalj.ai.domain.RecordReturnedValueInfrastructure
import org.opalj.ai.InterruptableAI
import org.opalj.ai.analyses.cg.CallGraphCache
import org.opalj.br.MethodSignature

/**
 * A shallow analysis that tries to refine the return types of methods.
 *
 * The analysis terminates itself when it realizes that the return type cannot be
 * refined.
 *
 * @author Michael Eichberg
 */
class BaseMethodReturnValuesAnalysisDomain(
        override val project:      SomeProject,
        val fieldValueInformation: FieldValueInformation,
        val ai:                    InterruptableAI[_],
        val method:                Method
) extends CorrelationalDomain
    with TheProject
    with TheMethod
    with DefaultDomainValueBinding
    with ThrowAllPotentialExceptionsConfiguration
    with l0.DefaultTypeLevelIntegerValues
    // with l1.DefaultIntegerRangeValues
    with l0.DefaultTypeLevelLongValues
    with l0.TypeLevelLongValuesShiftOperators
    with l0.TypeLevelPrimitiveValuesConversions
    with l0.DefaultTypeLevelFloatValues
    with l0.DefaultTypeLevelDoubleValues
    //with l0.DefaultReferenceValuesBinding
    with l1.DefaultReferenceValuesBinding
    with la.RefinedTypeLevelFieldAccessInstructions
    with l0.TypeLevelInvokeInstructions
    with DefaultHandlingOfMethodResults
    with IgnoreSynchronization
    with RecordReturnedValueInfrastructure {

    type ReturnedValue = DomainValue

    private[this] val originalReturnType: Type = method.descriptor.returnType

    private[this] var theReturnedValue: DomainValue = null

    // A method that always throws an exception will never return a value.
    def returnedValue: Option[DomainValue] = Option(theReturnedValue)

    protected[this] def doRecordReturnedValue(pc: PC, value: DomainValue): Unit = {
        val oldReturnedValue = theReturnedValue
        if (oldReturnedValue eq value)
            return ;

        val newValue =
            if (oldReturnedValue == null) {
                value
            } else {
                val joinedValue = oldReturnedValue.join(Int.MinValue, value)
                if (joinedValue.isNoUpdate)
                    return ;
                joinedValue.value
            }
        newValue match {
            case value @ TypeOfReferenceValue(utb) if value.isNull.isUnknown &&
                (utb.isSingletonSet) &&
                (utb.head eq originalReturnType) &&
                !value.isPrecise ⇒
                // the return type will not be more precise than the original type
                ai.interrupt()
            case _ ⇒
                theReturnedValue = newValue
        }
    }
}

class FPMethodReturnValuesAnalysisDomain(
        project:                          SomeProject,
        fieldValueInformation:            FieldValueInformation,
        val methodReturnValueInformation: MethodReturnValueInformation,
        override val cache:               CallGraphCache[MethodSignature, scala.collection.Set[Method]],
        ai:                               InterruptableAI[_],
        method:                           Method
) extends BaseMethodReturnValuesAnalysisDomain(project, fieldValueInformation, ai, method)
    with la.RefinedTypeLevelInvokeInstructions
