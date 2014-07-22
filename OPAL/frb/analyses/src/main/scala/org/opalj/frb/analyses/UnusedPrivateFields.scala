/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package frb
package analyses

import br._
import br.analyses._
import br.instructions._

/**
 * This analysis reports `private` fields that are not used.
 *
 * For normal fields it's enough to check whether there are any field access instructions
 * referencing them in the class'es methods.
 *
 * `final` fields with a constant initializer are special cases: The Java compiler will
 * inline accesses to them. There is no field access instruction, but only a constant
 * load instruction. For such fields, we search for loads of constant values matching
 * their constant initializers, and if found, assume that the field is used.
 *
 * Possible false-negative: The program may really have used a literal constant instead of
 * the constant field and it just happens to be the same value and thus is misinterpreted
 * as access to that field.
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 * @author Peter Spieler
 */
class UnusedPrivateFields[Source] extends FindRealBugsAnalysis[Source] {

    def description: String = "Reports unused private fields."

    private def foreachConstantLoad(body: Code, f: ConstantValue[_] ⇒ Unit): Unit = {
        body.instructions.foreach {
            case ICONST_M1           ⇒ f(ConstantInteger(-1))
            case ICONST_0            ⇒ f(ConstantInteger(0))
            case ICONST_1            ⇒ f(ConstantInteger(1))
            case ICONST_2            ⇒ f(ConstantInteger(2))
            case ICONST_3            ⇒ f(ConstantInteger(3))
            case ICONST_4            ⇒ f(ConstantInteger(4))
            case ICONST_5            ⇒ f(ConstantInteger(5))
            case BIPUSH(value)       ⇒ f(ConstantInteger(value))
            case LCONST_0            ⇒ f(ConstantLong(0))
            case LCONST_1            ⇒ f(ConstantLong(1))
            case DCONST_0            ⇒ f(ConstantDouble(0))
            case DCONST_1            ⇒ f(ConstantDouble(1))
            case FCONST_0            ⇒ f(ConstantFloat(0))
            case FCONST_1            ⇒ f(ConstantFloat(1))
            case FCONST_2            ⇒ f(ConstantFloat(2))

            // LDC
            case LoadInt(value)      ⇒ f(ConstantInteger(value))
            case LoadFloat(value)    ⇒ f(ConstantFloat(value))
            case LoadClass(value)    ⇒ f(ConstantClass(value))
            case LoadString(value)   ⇒ f(ConstantString(value))

            // LDC_W
            case LoadInt_W(value)    ⇒ f(ConstantInteger(value))
            case LoadFloat_W(value)  ⇒ f(ConstantFloat(value))
            case LoadClass_W(value)  ⇒ f(ConstantClass(value))
            case LoadString_W(value) ⇒ f(ConstantString(value))

            // LDC2_W
            case LoadLong(value)     ⇒ f(ConstantLong(value))
            case LoadDouble(value)   ⇒ f(ConstantDouble(value))

            case _                   ⇒
        }
    }

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[FieldBasedReport[Source]] = {

        // TODO: Currently doesn't detect cases where Serializable is implemented 
        //indirectly, e.g. through java.io.File which implements Serializable but is 
        //typically not analyzed by OPAL. Thus, in general, if some super types are 
        //unknown, this analysis should generate reports with lower severity, or perhaps 
        //none at all, about serialVersionUID.
        val serializables = project.classHierarchy.allSubtypes(ObjectType.Serializable,
            false)

        /**
         * Check whether a field is the special `serialVersionUID` field of a
         * `Serializable` class. It is always used internally by the JVM, and should not
         * be reported by this analysis.
         */
        def isSerialVersionUID(declaringClass: ObjectType, field: Field): Boolean = {
            // Declaring class must implement Serializable, or else serialVersionUID
            // fields are not special.
            serializables.contains(declaringClass) &&
                // The field must be "long serialVersionUID". Access flags do not matter
                // though.
                (field match {
                    case Field(_, "serialVersionUID", LongType) ⇒ true
                    case _                                      ⇒ false
                })
        }

        var reports: List[FieldBasedReport[Source]] = List.empty

        for {
            classFile ← project.classFiles
            if !classFile.isInterfaceDeclaration &&
                !project.isLibraryType(classFile)
        } {
            val declaringClass = classFile.thisType
            val privateFields = scala.collection.mutable.Map[String, Field]()
            val unusedConstants = scala.collection.mutable.Set[ConstantValue[_]]()

            // Collect private fields
            for {
                field ← classFile.fields
                if field.isPrivate && !isSerialVersionUID(declaringClass, field)
            } {
                privateFields += field.name -> field
                if (field.isFinal && field.constantFieldValue.isDefined) {
                    unusedConstants += field.constantFieldValue.get
                }
            }

            // Check for field read accesses by name
            // TODO: early abort if all fields known to be used
            if (privateFields.nonEmpty) {
                for (method @ MethodWithBody(body) ← classFile.methods) {
                    for (FieldReadAccess(`declaringClass`, name, _) ← body.instructions) {
                        privateFields -= name
                    }
                }
            }

            // Check constructors: Constant values occurring more than once indicate the
            // corresponding field(s) to be used. (more than once because constructors
            // initialize fields, so they will always contain at least one constant load
            // of each field's value)
            // Possible false-negatives: In case multiple final fields have the same
            // constant initializer, the corresponding value will appear multiple times
            // during constant loads in the constructor. In that case the >= 2 heuristic
            // will cause all these fields to be treated as used.
            if (unusedConstants.nonEmpty) {
                val occurrences = scala.collection.mutable.Map[ConstantValue[_], Int]() ++
                    unusedConstants.map(value ⇒ value -> 0)

                for (MethodWithBody(body) ← classFile.constructors) {
                    foreachConstantLoad(body, { value ⇒
                        if (occurrences.contains(value)) {
                            occurrences(value) += 1
                        }
                    })
                }

                // Remove constants used >= 2 times in constructors from the unused list
                unusedConstants --= occurrences.filter {
                    case (value, occurrenceCount) ⇒ occurrenceCount >= 2
                }.map {
                    case (value, occurrenceCount) ⇒ value
                }
            }

            // Check other methods: Any occurrence indicates a use.
            if (unusedConstants.nonEmpty) {
                for {
                    method @ MethodWithBody(body) ← classFile.methods
                    if !method.isConstructor
                } {
                    foreachConstantLoad(body, { value ⇒
                        unusedConstants -= value
                    })
                }
            }

            // Remove final constant fields from the unused list, if their constant was
            // seen used
            for ((name, field) ← privateFields) {
                if (field.isFinal &&
                    field.constantFieldValue.isDefined &&
                    !unusedConstants.contains(field.constantFieldValue.get)) {
                    privateFields -= name
                }
            }

            for (field ← privateFields.values) {
                reports = FieldBasedReport(
                    project.source(declaringClass),
                    Severity.Info,
                    declaringClass,
                    field,
                    "Is private and unused") :: reports
            }
        }

        reports
    }
}
