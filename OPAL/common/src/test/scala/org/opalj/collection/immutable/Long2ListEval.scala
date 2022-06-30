/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

import org.opalj.util.PerformanceEvaluation

object Long2ListEval extends App {

    val Elements = 3000000
    val seed = 123456789L
    val rngGen = new java.util.Random(seed)

    for { i <- 1 to 10 } {
        var l = Long2List.empty

        PerformanceEvaluation.memory {
            PerformanceEvaluation.time {
                var i = Elements
                do {
                    l = Math.abs(rngGen.nextLong()) +: l
                    i -= 1
                } while (i > 0)
            } { t => println(s"creation took ${t.toSeconds}") }
        } { mu => println(s"required $mu bytes") }

        var sumForeach = 0L
        PerformanceEvaluation.time {
            l.foreach(sumForeach += _)
        } { t => println(s"foreach sum took ${t.toSeconds}") }

        var sumForFirstThird = 0L
        PerformanceEvaluation.time {
            l.forFirstN(Elements / 3)(sumForFirstThird += _)
        } { t => println(s"forFirstN(1/3*Elements) sum took ${t.toSeconds}") }

        val sumIterator =
            PerformanceEvaluation.time {
                l.iterator.sum
            } { t => println(s"iterator sum took ${t.toSeconds}") }

        assert(sumForeach == sumIterator)
        println(s"summarized value: ${sumIterator}")
    }

}
