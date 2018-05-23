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

import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._

import org.opalj.br.instructions.NEW

/**
 * A very basic analysis which identifies those classes that can never be instantiated by user code.
 * A famous example is "java.lang.Math" which just defines a single private constructor which
 * is never called.
 *
 * A class is not instantiable if it only defines private constructors and these constructors
 * are not called by any static method and the class is also not Serializable. However,
 * if the static initializer creates an instance of the respective class, then it is possible that
 * this a class is rated as not client instantiable though it is possible that an instance
 * exists and may even be used.
 *
 *
 * @note This analysis does not consider protected and/or package visible constructors as
 *      it assumes that classes may be added to the respective package later on (open-packages
 *      assumption).
 *
 * @note If this class is queried (after performing the analysis) about a class that
 *      was not analyzed, the result will be that the class is considered to be
 *      instantiable.
 *
 * This information is relevant in various contexts, e.g., to determine a
 * precise call graph. For example, instance methods of those objects that cannot be
 * created are always dead. However, this analysis only provides the information whether the
 * class is instantiable by client code!
 *
 * ==Usage==
 * Use the [[InstantiableClassesKey]] to query a project about the instantiable classes.
 * {{{
 * val instantiableClasses = project.get(InstantiableClassesKey)
 * }}}
 *
 * @note The analysis does not take reflective instantiations into account!
 *
 * @note A more precise analysis is available that uses the fixpoint computations framework.
 *
 * @author Michael Eichberg
 */
object InstantiableClassesAnalysis {

    def doAnalyze(project: SomeProject, isInterrupted: () ⇒ Boolean): InstantiableClasses = {

        import project.classHierarchy.isSubtypeOf

        val notInstantiable = new ConcurrentLinkedQueue[ObjectType]()

        def analyzeClassFile(cf: ClassFile): Unit = {
            if (cf.isAbstract)
                // A class that either never has any constructor (interfaces)
                // or that must have at least one non-private constructor to make
                // sense at all.
                return ;

            if (!cf.constructors.forall { c ⇒ c.isPrivate })
                // We have at least one non-private constructor.
                return ;

            val thisClassType = cf.thisType

            if (isSubtypeOf(thisClassType, ObjectType.Serializable).isYesOrUnknown)
                return ;

            val hasFactoryMethod =

                cf.methods.exists { method ⇒
                    // Check that the method is potentially a factory method...
                    // or creates the singleton instance...
                    method.isStatic && (
                        {

                            method.isNative
                            // If the method is a static method, we don't know what the method may do,
                            // hence, we assume that it may act as a factory method.
                        } || {
                            method.body.isEmpty
                            // We have a static method without a body; this is only possible if the
                            // bytecode reader does not read method bodies; otherwise this can't happen;
                            // such a class file would be invalid.
                        } || {
                            method.body.get.exists { (pc, instruction) ⇒
                                instruction.opcode == NEW.opcode &&
                                    (instruction.asInstanceOf[NEW].objectType eq thisClassType)
                            }
                        }
                    )
                }

            if (!hasFactoryMethod) notInstantiable.add(thisClassType)
        }

        project.parForeachProjectClassFile(isInterrupted)(analyzeClassFile)

        new InstantiableClasses(project, notInstantiable.asScala.toSet)
    }
}
