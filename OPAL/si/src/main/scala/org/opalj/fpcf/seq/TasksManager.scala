/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

import java.util.ArrayDeque
import java.util.PriorityQueue

private[seq] trait TasksManager {

    import PKESequentialPropertyStore.EntityDependersView

    def pushInitialTask(task: QualifiedTask): Unit

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: EntityDependersView, // may change between the time the task is scheduled and the time it is evaluated!
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
private[seq] object LIFOTasksManager extends TasksManager {

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)

    def pushInitialTask(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: PKESequentialPropertyStore.EntityDependersView,
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
}

/**
 * Processes the tasks that are schedulued for the longest time first.
 */
private[seq] object FIFOTasksManager extends TasksManager {

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)

    def pushInitialTask(task: QualifiedTask): Unit = {
        this.initialTasks.addLast(task)
    }

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: PKESequentialPropertyStore.EntityDependersView,
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
private[seq] object ManyDependenciesLastTasksManager extends TasksManager {

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    def pushInitialTask(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: PKESequentialPropertyStore.EntityDependersView,
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
}

private[seq] class ManyDependeesOfDependersLastTasksManager(
        store: PKESequentialPropertyStore
) extends TasksManager {

    private[this] var initialTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)
    private[this] var tasks: PriorityQueue[WeightedQualifiedTask] = new PriorityQueue(50000)

    def pushInitialTask(task: QualifiedTask): Unit = {
        this.initialTasks.addFirst(task)
    }

    def push(
        task:             QualifiedTask,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: PKESequentialPropertyStore.EntityDependersView,
        bottomness:       Int,
        hint:             PropertyComputationHint
    ): Unit = {
        val weight = currentDependers.keys.map(epk ⇒ store.dependeesCount(epk)).sum
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
}