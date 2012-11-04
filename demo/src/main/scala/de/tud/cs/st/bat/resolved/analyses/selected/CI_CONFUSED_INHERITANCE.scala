package de.tud.cs.st.bat.resolved.analyses.selected

import de.tud.cs.st.bat.resolved.analyses.Project
import de.tud.cs.st.bat.resolved.{Field, ClassFile}

/**
 *
 * @author Ralf Mitschke
 *
 */
object CI_CONFUSED_INHERITANCE
    extends (Project => Iterable[(ClassFile, Field)])
{

    def apply(project: Project) =
        for (
            classFile ← project.classFiles if classFile.isFinal;
            field ← classFile.fields if field.isProtected
        ) yield (classFile, field)

}