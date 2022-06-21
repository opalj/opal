/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.jdk.CollectionConverters._

import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.Error
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import org.opalj.log.StandardLogContext

/**
 * Enables the querying of a project.
 *
 * @note '''The interface of this class was designed with Java interoperability in mind!'''
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
class JavaProject( final val project: Project[java.net.URL]) {

    /**
     * @param classPath A list of files and jars, where to look for classes.
     */
    def this(classPath: java.util.List[java.io.File]) =
        this({
            implicit val logCtx = new StandardLogContext()
            OPALLogger.register(logCtx, JavaProject.Logger)
            val cp = classPath.asScala
            Project(
                Project.JavaClassFileReader(theLogContext = logCtx).AllClassFiles(cp),
                Iterable.empty, true, /*true or false... doesn't matter when we have no lib. */
                Iterable.empty,
                Project.defaultHandlerForInconsistentProjects,
                BaseConfig,
                logCtx
            )
        })

    /**
     * Returns the list of all classes that derive from `objectType`.
     *
     * @param objectType The object type in jvm annotation (using "/" instead of ".", e.g.
     *                   "java/util/List")
     * @return A list of classes that derive from objectType.
     */
    def getAllSubclassesOfObjectType(objectType: String): java.util.List[String] = {
        project
            .classHierarchy
            .allSubtypes(ObjectType(objectType), reflexive = false)
            .map(ot => ot.toJava)
            .toList
            .asJava
    }
}

object JavaProject {

    // Suppress all opal info related log messages and show only errors.
    private final val Logger = new ConsoleOPALLogger(true, Error)

    OPALLogger.updateLogger(GlobalLogContext, Logger)

}
