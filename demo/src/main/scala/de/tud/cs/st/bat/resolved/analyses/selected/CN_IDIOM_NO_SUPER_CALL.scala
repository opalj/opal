package de.tud.cs.st.bat.resolved.analyses.selected

import de.tud.cs.st.bat.resolved.analyses.Project
import de.tud.cs.st.bat.resolved.{INVOKESPECIAL, ObjectType, MethodDescriptor, Method}

/**
 *
 * @author Ralf Mitschke
 *
 */
object CN_IDIOM_NO_SUPER_CALL
{

    def apply(project: Project) =
        for {
            classFile ← project.classFiles
            if !classFile.isInterfaceDeclaration && !classFile.isAnnotationDeclaration
            superClass ← classFile.superClass.toList
            method@Method(_, "clone", MethodDescriptor(Seq(), ObjectType.Object), _) ← classFile.methods
            if !method.isAbstract
            if !method.body.get.instructions.exists({
                case INVOKESPECIAL(`superClass`, "clone", MethodDescriptor(Seq(), ObjectType.Object)) ⇒ true
                case _ ⇒ false
            })
        } yield (classFile /*.thisClass.className*/ , method /*.name*/ )

}