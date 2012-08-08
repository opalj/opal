package de.tud.cs.st.bat.resolved.analyses

import de.tud.cs.st.bat.resolved._
import de.tud.cs.st.bat.resolved.Field
import de.tud.cs.st.bat.resolved.GETSTATIC
import de.tud.cs.st.bat.resolved.GETFIELD

/**
 *
 * Author: Ralf Mitschke
 * Date: 07.08.12
 * Time: 16:36
 *
 */
object BaseAnalyses
{
    /**
     * Returns all declared fields ever read by any method in any analyzed class
     * as tuple (declaringClass, name, fieldType)
     */
    def readFields(classFiles: Traversable[ClassFile]): Set[(ObjectType, String, Type)] = {
        (for (classFile ← classFiles if !classFile.isInterfaceDeclaration;
              method ← classFile.methods if method.body.isDefined;
              instruction ← method.body.get.instructions
              if (
                      instruction match {
                          case _: GETFIELD ⇒ true
                          case _: GETSTATIC ⇒ true
                          case _ ⇒ false
                      }
                      )
        ) yield {
            instruction match {
                case GETFIELD(declaringClass, name, fieldType) ⇒ (declaringClass, name, fieldType)
                case GETSTATIC(declaringClass, name, fieldType) ⇒ (declaringClass, name, fieldType)
            }
        }).toSet
    }
}