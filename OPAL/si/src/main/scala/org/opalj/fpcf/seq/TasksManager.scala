/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

import java.util.ArrayDeque
import java.util.PriorityQueue

private[seq] trait TasksManager {

    def pushInitialTask(task: QualifiedTask): Unit

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK], // may change between the time the task is scheduled and the time it is evaluated!
        bottomness:       Int                       = OrderedProperty.DefaultBottomness,
        hint:             PropertyComputationHint   = DefaultPropertyComputation
    ): Unit

    def poll(): QualifiedTask

    def isEmpty: Boolean

    def size: Int
}

/**
 * Processes the task that was added last first.
 */
private[seq] class LIFOTasksManager extends TasksManager {

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)

    def pushInitialTask(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK],
        bottomness:       Int,
        hint:             PropertyComputationHint
    ): Unit = {
        this.tasks.addFirst(task)
    }

    def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.pollFirst()
    }

    def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "LIFOTasksManager"
}

/**
 * Processes the tasks that are schedulued for the longest time first.
 */
private[seq] class FIFOTasksManager extends TasksManager {

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)

    def pushInitialTask(task: QualifiedTask): Unit = {
        this.initialTasks.addLast(task)
    }

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK],
        bottomness:       Int,
        hint:             PropertyComputationHint
    ): Unit = {
        this.tasks.addLast(task)
    }

    def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.pollFirst()
    }

    def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "FIFOTasksManager"
}

private class WeightedQualifiedTask(
        val task:   QualifiedTask,
        val weight: Int
) extends Comparable[WeightedQualifiedTask] {
    def compareTo(other: WeightedQualifiedTask) = this.weight - other.weight
}

/**
 * Schedules tasks that have many depender and dependee relations last.
 */
private[seq] class ManyDirectDependenciesLastTasksManager extends TasksManager {

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    def pushInitialTask(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK],
        bottomness:       Int,
        hint:             PropertyComputationHint
    ): Unit = {
        val weight = Math.max(1, dependees.size) * Math.max(1, currentDependers.size)
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.poll().task
    }

    def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "ManyDirectDependenciesLastTasksManager"
}

private[seq] class ManyDirectDependersLastTasksManager extends TasksManager {

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    def pushInitialTask(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK],
        bottomness:       Int,
        hint:             PropertyComputationHint
    ): Unit = {
        this.tasks.add(new WeightedQualifiedTask(task, currentDependers.size))
    }

    def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.poll().task
    }

    def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "ManyDirectDependersLastTasksManager"
}

private[seq] class ManyDependeesOfDirectDependersLastTasksManager extends TasksManager {

    private[this] var ps: PKESequentialPropertyStore = null

    private[seq] def setSeqPropertyStore(ps: PKESequentialPropertyStore): Unit = {
        if (this.ps != null)
            throw new IllegalStateException(s"property store is already set: ${this.ps}")

        this.ps = ps
    }

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    def pushInitialTask(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK],
        bottomness:       Int,
        hint:             PropertyComputationHint
    ): Unit = {
        var weight = currentDependers.toIterator.map(epk ⇒ ps.dependeesCount(epk)).sum
        if (hint == CheapPropertyComputation) weight -= 1
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.poll().task
    }

    def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "ManyDependeesOfDirectDependersLastTasksManager"
}

private[seq] class ManyDependeesAndDependersOfDirectDependersLastTasksManager extends TasksManager {

    private[this] var ps: PKESequentialPropertyStore = null

    private[seq] def setSeqPropertyStore(ps: PKESequentialPropertyStore): Unit = {
        if (this.ps != null)
            throw new IllegalStateException(s"property store is already set: ${this.ps}")

        this.ps = ps
    }

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    def pushInitialTask(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK],
        bottomness:       Int,
        hint:             PropertyComputationHint
    ): Unit = {
        val weight =
            currentDependers.toIterator.map(epk ⇒
                ps.dependeesCount(epk) + ps.dependersCount(epk)).sum
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.poll().task
    }

    def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "ManyDependeesAndDependersOfDirectDependersLastTasksManager"
}

private[seq] class AllDependeesTasksManager(
        final val forward:           Boolean = true,
        final val manyDependeesLast: Boolean = true
) extends TasksManager {

    private[this] var ps: PKESequentialPropertyStore = null

    private[seq] def setSeqPropertyStore(ps: PKESequentialPropertyStore): Unit = {
        if (this.ps != null)
            throw new IllegalStateException(s"property store is already set: ${this.ps}")

        this.ps = ps
    }

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    def pushInitialTask(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK],
        bottomness:       Int,
        hint:             PropertyComputationHint
    ): Unit = {
        var weight = 0

        if (forward) {
            var allDependees = Set.empty[SomeEOptionP]
            var newDependees = dependees.toList
            while (newDependees.nonEmpty) {
                val nextDependee = newDependees.head
                newDependees = newDependees.tail
                allDependees += nextDependee
                ps.dependees(nextDependee.toEPK) foreach { nextNextDependee ⇒
                    if (!allDependees.contains(nextNextDependee)) {
                        newDependees ::= nextNextDependee
                        allDependees += nextNextDependee
                    }
                }
            }
            weight = allDependees.size
        } else {
            // dependees of dependers
            var allDependers = Set.empty[SomeEPK]
            var newDependers = currentDependers.toList
            while (newDependers.nonEmpty) {
                val nextDepender = newDependers.head
                newDependers = newDependers.tail
                allDependers += nextDepender
                ps.dependers(nextDepender) foreach { nextNextDepender ⇒
                    if (!allDependers.contains(nextNextDepender)) {
                        newDependers ::= nextNextDepender
                        weight += ps.dependeesCount(nextNextDepender)
                        allDependers += nextNextDepender
                    }
                }
            }
        }

        if (!manyDependeesLast) weight = -weight

        //println("Weight: "+weight+"   -     Tasks:"+size)
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.poll().task
    }

    def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = {
        s"AllDependeesTasksManager(forward=$forward,manyDependeesLast=$manyDependeesLast)"
    }
}