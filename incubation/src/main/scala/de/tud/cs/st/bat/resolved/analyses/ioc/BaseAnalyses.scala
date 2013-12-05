/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st.bat.resolved
package analyses
package bug_patterns.ioc

import instructions._

/**
 *
 * @author Ralf Mitschke
 */
object BaseAnalyses {
    /**
     * Returns all declared fields ever read by any method in any analyzed class
     * as tuple (from,field) = ((classFile,Method)(declaringClass, name, fieldType))
     */
    def readFields(classFiles: Traversable[ClassFile]): Set[((ClassFile, Method), (ObjectType, String, Type))] = {
        (for (
            classFile ← classFiles if !classFile.isInterfaceDeclaration;
            method ← classFile.methods if method.body.isDefined;
            instruction ← method.body.get.instructions if (
                instruction match {
                    case _: GETFIELD  ⇒ true
                    case _: GETSTATIC ⇒ true
                    case _            ⇒ false
                }
            )
        ) yield {
            instruction match {
                case GETFIELD(declaringClass, name, fieldType)  ⇒ ((classFile, method), (declaringClass, name, fieldType))
                case GETSTATIC(declaringClass, name, fieldType) ⇒ ((classFile, method), (declaringClass, name, fieldType))
            }
        }).toSet
    }

    /**
     * Returns true if the method is also declared in the superclass; regardless of abstract or interface methods
     */
    def isOverride(project: Project[_])(classFile: ClassFile)(method: Method): Boolean = {
        // TODO we could also check for an @Override annotation
        val superMethods =
            for (
                superclass ← project.classHierarchy.allSupertypes(classFile.thisType);
                method ← project.classHierarchy.resolveMethodReference(superclass, method.name, method.descriptor, project)
            ) yield {
                method
            }

        superMethods.size > 0
    }

    /**
     * Returns the field declared in a given classFile
     */
    def findField(classFile: ClassFile)(name: String, fieldType: FieldType): Option[Field] = {
        classFile.fields.find {
            case Field(_, `name`, `fieldType`) ⇒ true
            case _                             ⇒ false
        }
    }

    /**
     * Returns true if classFile declares the given Field
     */
    def declaresField(classFile: ClassFile)(name: String, fieldType: FieldType): Boolean = {
        classFile.fields.exists {
            case Field(_, `name`, `fieldType`) ⇒ true
            case _                             ⇒ false
        }
    }

    /**
     * Returns the super constructor called in the given constructor or None
     */
    def calledSuperConstructor(project: Project[_])(classFile: ClassFile,
                                                    constructor: Method): Option[(ClassFile, Method)] = {
        if (!project.classHierarchy.isKnown(classFile.thisType))
            return None

        val constructorCall = constructor.body.get.instructions.collectFirst {
            case INVOKESPECIAL(trgt, n, d) if project.classHierarchy.allSupertypes(classFile.thisType).contains(trgt.asInstanceOf[ObjectType]) ⇒
                (trgt.asInstanceOf[ObjectType], n, d)

        }

        if (!constructorCall.isDefined)
            return None

        val Some((targetType, name, desc)) = constructorCall
        project.classHierarchy.resolveMethodReference(targetType, name, desc, project).map { method ⇒
            val classFile = project.classFile(method)
            (classFile, method)
        }
    }

    def calls(sourceMethod: Method, targetClass: ClassFile, targetMethod: Method): Boolean = {
        sourceMethod.body.isDefined &&
            sourceMethod.body.get.instructions.exists {
                case INVOKEINTERFACE(targetType, name, desc) ⇒ targetClass.thisType == targetType && targetMethod
                    .name == name && targetMethod.descriptor == desc
                case INVOKEVIRTUAL(targetType, name, desc) ⇒ targetClass.thisType == targetType && targetMethod
                    .name == name && targetMethod.descriptor == desc
                case INVOKESTATIC(targetType, name, desc) ⇒ targetClass.thisType == targetType && targetMethod
                    .name == name && targetMethod.descriptor == desc
                case INVOKESPECIAL(targetType, name, desc) ⇒ targetClass.thisType == targetType && targetMethod
                    .name == name && targetMethod.descriptor == desc
                case _ ⇒ false
            }
    }

    /**
     * Returns a filtered sequence of instructions without the bytecode padding
     * // FIXME Usage of "BaseAnalyses.withIndex" leads to grossly inefficient code!
     */
    def withIndex(instructions: Array[Instruction]): Seq[(Instruction, Int)] = {
        instructions.zipWithIndex.filter {
            case (instr, _) ⇒ instr != null
        }
    }
}