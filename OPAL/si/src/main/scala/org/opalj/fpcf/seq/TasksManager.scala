/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

import java.util.ArrayDeque
import java.util.PriorityQueue
import java.util.LinkedHashMap

import scala.collection.mutable
import scala.collection.JavaConverters._

trait TasksManager {

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
        task:             QualifiedTask,
        eOptionP:         SomeEOptionP, // the current eOptionP to which the task is related
        dependees:        Traversable[SomeEOptionP], // the dependees of the eOptionP
        currentDependers: Traversable[SomeEPK]
    ): Unit

    def push(
        task:      QualifiedTask,
        eOptionPs: Traversable[SomeEPK], // the current eOptionP to which the task is related
        dependees: Traversable[SomeEOptionP] // the dependees of the eOptionP
    ): Unit

    def poll(): QualifiedTask

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
        eOptionP:         SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        this.tasks.addFirst(task)
    }

    override def push(
        task:      QualifiedTask,
        eOptionPs: Traversable[SomeEPK],
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        this.tasks.addFirst(task)
    }

    override def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.pollFirst()
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
        eOptionPs:        SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        this.tasks.addLast(task)
    }

    override def push(
        task:      QualifiedTask,
        eOptionP:  Traversable[SomeEPK],
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        this.tasks.addLast(task)
    }

    override def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.pollFirst()
    }

    override def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    override def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "FIFOTasksManager"
}

private[seq] final class EntityAccessOrderingBasedTasksManager extends TasksManager {

    private[this] val otherTasks: ArrayDeque[QualifiedTask] = new ArrayDeque(65536)

    private[this] var currentEntity: Entity = null
    private[this] var currentTasks: List[QualifiedTask] = Nil
    private[this] val entityBasedTasks: LinkedHashMap[Entity, List[QualifiedTask]] = {
        new LinkedHashMap(256, 0.75f, true /* in access order! */ )
    }

    override def push(task: QualifiedTask): Unit = {
        if (task.isEntityBasedTask) {
            val e = task.asEntityBasedTask.e
            val tasks = entityBasedTasks.getOrDefault(e, Nil)
            entityBasedTasks.put(e, task :: tasks)
        } else {
            this.otherTasks.addFirst(task)
        }
    }

    override def push(
        task:             QualifiedTask,
        eOptionP:         SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        push(task)
    }

    override def push(
        task:      QualifiedTask,
        eOptionPs: Traversable[SomeEPK],
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        push(task)
    }

    override def poll(): QualifiedTask = {
        if (currentTasks.nonEmpty) {
            val currentTask = currentTasks.head
            currentTasks = currentTasks.tail
            currentTask
        } else if (!entityBasedTasks.isEmpty) {
            // let's check if we have more tasks related to the entity that we
            // were just processing...
            val newTasks = entityBasedTasks.remove(currentEntity)
            if (newTasks != null) {
                currentTasks = newTasks.tail
                newTasks.head
            } else if (!this.otherTasks.isEmpty) {
                // before we process the next entity, we first process all new tasks
                this.otherTasks.pollFirst()
            } else {
                val it = entityBasedTasks.entrySet.iterator
                val currentEntityTasks = it.next()
                it.remove()
                currentEntity = currentEntityTasks.getKey
                val nextTasks = currentEntityTasks.getValue
                currentTasks = nextTasks.tail
                nextTasks.head
            }
        } else {
            this.otherTasks.pollFirst()
        }
    }

    override def isEmpty: Boolean = {
        this.entityBasedTasks.isEmpty && this.otherTasks.isEmpty && currentTasks.isEmpty
    }

    override def size: Int = {
        this.otherTasks.size +
            this.entityBasedTasks.values.iterator.asScala.map(_.length).sum +
            currentTasks.length
    }

    override def toString: String = "EntityAccessOrderingBasedTasksManager"
}

private class WeightedQualifiedTask(
        val task:   QualifiedTask,
        val weight: Int
) extends Comparable[WeightedQualifiedTask] {
    def compareTo(other: WeightedQualifiedTask) = this.weight - other.weight
}

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
        eOptionP:         SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        val weight = Math.max(1, dependees.size) * Math.max(1, currentDependers.size)
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    override def push(
        task:      QualifiedTask,
        eOptionPs: Traversable[SomeEPK],
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        var currentDependersSize = 0
        eOptionPs foreach { eOptionP ⇒
            currentDependersSize += ps.dependers(eOptionP).size
        }
        val weight = Math.max(1, dependees.size) * Math.max(1, currentDependersSize)
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    override def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.poll().task
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
        eOptionP:         SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        this.tasks.add(new WeightedQualifiedTask(task, currentDependers.size))
    }

    override def push(
        task:      QualifiedTask,
        eOptionPs: Traversable[SomeEPK],
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        var currentDependersSize = 0
        eOptionPs foreach { eOptionP ⇒ currentDependersSize += ps.dependers(eOptionP).size }
        this.tasks.add(new WeightedQualifiedTask(task, currentDependersSize))
    }

    override def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.poll().task
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
        task:             QualifiedTask,
        eOptionP:         SomeEOptionP,
        dependees:        Traversable[SomeEOptionP],
        currentDependers: Traversable[SomeEPK]
    ): Unit = {
        var weight = 0
        currentDependers foreach { epk ⇒ weight += ps.dependeesCount(epk) }
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    override def push(
        task:      QualifiedTask,
        eOptionPs: Traversable[SomeEPK],
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        var weight = 0
        eOptionPs foreach { eOptionP ⇒
            ps.dependers(eOptionP) foreach (epk ⇒ weight += ps.dependeesCount(epk))
        }
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    override def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.poll().task
    }

    override def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    override def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = "ManyDependeesOfDirectDependersLastTasksManager"
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

    override def push(
        task:      QualifiedTask,
        eOptionPs: Traversable[SomeEPK],
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        var weight = 0
        eOptionPs foreach { eOptionP ⇒
            ps.dependers(eOptionP) foreach { epk ⇒
                weight += ps.dependeesCount(epk) + ps.dependersCount(epk)
            }
        }
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    override def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.poll().task
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

    override def push(
        task:      QualifiedTask,
        eOptionPs: Traversable[SomeEPK],
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        var weight = 0
        if (forward) {
            weight = computeForwardWeight(dependees)
        } else {
            eOptionPs foreach { eOptionP ⇒
                val currentDependers = ps.dependers(eOptionP)
                weight += computeBackwardWeight(currentDependers)
            }
        }

        if (!manyDependeesLast) weight = -weight

        //println("Weight: "+weight+"   -     Tasks:"+size)
        this.tasks.add(new WeightedQualifiedTask(task, weight))
    }

    override def poll(): QualifiedTask = {
        val t = this.initialTasks.pollFirst()
        if (t ne null)
            t
        else
            this.tasks.poll().task
    }

    override def isEmpty: Boolean = this.initialTasks.isEmpty && this.tasks.isEmpty

    override def size: Int = this.initialTasks.size + this.tasks.size

    override def toString: String = {
        s"AllDependeesTasksManager(forward=$forward,manyDependeesLast=$manyDependeesLast)"
    }
}
