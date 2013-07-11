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
package de.tud.cs.st
package bat
package resolved
package analyses

import util.graphs.{ Node, toDot }

import reader.Java7Framework

/**
 * Container for all class files of a project and the resulting class hierarchy.
 *
 * ==Usage==
 * To create a representation of a project use the `++` and `+` method.
 *
 * ==Thread Safety==
 * This class is immutable.
 *
 * @tparam S The type of the source of the class file. E.g., a `URL`, a `File` object,
 *    a `String` or a Pair `(JarFile,JarEntry)`. This information is needed for, e.g.,
 *    presenting the user meaningful message w.r.t. the location of some analyis.
 * @param classes A mapping of `ObjectType`s to `ClassFile`s. When the analysis does not
 *    load all classes related to a project, it is possible that no class file is
 *    associated with a specific `ObjectType`.
 * @param sources A mapping of an `ObjectType` to its defining source.
 * @param classHierarchy This project's class hierarchy.
 *
 * @author Michael Eichberg
 */
class Project[S](
        val classes: Map[ObjectType, ClassFile],
        val sources: Map[ObjectType, S],
        val classHierarchy: ClassHierarchy = new ClassHierarchy()) {

    def this(classHierarchy: ClassHierarchy = new ClassHierarchy()) {
        this(Map[ObjectType, ClassFile](), Map[ObjectType, S](), classHierarchy)
    }

    /**
     * Adds the class files to this project by calling the simple "+" method
     * for each class file.
     */
    def ++(classFiles: Traversable[(ClassFile, S)]): Project[S] = (this /: classFiles)(_ + _)

    /**
     * Adds the given class file to this project. If the class defines an object
     * type that was previously added, the old class file will be replaced
     * by the given one.
     */
    def +(cs: (ClassFile, S)): Project[S] = {
        val (classFile, source) = cs
        new Project(
            classes + ((classFile.thisClass, classFile)),
            sources + ((classFile.thisClass, source)),
            classHierarchy + classFile)
    }

    /**
     * This project's current class files.
     */
    def classFiles: Iterable[ClassFile] = classes.values

    /**
     * Looks up the class file and method which actually declares the method that is referred
     * to by the given receiver type, method name and method descriptor.
     *
     * In most cases this will be the receiver's class. For example, if you look
     * up the method declaration of a method that is called using invokestatic then
     * (if the project is valid) the class of receiver must define the respective method.
     * In some cases – however – it might be one (or more) superclasses. In the latter
     * case the declaration of the method by a superclass has precedence over a
     * declaration by an interface.
     *
     * This method does not take visibility modifiers or the static modifier into account; i.e,
     * it assumes that the presented project is valid. In the latter case this method can
     * also be used to reliably lookup a private method's declaration or the declaration of
     * a constructor/a static method.
     *
     * ==Note==
     * This method might be of limited value if static source code dependencies
     * are analyzed. If an invoke instruction refers to a method that is not declared
     * by the receiver's class, then it might be more meaningful to still create a dependency
     * to the receiver's class than to look up the actual declaration in one of the
     * receiver's super classes.
     *
     * @return `Some((ClassFile,Method))` if the method is found. `None` if the method is not
     *      found. This can happen under two circumstances. First, not all class files
     *      referred to/used by the project are (yet) analyzed; i.e., we do not have the
     *      complete view on all class files belonging to the project. Second, the analyzed
     *      class files do not belong together (they either belong to different projects or
     *      to incompatible versions of the same project.)
     */
    def lookupMethodDeclaration(receiver: ObjectType,
                                methodName: String,
                                methodDescriptor: MethodDescriptor): Option[(ClassFile, Method)] = {
        // TODO [Java 7] How to support lookupMethod for dynamic method calls?
        val clazz = classes.get(receiver).
            getOrElse({ return None; })

        (clazz.methods.collectFirst { case m @ Method(_, `methodName`, `methodDescriptor`, _) ⇒ m }) match {
            case Some(method) ⇒ return Some(clazz, method)
            case None ⇒ {
                if (clazz.superClass.isDefined) {
                    val result = lookupMethodDeclaration(clazz.superClass.get, methodName, methodDescriptor);
                    if (result.isDefined) {
                        return result;
                    }
                }
                return clazz.interfaces.collectFirst(
                    lookupMethodDeclaration(_, methodName, methodDescriptor) match {
                        case Some(m) ⇒ m
                    }
                )

            }
        }
    }
}

/**
 * Factory for [[de.tud.cs.st.bat.resolved.analyses.Project]] objects.
 *
 * @author Michael Eichberg
 */
object Project {

    /**
     * Creates a project that contains no class files, but where the class hierarchy
     * already contains the information about the exceptions thrown by JVM
     * instructions.
     */
    def empty[S]() =
        new Project[S](classHierarchy = ClassHierarchy.createPreInitializedClassHierarchy)

}
