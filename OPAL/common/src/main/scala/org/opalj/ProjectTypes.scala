/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

/**
 * The project type specifies the type of the project/the kind of sources which will be
 * analyzed.
 *
 * @author Michael Eichberg
 */
object ProjectTypes extends Enumeration {

    final val Library = Value("library")

    final val CommandLineApplication = Value("command-line application")

    /**
     * This mode shall be used if a standard Java GUI application is analyzed which is started by
     * the JVM by calling the application's main method.
     */
    final val GUIApplication = Value("gui application")

    final val JEE6WebApplication = Value("jee6+ web application")

}

/**
 * Common constants related to the project type.
 *
 * @note The package defines the type `ProjectType`.
 */
object ProjectType {
    final val ConfigKey = "org.opalj.project.type"
}
