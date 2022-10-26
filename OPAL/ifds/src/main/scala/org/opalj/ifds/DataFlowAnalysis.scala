/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ifds

import scala.collection.mutable

abstract class DataFlowAnalysis[Facts >: Null <: AnyRef, C <: AnyRef, S <: Statement[_ <: C, _]] {
    def icfg: ICFG[C, S]
    def entryFacts: Facts
    def transferFunction(facts: Facts, statement: S, successor: S): Facts
    def join(left: Facts, right: Facts): Facts

    def perform(callable: C): Map[S, Facts] = {
        var facts = Map.empty[S, Facts]
        val workList = new mutable.Queue[S]()

        for (entryStatement <- icfg.startStatements(callable)) {
            facts = facts.updated(entryStatement, entryFacts)
            workList.enqueue(entryStatement)
        }

        while (workList.nonEmpty) {
            val statement = workList.dequeue()
            val inFacts = facts.get(statement).get

            for (successor <- icfg.nextStatements(statement)) {
                val newOutFacts = transferFunction(inFacts, statement, successor)
                facts.get(successor) match {
                    case None => {
                        facts = facts.updated(successor, newOutFacts)
                        workList.enqueue(successor)
                    }
                    case Some(existingOutFacts) => {
                        val outFacts = join(existingOutFacts, newOutFacts)
                        if (outFacts ne existingOutFacts) {
                            facts = facts.updated(successor, outFacts)
                            workList.enqueue(successor)
                        }
                    }
                }
            }
        }

        facts
    }
}