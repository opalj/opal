/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.collection.immutable._
import org.opalj.util.{Nanoseconds => NS}

object UIDSetDemo extends App {

    val o1 = ObjectType("o1")
    val o2 = ObjectType("o2")
    val o3 = ObjectType("o3")
    val o4 = ObjectType("o4")
    val o5 = ObjectType("o5")
    val o6 = ObjectType("o6")
    val o7 = ObjectType("o7")

    val s1 = UIDSet(o1)
    val s2 = UIDSet(o2)
    val s12 = s1 + o2
    val s21 = s2 + o1
    s21 == s12

    UIDSet(o1, o3) == UIDSet(o3, o1)

    s12.map(_.id)

    s12.filter(_.id < 20)
    s12.filter(_.id < 23)

    s12.filterNot(_.id < 20)
    s12.filterNot(_.id < 23)

    !s12.exists(_ == o3)

    s12.exists(_ == o1)
    s12.exists(_ == o2)
    s12.forall(_.id >= 0)
    s12.find(_.id > 10).isDefined
    !s12.find(_.id < 10).isDefined

    val se = UIDSet.empty[ObjectType]
    !se.exists(_ == o2)
    !se.contains(o2)
    se.filter(_.id < 20).map(_.id)
    se.filterNot(_.id < 20).map(_.id)

    s12.filter(_.id < 100) eq s12
    s12.filterNot(_.id > 100) eq s12

    val s1234 = s12 + o3 + o4
    s1234.map(_.id).mkString(", ")
    s1234.foldLeft(0)(_ + _.id)
    s2.foldLeft(o1.id)(_ + _.id)

    s1234 + o7 + o5 + o6

    case class SUID(val id: Int) extends org.opalj.collection.UID

    def evalAdd(): Unit = {
        /*
        { // using standard + method
            val r = new java.util.Random(10002323323l)
            var runs = 0
            val t = System.nanoTime
            while (runs < 20) {
                var s = UIDSet.empty[SUID]
                val addCount = r.nextInt(100000)
                var i = 0
                while (i < addCount) {
                    s += SUID(r.nextInt(addCount * 2))
                    i += 1
                }
                runs += 1
            }
            println("Using +:   "+NS(System.nanoTime - t).toSeconds)
        }
        */

        { // using +! method (by means of a builder)
            val r = new java.util.Random(10002323323L)
            var runs = 0
            val t = System.nanoTime
            while (runs < 20) {
                val s = UIDSet.newBuilder[SUID]
                val addCount = r.nextInt(100000)
                var i = 0
                while (i < addCount) {
                    s += SUID(r.nextInt(addCount * 2))
                    i += 1
                }
                s.result()
                runs += 1
            }
            println("Using +!:  "+NS(System.nanoTime - t).toSeconds)
        }

        { // comparison with Scala set
            val r = new java.util.Random(10002323323L)
            var runs = 0
            val t = System.nanoTime
            while (runs < 20) {
                var s = Set.empty[SUID]
                val addCount = r.nextInt(100000)
                var i = 0
                while (i < addCount) {
                    s += SUID(r.nextInt(addCount * 2))
                    i += 1
                }
                runs += 1
            }
            println("Using Set: "+NS(System.nanoTime - t).toSeconds)
        }
    }
    (0 to 5).foreach(e => evalAdd())

    /////////////////////////////////// EXTENSIVE EVAL ///////////////////////////////////

    def eval(factory: Set[SUID]): Unit = {
        val r = new java.util.Random(10002323323L)
        var runs = 0

        var addedValues = 0
        var uniqueValues = 0
        var filteredValues = 0
        var containsSucceeded = 0
        var containsFailed = 0
        var removedValues = 0
        // var removedValuesByTail = 0
        var timeForAddingValues = 0L
        var timeForFilteringValues = 0L
        var timeForContainsCheck = 0L
        var timeForRemovingValues = 0L
        // var timeForXTailCalls = 0L

        val startTime = System.nanoTime
        while (runs < 20) {
            var s = factory.empty
            val addCount = r.nextInt(100000)
            addedValues += addCount

            // adding
            var t = System.nanoTime
            var i = 0
            while (i < addCount) {
                s += SUID(r.nextInt(addCount * 2))
                i += 1
            }
            uniqueValues += s.size
            timeForAddingValues += (System.nanoTime - t)

            // contains
            t = System.nanoTime
            i = 0
            while (i < addCount / 2) {
                val targetValue = SUID(r.nextInt(addCount * 2))
                if (s.contains(targetValue))
                    containsSucceeded += 1
                else
                    containsFailed += 1
                i += 1
            }
            timeForContainsCheck += (System.nanoTime - t)

            // filtering
            t = System.nanoTime
            val sizeBeforeFiltering = s.size
            i = 0
            while (i < addCount / 100) {
                val valueToFilter = SUID(r.nextInt(addCount * 2))
                s = s.filter(_ != valueToFilter)
                i += 1
            }
            filteredValues += sizeBeforeFiltering - s.size
            timeForFilteringValues += (System.nanoTime - t)

            // removing
            t = System.nanoTime
            val sizeBeforeRemoving = s.size
            i = 0
            while (i < addCount / 10) {
                s -= SUID(r.nextInt(addCount * 2))
                i += 1
            }
            removedValues += sizeBeforeRemoving - s.size
            timeForRemovingValues += (System.nanoTime - t)

            // tail ... is implemented so inefficiently for HashTrieSets, that we don't measure it...
            /*
            t = System.nanoTime
            val sizeBeforeTail = s.size
            while (s.size > (sizeBeforeTail *0.95)) {
                s = s.tail
                i += 1
            }
            removedValuesByTail += sizeBeforeTail - s.size
            timeForXTailCalls += (System.nanoTime - t)
            */

            runs += 1
        }
        val time = org.opalj.util.Nanoseconds(System.nanoTime - startTime).toSeconds
        println(
            s"${factory.getClass.getSimpleName} - runs executed: $runs in $time"+
                s"\n\tadded: $addedValues; unique: $uniqueValues - "+NS(timeForAddingValues).toSeconds +
                s"\n\tfiltered: $filteredValues - "+NS(timeForFilteringValues).toSeconds +
                s"\n\tsuccessful contains: $containsSucceeded; failed contains: $containsFailed - "+NS(timeForContainsCheck).toSeconds +
                // s"\n\ttail: $removedValuesByTail - "+NS(timeForXTailCalls).toSeconds+
                s"\n\tremoved: $removedValues - "+NS(timeForRemovingValues).toSeconds
        )
    }

    for { i <- 0 until 5 } {
        org.opalj.util.gc()
        eval(scala.collection.immutable.Set.empty)
        org.opalj.util.gc()
        eval(UIDSet.empty)
        println()
    }
}
