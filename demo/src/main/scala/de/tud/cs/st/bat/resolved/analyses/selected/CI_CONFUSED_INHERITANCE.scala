package de.tud.cs.st.bat.resolved.analyses.selected

import de.tud.cs.st.bat.resolved.analyses.Project

/**
 *
 * @author Ralf Mitschke
 *
 */
object CI_CONFUSED_INHERITANCE
{

    def apply(project: Project) =
        for (
            classFile ← project.classFiles if classFile.isFinal;
            field ← classFile.fields if field.isProtected
        ) yield (classFile, field)

}