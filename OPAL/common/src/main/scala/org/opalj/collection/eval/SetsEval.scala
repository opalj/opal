/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package eval

import org.opalj.util
import org.opalj.util.PerformanceEvaluation.time

/**
 * A small evaluation of the performance of the different map implementations supported by
 * Scala and Java. The evaluation is done w.r.t. typical workloads found in OPAL.
 *
 * <pre>
 * Fill maps...
 * AnyRefMap.add: 0,1480 s
 * OpenHashMap.add: 0,1233 s
 * Java ConcurrentHashMap.add: 0,1276 s
 * Java HashMap.add: 0,0987 s
 * HashMap.add: 0,5341 s
 * TreeMap.add: 1,1166 s
 *
 * Query maps...
 * AnyRefMap.get: 1,8579 s
 * OpenHashMap.get: 1,6853 s
 * Java ConcurrentHashMap.get: 1,6284 s
 * Java HashMap.get: 1,3532 s
 * immutable HashMap.get: 1,8304 s
 * immutable TreeMap.get: 19,0907 s
 * </pre>
 *
 */
object SetsEval extends App {

    util.gc();

    type T = String
    val Repetitions = 50
    val Threads = 4

    val ls: Seq[T] = (1 to 400000).map(i => (Math.random() * 5000000d).toString).reverse
    ls.foreach(_.hashCode) // <- they are lazily initialized!

    // SCALA Sets
    //
    val mHashSet = scala.collection.mutable.HashSet.empty[T]
    val mTreeSet = scala.collection.mutable.TreeSet.empty[T]
    // imutable maps...
    var iHashSet = scala.collection.immutable.HashSet.empty[T]
    var iTreeSet = scala.collection.immutable.TreeSet.empty[T]

    // JAVA Sets
    val jHashSet = new java.util.HashSet[T]

    println("Fill maps...")
    // fill maps
    time { ls.foreach(jHashSet.add) } { t => println("jHashSet.add: "+t.toSeconds) }
    time { ls.foreach(mHashSet.+=) } { t => println("mHashSet.add: "+t.toSeconds) }
    time { ls.foreach(mTreeSet.+=) } { t => println("mTreeSet.add: "+t.toSeconds) }
    time { ls.foreach(v => iHashSet += v) } { t => println("iHashSet.add: "+t.toSeconds) }
    time { ls.foreach(v => iTreeSet += v) } { t => println("iTreeSet.add: "+t.toSeconds) }

    // query maps
    var t = 0
    println("\nQuery maps...")

    time {
        (1 to Repetitions).foreach { i =>
            ls.foreach { s => if (jHashSet.contains(s)) { t += 1 } }
        }
    } { t => println("jHashSet.contains: "+t.toSeconds) }

    time {
        (1 to Repetitions).foreach { i =>
            ls.foreach { s => if (mHashSet.contains(s)) t += 1 }
        }
    } { t => println("mHashSet.contains: "+t.toSeconds) }

    time {
        (1 to Repetitions).foreach { i =>
            ls.foreach { s => if (mTreeSet.contains(s)) t += 1 }
        }
    } { t => println("mTreeSet.contains: "+t.toSeconds) }

    time {
        (1 to Repetitions).foreach { i =>
            ls.foreach { s => if (iHashSet.contains(s)) t += 1 }
        }
    } { t => println("iHashSet.contains: "+t.toSeconds) }

    time {
        (1 to Repetitions).foreach { i =>
            ls.foreach { s => if (iTreeSet.contains(s)) t += 1 }
        }
    } { t => println("iTreeSet.contains: "+t.toSeconds) }

    println(s"\n Run: $t")
}
