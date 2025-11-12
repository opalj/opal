/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import scala.annotation.tailrec

import scala.collection.mutable

import org.opalj.collection.mutable.ArrayMap

/**
 * Determines if a type (class or interface) is further (transitively) extensible by yet unknown
 * types (that is, can be (transitively) inherited from).
 *
 * == Special cases ==
 * If a class is defined in a package starting with '''java.*''', it always has to be treated like
 * classes that belong to a closed package. This is necessary because the default `ClassLoader`
 * prevents the definition of further classes within these packages, hence, they are closed by
 * definition.
 *
 * If the analyzed codebase has an incomplete type hierarchy, which leads to unknown subtype
 * relationships, it is necessary to add these particular classes to the computed set of
 * extensible classes.
 *
 * @author Michael Reif
 */
class TypeExtensibilityAnalysis(val project: SomeProject) extends (ClassType => Answer) {

    // format: off
    import project.classHierarchy
    import classHierarchy.foreachDirectSupertype
    // format: on

    @tailrec private def determineExtensibility(
        typesToProcess:       mutable.Queue[ClassType],
        subtypeExtensibility: Array[Answer],
        isEnqueued:           Array[Boolean],
        typeExtensibility:    ArrayMap[Answer]
    )(
        implicit isClassExtensible: ClassType => Answer
    ): ArrayMap[Answer] = {
        val classType = typesToProcess.dequeue()
        val cid = classType.id

        val thisSubtypeExtensibility = {
            val thisSubtypeExtensibility = subtypeExtensibility(cid)
            if (thisSubtypeExtensibility eq null) No else thisSubtypeExtensibility
        }
        val thisTypeExtensibility = isClassExtensible(classType) match {
            case Yes     => Yes
            case Unknown => if (thisSubtypeExtensibility.isYes) Yes else Unknown
            case No      => thisSubtypeExtensibility
        }
        typeExtensibility(cid) = thisTypeExtensibility
        var update = false
        foreachDirectSupertype(classType) { st =>
            val scid = st.id
            subtypeExtensibility(scid) match {
                case null | No => {
                    update = subtypeExtensibility(scid) ne thisTypeExtensibility
                    subtypeExtensibility(scid) = thisTypeExtensibility
                }
                case Yes     => // do nothing
                case Unknown => {
                    update = subtypeExtensibility(scid) ne thisTypeExtensibility
                    if (thisTypeExtensibility.isYes) subtypeExtensibility(scid) = Yes
                }
            }

            // schedule supertypes
            if (!isEnqueued(scid) || update) {
                typesToProcess.enqueue(st)
                isEnqueued(scid) = true
            }
        }

        if (typesToProcess.nonEmpty) {
            // tail-recursive call... to process the workqueue
            determineExtensibility(
                typesToProcess,
                subtypeExtensibility,
                isEnqueued,
                typeExtensibility
            )
        } else {
            typeExtensibility
        }
    }

    private val typeExtensibility: ArrayMap[Answer] = {
        implicit val isClassExtensible: ClassExtensibility = project.get(ClassExtensibilityKey)

        val leafTypes = classHierarchy.leafTypes
        val classTypesCount = ClassType.classTypesCount
        val typeExtensibility = ArrayMap[Answer](classTypesCount)
        val subtypeExtensibility = new Array[Answer](classTypesCount)
        val isEnqueued = new Array[Boolean](classTypesCount)

        // We use a queue to ensure that we always first process all subtypes of a type. This
        // guarantees that we have final knowledge about the extensibility of all subtypes
        // of a type before we are processing the supertype.
        val typesToProcess = mutable.Queue.empty[ClassType] ++ leafTypes
        typesToProcess.foreach { ct => isEnqueued(ct.id) = true }

        determineExtensibility(
            typesToProcess,
            subtypeExtensibility,
            isEnqueued,
            typeExtensibility
        )
    }

    override def apply(t: ClassType): Answer = typeExtensibility.getOrElse(t.id, Unknown)
}
