/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import scala.collection.Set
import scala.collection.immutable

import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.br.ObjectType

/**
 * Records the results of the evaluation of the `current` method such that the results
 * can directly be adapted to the calling context and can be used by the caller to continue
 * the abstract interpretation of the calling method.
 *
 * '''The returned value will only be a given parameter, if the given parameter is not mutated.'''
 * For example, if an array is passed to a method where the content is reified, the
 * returned value will only be mapped back to the parameter value if the array is not changed at all.
 * In other words, the returned value, which may get the pc of the method caller, may refer to
 * any parameter given to the method. Only, if the returned value is a parameter, we know that
 * this specific parameter was not mutated at all.
 *
 * @author Michael Eichberg
 */
trait RecordMethodCallResults
    extends MethodCallResults
    with RecordReturnedValues
    with RecordThrownExceptions {
    this: Domain =>

    type ThrownException <: Set[this.ExceptionValue]

    type ReturnedValue <: DomainValue

    private[this] var hasReturnedNormally: Boolean = false

    abstract override def returnVoid(pc: Int): Computation[Nothing, ExceptionValue] = {
        hasReturnedNormally = true
        super.returnVoid(pc)
    }

    override def returnedNormally: Boolean = hasReturnedNormally || allReturnedValues.nonEmpty

    override def returnedValue(target: TargetDomain, callerPC: Int): Option[target.DomainValue] = {
        if (allReturnedValues.isEmpty)
            None
        else {
            Some(summarize(callerPC, allReturnedValues.values).adapt(target, callerPC))
        }
    }

    /**
     * Remaps the returned value to the domain value used by the calling domain.
     *
     * @note Even if the current domain provides origin information then the returned value is not
     *       refined by this default implementation. For example, imagine the following code:
     *       {{{
     *       def isString(o : Object) : Object = {
     *          if(o.isInstanceOf[String])
     *              o // here, we know that o is actually a String.
     *          else
     *              null
     *       }
     *       }}}
     *       Here, the value that is returned is the original "object" value; the information
     *       that it is a String is not available in the calling method's context.
     *       Furthermore, "MultipleReferenceValues" are also not supported.
     *       Support of these features requires that the "current" domain is at least the
     *       l1.DefaultReferenceValuesDomain which we do not assume here.
     */
    override def returnedValueRemapped(
        callerDomain: TargetDomain,
        callerPC:     Int
    )(
        originalOperands: callerDomain.Operands,
        passedParameters: Locals
    ): Option[callerDomain.DomainValue] = {

        if (allReturnedValues.isEmpty)
            None
        else {
            /* THE FOLLOWING IS THE MOST BASIC HANDLING WHICH ONLY SUPPORTS IDENTITY FUNCTIONS:
            val summarizedValue = summarize(callerPC, allReturnedValues.values)

            val nthParameter = passedParameters.nthValue { _ eq summarizedValue }
            if (nthParameter == -1)
                Some(summarizedValue.adapt(callerDomain, callerPC))
            else {
                // map back to operand...
                val mappedBackValue = originalOperands.reverse(nthParameter)
                Some(mappedBackValue)
            }
            */

            // If we have multiple return sites where some refer to parameters and
            // some to local variables, we map back the information regarding
            // the parameters and summarize only w.r.t. the local variables.
            val (returnedParameters, returnedLocals) =
                allReturnedValues.values.partition(passedParameters.contains)
            var summarizedValue: callerDomain.DomainValue = // <= summarized in the target domain!
                if (returnedLocals.nonEmpty)
                    summarize(callerPC, returnedLocals).adapt(callerDomain, callerPC)
                else
                    null
            returnedParameters foreach { p =>
                val nthParameter = passedParameters.nthValue { _ == p }
                val originalParameter = originalOperands.reverse(nthParameter)
                if (summarizedValue == null || summarizedValue == originalParameter) {
                    summarizedValue = originalParameter
                } else {
                    summarizedValue.join(callerPC, originalParameter) match {
                        case SomeUpdate(newSummarizedValue) => summarizedValue = newSummarizedValue
                        case _                              => summarizedValue
                    }
                }
            }
            Some(summarizedValue)
        }
    }

    // IMPROVE Remap returned exceptions
    def thrownExceptions(target: TargetDomain, callerPC: Int): target.ExceptionValues = {

        val allThrownExceptions = this.allThrownExceptions //: Map[PC, ThrownException]
        if (allThrownExceptions.isEmpty) {
            Iterable.empty
        } else {
            var exceptionValuesPerType: Map[ObjectType, immutable.Set[ExceptionValue]] = Map.empty

            def handleExceptionValue(exceptionValue: ExceptionValue): Unit = {
                exceptionValue.upperTypeBound match {
                    case EmptyUpperTypeBound =>
                        exceptionValuesPerType = exceptionValuesPerType.updated(
                            ObjectType.Throwable,
                            exceptionValuesPerType.getOrElse(
                                ObjectType.Throwable, immutable.Set.empty
                            ) + exceptionValue
                        )
                    case UIDSet1(exceptionType: ObjectType) =>
                        exceptionValuesPerType = exceptionValuesPerType.updated(
                            exceptionType,
                            exceptionValuesPerType.getOrElse(
                                exceptionType, immutable.Set.empty
                            ) + exceptionValue
                        )
                    case utb =>
                        val exceptionType =
                            classHierarchy.joinObjectTypesUntilSingleUpperBound(
                                utb.asInstanceOf[UIDSet[ObjectType]]
                            )
                        exceptionValuesPerType = exceptionValuesPerType.updated(
                            exceptionType,
                            exceptionValuesPerType.getOrElse(
                                exceptionType, immutable.Set.empty
                            ) + exceptionValue
                        )
                }
            }

            for {
                exceptionValuesPerInstruction <- allThrownExceptions.values
                exceptionValues <- exceptionValuesPerInstruction
                exceptionValue <- exceptionValues.allValues
            } {
                handleExceptionValue(exceptionValue)
            }

            exceptionValuesPerType.values.map { exceptionValuesPerType =>
                summarize(callerPC, exceptionValuesPerType)
            }.map { exceptionValuePerType =>
                exceptionValuePerType.adapt(target, callerPC).asInstanceOf[target.ExceptionValue]
            }
        }
    }
}
