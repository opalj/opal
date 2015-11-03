/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package fpcf
package analysis

import org.opalj.br.{ObjectType, ClassFile}
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKESPECIAL

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
 *  This information is relevant in various contexts, e.g., to determine
 * precise call graph. For example, instance methods of those objects that cannot be
 * created are always dead.
 *
 * ==Usage==
 * Use the [[FPCFAnalysisManagerKey]] to query the analysis manager of a project. You can run
 * the analysis afterwards as follows:
 * {{{
 *  val analysisManager = project.get(FPCFAnalysisManagerKey)
 *  analysisManager.run(InstantiabilityAnalysis)
 * }}}
 * For detailed information see the documentation of the analysis manager.
 *
 * The results of this analysis are stored in the property store of the project. You can receive
 * the results as follows:
 * {{{
 * val theProjectStore = theProject.get(SourceElementsPropertyStoreKey)
 * val instantiableClasses = theProjectStore.entities { (p: Property) ⇒
 * p == Instantiable
 * }
 * }}}
 *
 * This information is relevant in various contexts, e.g., to determine
 * precise call graph. For example, instance methods of those objects that cannot be
 * created are always dead.
 *
 * @note The analysis does not take reflective instantiations into account!
 */
class SimpleInstantiabilityAnalysis private (
    project: SomeProject
)
        extends AbstractGroupedFPCFAnalysis[String, ClassFile](
            project,
            SimpleInstantiabilityAnalysis.groupBy,
            SimpleInstantiabilityAnalysis.entitySelector
        ) with AnalysisMode {

    def determineProperty(
        key: String, classFiles: Seq[ClassFile]
    ): Traversable[EP] = {

        var instantiatedClasses = Set.empty[EP]

        for {
            cf ← classFiles
            method ← cf.methods if !method.isAbstract
        } {
            val visibleMethod =
                if (isOpenLibrary) !method.isPrivate
                else method.isPublic || (method.isProtected && !cf.isFinal)

            if (method.isNative && method.isStatic && visibleMethod) {
                println(cf.thisType.toJava+" with "+method.descriptor.toJava(method.name))
            } else if (method.body.nonEmpty) {
                val body = method.body.get
                val instructions = body.instructions
                val max = instructions.length
                var pc = 0
                while (pc < max) {
                    val instruction = instructions(pc)
                    if (instruction.opcode == INVOKESPECIAL.opcode) {
                        instruction match {
                            case INVOKESPECIAL(classType, "<init>", _) ⇒
                                // We found a constructor call.
                                val classFile = project.classFile(classType)
                                if (classFile.nonEmpty) {
                                    instantiatedClasses += EP(classFile.get, Instantiable)
                                }
                            case _ ⇒
                        }
                    }
                    pc = body.pcOfNextInstruction(pc)
                }
            } else instantiatedClasses += EP(cf, Instantiable)
        }

        val usedClassFiles = instantiatedClasses.collect { case EP(cf: ClassFile, _) ⇒ cf }
        val remainingClassFiles: Set[ClassFile] = classFiles.toSet -- usedClassFiles

        remainingClassFiles foreach { classFile ⇒
            instantiatedClasses += determineClassInstantiability(classFile)
        }
        instantiatedClasses
    }

    def determineClassInstantiability(classFile: ClassFile): EP = {
        import project.classHierarchy.isSubtypeOf

        if (classFile.isAbstract || classFile.isInterfaceDeclaration)
            // A class that either never has any constructor (interfaces)
            // or that must have at least one non-private constructor to make
            // sense at all.
            return EP(classFile, NotInstantiable)

        val classType = classFile.thisType

        if (isSubtypeOf(classType, ObjectType.Serializable).isYesOrUnknown &&
            classFile.hasDefaultConstructor)
            //if the class is Serializable or it is unknown, we have to count it as instantiated.
            return EP(classFile, Instantiable)

        val nonFinalClass = !classFile.isFinal

        if (classFile.isPublic || isOpenLibrary) {

            classFile.constructors foreach { cons ⇒
                if (cons.isPublic || (isOpenLibrary && !cons.isPrivate))
                    return EP(classFile, Instantiable)
                else if (nonFinalClass &&
                    ((cons.isPackagePrivate && isOpenLibrary) || cons.isProtected))
                    //If the class not final and public or we analyze an open library we have
                    //to assume that a subclass is created and instantiated later on.
                    //Hence, every time a subclass is instantiated all superclass's have to be
                    //considered instantiated as well.
                    return EP(classFile, Instantiable)
            }
        }

        return EP(classFile, NotInstantiable)
    }
}

/**
 * Companion object for the [[InstantiabilityAnalysis]] class.
 */
object SimpleInstantiabilityAnalysis
        extends FPCFAnalysisRunner[SimpleInstantiabilityAnalysis] {

    private[SimpleInstantiabilityAnalysis] def groupBy: Function[ClassFile, String] = {
        case cf: ClassFile ⇒ cf.thisType.packageName
    }

    private[SimpleInstantiabilityAnalysis] def entitySelector: PartialFunction[Entity, ClassFile] = {
        case cf: ClassFile ⇒ cf
    }

    protected def start(project: SomeProject): Unit = {
        new SimpleInstantiabilityAnalysis(project)
    }
}
