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
package br
package analyses

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.Set
import scala.collection.mutable.HashSet
import org.opalj.collection.mutable.UShortSet
import org.opalj.collection.immutable.IdentityPair
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.INVOKESPECIAL

/**
 * This analysis determines which classes can never be instantiated (e.g.,
 * `java.lang.Math`).
 *
 * A class is not instantiable if:
 *  - it only defines private constructors and these constructors are not called
 *    by any static method.
 *
 * @note This analysis does not consider protected and/or package visible constructor as
 *      it assumes that classes may be added to the respective package later on (open-packages
 *      assumption.)
 *
 * @note If this class is queried (after performing the analysis) about a class that
 *      was not analyzed, the result will be that the class is considered to be
 *      instantiable.
 *
 * This information is relevant in various contexts, e.g., to determine
 * precise call graph. For example, instance methods of those objects that cannot be
 * created are always dead.
 *
 * ==Usage==
 * Use the [[InstantiableClassesKey]] to query a project about the instantiable classes.
 * {{{
 * val instantiableClasses = project.get(InstantiableClassesKey)
 * }}}
 *
 * @note The analysis does not take reflective instantiations into account!
 *
 * @author Michael Eichberg
 */
object InstantiableClassesAnalysis {

    // IMPROVE Add support for close-packages assumptions.
    // IMPROVE Use the fcf to react on results related to constructor calls from factory methods.

    def doAnalyze(
        project: SomeProject,
        isInterrupted: () ⇒ Boolean): InstantiableClasses = {

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

            for {
                method ← cf.methods
                // Check that the method is potentially a factory method...
                if !method.isConstructor
                if method.isStatic
            } {
                if (method.isNative)
                    // We don't now what this static method is doing, hence, we assume that
                    // it may act as a factory method; we can now abort the entire
                    // analysis.
                    return ;

                method.body.get.foreach { (pc, instruction) ⇒
                    if (instruction.opcode == INVOKESPECIAL.opcode) {
                        val call = instruction.asInstanceOf[INVOKESPECIAL]
                        if ((call.declaringClass eq thisClassType) && call.name == "<init>") {
                            // We found a static factory method that is responsible
                            // for creating instances of this class.
                            return ;
                        }
                    }
                }
            }
            // If we reach this point, no static method ever creates an instance
            // of the respective class
            notInstantiable.add(thisClassType)
        }

        project.parForeachProjectClassFile(isInterrupted)(analyzeClassFile)

        import scala.collection.JavaConverters._
        new InstantiableClasses(project, HashSet.empty ++ notInstantiable.asScala)
    }
}

/**
 * Stores the information about those classes that are not instantiable (which is
 * usually only a small fraction of all classes and hence, more
 * efficient to store/access).
 *
 * @author Michael Eichberg
 */
class InstantiableClasses(
        val project: SomeProject,
        val notInstantiable: Set[ObjectType]) {

    def isInstantiable(classType: ObjectType): Boolean =
        !notInstantiable.contains(classType)

    def statistics: Map[String, Int] = Map(
        "# of not instantiable classes in the project" -> notInstantiable.size
    )

}

