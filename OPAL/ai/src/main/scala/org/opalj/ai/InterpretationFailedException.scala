/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.mutable.IntArrayStack

/**
 * Exception that is thrown by the abstract interpreter when the abstract
 * interpretation of a method's implementation failed.
 *
 * To create an instance use the companion object [[InterpretationFailedException$]].
 *
 * @param   pc The program counter of the instruction for which the interpretation failed.
 *          If `pc == code.length` then the interpretation completed successfully,
 *          but the post-processing failed.
 *
 * @author Michael Eichberg
 */
sealed trait InterpretationFailedException {
    def cause: Throwable

    val domain: Domain
    val ai: AI[_ >: domain.type]
    val pc: Int
    val worklist: List[Int /*PC*/ ]
    val evaluatedPCs: IntArrayStack
    val cfJoins: IntTrieSet
    val operandsArray: domain.OperandsArray
    val localsArray: domain.LocalsArray
    val memoryLayoutBeforeSubroutineCall: List[(Int /*PC*/ , domain.OperandsArray, domain.LocalsArray)]
}

/**
 * Factory for [[InterpretationFailedException]]s.
 *
 * @author Michael Eichberg
 */
object InterpretationFailedException {

    def apply(
        theCause:  Throwable,
        theDomain: Domain
    )(
        theAI:                               AI[_ >: theDomain.type],
        thePc:                               Int,
        theCFJoins:                          IntTrieSet,
        theWorklist:                         List[Int /*PC*/ ],
        theEvaluatedPCs:                     IntArrayStack,
        theOperandsArray:                    theDomain.OperandsArray,
        theLocalsArray:                      theDomain.LocalsArray,
        theMemoryLayoutBeforeSubroutineCall: List[(Int /*PC*/ , theDomain.OperandsArray, theDomain.LocalsArray)]
    ): AIException with InterpretationFailedException = {

        new AIException("the interpretation failed", theCause) with InterpretationFailedException {
            def cause = super.getCause
            val ai: AI[_ >: theDomain.type] = theAI
            val domain: theDomain.type = theDomain
            val pc: Int = thePc
            val cfJoins: IntTrieSet = theCFJoins

            val worklist: List[Int /*PC*/ ] = theWorklist
            val evaluatedPCs: IntArrayStack = theEvaluatedPCs

            val operandsArray: theDomain.OperandsArray = theOperandsArray
            val localsArray: theDomain.LocalsArray = theLocalsArray
            val memoryLayoutBeforeSubroutineCall: List[(Int /*PC*/ , theDomain.OperandsArray, theDomain.LocalsArray)] = theMemoryLayoutBeforeSubroutineCall

            final override def toString: String = {
                s"InterpretationFailedException(\n\tdomain=$domain,"+
                    s"\n\tai=${ai.getClass},\n\tcause=$cause,"+
                    s"\n\tpc=$pc,"+
                    (
                        if (pc < theOperandsArray.length)
                            s"\n\toperands=${operandsArray(pc)},"
                        else
                            s"\n\toperands=N/A (the pc is invalid: $pc)"
                    ) +
                        (
                            if (pc < theLocalsArray.length)
                                s"\n\tregisters=${localsArray(pc).zipWithIndex.map(_.swap).mkString(",")}"
                            else
                                s"\n\tregisters=N/A (the pc is invalid: $pc)"
                        ) +
                            s"\n)"
            }

            final override def getMessage(): String = toString
        }
    }
}
