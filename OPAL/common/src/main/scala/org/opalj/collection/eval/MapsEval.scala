/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package eval

import org.opalj.util.PerformanceEvaluation.time

/**
 * A small evaluation of the performance of the different map implementations supported by
 * Scala and Java. The evaluation is done w.r.t. typical workloads found in OPAL.
 *
 * <pre>
 * Fill maps...
 * AnyRefMap.add: 0,1480 s
 * Java ConcurrentHashMap.add: 0,1276 s
 * Java HashMap.add: 0,0987 s
 * HashMap.add: 0,5341 s
 * TreeMap.add: 1,1166 s
 *
 * Query maps...
 * AnyRefMap.get: 1,8579 s
 * Java ConcurrentHashMap.get: 1,6284 s
 * Java HashMap.get: 1,3532 s
 * immutable HashMap.get: 1,8304 s
 * immutable TreeMap.get: 19,0907 s
 * </pre>
 */
object MapsEval extends App {

    util.gc();

    type T = String
    val Repetitions = 50
    val Threads = 4
    var t = 0

    // val ls: Seq[T] = (1 to 400000).map(_.toString).reverse
    val ls: Seq[T] = (1 to 400000).map(i => (Math.random() * 5000000d).toString).reverse
    ls.foreach(_.hashCode) // <- they are lazily initialized!
    val theObject = new Object()

    // SCALA maps
    //
    val anyRefMap = scala.collection.mutable.AnyRefMap.empty[T, Object]
    val trieMap = scala.collection.concurrent.TrieMap.empty[T, Object]
    // immmutable maps...
    var hashMap = scala.collection.immutable.HashMap.empty[T, Object]
    var treeMap = scala.collection.immutable.TreeMap.empty[T, Object]

    // JAVA maps
    val jConcurrentMap = new java.util.concurrent.ConcurrentHashMap[T, Object]
    val jHashMap = new java.util.HashMap[T, Object]

    println("Fill maps...")
    // fill maps
    time {
        ls.foreach { s => jConcurrentMap.put(s, theObject) }
    } { t => println("Java ConcurrentHashMap.add: "+t.toSeconds) }

    time {
        ls.foreach { s => jHashMap.put(s, theObject) }
    } { t => println("Java HashMap.add: "+t.toSeconds) }

    time {
        ls.foreach { s =>
            anyRefMap += (s -> theObject) // <= faster then adding it using pairs...
            //anyRefMap += ((s, theObject()))
        }
    } { t => println("mutable AnyRefMap.add: "+t.toSeconds) }

    time {
        ls.foreach { s => trieMap += (s -> theObject) }
    } { t => println("concurrent mutable TrieMap.add: "+t.toSeconds) }

    time {
        ls.foreach { s => hashMap += (s -> theObject) }
    } { t => println("immutable HashMap.add: "+t.toSeconds) }

    time {
        ls.foreach { s => treeMap += (s -> theObject) }
    } { t => println("immutable TreeMap.add: "+t.toSeconds) }

    // query maps

    println("\nQuery maps...")

    time {
        val ts = Array.fill(Threads)(new Thread() {
            override def run(): Unit = {
                (1 to Repetitions).foreach { i =>
                    ls.foreach { s => t += jConcurrentMap.get(s).hashCode }
                }
            }
        })
        ts.foreach(t => t.start)
        ts.foreach(t => t.join)
    } { t => println("Java ConcurrentHashMap.get: "+t.toSeconds) }

    time {
        val ts = Array.fill(Threads)(new Thread() {
            override def run(): Unit = {
                (1 to Repetitions).foreach { i => ls.foreach { s => t += jHashMap.get(s).hashCode } }
            }
        })
        ts.foreach(t => t.start)
        ts.foreach(t => t.join)
    } { t => println("Java HashMap.get: "+t.toSeconds) }

    time {
        val ts = Array.fill(Threads)(new Thread() {
            override def run(): Unit = {
                (1 to Repetitions).foreach { i => ls.foreach { s => t += anyRefMap(s).hashCode } }
            }
        })
        ts.foreach(t => t.start)
        ts.foreach(t => t.join)
    } { t => println("AnyRefMap.get: "+t.toSeconds) }

    time {
        val ts = Array.fill(Threads)(new Thread() {
            override def run(): Unit = {
                (1 to Repetitions).foreach { i => ls.foreach { s => t += trieMap(s).hashCode } }
            }
        })
        ts.foreach(t => t.start)
        ts.foreach(t => t.join)
    } { t => println("concurrent.TrieMap.get: "+t.toSeconds) }

    time {
        val ts = Array.fill(Threads)(new Thread() {
            override def run(): Unit = {
                (1 to Repetitions).foreach { i => ls.foreach { s => t += anyRefMap(s).hashCode } }
            }
        })
        ts.foreach(t => t.start)
        ts.foreach(t => t.join)
    } { t => println("immutable HashMap.get: "+t.toSeconds) }

    time {
        val ts = Array.fill(Threads)(new Thread() {
            override def run(): Unit = {
                (1 to Repetitions).foreach { i => ls.foreach { s => t += treeMap(s).hashCode } }
            }
        })
        ts.foreach(t => t.start)
        ts.foreach(t => t.join)
    } { t => println("immutable TreeMap.get: "+t.toSeconds) }

    println(s"\n Run: $t")
}
