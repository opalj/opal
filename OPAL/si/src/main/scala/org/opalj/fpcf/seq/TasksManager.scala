/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

import java.util.ArrayDeque
import java.util.PriorityQueue

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.AnyRefMap

trait TasksManager {

    /**
     * Just a hint from the property store to the tasksmanager that some
     * computations related to entities are done directly by the property store.
     */
    def processing(e: Entity): Unit = {}

    def push(task: QualifiedTask): Unit

    // Please recall that the store can be queried for the current dependers, because
    // the push is done before the set of dependers is potentially deleted in an update.
    //
    // To get the "bottomness", you can use:
    // val bottomness =
    // if (eps.hasUBP && eps.ub.isOrderedProperty)
    //  eps.ub.asOrderedProperty.bottomness
    // else
    //  OrderedProperty.DefaultBottomness
    def push(
        task:                              QualifiedTask,
        taskEPK:                           SomeEPK,
        updatedEOptionP:                   SomeEOptionP, // the current eOptionP to which the task is related
        updatedEOptionPDependees:          Traversable[SomeEOptionP], // the dependees of the eOptionP
        currentDependersOfUpdatedEOptionP: Traversable[SomeEPK]
    ): Unit

    def pollAndExecute(): Unit

    def isEmpty: Boolean

    def size: Int
}

/**
 * Processes the task that was added last first.
 */
private[seq] final class LIFOTasksManager extends TasksManager {

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)

    override def push(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    override def push(
        task:             QualifiedTask,
        taskEPK:          SomeEPK,
        eOptionP:         SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        this.tasks.addFirst(task)
    }

    override def pollAndExecute(): Unit = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t()
        else
            this.tasks.pollFirst()()
    }

    override def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    override def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "LIFOTasksManager"
}

/**
 * Processes the tasks that are schedulued for the longest time first.
 */
private[seq] final class FIFOTasksManager extends TasksManager {

    private[this] val initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] val tasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)

    override def push(task: QualifiedTask): Unit = {
        this.initialTasks.addLast(task)
    }

    override def push(
        task:             QualifiedTask,
        taskEPK:          SomeEPK,
        eOptionPs:        SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        this.tasks.addLast(task)
    }

    override def pollAndExecute(): Unit = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t()
        else
            this.tasks.pollFirst()()
    }

    override def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    override def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "FIFOTasksManager"
}

private class WeightedQualifiedTask(
        val task:   QualifiedTask,
        val weight: Int
) extends Comparable[WeightedQualifiedTask] {
    def compareTo(other: WeightedQualifiedTask) = this.weight - other.weight
}

private class WeightedExtendedQualifiedTask(
        val task:    QualifiedTask,
        val taskEPK: SomeEPK,
        val weight:  Int
) extends Comparable[WeightedQualifiedTask] {
    def compareTo(other: WeightedQualifiedTask) = this.weight - other.weight
}

private class ExtendedQualifiedTask(
        val task:    QualifiedTask,
        val taskEPK: SomeEPK
)

trait PropertyStoreDependentTasksManager extends TasksManager {

    protected[this] var ps: PKESequentialPropertyStore = null

    private[seq] def setSeqPropertyStore(ps: PKESequentialPropertyStore): Unit = {
        if (this.ps != null)
            throw new IllegalStateException(s"property store is already set: ${this.ps}")

        this.ps = ps
    }

}

/**
 * Schedules tasks that have many depender and dependee relations last.
 */
private[seq] final class ManyDirectDependenciesLastTasksManager
    extends PropertyStoreDependentTasksManager {

    private[this] val initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] val tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    override def push(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    override def push(
        task:             QualifiedTask,
        taskEPK:          SomeEPK,
        eOptionP:         SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        val weight = Math.max(1, dependees.size) * Math.max(1, currentDependers.size)
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    override def pollAndExecute(): Unit = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t()
        else
            this.tasks.poll().task()
    }

    override def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    override def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "ManyDirectDependenciesLastTasksManager"
}

private[seq] final class ManyDirectDependersLastTasksManager
    extends PropertyStoreDependentTasksManager {

    private[this] val initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] val tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    override def push(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    override def push(
        task:             QualifiedTask,
        taskEPK:          SomeEPK,
        eOptionP:         SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        this.tasks.add(new WeightedQualifiedTask(task, currentDependers.size))
    }

    override def pollAndExecute(): Unit = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t()
        else
            this.tasks.poll().task()
    }

    override def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    override def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "ManyDirectDependersLastTasksManager"
}

private[seq] final class ManyDependeesOfDirectDependersLastTasksManager
    extends PropertyStoreDependentTasksManager {

    private[this] val initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] val tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    override def push(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    override def push(
        task:                              QualifiedTask,
        taskEPK:                           SomeEPK,
        updatedEOptionP:                   SomeEOptionP,
        updatedEOptionPDependees:          Traversable[SomeEOptionP],
        currentDependersOfUpdatedEOptionP: Traversable[SomeEPK]
    ): Unit = {
        var weight = 0
        currentDependersOfUpdatedEOptionP foreach { epk ⇒ weight += ps.dependeesCount(epk) }
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    override def pollAndExecute(): Unit = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t()
        else {
            this.tasks.poll().task()
        }
    }

    override def isEmpty: Boolean = initialTasks.isEmpty && tasks.isEmpty

    override def size: Int = initialTasks.size + tasks.size

    override def toString: String = "ManyDependeesOfDirectDependersLastTasksManager"
}

private[seq] final class WeightedTaskDependenciesTasksManager
    extends PropertyStoreDependentTasksManager {

    private[this] val immediateTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(32768)
    private[this] val tasks: AnyRefMap[Entity, ArrayBuffer[ExtendedQualifiedTask]] = AnyRefMap.empty

    override def push(task: QualifiedTask): Unit = this.immediateTasks.addFirst(task)

    override def push(
        task:                              QualifiedTask,
        taskEPK:                           SomeEPK,
        updatedEOptionP:                   SomeEOptionP,
        updatedEOptionPDependees:          Traversable[SomeEOptionP],
        currentDependersOfUpdatedEOptionP: Traversable[SomeEPK]
    ): Unit = {
        val eqt = new ExtendedQualifiedTask(task, taskEPK)
        this.tasks.getOrElseUpdate(taskEPK.e, ArrayBuffer.empty) += eqt
    }

    override def pollAndExecute(): Unit = {
        val t = this.immediateTasks.pollFirst()
        if (t ne null)
            return t();

        val tIt = tasks.iterator
        val head = tIt.next
        val (_, selectedTasks) = tIt.take(10).foldLeft(head) { (c, n) ⇒
            val (_, cTasks) = c
            val (_, nTasks) = n
            print("size: "+cTasks.size+" vs. "+nTasks.size+"  --  ")
            val cLiveDependencies = cTasks.iterator.map(ct ⇒ ps.dependersCount(ct.taskEPK)).sum
            val nLiveDependencies = nTasks.iterator.map(nt ⇒ ps.dependersCount(nt.taskEPK)).sum
            println("live dependencies: "+cLiveDependencies+" vs. "+nLiveDependencies)
            if (cLiveDependencies < nLiveDependencies) {
                c
            } else {
                n
            }
        }
        selectedTasks.foreach(t ⇒ t.task())
    }

    override def isEmpty: Boolean = tasks.isEmpty && immediateTasks.isEmpty

    override def size: Int = immediateTasks.size + tasks.size

    override def toString: String = "WeightedTaskDependenciesTasksManager"
}

private[seq] final class ManyDependeesAndDependersOfDirectDependersLastTasksManager
    extends PropertyStoreDependentTasksManager {

    private[this] val initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] val tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    override def push(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    override def push(
        task:             QualifiedTask,
        taskEPK:          SomeEPK,
        eOptionP:         SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        var weight = 0
        currentDependers foreach { epk ⇒
            weight += ps.dependeesCount(epk) + ps.dependersCount(epk)
        }
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    override def pollAndExecute(): Unit = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t()
        else
            this.tasks.poll().task()
    }

    override def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    override def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "ManyDependeesAndDependersOfDirectDependersLastTasksManager"
}

private[seq] final class AllDependeesTasksManager(
        final val forward:           Boolean = true,
        final val manyDependeesLast: Boolean = true
) extends PropertyStoreDependentTasksManager {

    private[this] val initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] val tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    override def push(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    private[this] def computeForwardWeight(dependees: Traversable[SomeEOptionP]): Int = {
        val allDependees = mutable.HashSet.empty[SomeEPK]
        var newDependees = dependees.map(_.toEPK).toList
        while (newDependees.nonEmpty) {
            val nextDependee = newDependees.head
            newDependees = newDependees.tail
            val nextDependeeEPK = nextDependee
            allDependees += nextDependeeEPK
            ps.dependees(nextDependeeEPK) foreach { nextNextDependee ⇒
                val nextNextDependeeEPK = nextNextDependee.toEPK
                if (allDependees.add(nextNextDependeeEPK)) {
                    newDependees ::= nextNextDependeeEPK
                }
            }
        }
        allDependees.size
    }

    private[this] def computeBackwardWeight(currentDependers: Traversable[SomeEPK]): Int = {
        var weight = 0
        val allDependers = mutable.HashSet.empty[SomeEPK]
        var newDependers = currentDependers.toList
        while (newDependers.nonEmpty) {
            val nextDepender = newDependers.head
            newDependers = newDependers.tail
            allDependers += nextDepender
            ps.dependers(nextDepender) foreach { nextNextDepender ⇒
                if (allDependers.add(nextNextDepender)) {
                    newDependers ::= nextNextDepender
                    weight += ps.dependeesCount(nextNextDepender)
                }
            }
        }
        weight
    }

    override def push(
        task:             QualifiedTask,
        taskEPK:          SomeEPK,
        eOptionP:         SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        var weight =
            if (forward) {
                computeForwardWeight(dependees)
            } else {
                computeBackwardWeight(currentDependers)
            }
        if (!manyDependeesLast) weight = -weight
        //println("Weight: "+weight+"   -     Tasks:"+size)
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    override def pollAndExecute(): Unit = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t()
        else
            this.tasks.poll().task()
    }

    override def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    override def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = {
        s"AllDependeesTasksManager(forward=$forward,manyDependeesLast=$manyDependeesLast)"
    }
}
