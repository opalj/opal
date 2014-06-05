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

import org.opalj.br._
import org.opalj.br.analyses._
import org.opalj.br.instructions._
import org.opalj.ai._
import org.opalj.ai.domain._
import org.opalj.ai.domain.tracing._

/**
 * This analysis checks the static initializers (`<clinit>`) of all classes for accesses
 * to uninitialized static fields from subclasses.
 *
 * During the static initialization of a class, its subclasses are not initialized yet,
 * hence any accesses to the subclasses' static fields are typically bugs, unless the
 * fields were initialized manually before the first access.
 *
 * To detect this properly, this analysis uses the `AI` framework to analyze `<clinit>`
 * and also all methods called from it for read and write accesses to static fields.
 *
 * @author Daniel Klauer
 */
class UninitializedFieldAccessDuringStaticInitialization[Source]
        extends MultipleResultsAnalysis[Source, LineAndColumnBasedReport[Source]] {

    def description: String =
        "Detects accesses to uninitialized static fields from subclasses "+
            "during static initialization of a certain class."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[LineAndColumnBasedReport[Source]] = {

        /*
         * Analyze the method currently active in the given context. Any methods called
         * from this one will be analyzed recursively.
         *
         * TODO: Should only analyze each method once (specifically, only run AI once per
         * method). The results must be cached and they must also be independent from the
         * callers' contexts. Currently however we may have a different `initialStatus`
         * each time a method is analyzed...
         */
        def evaluateMethod(
            analyzedClass: ClassFile,
            classFile: ClassFile,
            method: Method,
            initialStatus: FieldStatus): FieldStatus = {

            val domain =
                new FieldStatusTracingDomain(project, analyzedClass, classFile, method,
                    initialStatus, evaluateMethod)

            BaseTracingAI(classFile, method, domain)

            domain.getMethodExitStatus()
        }

        var reports: List[LineAndColumnBasedReport[Source]] = List.empty

        // Analyze each class'es <clinit>...
        for {
            analyzedClass ← project.classFiles
            if !project.isLibraryType(analyzedClass) &&
                // We only need to analyze the class if it actually has strict subclasses
                // that have static fields.
                project.classHierarchy.existsSubclass(analyzedClass.thisType, project)(classFile ⇒
                    classFile.fields.exists { field ⇒ field.isStatic }
                )
            clinit @ MethodWithBody(_) ← analyzedClass.staticInitializer
        } {
            // Analyze this <clinit>
            val finalStatus =
                evaluateMethod(analyzedClass, analyzedClass, clinit,
                    FieldStatus(Set(), Set()))

            // Turn the uninitialized accesses collected in the context into reports.
            for (uninitializedAccess ← finalStatus.uninitializedAccesses) {
                val locationType = project.classFile(uninitializedAccess.method).thisType
                val declaringType = project.classFile(uninitializedAccess.field).thisType

                val message =
                    "Access to uninitialized static field '"+
                        // If the uninitialized access is found in a different class than
                        // the one owning the field, mention the field's parent class in
                        // the report.
                        (if (locationType != declaringType)
                            declaringType.fqn+"."
                        else
                            "") +
                        uninitializedAccess.field.name+"'"+
                        // If the uninitialized access is found in another method than the
                        // <clinit> we're analyzing, mention it in the report.
                        (if (uninitializedAccess.method != clinit)
                            " during static initialization"
                        else
                            "") +
                        // If the uninitialized access is found in another class than the
                        // one whose <clinit> we're analyzing, mention it in the report.
                        (if (locationType != analyzedClass.thisType)
                            " in '"+analyzedClass.fqn+"'"
                        else
                            "")

                reports = LineAndColumnBasedReport(
                    project.source(locationType),
                    Severity.Error,
                    locationType,
                    uninitializedAccess.method.descriptor,
                    uninitializedAccess.method.name,
                    uninitializedAccess.lineNumber,
                    None,
                    message) :: reports
            }
        }

        reports
    }
}

/**
 * A `Domain` that uses `PropertyTracing` to track the initialization status of static
 * fields, ultimately allowing us to detect accesses to uninitialized static fields.
 *
 * When analyzing a class'es `<clinit>`, that class'es own static fields will be
 * initialized, but the static fields of subclasses are still uninitialized. Any access
 * to the subclass could thus result in an access to an uninitialized static field via
 * `GETSTATIC` instructions. However, the subclass's static fields may also be initialized
 * manually via `PUTSTATIC` instructions before the first access.
 *
 * Any calls to other static methods (`INVOKESTATIC`) must be analyzed aswell, because
 * they may set/access static fields too (directly or indirectly). The analysis of called
 * methods must ultimately behave as if the called method's instructions were inlined at
 * the call site.
 *
 * TODO: We may have to analyze all method calls (not just `INVOKESTATIC`), in case the
 * analyzed static method creates some object and calls a normal method on it, which could
 * directly or indirectly access static fields.
 *
 * @author Daniel Klauer
 */
private class FieldStatusTracingDomain[Source](
    val project: Project[Source],

    // The class whose <clinit> we're analyzing (needed because we want to check whether
    // field accesses reference one of its strict subclasses or not)
    val analyzedClass: ClassFile,

    // The method (and its parent class file) analyzed during this run of the AI
    val classFile: ClassFile,
    val method: Method,

    // The initial status tells us what we can assume on the entry to the method. When
    // analyzing called methods recursively, the call site's status will be passed here.
    val initialStatus: FieldStatus,

    // Callback used to analyze invoked methods
    val evaluateMethod: (ClassFile, ClassFile, Method, FieldStatus) ⇒ FieldStatus)

        extends l1.DefaultDomain
        with PropertyTracing
        with RecordReturnFromMethodInstructions
        with IgnoreSynchronization {
    
    override def maxUpdateCountForIntegerValues: Int = 1

    /**
     * Check whether a class is a strict subclass of (and not equal to) the class we're
     * analyzing.
     */
    private def isStrictSubtype(declaringClassType: ObjectType): Boolean = {
        declaringClassType != analyzedClass.thisType &&
            project.classHierarchy.isSubtypeOf(declaringClassType,
                analyzedClass.thisType).isYes
    }

    /**
     * Handle a field access (`PUTSTATIC` or `GETSTATIC`).
     *
     * We need to check whether this access is relevant to the analysis - i.e. whether
     * it's an access to a subclass of the class whose <clinit> we're analyzing, and if
     * so, update the analysis status accordingly.
     *
     * In more detail:
     *
     * For `PUTSTATIC` it means storing the fact that field was initialized as part of
     * the instruction's property, from where `PropertyTracing` will propagate it to
     * following instructions.
     *
     * For `GETSTATIC` it means checking whether the field is currently known to be
     * uninitialized, and if so, storing the location of the uninitialized access as
     * part if the instruction's property.
     *
     * We only care about accesses to subclass fields, because (only) those are the ones
     * that will be uninitialized during their base class'es <clinit>.
     */
    private def handleFieldAccess(
        pc: PC,
        accessClass: ObjectType,
        fieldName: String,
        fieldType: FieldType)(actOnRelevantFieldAccess: (PC, Field) ⇒ Unit) {

        // The class type used in this instruction is just the context through which the
        // field is accessed, not necessarily the class that declared the field.
        // Specifically, it can be an access to a base class through the context of a
        // subclass (because subclasses inherit the base class'es fields). Thus, we need
        // to determine the (real) declaring class first.
        val field =
            project.classHierarchy.resolveFieldReference(
                accessClass, fieldName, fieldType, project)

        if (field.isDefined) {
            val declaringClassType = project.classFile(field.get).thisType

            // Known field. Check whether it belongs to one of analyzedClass'es
            // subclasses (if any).
            if (isStrictSubtype(declaringClassType)) {
                actOnRelevantFieldAccess(pc, field.get)
            }

        } else {
            // Unknown field. We know that it can't belong to the analyzedClass (because
            // we know its class file and thus its field), but we don't know whether it
            // belongs to a subclass of the analyzedClass or not. Since the field is
            // unknown, that means we don't know its parent class'es class file, which
            // means that we don't know what base classes that class has.
            //
            // Thus we can't do anything in this case.
        }
    }

    override def putstatic(
        pc: PC,
        value: DomainValue,
        accessClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[Nothing, Nothing] = {

        handleFieldAccess(pc, accessClass, name, fieldType) { (pc, field) ⇒
            // Mark the static field as initialized in the status of this instruction.
            setStatusAt(pc, getStatusAt(pc).addInitialized(field))
        }

        super.putstatic(pc, value, accessClass, name, fieldType)
    }

    override def getstatic(
        pc: PC,
        accessClass: ObjectType,
        name: String,
        fieldType: FieldType): Computation[DomainValue, Nothing] = {

        handleFieldAccess(pc, accessClass, name, fieldType) { (pc, field) ⇒
            val status = getStatusAt(pc)

            // If the field is currently uninitialized, then store the information about
            // this uninitialized access at this PC.
            //
            // Also: only store the first uninitialized access to this field (it seems
            // useless to report multiple uninitialized accesses to the same field on the
            // same code path).
            if (!status.isInitialized(field) && !status.havePreviousUninitializedAccess(field)) {
                setStatusAt(pc,
                    getStatusAt(pc).addUninitializedAccess(
                        UninitializedAccess(method, pc, field)))
            }
        }

        super.getstatic(pc, accessClass, name, fieldType)
    }

    override def invokestatic(
        pc: Int,
        accessClass: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult = {

        // Every static method call must be analyzed recursively, because it may
        // contain interesting `PUTSTATIC`/`GETSTATIC` instructions.

        val calledMethod =
            project.classHierarchy.resolveMethodReference(
                accessClass, name, descriptor, project)

        if (calledMethod.isDefined) {
            val theCalledMethod = calledMethod.get

            // Use the current status (at this instruction) as the initial status of the
            // called method
            val status = getStatusAt(pc)

            val exitStatus =
                evaluateMethod(analyzedClass, project.classFile(theCalledMethod),
                    theCalledMethod, status)

            // And combine its results back into the status of the current instruction
            setStatusAt(pc, status.add(exitStatus))

        } else {
            // Method unknown, cannot analyze.
            // TODO: This potentially decreases the analysis' accuracy because, for
            // example, the called static method may have invoked a static method of the
            // analyzedClass which may contain relevant static field accesses.
        }

        super.invokestatic(pc, accessClass, name, descriptor, operands)
    }

    override type Id = Method
    override def id = method
    override final type DomainProperty = FieldStatusProperty
    override final val DomainPropertyTag: reflect.ClassTag[DomainProperty] = implicitly

    /**
     * Tell `PropertyTracing` what it should use as initial status, for the first
     * instruction in the current method.
     */
    override def initialPropertyValue: DomainProperty = {
        new FieldStatusProperty(initialStatus)
    }

    /**
     * The custom property used to hold the static field status at each instruction.
     */
    class FieldStatusProperty(val status: FieldStatus) extends Property {
        override def merge(other: FieldStatusProperty): Update[FieldStatusProperty] = {
            if (status == other.status) {
                NoUpdate
            } else {
                StructuralUpdate(
                    new FieldStatusProperty(
                        status.mergeCodePaths(other.status)))
            }
        }
    }

    private def getStatusAt(pc: PC): FieldStatus = {
        getProperty(pc).status
    }

    private def setStatusAt(pc: PC, status: FieldStatus) {
        setProperty(pc, new FieldStatusProperty(status))
    }

    /**
     * Collect the statuses from all the methods return instructions, and merge them into
     * one final status representing the status after executing the method.
     *
     * This method must only be called after the AI run has finished!
     */
    def getMethodExitStatus(): FieldStatus = {
        allReturnFromMethodInstructions.map(pc ⇒ getStatusAt(pc)).
            reduce((l: FieldStatus, r: FieldStatus) ⇒ l.mergeCodePaths(r))
    }
}

/**
 * The class that tracks the context information needed by the analysis.
 *
 * Information can only be added by creating a new status, but never removed. Once a
 * field is initialized, it stays that way forever, and once we've seen an uninitialized
 * access, we don't want to forget it again.
 *
 * While the `AnalysisDomain` runs, each relevant instruction (`PUTSTATIC`, `GETSTATIC`,
 * `INVOKESTATIC`) may end up with its own `FieldStatus` instance, since they can all
 * modify the current status, which means creating a new status. All other instructions
 * will re-use the existing status because they don't modify it.
 *
 * A single status may also serve as input for multiple different code paths, all of which
 * may create new statuses on their own. Thus we also need the ability to merge statuses
 * when code paths join. This merge operation itself creates a new status too.
 *
 * This is a `case class` because that way we get `equals()/hashCode()`.
 */
private case class FieldStatus(
        // List of fields known to be initialized so far.
        val initialized: Set[Field],

        // The uninitialized accesses found so far.
        val uninitializedAccesses: Set[UninitializedAccess]) {

    /**
     * Check whether a field is known to be initialized.
     */
    def isInitialized(field: Field): Boolean = {
        initialized.contains(field)
    }

    /**
     * Check whether we have already seen an access to a certain field while it was
     * uninitialized.
     */
    def havePreviousUninitializedAccess(field: Field): Boolean = {
        uninitializedAccesses.exists(_.field == field)
    }

    /**
     * Register a single field as known-to-be-initialized.
     */
    def addInitialized(field: Field): FieldStatus = {
        FieldStatus(initialized + field, uninitializedAccesses)
    }

    /**
     * Register a single uninitialized access.
     */
    def addUninitializedAccess(uninitializedAccess: UninitializedAccess): FieldStatus = {
        FieldStatus(initialized, this.uninitializedAccesses + uninitializedAccess)
    }

    /**
     * Add information from one status to that of another, producing a new status
     * containing the information of both of them.
     */
    def add(other: FieldStatus): FieldStatus = {
        FieldStatus(initialized ++ other.initialized,
            uninitializedAccesses ++ other.uninitializedAccesses)
    }

    /**
     * Merge statuses from two separate code paths, such that the resulting status
     * represents the status after the join of those code paths.
     */
    def mergeCodePaths(other: FieldStatus): FieldStatus = {
        // After two code paths join, we can only assume those fields to be initialized
        // that were initialized before the branch, or that are initialized by both code
        // paths. However, any field initialized only on one code path cannot be assumed
        // to always be initialized after the join. Thus, we must use the intersection.
        val mergedInitialized = initialized.intersect(other.initialized)

        // For uninitialized accesses on the other hand, we want to preserve all of them.
        // Even if an uninitialized access happens only on one code path, we still want
        // to report it.
        val mergedUninitializedAccesses =
            uninitializedAccesses ++ other.uninitializedAccesses

        FieldStatus(mergedInitialized, mergedUninitializedAccesses)
    }
}

/**
 * Information about the location of an access to an uninitialized field, and the accessed
 * field itself.
 */
private case class UninitializedAccess(method: Method, pc: PC, field: Field) {
    def lineNumber = method.body.get.lineNumber(pc)

    // For debugging
    override def toString = "UninitializedAccess("+method.name+", "+pc+", "+field.name+")"
}
