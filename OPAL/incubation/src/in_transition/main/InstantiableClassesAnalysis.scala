/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
