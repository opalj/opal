/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package frb
package analyses

import br._
import br.analyses._
import br.instructions._

/**
 * This analysis reports methods which access their class' fields and are called by
 * their superclass' constructor.
 *
 * This is a bug, because while the super constructor is running, the derived class'
 * constructor hasn't run yet, and the derived class' fields aren't initialized yet.
 * Thus it's not safe to access them, and it's not safe for the superclass' constructor
 * to call a method in the subclass which does that.
 *
 * TODO: Ideas for improvement:
 * - Should also check all methods called from such methods
 * - Don't complain about accesses to Static Final fields from such methods, assuming
 *   such fields don't need initialization by a constructor (find out whether that's
 *   correct)
 * - Don't complain about accesses to fields if they don't have a specific initializer,
 *   then the constructor wouldn't do anything anyways and it doesn't make a difference
 *   whether the field is being accessed earlier or not.
 * - Don't complain if the field is written before being read (or written only and never
 *   read) in such methods. If it's written first, then there's no "uninitialized" access.
 * - Test on real Java projects and check whether/how many false-positives are reported
 * - Should ignore methods that don't rely on any uninitialized fields.
 *
 * We only need to check abstract super classes, but no super interfaces here, because
 * this analysis checks for a super constructor containing code calling the overridden
 * method. Interfaces, however, can't have constructors.
 *
 * @author Roberts Kolosovs
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class UrUninitReadCalledFromSuperConstructor[Source] extends FindRealBugsAnalysis[Source] {

    override def description: String =
        "Reports methods that access their class'es static fields and are called by a "+
            "super class constructor."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def doAnalyze(
        project:       Project[Source],
        parameters:    Seq[String]     = Seq.empty,
        isInterrupted: () => Boolean
    ): Iterable[SourceLocationBasedReport[Source]] = {

        import project.classHierarchy

        /*
         * Returns true if classFile declares the given Field
         *
         * @param classFile The ClassFile that is supposed to declare a given field
         * @param name The name of the field supposedly declared.
         * @param fieldType Type of the field supposedly declared.
         */
        def declaresField(classFile: ClassFile, name: String,
                          fieldType: FieldType): Boolean = {
            classFile.fields.exists {
                case Field(_, `name`, `fieldType`) => true
                case _                             => false
            }
        }

        /*
         * Check whether a method overrides a method in any super class.
         *
         * @param classFile Subclass with the supposed overriding method.
         * @param method Method to be checked for being an overriding method.
         * @return True if method in classFile is overriding a method in some superclass.
         */
        def methodOverridesAnything(classFile: ClassFile, method: Method): Boolean = {
            classHierarchy.allSupertypes(classFile.thisType).
                filter(!classHierarchy.isInterface(_)).
                exists(
                    classHierarchy.lookupMethodDefinition(
                        _,
                        method.name, method.descriptor,
                        project
                    ).isDefined
                )
        }

        /*
         * Checks whether the source method contains calls to the given target method.
         *
         * @param source The method which may contain calls to other methods.
         * @param targetType Type of the method supposedly called.
         * @param target The name of the method supposedly called.
         * @return True if source contains a call to target method of the given type.
         */
        def calls(source: Method, targetType: Type, target: Method): Boolean = {
            source.body.get.instructions.exists {
                case MethodInvocationInstruction(`targetType`, target.name,
                    target.descriptor) => true
                case _ => false
            }
        }

        var inconsistencyReports: Set[SourceLocationBasedReport[Source]] = Set.empty

        /*
         * Looks up a method reference from an `INVOKESPECIAL` instruction by using
         * `resolveMethodReference`, unless the given declaring class is an interface.
         *
         * @param classFile The class file containing the `INVOKESPECIAL` instruction.
         * @param constructor The method containing the `INVOKESPECIAL` instruction.
         * @param pc The `PC` of the `INVOKESPECIAL` instruction.
         * @param declaringClass The object type referenced by the `INVOKESPECIAL`.
         * @param name The method name referenced by the `INVOKESPECIAL`.
         * @param descriptor The method signature referenced by the `INVOKESPECIAL`.
         * @return The referenced `Method` or `None`.
         */
        def maybeResolveMethodReference(
            classFile:      ClassFile,
            constructor:    Method,
            pc:             PC,
            declaringClass: ObjectType,
            name:           String,
            descriptor:     MethodDescriptor
        ): Option[Method] = {

            // Excluding interfaces here, because resolveMethodReference() can't be called
            // on interfaces, and normally, constructors are not being called on
            // interfaces anyways. However, we've found a class file in the Qualitas
            // Corpus containing an INVOKESPECIAL doing a constructor call on an
            // interface. In this situation, we report that the project is inconsistent.
            if (classHierarchy.isInterface(declaringClass)) {
                inconsistencyReports +=
                    LineAndColumnBasedReport(
                        project.source(classFile.thisType),
                        Severity.Error,
                        classFile.thisType,
                        constructor.descriptor,
                        constructor.name,
                        constructor.body.get.lineNumber(pc),
                        None,
                        "INVOKESPECIAL on interface type; inconsistent project."
                    )
                None
            } else {
                classHierarchy.
                    resolveMethodReference(declaringClass, name, descriptor, project)
            }
        }

        /*
         * Returns the super class constructor called by the given constructor, or None.
         *
         * @param constructor Constructor which may or may not call a superconstructor.
         * @return The first superconstructor to be called or None.
         */
        def findCalledSuperConstructor(
            classFile:   ClassFile,
            constructor: Method
        ): Option[Method] = {
            constructor.body.get.associateWithIndex.collectFirst({
                case (pc, INVOKESPECIAL(typ, name, desc)) => maybeResolveMethodReference(
                    classFile, constructor, pc, typ, name, desc
                )
            }).flatten
        }

        var reports: Set[SourceLocationBasedReport[Source]] = Set.empty

        for {
            classFile <- project.allProjectClassFiles
            method @ MethodWithBody(body) <- classFile.methods
            if !method.isStatic &&
                !method.isConstructor &&
                methodOverridesAnything(classFile, method)
            GETFIELD(declaringClass, fieldName, fieldType) <- body.instructions
            constructor <- classFile.constructors
            if declaresField(classFile, fieldName, fieldType)
            superConstructor <- findCalledSuperConstructor(classFile, constructor)
            superClass = project.classFile(superConstructor)
            if superConstructor.body.isDefined &&
                calls(superConstructor, superClass.thisType, method)
        } {
            reports +=
                MethodBasedReport(
                    project.source(classFile.thisType),
                    Severity.Error,
                    classFile.thisType,
                    method,
                    "Called by super constructor ("+superClass.thisType.toJava+"), "+
                        "while the class' fields are still uninitialized"
                )
        }

        inconsistencyReports ++ reports
    }
}
