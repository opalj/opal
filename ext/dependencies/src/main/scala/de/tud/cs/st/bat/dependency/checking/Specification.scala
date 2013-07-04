/* License (BSD Style License):
 *  Copyright (c) 2009, 2011
 *  Software Technology Group
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
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
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st.bat
package dependency
package checking

import resolved._
import resolved.reader.Java6Framework
import resolved.analyses.ClassHierarchy

import scala.collection.immutable.SortedSet

/**
 * A specification of a project's architectural constraints.
 *
 * ===Usage===
 * First define the ensembles, then the rules and at last specify the
 * class files that should be analyzed. The rules will then be automatically
 * evaluated.
 *
 * ===Hints===
 * One ensemble is predefined: [[Specification.empty]] it represents an ensemble that contains no
 * source elements and which can, e.g., be used to specify that no "real" ensemble is allowed
 * to depend on a specific ensemble.
 *
 * @author Michael Eichberg
 */
class Specification
        extends SourceElementIDsMap
        with ReverseMapping
        with UseIDOfBaseTypeForArrayTypes
        with Project {

    private[this] var theClassHierarchy = new ClassHierarchy()
    override def classHierarchy = theClassHierarchy

    private[this] var theClassFiles = Map[ObjectType, ClassFile]()
    override def classFiles = theClassFiles

    private[this] var theEnsembles = Map[Symbol, (SourceElementsMatcher, SortedSet[SourceElementID])]()
    /**
     * The set of defined ensembles. An ensemble is identified by a symbol, a query which matches source
     * elements and the project's source elements that are matched. The latter is available only after
     * analyze was called.
     */
    def ensembles = theEnsembles

    // calculated after all class files have been loaded
    private[this] var theOutgoingDependencies = Map[SourceElementID, Set[(SourceElementID, DependencyType)]]()
    /**
     * Mapping between a source element and those source elements it depends on/uses.
     *
     * This mapping is automatically created when analyze is called.
     */
    def outgoingDependencies = theOutgoingDependencies

    // calculated after all class files have been loaded
    private[this] var theIncomingDependencies = Map[SourceElementID, Set[(SourceElementID, DependencyType)]]()
    /**
     * Mapping between a source element and those source elements that depend on it.
     *
     * This mapping is automatically created when analyze is called.
     */
    def incomingDependencies = theIncomingDependencies

    // calculated after the extension of all ensembles is determined
    private[this] var matchedSourceElements = SortedSet[SourceElementID]()

    private[this] var allSourceElements: Set[SourceElementID] = _

    private[this] var unmatchedSourceElements: Set[SourceElementID] = _

    /**
     * Adds a new ensemble definition to this architecture specification.
     *
     * @throws SpecificationError If the ensemble is already defined.
     */
    @throws(classOf[SpecificationError])
    def ensemble(ensembleSymbol: Symbol)(sourceElementMatcher: SourceElementsMatcher) {
        if (ensembles.contains(ensembleSymbol))
            throw new SpecificationError("The ensemble is already defined: "+ensembleSymbol)

        theEnsembles = theEnsembles + ((ensembleSymbol, (sourceElementMatcher, SortedSet[SourceElementID]())))
    }

    /**
     * Represents an ensemble that contains no source elements. This can be used, e.g., to specify that
     * a (set of) specific source element(s) is not allowed to depend on any other source elements (belonging
     * to the project).
     */
    val empty = {
        ensemble('empty)(NoSourceElementsMatcher)
        'empty
    }

    /**
     * Facilitates the definition of common source element matchers by means of common String patterns.
     */
    @throws(classOf[SpecificationError])
    implicit def StringToSourceElementMatcher(matcher: String): SourceElementsMatcher = {
        if (matcher endsWith ".*")
            return new PackageNameBasedMatcher(matcher.substring(0, matcher.length() - 2).replace('.', '/'))
        if (matcher endsWith ".**")
            return new PackageNameBasedMatcher(matcher.substring(0, matcher.length() - 3).replace('.', '/'), true)
        if (matcher endsWith "*")
            return new ClassMatcher(matcher.substring(0, matcher.length() - 1).replace('.', '/'), true)
        if (matcher.indexOf('*') == -1)
            return new ClassMatcher(matcher.replace('.', '/'))

        throw new SpecificationError("unsupported pattern: "+matcher);
    }

    /**
     * Given
     */
    implicit def FileToClassFileProvider(file: java.io.File): Seq[ClassFile] = Java6Framework.ClassFiles(file)

    case class Violation(source: SourceElementID, target: SourceElementID, dependencyType: DependencyType, description: String) {

        override def toString(): String = {
            description+": "+
                sourceElementIDtoString(source)+" "+dependencyType+" "+sourceElementIDtoString(target)
        }

    }

    trait DependencyChecker {
        def violations(): Set[Violation]
    }

    var dependencyCheckers: List[DependencyChecker] = Nil

    case class GlobalIncomingConstraint(targetEnsemble: Symbol, sourceEnsembles: Seq[Symbol]) extends DependencyChecker {
        def violations() = {
            val sourceEnsembleElements = (SortedSet[SourceElementID]() /: sourceEnsembles)(_ ++ ensembles(_)._2)
            val (_, targetEnsembleElements) = ensembles(targetEnsemble)
            for (
                targetEnsembleElement ← targetEnsembleElements if incomingDependencies.contains(targetEnsembleElement);
                (incomingElement, dependencyType) ← incomingDependencies(targetEnsembleElement) if !(sourceEnsembleElements.contains(incomingElement) || targetEnsembleElements.contains(incomingElement))
            ) yield Violation(incomingElement, targetEnsembleElement, dependencyType, "violation of a global incoming constraint ")
        }

        override def toString =
            targetEnsemble+" is_only_to_be_used_by ("+sourceEnsembles.mkString(",")+")"
    }

    case class LocalOutgoingConstraint(sourceEnsembleSymbol: Symbol, targetEnsembles: Seq[Symbol]) extends DependencyChecker {

        def violations() = {

            val unknownEnsembles = targetEnsembles.filterNot(ensembles.contains(_)).mkString(",")
            if (unknownEnsembles.length() > 0)
                throw new SpecificationError("Unknown ensemble(s): "+unknownEnsembles);

            val allAllowedLocalTargetSourceElements =
                // self references are allowed as well as references to source elements belonging
                // to a target ensemble
                (ensembles(sourceEnsembleSymbol)._2 /: targetEnsembles)(_ ++ ensembles(_)._2)

            for (
                sourceElementID ← ensembles(sourceEnsembleSymbol)._2;
                (targetElementID, dependencyType) ← outgoingDependencies(sourceElementID) if !(allAllowedLocalTargetSourceElements contains targetElementID) && !(unmatchedSourceElements contains targetElementID)
            ) yield Violation(sourceElementID, targetElementID, dependencyType, "violation of a local outgoing constraint")

        }

        override def toString =
            sourceEnsembleSymbol+" is_only_allowed_to_use ("+targetEnsembles.mkString(",")+")"
    }

    case class SpecificationFactory(contextEnsembleSymbol: Symbol) {

        def apply(sourceElementsMatcher: SourceElementsMatcher) {
            ensemble(contextEnsembleSymbol)(sourceElementsMatcher)
        }

        def is_only_to_be_used_by(sourceEnsembleSymbols: Symbol*) {
            dependencyCheckers = GlobalIncomingConstraint(contextEnsembleSymbol, sourceEnsembleSymbols.toSeq) :: dependencyCheckers
        }

        def allows_incoming_dependencies_from(sourceEnsembleSymbols: Symbol*) {
            dependencyCheckers = GlobalIncomingConstraint(contextEnsembleSymbol, sourceEnsembleSymbols.toSeq) :: dependencyCheckers
        }

        def is_only_allowed_to_use(targetEnsembles: Symbol*) {
            dependencyCheckers = LocalOutgoingConstraint(contextEnsembleSymbol, targetEnsembles.toSeq) :: dependencyCheckers
        }
    }

    protected implicit def EnsembleSymbolToSpecificationElementFactory(ensembleSymbol: Symbol): SpecificationFactory =
        SpecificationFactory(ensembleSymbol)

    protected implicit def EnsembleToSourceElementMatcher(ensembleSymbol: Symbol): SourceElementsMatcher = {
        if (!ensembles.contains(ensembleSymbol))
            throw new SpecificationError("The ensemble: "+ensembleSymbol+" is not yet defined.")

        ensembles(ensembleSymbol)._1
    }

    /**
     * Returns a textual representation (as defined in a specification file) of an ensemble.
     */
    def ensembleToString(ensembleSymbol: Symbol): String = {
        var (sourceElementsMatcher, extension) = ensembles(ensembleSymbol)
        ensembleSymbol+"{"+
            sourceElementsMatcher+"  "+
            {
                if (extension.isEmpty)
                    "/* NO ELEMENTS */ "
                else {
                    val ex = extension.toList
                    (("\n\t//"+extension.head.toString+":"+sourceElementIDtoString(extension.head)+"\n") /: extension.tail)((s, id) ⇒ s+"\t//"+id+":"+sourceElementIDtoString(id)+"\n")
                }
            }+"}"
    }

    def analyze(classFileProviders: Traversable[ClassFile]*) {

        val dependencyExtractor = new DependencyExtractor(Specification.this) with NoSourceElementsVisitor {

            def processDependency(sourceID: SourceElementID, targetID: SourceElementID, dType: DependencyType) {
                theOutgoingDependencies =
                    theOutgoingDependencies.updated(
                        sourceID,
                        theOutgoingDependencies.getOrElse(sourceID, Set()) + ((targetID, dType)))
                theIncomingDependencies =
                    theIncomingDependencies.updated(
                        targetID,
                        theIncomingDependencies.getOrElse(targetID, Set()) + ((sourceID, dType))
                    )
            }
        }

        val performance = new de.tud.cs.st.util.debug.PerformanceEvaluation {}
        import performance._
        import de.tud.cs.st.util.debug._

        // 1. create and update the support data structures
        Console.print(Console.GREEN+"1. Reading class files and extracting dependencies took ")
        time(t ⇒ Console.println(nsToSecs(t).toString+" seconds.")) {
            for (
                classFileProvider ← classFileProviders;
                classFile ← classFileProvider
            ) {
                theClassHierarchy = theClassHierarchy + classFile
                theClassFiles = theClassFiles.updated(classFile.thisClass, classFile)
                dependencyExtractor.process(classFile)
            }
        }

        // 2. calculate the extension of the ensembles
        Console.print(Console.GREEN+"2. Determing the extension of the ensembles..."+Console.BLACK)
        time(t ⇒ Console.println(Console.GREEN+"   ...finished in "+nsToSecs(t).toString+" seconds.")) {
            val instantiatedEnsembles = ensembles.par.map((ensemble) ⇒ {
                val (ensembleSymbol, (sourceElementMatcher, _)) = ensemble
                val extension = sourceElementMatcher.extension(this)
                if (extension.isEmpty && sourceElementMatcher != NoSourceElementsMatcher)
                    Console.println(Console.RED+"   "+ensembleSymbol+" ("+extension.size+")"+Console.BLACK)
                else
                    Console.println("   "+ensembleSymbol+" ("+extension.size+")")

                Specification.this.synchronized {
                    matchedSourceElements = matchedSourceElements ++ extension
                }
                (ensembleSymbol, (sourceElementMatcher, extension))
            })
            theEnsembles = theEnsembles ++ instantiatedEnsembles

            allSourceElements = allSourceElementIDs().toSet
            unmatchedSourceElements = allSourceElements -- matchedSourceElements

            Console.println("   => Matched source elements: "+matchedSourceElements.size)
            Console.println("   => Other source elements: "+unmatchedSourceElements.size)
        }

        // 3. check all rules
        Console.println(Console.GREEN+"3. Checking the specified dependency constraints..."+Console.BLACK)
        time(t ⇒ println(Console.GREEN+"   ...finished in "+nsToSecs(t).toString+" seconds.")) {
            for (dependencyChecker ← dependencyCheckers.par) {
                Console.println("   Checking: "+dependencyChecker)
                for (violation ← dependencyChecker.violations) println(violation)
            }
        }
        Console.print(Console.BLACK);

    }

    @throws(classOf[SpecificationError])
    def Directory(directoryName: String): Seq[ClassFile] = {
        val file = new java.io.File(directoryName)
        if (!file.exists)
            throw new SpecificationError("The specified directory does not exist: "+directoryName+".")
        if (!file.canRead)
            throw new SpecificationError("Cannot read the specified directory: "+directoryName+".")
        if (!file.isDirectory)
            throw new SpecificationError("The specified directory is not a directory: "+directoryName+".")

        Java6Framework.ClassFiles(file)
    }

}

case class SpecificationError(val description: String) extends Exception(description)

