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
package av
package checking

import scala.language.implicitConversions
import java.net.URL
import scala.util.matching.Regex
import scala.collection.{ Map ⇒ AMap, Set ⇒ ASet }
import scala.collection.immutable.SortedSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{ Map ⇒ MutableMap, HashSet }
import scala.Console.{ GREEN, RED, BLUE, RESET }
import org.opalj.util.PerformanceEvaluation.{ ns2sec, time, run }
import org.opalj.br._
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.br.analyses.{ ClassHierarchy, Project }
import org.opalj.de._
import org.opalj.io.processSource

/**
 * A specification of a project's architectural constraints.
 *
 * ===Usage===
 * First define the ensembles, then the rules and at last specify the
 * class files that should be analyzed. The rules will then automatically be
 * evaluated.
 *
 * ===Note===
 * One ensemble is predefined: `Specification.empty` it represents an ensemble that
 * contains no source elements and which can, e.g., be used to specify that no "real"
 * ensemble is allowed to depend on a specific ensemble.
 *
 * @author Michael Eichberg
 * @author Samuel Beracasa
 * @author Marco Torsello
 */
class Specification(
        val project: Project[URL],
        val useAnsiColors: Boolean) {

    private[this] def ifUseAnsiColors(ansiEscapeSequence: String): String =
        if (useAnsiColors) ansiEscapeSequence else ""

    def this(project: Project[URL]) {
        this(project, false)
    }

    def this(
        classFiles: Traversable[(ClassFile, URL)],
        useAnsiColors: Boolean = false) {
        this(
            run {
                Project(projectClassFilesWithSources = classFiles)
            } { (executionTime, project) ⇒
                println((if (useAnsiColors) GREEN else "")+
                    "1. Reading "+
                    project.classFilesCount+" class files took "+
                    ns2sec(executionTime).toString+" seconds."+
                    (if (useAnsiColors) RESET else ""))
                project
            },
            useAnsiColors)
    }

    @volatile
    private[this] var theEnsembles: MutableMap[Symbol, (SourceElementsMatcher, ASet[VirtualSourceElement])] =
        scala.collection.mutable.OpenHashMap.empty

    /**
     * The set of defined ensembles. An ensemble is identified by a symbol, a query
     * which matches source elements and the project's source elements that are matched.
     * The latter is available only after [[analyze]] was called.
     */
    def ensembles: AMap[Symbol, (SourceElementsMatcher, ASet[VirtualSourceElement])] =
        theEnsembles

    // calculated after all class files have been loaded
    private[this] var theOutgoingDependencies: MutableMap[VirtualSourceElement, AMap[VirtualSourceElement, DependencyTypesSet]] =
        scala.collection.mutable.OpenHashMap.empty

    /**
     * Mapping between a source element and those source elements it depends on/uses.
     *
     * This mapping is automatically created when analyze is called.
     */
    def outgoingDependencies: AMap[VirtualSourceElement, AMap[VirtualSourceElement, DependencyTypesSet]] =
        theOutgoingDependencies

    // calculated after all class files have been loaded
    private[this] var theIncomingDependencies: MutableMap[VirtualSourceElement, ASet[(VirtualSourceElement, DependencyType)]] =
        scala.collection.mutable.OpenHashMap.empty

    /**
     * Mapping between a source element and those source elements that depend on it.
     *
     * This mapping is automatically created when analyze is called.
     */
    def incomingDependencies: AMap[VirtualSourceElement, ASet[(VirtualSourceElement, DependencyType)]] = theIncomingDependencies

    // calculated after the extension of all ensembles is determined
    private[this] val matchedSourceElements: HashSet[VirtualSourceElement] = HashSet.empty

    private[this] val allSourceElements: HashSet[VirtualSourceElement] = HashSet.empty

    private[this] var unmatchedSourceElements: ASet[VirtualSourceElement] = _

    /**
     * Adds a new ensemble definition to this architecture specification.
     *
     * @throws SpecificationError If the ensemble is already defined.
     */
    @throws(classOf[SpecificationError])
    def ensemble(
        ensembleSymbol: Symbol)(
            sourceElementMatcher: SourceElementsMatcher): Unit = {
        if (ensembles.contains(ensembleSymbol))
            throw SpecificationError("the ensemble is already defined: "+ensembleSymbol)

        theEnsembles += (
            (ensembleSymbol, (sourceElementMatcher, Set.empty[VirtualSourceElement]))
        )
    }

    /**
     * Creates a `Symbol` with the given name.
     *
     * This method is primarily useful if ensemble names are created programmatically
     * and the code should communicate that the created name identifies an ensemble.
     * E.g., instead of
     * {{{
     *  for (moduleID <- 1 to 10) Symbol("module"+moduleID)
     * }}}
     * it is now possible to write
     * {{{
     *  for (moduleID <- 1 to 10) EnsembleID("module"+moduleID)
     * }}}
     * which better communicates the intention.
     */
    def EnsembleID(ensembleName: String): Symbol = Symbol(ensembleName)

    /**
     * Represents an ensemble that contains no source elements. This can be used, e.g.,
     * to specify that a (set of) specific source element(s) is not allowed to depend
     * on any other source elements (belonging to the project).
     */
    val empty = {
        ensemble('empty)(NoSourceElementsMatcher)
        'empty
    }

    /**
     * Facilitates the definition of common source element matchers by means of common
     * String patterns.
     */
    @throws(classOf[SpecificationError])
    implicit def StringToSourceElementMatcher(matcher: String): SourceElementsMatcher = {
        if (matcher endsWith ".*")
            PackageMatcher(matcher.substring(0, matcher.length() - 2).replace('.', '/'))
        else if (matcher endsWith ".**")
            PackageMatcher(matcher.substring(0, matcher.length() - 3).replace('.', '/'), true)
        else if (matcher endsWith "*")
            SimpleClassMatcher(matcher.substring(0, matcher.length() - 1).replace('.', '/'), true)
        else if (matcher.indexOf('*') == -1)
            SimpleClassMatcher(matcher.replace('.', '/'))
        else
            throw SpecificationError("unsupported matcher pattern: "+matcher);
    }

    def classes(matcher: Regex): SourceElementsMatcher = SimpleClassMatcher(matcher)

    /**
     * Returns the class files stored at the given location.
     */
    implicit def FileToClassFileProvider(file: java.io.File): Seq[(ClassFile, URL)] =
        ClassFiles(file)

    var dependencyCheckers: List[DependencyChecker] = Nil

    case class GlobalIncomingConstraint(
        targetEnsemble: Symbol,
        sourceEnsembles: Seq[Symbol])
            extends DependencyChecker {

        override def targetEnsembles: Seq[Symbol] = Seq(targetEnsemble)

        override def violations(): ASet[SpecificationViolation] = {
            val sourceEnsembleElements =
                (Set[VirtualSourceElement]() /: sourceEnsembles)(_ ++ ensembles(_)._2)
            val (_, targetEnsembleElements) = ensembles(targetEnsemble)
            for {
                targetEnsembleElement ← targetEnsembleElements
                if incomingDependencies.contains(targetEnsembleElement)
                (incomingElement, dependencyType) ← incomingDependencies(targetEnsembleElement)
                if !(
                    sourceEnsembleElements.contains(incomingElement) ||
                    targetEnsembleElements.contains(incomingElement))
            } yield {
                SpecificationViolation(
                    project,
                    this,
                    incomingElement,
                    targetEnsembleElement,
                    dependencyType,
                    "violation of a global incoming constraint ")
            }
        }

        override def toString =
            targetEnsemble+" is_only_to_be_used_by ("+sourceEnsembles.mkString(",")+")"
    }

    case class LocalOutgoingIsOnlyAllowedToConstraint(
        sourceEnsemble: Symbol,
        targetEnsembles: Seq[Symbol])
            extends DependencyChecker {

        if (targetEnsembles.isEmpty)
            throw SpecificationError("no target ensembles specified: "+toString())

        override def sourceEnsembles: Seq[Symbol] = Seq(sourceEnsemble)

        override def violations(): ASet[SpecificationViolation] = {
            val unknownEnsembles = targetEnsembles.filterNot(ensembles.contains(_))
            if (unknownEnsembles.nonEmpty)
                throw SpecificationError(
                    unknownEnsembles.mkString("unknown ensemble(s): ", ",", ""))

            val (_ /*ensembleName*/ , sourceEnsembleElements) = ensembles(sourceEnsemble)
            val allAllowedLocalTargetSourceElements =
                // self references are allowed as well as references to source elements belonging
                // to a target ensemble
                (sourceEnsembleElements /: targetEnsembles)(_ ++ ensembles(_)._2)

            for {
                sourceElement ← sourceEnsembleElements
                // outgoingDependences : Map[VirtualSourceElement, Set[(VirtualSourceElement, DependencyType)]]
                targets = outgoingDependencies.get(sourceElement)
                if targets.isDefined
                (targetElement, dependencyTypes) ← targets.get
                if !(allAllowedLocalTargetSourceElements contains targetElement)
                // references to unmatched source elements are ignored
                if !(unmatchedSourceElements contains targetElement)
                // from here on, we have found a violation
                dependencyType ← dependencyTypes
            } yield {
                SpecificationViolation(
                    project,
                    this,
                    sourceElement,
                    targetElement,
                    dependencyType,
                    "violation of a local outgoing constraint")
            }
        }

        override def toString =
            sourceEnsemble+" is_only_allowed_to_use ("+targetEnsembles.mkString(",")+")"
    }

    /**
     * Forbids any locals dependency between a specific sourcs ensemble and
     * several target ensembles.
     *
     * ==Example Scenario==
     * If the ensemble `ex` is not allowed to use `ey` and the source element `x` which
     * belongs to ensemble `ex` depends on a source element belonging to `ey` then
     * a [[SpecificationViolation]] is generated.
     */
    case class LocalOutgoingNotAllowedConstraint(
        sourceEnsemble: Symbol,
        targetEnsembles: Seq[Symbol])
            extends DependencyChecker {

        if (targetEnsembles.isEmpty)
            throw SpecificationError("no target ensembles specified: "+toString())

        // WE DO NOT WANT TO CHECK THE VALIDITY OF THE ENSEMBLE IDS NOW TO MAKE IT EASY
        // TO INTERMIX THE DEFINITION OF ENSEMBLES AND CONSTRAINTS

        override def sourceEnsembles: Seq[Symbol] = Seq(sourceEnsemble)

        override def violations(): ASet[SpecificationViolation] = {
            val unknownEnsembles = targetEnsembles.filterNot(ensembles.contains(_))
            if (unknownEnsembles.nonEmpty)
                throw SpecificationError(
                    unknownEnsembles.mkString("unknown ensemble(s): ", ",", ""))

            val (_ /*ensembleName*/ , sourceEnsembleElements) = ensembles(sourceEnsemble)
            val notAllowedTargetSourceElements =
                (Set.empty[VirtualSourceElement] /: targetEnsembles)(_ ++ ensembles(_)._2)

            for {
                sourceElement ← sourceEnsembleElements
                targets = outgoingDependencies.get(sourceElement)
                if targets.isDefined
                (targetElement, dependencyTypes) ← targets.get
                dependencyType ← dependencyTypes
                if (notAllowedTargetSourceElements contains targetElement)
            } yield {
                SpecificationViolation(
                    project,
                    this,
                    sourceElement,
                    targetElement,
                    dependencyType,
                    "violation of a local outgoing not allowed constraint")
            }
        }

        override def toString =
            targetEnsembles.mkString(s"$sourceEnsemble is_not_allowed_to_use (", ",", ")")
    }

    case class SpecificationFactory(contextEnsembleSymbol: Symbol) {

        def apply(sourceElementsMatcher: SourceElementsMatcher): Unit = {
            ensemble(contextEnsembleSymbol)(sourceElementsMatcher)
        }

        def is_only_to_be_used_by(sourceEnsembleSymbols: Symbol*): Unit = {
            dependencyCheckers =
                GlobalIncomingConstraint(
                    contextEnsembleSymbol,
                    sourceEnsembleSymbols.toSeq) :: dependencyCheckers
        }

        def allows_incoming_dependencies_from(sourceEnsembleSymbols: Symbol*): Unit = {
            dependencyCheckers =
                GlobalIncomingConstraint(
                    contextEnsembleSymbol,
                    sourceEnsembleSymbols.toSeq) :: dependencyCheckers
        }

        def is_only_allowed_to_use(targetEnsembles: Symbol*): Unit = {
            dependencyCheckers =
                LocalOutgoingIsOnlyAllowedToConstraint(
                    contextEnsembleSymbol,
                    targetEnsembles.toSeq) :: dependencyCheckers
        }

        def is_not_allowed_to_use(targetEnsembles: Symbol*): Unit = {
            dependencyCheckers =
                LocalOutgoingNotAllowedConstraint(
                    contextEnsembleSymbol,
                    targetEnsembles.toSeq) :: dependencyCheckers
        }
    }

    protected implicit def EnsembleSymbolToSpecificationElementFactory(
        ensembleSymbol: Symbol): SpecificationFactory =
        SpecificationFactory(ensembleSymbol)

    protected implicit def EnsembleToSourceElementMatcher(
        ensembleSymbol: Symbol): SourceElementsMatcher = {
        if (!ensembles.contains(ensembleSymbol))
            throw SpecificationError(s"the ensemble: $ensembleSymbol is not yet defined")

        ensembles(ensembleSymbol)._1
    }

    /**
     * Returns a textual representation of an ensemble.
     */
    def ensembleToString(ensembleSymbol: Symbol): String = {
        val (sourceElementsMatcher, extension) = ensembles(ensembleSymbol)
        ensembleSymbol+"{"+
            sourceElementsMatcher+"  "+
            {
                if (extension.isEmpty)
                    "/* NO ELEMENTS */ "
                else {
                    (("\n\t//"+extension.head.toString+"\n") /: extension.tail)((s, vse) ⇒ s+"\t//"+vse.toJava+"\n")
                }
            }+"}"
    }

    /**
     * Can be called after the evaluation of the extents of the ensembles to print
     * out the current configuration.
     */
    def ensembleExtentsToString: String = {
        var s = ""
        for ((ensemble, (_, elements)) ← theEnsembles) {
            s += ensemble+"\n"
            for (element ← elements) {
                s += "\t\t\t"+element.toJava+"\n"
            }
        }
        s
    }

    def analyze(): Set[SpecificationViolation] = {

        import util.PerformanceEvaluation.{ ns2sec, time, run }

        val dependencyStore = time {
            project.get(DependencyStoreWithoutSelfDependenciesKey)
        } { executionTime ⇒
            println(ifUseAnsiColors(GREEN)+
                "2.1. Preprocessing dependencies took "+
                ns2sec(executionTime).toString+" seconds."+ifUseAnsiColors(RESET))
        }
        println("Dependencies between source elements: "+dependencyStore.dependencies.size)
        println("Dependencies on primitive types: "+dependencyStore.dependenciesOnBaseTypes.size)
        println("Dependencies on array types: "+dependencyStore.dependenciesOnArrayTypes.size)

        time {
            for {
                (source, targets) ← dependencyStore.dependencies
                (target, dTypes) ← targets
            } {
                allSourceElements += source
                allSourceElements += target

                theOutgoingDependencies.update(source, targets)

                for { dType ← dTypes } {
                    theIncomingDependencies.update(
                        target,
                        theIncomingDependencies.getOrElse(target, Set.empty) +
                            ((source, dType)))
                }
            }
        } { executionTime ⇒
            println(ifUseAnsiColors(GREEN)+
                "2.2. Postprocessing dependencies took "+
                ns2sec(executionTime).toString+" seconds."+ifUseAnsiColors(RESET))
        }
        println("Number of source elements: "+allSourceElements.size)
        println("Outgoing dependencies: "+theOutgoingDependencies.size)
        println("Incoming dependencies: "+theIncomingDependencies.size)

        // Calculate the extension of the ensembles
        //
        time {
            val instantiatedEnsembles =
                theEnsembles.par map { ensemble ⇒
                    val (ensembleSymbol, (sourceElementMatcher, _)) = ensemble
                    // if a sourceElementMatcher is reused!
                    sourceElementMatcher.synchronized {
                        val extension = sourceElementMatcher.extension(project)
                        if (extension.isEmpty && sourceElementMatcher != NoSourceElementsMatcher)
                            println(ifUseAnsiColors(RED)+
                                "   "+ensembleSymbol+" ("+extension.size+")"+
                                ifUseAnsiColors(RESET))
                        else
                            println(s"   $ensembleSymbol (${extension.size})")

                        Specification.this.synchronized {
                            matchedSourceElements ++= extension
                        }
                        (ensembleSymbol, (sourceElementMatcher, extension))
                    }
                }
            theEnsembles = instantiatedEnsembles.seq

            unmatchedSourceElements = allSourceElements -- matchedSourceElements

            println("   => Matched source elements: "+matchedSourceElements.size)
            println("   => Other source elements: "+unmatchedSourceElements.size)
        } { executionTime ⇒
            println(ifUseAnsiColors(GREEN)+
                "3. Determing the extension of the ensembles finished in "+
                ns2sec(executionTime).toString+" seconds."+ifUseAnsiColors(RESET))
        }

        // Check all rules
        //
        time {
            val result =
                for (dependencyChecker ← dependencyCheckers.par) yield {
                    println("   Checking: "+dependencyChecker)
                    for (violation ← dependencyChecker.violations) yield {
                        //println(violation)
                        violation
                    }
                }
            Set.empty ++ (result.filter(_.nonEmpty).flatten)
        } { executionTime ⇒
            println(ifUseAnsiColors(GREEN)+
                "4. Checking the specified dependency constraints finished in "+
                ns2sec(executionTime).toString+" seconds."+ifUseAnsiColors(RESET))
        }
    }

}
object Specification {

    def ProjectDirectory(directoryName: String): Seq[(ClassFile, URL)] = {
        val file = new java.io.File(directoryName)
        if (!file.exists)
            throw SpecificationError("the specified directory does not exist: "+directoryName)
        if (!file.canRead)
            throw SpecificationError("cannot read the specified directory: "+directoryName)
        if (!file.isDirectory)
            throw SpecificationError("the specified directory is not a directory: "+directoryName)

        Project.Java8ClassFileReader.ClassFiles(file)
    }

    def ProjectJAR(jarName: String): Seq[(ClassFile, URL)] = {
        val file = new java.io.File(jarName)
        if (!file.exists)
            throw SpecificationError("the specified directory does not exist: "+jarName)
        if (!file.canRead)
            throw SpecificationError("cannot read the specified JAR: "+jarName)
        if (file.isDirectory)
            throw SpecificationError("the specified jar file is a directory: "+jarName)

        Project.Java8ClassFileReader.ClassFiles(file)
    }

    /**
     * Load all jar files.
     */
    def ProjectJARs(jarNames: Seq[String]): Seq[(ClassFile, URL)] = {
        jarNames.map(ProjectJAR(_)).flatten
    }

    /**
     * Loads all class files of the specified jar file using the library class file reader.
     * (I.e., the all method implementations are skipped.)
     *
     * @param jarName The name of a jar file.
     */
    def LibraryJAR(jarName: String): Seq[(ClassFile, URL)] = {
        val file = new java.io.File(jarName)
        if (!file.exists)
            throw SpecificationError("the specified directory does not exist: "+jarName)
        if (!file.canRead)
            throw SpecificationError("cannot read the specified JAR: "+jarName)
        if (file.isDirectory)
            throw SpecificationError("the specified jar file is a directory: "+jarName)

        Project.Java8LibraryClassFileReader.ClassFiles(file)
    }

    /**
     * Load all jar files using the library class loader.
     */
    def LibraryJARs(jarNames: Seq[String]): Seq[(ClassFile, URL)] = {
        jarNames.map(LibraryJAR(_)).flatten
    }

    /**
     * Returns a list of paths contained inside the given classpath file.
     * A classpath file should contain paths as text seperated by a path-separator character.
     * On UNIX systems, this character is <code>':'</code>; on Microsoft Windows systems it
     * is <code>';'</code>.
     *
     * ===Example===
     * /path/to/jar/library.jar:/path/to/library/example.jar:/path/to/library/example2.jar
     *
     * Classpath files should be used to prevent absolute paths in tests.
     */
    def Classpath(
        fileName: String,
        pathSeparatorChar: Char = java.io.File.pathSeparatorChar): Iterable[String] = {
        processSource(scala.io.Source.fromFile(new java.io.File(fileName))) { s ⇒
            s.getLines().map(_.split(pathSeparatorChar)).flatten.toSet
        }
    }

    /**
     * Returns the path to the given JAR from the given list of paths.
     */
    def PathToJAR(paths: Iterable[String], jarName: String): String = {
        paths.collectFirst {
            case p if (p.endsWith(jarName)) ⇒ p
        }.getOrElse {
            throw SpecificationError(s"cannot find a path to the specified JAR: $jarName.")
        }
    }

    /**
     * Returns a list of paths to the given JARs from the given list of paths.
     */
    def PathToJARs(paths: Iterable[String], jarNames: Iterable[String]): Iterable[String] = {
        jarNames.foldLeft(Set.empty[String])((c, n) ⇒ c + PathToJAR(paths, n))
    }
}

