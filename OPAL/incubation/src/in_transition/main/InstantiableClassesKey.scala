/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package org.opalj
package br
package analyses
package cg

import org.opalj.concurrent.defaultIsInterrupted

/**
 * The ''key'' object to get information about the classes of which we could have an
 * instance at runtime.
 *
 * @example To get the index use the [[Project]]'s `get` method and pass in `this` object.
 *
 * @author Michael Eichberg
 */
object InstantiableClassesKey extends ProjectInformationKey[InstantiableClasses, Nothing] {

    /**
     * The [[InstantiableClasses]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Computes the information which classes are (not) instantiable.
     *
     * @see [[InstantiableClasses]] and [[InstantiableClassesAnalysis]]
     */
    override protected def compute(project: SomeProject): InstantiableClasses = {
        // The "MayHaveInstancesAnalysis" considers the information of the [[TypeExtensibilityAnalysis]].
        InstantiableClassesAnalysis.doAnalyze(project, defaultIsInterrupted)
    }
}

/* ******************************* OLD *****************************
/**
 * This analysis determines which classes can never be instantiated (e.g.,
 * `java.lang.Math`).
 *
 * A class is not instantiable if:
 *  - it only defines private constructors and these constructors are not called
 *    by any static method and the class does not implement Serializable.
 *
 * @note This analysis depends on the project configuration which encodes the analysis mode.
 *       Different analysis modes are: library with open or closed packages assumption or application
 *
 * This information is relevant in various contexts, e.g., to determine
 * precise call graph. For example, instance methods of those objects that cannot be
 * created are always dead.
 *
 * @note The analysis does not take reflective instantiations into account!
 */
class SimpleInstantiabilityAnalysis private (val project: SomeProject) extends ProjectBasedAnalysis {

    import project.classHierarchy.allSubclassTypes

    def determineProperty(
        key:        String,
        classFiles: Seq[ClassFile]
    ): Traversable[EP[ClassFile, Instantiability]] = {

        val instantiatedClasses = new Array[EP[ClassFile, Instantiability]](classFiles.length)
        val seenConstructors = mutable.Set.empty[ClassFile]

        var i = 0
        while (i < classFiles.length) {
            val classFile = classFiles(i)
            if (project.libraryClassFilesAreInterfacesOnly && project.isLibraryType(classFile)) {
                if (classFile.isAbstract) {
                    val hasInstantiableSubtype = allSubclassTypes(classFile.thisType, reflexive = false).exists {
                        subtype ⇒
                            project.classFile(subtype) match {
                                case Some(cf) ⇒ !cf.isAbstract
                                case None     ⇒ true
                            }
                    }
                    if (hasInstantiableSubtype)
                        instantiatedClasses(i) = EP(classFile, Instantiable)
                    else
                        instantiatedClasses(i) = EP(classFile, NotInstantiable)
                } else
                    instantiatedClasses(i) = EP(classFile, Instantiable)
            } else {
                classFile.methods foreach { method ⇒
                    if (method.isNative && method.isStatic) {
                        val instantiatedClasses = mutable.Set.empty[EP[ClassFile, Instantiability]]
                        classFiles.foreach { classFile ⇒
                            if (classFile.isAbstract &&
                                (isDesktopApplication || (isClosedLibrary && classFile.isPackageVisible)))
                                instantiatedClasses += EP(classFile, NotInstantiable)
                            else
                                instantiatedClasses += EP(classFile, Instantiable)
                        }
                        // we can stop here, we have to assume that native methods instantiate every package visible class
                        return instantiatedClasses;

                    } else if (method.body.nonEmpty) {
                        // prevents the analysis of native instance methods..

                        val body = method.body.get
                        val instructions = body.instructions
                        val max = instructions.length
                        var pc = 0
                        while (pc < max) {
                            val instruction = instructions(pc)
                            if (instruction.opcode == INVOKESPECIAL.opcode) {
                                instruction match {
                                    case INVOKESPECIAL(classType, _, "<init>", _) if classType.packageName == key ⇒
                                        // We found a constructor call.
                                        val classFile = project.classFile(classType)
                                        if (classFile.nonEmpty) {
                                            seenConstructors += classFile.get
                                        }
                                    case _ ⇒
                                }
                            }
                            pc = body.pcOfNextInstruction(pc)
                        }
                    } else {
                        // we dont know what happens, be conservative
                        if (!method.isAbstract)
                            instantiatedClasses(i) = EP(classFile, Instantiable)
                    }
                }
            }
            i += 1
        }

        i = 0
        while (i < classFiles.length) {
            val resultClassFile = instantiatedClasses(i)
            if (resultClassFile eq null) {
                val entityClassFile = classFiles(i)
                val constructorInvoked = seenConstructors.collectFirst {
                    case cf: ClassFile if (cf eq entityClassFile) ⇒ cf
                }
                constructorInvoked match {
                    case Some(_) ⇒
                        instantiatedClasses(i) = EP(entityClassFile, Instantiable)
                    case None ⇒
                        instantiatedClasses(i) = determineClassInstantiability(entityClassFile)
                }
            }
            i += 1
        }

        instantiatedClasses
    }

    def determineClassInstantiability(classFile: ClassFile): EP[ClassFile, Instantiability] = {
        import project.classHierarchy.isSubtypeOf

        if (classFile.isAbstract || classFile.isInterfaceDeclaration) {
            if (isDesktopApplication || (isClosedLibrary && classFile.isPackageVisible))
                // if we analyze an application, abstract classes are not instantiable
                // if we analyze an library, abstract classes could have subtypes in the future
                // hence, we have to assume that the methods of the class are called by future subtypes.
                // if the class is not visible to client, we can consider it as not instantiable, because
                // we know all subtypes and if a method is invoked then, we will recognize it.
                return EP(classFile, NotInstantiable);
        }

        val classType = classFile.thisType

        if (isSubtypeOf(classType, ObjectType.Serializable).isYesOrUnknown &&
            classFile.hasDefaultConstructor)
            //if the class is Serializable or it is unknown, we have to count it as instantiated.
            return EP(classFile, Instantiable)

        val notFinal = !classFile.isFinal
        if ((classFile.isPublic || isOpenLibrary)) {
            if (classFile.constructors exists { cons ⇒
                cons.isPublic ||
                    (isOpenLibrary && !cons.isPrivate) ||
                    (notFinal && cons.isProtected)
                //If the class not final and public or we analyze an open library we have
                //to assume that a subclass is created and instantiated later on.
                //Hence, every time a subclass is instantiated all superclass' have to be
                //considered instantiated as well.
            })
                return EP(classFile, Instantiable);
        }

        return EP(classFile, NotInstantiable);
    }
}

/**
 * Companion object for the [[SimpleInstantiabilityAnalysis]] class.
 */
object SimpleInstantiabilityAnalysis {

    final def definingPackage(cf: ClassFile): String = cf.thisType.packageName

    def run(project: SomeProject): Traversable[EP[ClassFile, Instantiability]] = {
        val analysis = new SimpleInstantiabilityAnalysis(project)
        project.allProjectClassFiles.groupBy(definingPackage).par.flatMap { groupedCFs ⇒
            val (key, cfs) = groupedCFs
            analysis.determineProperty(key, cfs)
        }(ParIterable.canBuildFrom).seq
    }

    def apply(project: SomeProject): Unit = {
        val ps = project.get(PropertyStoreKey)
        run(project).foreach(ep ⇒ ps.set(ep.e, ep.p))
    }
}
*/ 