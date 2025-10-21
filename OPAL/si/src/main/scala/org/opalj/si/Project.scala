/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package si

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReferenceArray

import com.typesafe.config.Config

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.util.PerformanceEvaluation.time

trait Project {

    protected var projectInformation: AtomicReferenceArray[AnyRef]
    implicit val logContext: LogContext
    implicit val config: Config
    // --------------------------------------------------------------------------------------------
    //
    //    CODE TO MAKE IT POSSIBLE TO ATTACH SOME INFORMATION TO A PROJECT (ON DEMAND)
    //
    // --------------------------------------------------------------------------------------------

    /**
     * Here, the usage of the project information key does not lead to its initialization!
     */
    private val projectInformationKeyInitializationData = {
        new ConcurrentHashMap[ProjectInformationKey[? <: Project, AnyRef, AnyRef], AnyRef]()
    }

    /**
     * Returns the project specific initialization information for the given project information
     * key.
     */
    def getProjectInformationKeyInitializationData[P <: Project, T <: AnyRef, I <: AnyRef](
        key: ProjectInformationKey[P, T, I]
    ): Option[I] = {
        Option(projectInformationKeyInitializationData.get(key).asInstanceOf[I])
    }

    /**
     * Gets the project information key specific initialization object. If an object is already
     * registered, that object will be used otherwise `info` will be evaluated and that value
     * will be added and also returned.
     *
     * @note Initialization data is discarded once the key is used.
     */
    def getOrCreateProjectInformationKeyInitializationData[P <: Project, T <: AnyRef, I <: AnyRef](
        key:  ProjectInformationKey[P, T, I],
        info: => I
    ): I = {
        projectInformationKeyInitializationData.computeIfAbsent(
            key.asInstanceOf[ProjectInformationKey[P, AnyRef, AnyRef]],
            (_: ProjectInformationKey[?, AnyRef, AnyRef]) => info
        ).asInstanceOf[I]
    }

    /**
     * Updates project information key specific initialization object. If an object is already
     * registered, that object will be given to `info`.
     *
     * @note Initialization data is discarded once the key is used.
     */
    def updateProjectInformationKeyInitializationData[P <: Project, T <: AnyRef, I <: AnyRef](
        key: ProjectInformationKey[P, T, I]
    )(
        info: Option[I] => I
    ): I = {
        projectInformationKeyInitializationData.compute(
            key.asInstanceOf[ProjectInformationKey[P, AnyRef, AnyRef]],
            (_, current: AnyRef) =>
                {
                    info(Option(current.asInstanceOf[I]))
                }: I
        ).asInstanceOf[I]
    }

    /**
     * Returns the additional project information that is ''currently'' available.
     *
     * If some analyses are still running it may be possible that additional
     * information will be made available as part of the execution of those
     * analyses.
     *
     * @note This method redetermines the available project information on each call.
     */
    def availableProjectInformation: List[AnyRef] = {
        var pis = List.empty[AnyRef]
        val projectInformation = this.projectInformation
        for (i <- 0 until projectInformation.length()) {
            val pi = projectInformation.get(i)
            if (pi != null) {
                pis = pi :: pis
            }
        }
        pis
    }

    /**
     * Returns the information attached to this project that is identified by the
     * given `ProjectInformationKey`.
     *
     * If the information was not yet required, the information is computed and
     * returned. Subsequent calls will directly return the information.
     *
     * @note (Development Time)
     *       Every analysis using [[ProjectInformationKey]]s must list '''All
     *       requirements; failing to specify a requirement can end up in a deadlock.'''
     * @see [[ProjectInformationKey]] for further information.
     */
    def get[P <: Project, T <: AnyRef](pik: ProjectInformationKey[P, T, ?]): T = {
        val pikUId = pik.uniqueId

        /* Synchronization is done by the caller! */
        def derive(projectInformation: AtomicReferenceArray[AnyRef]): T = {
            var className = pik.getClass.getSimpleName
            if (className.endsWith("Key"))
                className = className.substring(0, className.length - 3)
            else if (className.endsWith("Key$"))
                className = className.substring(0, className.length - 4)

            for (requiredProjectInformationKey <- pik.requirements(this.asInstanceOf[P])) {
                get(requiredProjectInformationKey)
            }
            val pi = time {
                val pi = pik.compute(this.asInstanceOf[P])
                // we don't need the initialization data anymore
                projectInformationKeyInitializationData.remove(pik)
                pi
            } { t => info("project", s"initialization of $className took ${t.toSeconds}") }
            projectInformation.set(pikUId, pi)
            pi
        }

        val projectInformation = this.projectInformation
        if (pikUId < projectInformation.length()) {
            val pi = projectInformation.get(pikUId)
            if (pi ne null) {
                pi.asInstanceOf[T]
            } else {
                this.synchronized {
                    // It may be the case that the underlying array was replaced!
                    val projectInformation = this.projectInformation
                    // double-checked locking (works with Java >=6)
                    val pi = projectInformation.get(pikUId)
                    if (pi ne null) {
                        pi.asInstanceOf[T]
                    } else {
                        derive(projectInformation)
                    }
                }
            }
        } else {
            // We have to synchronize w.r.t. "this" object on write accesses
            // to make sure that we do not loose a concurrent update or
            // derive an information more than once.
            this.synchronized {
                val projectInformation = this.projectInformation
                if (pikUId >= projectInformation.length()) {
                    val newLength = Math.max(projectInformation.length * 2, pikUId * 2)
                    val newProjectInformation = new AtomicReferenceArray[AnyRef](newLength)
                    org.opalj.control.iterateUntil(0, projectInformation.length()) { i =>
                        newProjectInformation.set(i, projectInformation.get(i))
                    }
                    this.projectInformation = newProjectInformation
                    return derive(newProjectInformation)
                }
            }
            // else (pikUId < projectInformation.length()) => the underlying array is "large enough"
            get(pik)
        }
    }

    /**
     * Tests if the information identified by the given [[ProjectInformationKey]]
     * is available. If the information is not (yet) available, the information
     * will not be computed; `None` will be returned.
     *
     * @see [[ProjectInformationKey]] for further information.
     */
    def has[P <: Project, T <: AnyRef](pik: ProjectInformationKey[P, T, ?]): Option[T] = {
        val pikUId = pik.uniqueId

        if (pikUId < this.projectInformation.length())
            Option(this.projectInformation.get(pikUId).asInstanceOf[T])
        else
            None
    }
}
