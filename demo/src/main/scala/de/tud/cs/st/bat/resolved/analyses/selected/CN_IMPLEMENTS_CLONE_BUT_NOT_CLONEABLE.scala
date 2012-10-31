package de.tud.cs.st.bat.resolved.analyses.selected

import de.tud.cs.st.bat.resolved.{ObjectType, MethodDescriptor, Method}
import de.tud.cs.st.bat.resolved.analyses.Project

/**
 *
 * @author Ralf Mitschke
 *
 */
object CN_IMPLEMENTS_CLONE_BUT_NOT_CLONEABLE
{

    def apply(project: Project) =
        for {
            classFile ← project.classFiles if !classFile.isAnnotationDeclaration && classFile.superClass.isDefined
            method@Method(_, "clone", MethodDescriptor(Seq(), ObjectType.Object), _) ← classFile.methods
            if !project.classHierarchy.isSubtypeOf(classFile.thisClass, ObjectType("java/lang/Cloneable"))
                    .getOrElse(false)
        } yield (classFile, method)

}