/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import scala.language.implicitConversions
import java.net.URL
import scala.util.matching.Regex
import scala.collection.{immutable, mutable, Map => AMap, Set => ASet}
import scala.collection.mutable.{Map => MutableMap}
import scala.Console.{GREEN, RED, RESET}
import scala.io.Source
import org.opalj.util.PerformanceEvaluation.{run, time}
import org.opalj.br._
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.br.analyses.Project
import org.opalj.de._
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.io.processSource
import org.opalj.de.DependencyTypes.toUsageDescription

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * A specification of a project's architectural constraints.
 *
 * ===Usage===
 * First define the ensembles, then the rules and at last specify the
 * class files that should be analyzed. The rules will then automatically be
 * evaluated.
 *
 * The intended way to create a specification is to create a new anonymous Specification
 * class that contains the specification of the architecture. Afterwards the specification
 * object can be used to get the list of architectural violations.
 *
 * {{{
 * new Specification(project) {
 *            ensemble('Number) { "mathematics.Number*" }
 *            ensemble('Rational) { "mathematics.Rational*" }
 *            ensemble('Mathematics) { "mathematics.Mathematics*" }
 *            ensemble('Example) { "mathematics.Example*" }
 *
 *            'Example is_only_allowed_to (USE, 'Mathematics)
 *       }
 * }}}
 *
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
class Specification(val project: Project[URL], val useAnsiColors: Boolean) { spec =>

    /**
     * Creates a new `Specification` for the given `Project`. Error messages will
     * not use ANSI colors.
     */
    def this(project: Project[URL]) =
        this(project, useAnsiColors = false)

    def this(
        classFiles:    Iterable[(ClassFile, URL)],
        useAnsiColors: Boolean                    = false
    ) =
        this(
            run {
                Project(
                    projectClassFilesWithSources = classFiles,
                    Iterable.empty,
                    libraryClassFilesAreInterfacesOnly = true /*actually not relevant*/ )
            } { (t, project) =>
                import project.logContext
                val logMessage = "1. reading "+project.classFilesCount+" class files took "+t.toSeconds
                val message = if (useAnsiColors) GREEN + logMessage + RESET else logMessage
                OPALLogger.progress(message)
                project
            },
            useAnsiColors
        )

    import project.logContext

    private[this] def logProgress(logMessage: String): Unit = {
        OPALLogger.progress(if (useAnsiColors) GREEN + logMessage + RESET else logMessage)
    }

    private[this] def logWarn(logMessage: String): Unit = {
        val message = if (useAnsiColors) RED + logMessage + RESET else logMessage
        OPALLogger.warn("project warn", message)
    }

    private[this] def logInfo(logMessage: String): Unit = {
        OPALLogger.info("project info", logMessage)
    }

    @volatile
    private[this] var theEnsembles: MutableMap[Symbol, (SourceElementsMatcher, ASet[VirtualSourceElement])] =
        scala.collection.mutable.HashMap.empty

    /**
     * The set of defined ensembles. An ensemble is identified by a symbol, a query
     * which matches source elements and the project's source elements that are matched.
     * The latter is available only after [[analyze]] was called.
     */
    def ensembles: AMap[Symbol, (SourceElementsMatcher, ASet[VirtualSourceElement])] =
        theEnsembles

    // calculated after all class files have been loaded
    private[this] val theOutgoingDependencies: MutableMap[VirtualSourceElement, AMap[VirtualSourceElement, DependencyTypesSet]] =
        scala.collection.mutable.HashMap.empty

    /**
     * Mapping between a source element and those source elements it depends on/uses.
     *
     * This mapping is automatically created when analyze is called.
     */
    def outgoingDependencies: AMap[VirtualSourceElement, AMap[VirtualSourceElement, DependencyTypesSet]] =
        theOutgoingDependencies

    // calculated after all class files have been loaded
    private[this] val theIncomingDependencies: mutable.Map[VirtualSourceElement, immutable.Set[(VirtualSourceElement, DependencyType)]] = {
        scala.collection.mutable.HashMap.empty
    }

    /**
     * Mapping between a source element and those source elements that depend on it.
     *
     * This mapping is automatically created when analyze is called.
     */
    def incomingDependencies: AMap[VirtualSourceElement, ASet[(VirtualSourceElement, DependencyType)]] = theIncomingDependencies

    // calculated after the extension of all ensembles is determined
    private[this] val matchedSourceElements: mutable.HashSet[VirtualSourceElement] = mutable.HashSet.empty

    private[this] val allSourceElements: mutable.HashSet[VirtualSourceElement] = mutable.HashSet.empty

    private[this] var unmatchedSourceElements: ASet[VirtualSourceElement] = mutable.HashSet.empty

    /**
     * Adds a new ensemble definition to this architecture specification.
     *
     * @throws SpecificationError If the ensemble is already defined.
     */
    @throws(classOf[SpecificationError])
    def ensemble(
        ensembleSymbol: Symbol
    )(
        sourceElementsMatcher: SourceElementsMatcher
    ): Unit = {
        if (ensembles.contains(ensembleSymbol))
            throw SpecificationError("the ensemble is already defined: "+ensembleSymbol)

        theEnsembles += (
            (ensembleSymbol, (sourceElementsMatcher, Set.empty[VirtualSourceElement]))
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
        ensemble(Symbol("Empty"))(NoSourceElementsMatcher)
        Symbol("Empty")
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
            ClassMatcher(matcher.substring(0, matcher.length() - 1).replace('.', '/'), true)
        else if (matcher.indexOf('*') == -1)
            ClassMatcher(matcher.replace('.', '/'))
        else
            throw SpecificationError("unsupported pattern: "+matcher);
    }

    def classes(matcher: Regex): SourceElementsMatcher = ClassMatcher(matcher)

    /**
     * Returns the class files stored at the given location.
     */
    implicit def FileToClassFileProvider(file: java.io.File): Seq[(ClassFile, URL)] = {
        ClassFiles(file)
    }

    var architectureCheckers: List[ArchitectureChecker] = Nil

    case class GlobalIncomingConstraint(
            targetEnsemble:  Symbol,
            sourceEnsembles: Seq[Symbol]
    ) extends DependencyChecker {

        override def targetEnsembles: Seq[Symbol] = Seq(targetEnsemble)

        override def violations(): ASet[SpecificationViolation] = {
            val sourceEnsembleElements =
                sourceEnsembles.foldLeft(Set[VirtualSourceElement]())(_ ++ ensembles(_)._2)
            val (_, targetEnsembleElements) = ensembles(targetEnsemble)
            for {
                targetEnsembleElement <- targetEnsembleElements
                if incomingDependencies.contains(targetEnsembleElement)
                (incomingElement, dependencyType) <- incomingDependencies(targetEnsembleElement)
                if !(
                    sourceEnsembleElements.contains(incomingElement) ||
                    targetEnsembleElements.contains(incomingElement)
                )
            } yield {
                DependencyViolation(
                    project,
                    this,
                    incomingElement,
                    targetEnsembleElement,
                    dependencyType,
                    "not allowed global incoming dependency found"
                )
            }
        }

        override def toString: String = {
            s"$targetEnsemble is_only_to_be_used_by (${sourceEnsembles.mkString(",")})"
        }
    }

    /**
     * Forbids the given local dependencies between a specific source ensemble and
     * several target ensembles.
     *
     * ==Example Scenario==
     * If the ensemble `ex` is not allowed to use `ey` and the source element `x` which
     * belongs to ensemble `ex` has one if the given dependencies on a source element
     * belonging to `ey` then a [[SpecificationViolation]] is generated.
     */
    case class LocalOutgoingNotAllowedConstraint(
            dependencyTypes: Set[DependencyType],
            sourceEnsemble:  Symbol,
            targetEnsembles: Seq[Symbol]
    ) extends DependencyChecker {

        if (targetEnsembles.isEmpty)
            throw SpecificationError("no target ensembles specified: "+toString())

        // WE DO NOT WANT TO CHECK THE VALIDITY OF THE ENSEMBLE IDS NOW TO MAKE IT EASY
        // TO INTERMIX THE DEFINITION OF ENSEMBLES AND CONSTRAINTS

        override def sourceEnsembles: Seq[Symbol] = Seq(sourceEnsemble)

        override def violations(): ASet[SpecificationViolation] = {
            val unknownEnsembles = targetEnsembles.filterNot(ensembles.contains(_))
            if (unknownEnsembles.nonEmpty)
                throw SpecificationError(
                    unknownEnsembles.mkString("unknown ensemble(s): ", ",", "")
                )

            val (_ /*ensembleName*/ , sourceEnsembleElements) = ensembles(sourceEnsemble)
            val notAllowedTargetSourceElements =
                targetEnsembles.foldLeft(Set.empty[VirtualSourceElement])(_ ++ ensembles(_)._2)

            for {
                sourceElement <- sourceEnsembleElements
                targets = outgoingDependencies.get(sourceElement)
                if targets.isDefined
                (targetElement, currentDependencyTypes) <- targets.get
                currentDependencyType <- currentDependencyTypes
                if ((notAllowedTargetSourceElements contains targetElement) &&
                    ((dependencyTypes equals USE) || (dependencyTypes contains currentDependencyType)))
            } yield {
                DependencyViolation(
                    project,
                    this,
                    sourceElement,
                    targetElement,
                    currentDependencyType,
                    "not allowed local outgoing dependency found"
                )
            }
        }

        override def toString: String = {
            if (dependencyTypes equals USE) {
                targetEnsembles.mkString(s"$sourceEnsemble is_not_allowed_to use (", ",", ")")
            } else {
                val start =
                    s"$sourceEnsemble is_not_allowed_to ${
                        dependencyTypes.map(d => toUsageDescription(d)).mkString(" and ")
                    } ("
                targetEnsembles.mkString(start, ",", ")")
            }
        }
    }

    /**
     * Allows only the given local dependencies between a specific source ensemble and
     * several target ensembles.
     *
     * ==Example Scenario==
     * If the ensemble `ex` is only allowed to throw exceptions `ey` and the source
     * element `x` which belongs to ensemble `ex` throws an exception not belonging
     * to `ey` then a [[SpecificationViolation]] is generated.
     */
    case class LocalOutgoingOnlyAllowedConstraint(
            dependencyTypes: Set[DependencyType],
            sourceEnsemble:  Symbol,
            targetEnsembles: Seq[Symbol]
    ) extends DependencyChecker {

        if (targetEnsembles.isEmpty)
            throw SpecificationError("no target ensembles specified: "+toString())

        // WE DO NOT WANT TO CHECK THE VALIDITY OF THE ENSEMBLE IDS NOW TO MAKE IT EASY
        // TO INTERMIX THE DEFINITION OF ENSEMBLES AND CONSTRAINTS

        override def sourceEnsembles: Seq[Symbol] = Seq(sourceEnsemble)

        override def violations(): ASet[SpecificationViolation] = {
            val unknownEnsembles = targetEnsembles.filterNot(ensembles.contains(_))
            if (unknownEnsembles.nonEmpty)
                throw SpecificationError(
                    unknownEnsembles.mkString("unknown ensemble(s): ", ",", "")
                )

            val (_ /*ensembleName*/ , sourceEnsembleElements) = ensembles(sourceEnsemble)
            val allAllowedLocalTargetSourceElements =
                // self references are allowed as well as references to source elements belonging
                // to a target ensemble
                targetEnsembles.foldLeft(sourceEnsembleElements)(_ ++ ensembles(_)._2)

            for {
                sourceElement <- sourceEnsembleElements
                targets = outgoingDependencies.get(sourceElement)
                if targets.isDefined
                (targetElement, currentDependencyTypes) <- targets.get
                currentDependencyType <- currentDependencyTypes
                if (!(allAllowedLocalTargetSourceElements contains targetElement) &&
                    ((dependencyTypes equals USE) || (dependencyTypes contains currentDependencyType)))
                // references to unmatched source elements are ignored
                if !(unmatchedSourceElements contains targetElement)
            } yield {
                DependencyViolation(
                    project,
                    this,
                    sourceElement,
                    targetElement,
                    currentDependencyType,
                    "violation of a local outgoing dependency constraint"
                )
            }
        }

        override def toString: String = {
            if (dependencyTypes equals USE) {
                targetEnsembles.mkString(s"$sourceEnsemble is_only_allowed_to use (", ",", ")")
            } else {
                val start =
                    s"$sourceEnsemble is_only_allowed_to ${
                        dependencyTypes.map(d => toUsageDescription(d)).mkString(" and ")
                    } ("
                targetEnsembles.mkString(start, ",", ")")
            }
        }
    }

    /**
     * Checks whether all elements in the source ensemble are annotated with the given
     * annotation.
     *
     * ==Example Scenario==
     * If every element in the ensemble `ex` should be annotated with `ey` and the
     * source element `x` which belongs to ensemble `ex` has no annotation that matches
     * `ey` then a [[SpecificationViolation]] is generated.
     *
     *  @param sourceEnsemble An ensemble containing elements, that should be annotated.
     *  @param annotationPredicates The annotations that should match.
     *  @param property A description of the property that is checked.
     *  @param matchAny true if only one match is needed, false if all annotations should match
     */
    case class LocalOutgoingAnnotatedWithConstraint(
            sourceEnsemble:       Symbol,
            annotationPredicates: Seq[AnnotationPredicate],
            property:             String,
            matchAny:             Boolean
    ) extends PropertyChecker {

        def this(
            sourceEnsemble:       Symbol,
            annotationPredicates: Seq[AnnotationPredicate],
            matchAny:             Boolean                  = false
        ) =
            this(
                sourceEnsemble,
                annotationPredicates,
                annotationPredicates.map(_.toDescription()).mkString("(", " - ", ")"),
                matchAny
            )

        override def ensembles: Seq[Symbol] = Seq(sourceEnsemble)

        override def violations(): ASet[SpecificationViolation] = {
            val (_ /*ensembleName*/ , sourceEnsembleElements) = spec.ensembles(sourceEnsemble)

            for {
                sourceElement <- sourceEnsembleElements
                classFile <- project.classFile(sourceElement.classType.asObjectType)
                annotations = sourceElement match {
                    case _: VirtualClass => classFile.annotations

                    case vf: VirtualField =>
                        classFile.fields collectFirst {
                            case f if f.asVirtualField(classFile).compareTo(vf) == 0 => f
                        } match {
                            case Some(f) => f.annotations
                            case _       => IndexedSeq.empty
                        }

                    case vm: VirtualMethod =>
                        classFile.methods collectFirst {
                            case m if m.asVirtualMethod(classFile.thisType).compareTo(vm) == 0 => m
                        } match {
                            case Some(m) => m.annotations
                            case _       => IndexedSeq.empty
                        }

                    case _ => IndexedSeq.empty
                }

                //              if !annotations.foldLeft(false) {
                //                  (v: Boolean, a: Annotation) =>
                //                      v || annotationPredicates.foldLeft(!matchAny) {
                //                          (matched: Boolean, m: AnnotationPredicate) =>
                //                              if (matchAny) {
                //                                  matched || m(a)
                //                              } else {
                //                                  matched && m(a)
                //                              }
                //                      }
                //              }
                if !annotationPredicates.foldLeft(!matchAny) {
                    (v: Boolean, m: AnnotationPredicate) =>
                        if (!matchAny) {
                            v && annotations.exists { a => m(a) }
                        } else {
                            v || annotations.exists { a => m(a) }
                        }
                }
            } yield {
                PropertyViolation(
                    project,
                    this,
                    sourceElement,
                    "the element should be ANNOTATED WITH",
                    "required annotation not found"
                )
            }
        }

        override def toString: String = {
            s"$sourceEnsemble every_element_should_be_annotated_with $property"
        }
    }

    /**
     * Checks whether all elements in the source ensemble implement the given
     * method. The source ensemble should contain only class elements
     * otherwise a [[SpecificationError]] will be thrown.
     *
     *  @param sourceEnsemble An ensemble containing classes, that should implement the given method.
     *  @param methodPredicate The method to match.
     */
    case class LocalOutgoingShouldImplementMethodConstraint(
            sourceEnsemble:  Symbol,
            methodPredicate: SourceElementPredicate[Method]
    )
        extends PropertyChecker {

        override def property: String = methodPredicate.toDescription()

        override def ensembles: Seq[Symbol] = Seq(sourceEnsemble)

        override def violations(): ASet[SpecificationViolation] = {
            val (_ /*ensembleName*/ , sourceEnsembleElements) = spec.ensembles(sourceEnsemble)

            for {
                sourceElement <- sourceEnsembleElements
                sourceClassFile = sourceElement match {
                    case s: VirtualClass => project.classFile(s.classType.asObjectType).get
                    case _               => throw SpecificationError(sourceElement.toJava+" is not a class")
                }
                if sourceClassFile.methods.forall(m => !methodPredicate(m))
            } yield {
                PropertyViolation(
                    project,
                    this,
                    sourceElement,
                    "the element should IMPLEMENT METHOD",
                    "required method implementation not found"
                )
            }
        }

        override def toString: String = {
            s"$sourceEnsemble every_element_should_implement_method ($property)"
        }
    }

    /**
     * Checks whether all elements in the source ensemble extends any of
     * the given elements. The source ensemble should contain only class elements
     * otherwise a [[SpecificationError]] will be thrown.
     *
     *  @param sourceEnsemble An ensemble containing classes, that should implement the given method.
     *  @param targetEnsembles Ensembles containing elements, that should be extended by the given classes.
     */
    case class LocalOutgoingShouldExtendConstraint(
            sourceEnsemble:  Symbol,
            targetEnsembles: Seq[Symbol]
    ) extends PropertyChecker {

        override def property: String = targetEnsembles.mkString(", ")

        override def ensembles: Seq[Symbol] = Seq(sourceEnsemble)

        override def violations(): ASet[SpecificationViolation] = {
            val (_ /*ensembleName*/ , sourceEnsembleElements) = spec.ensembles(sourceEnsemble)
            val allLocalTargetSourceElements =
                // self references are allowed as well as references to source elements belonging
                // to a target ensemble
                targetEnsembles.foldLeft(sourceEnsembleElements)(_ ++ spec.ensembles(_)._2)

            for {
                sourceElement <- sourceEnsembleElements
                sourceClassFile = sourceElement match {
                    case s: VirtualClass => project.classFile(s.classType.asObjectType).get
                    case _               => throw SpecificationError(sourceElement.toJava+" is not a class")
                }
                if sourceClassFile.superclassType.map(s =>
                    !allLocalTargetSourceElements.exists(v =>
                        v.classType.asObjectType.equals(s))).getOrElse(false)
            } yield {
                PropertyViolation(
                    project,
                    this,
                    sourceElement,
                    "the element should extend any of the given classes",
                    "required inheritance not found"
                )
            }
        }

        override def toString: String = {
            targetEnsembles.mkString(s"$sourceEnsemble every_element_should_extend (", ",", ")")
        }
    }

    /**
     * The set of all [[org.opalj.de.DependencyTypes]].
     */
    final val USE: Set[DependencyType] = DependencyTypes.values

    case class SpecificationFactory(contextEnsembleSymbol: Symbol) {

        def apply(sourceElementsMatcher: SourceElementsMatcher): Unit = {
            ensemble(contextEnsembleSymbol)(sourceElementsMatcher)
        }

        def is_only_to_be_used_by(sourceEnsembleSymbols: Symbol*): Unit = {
            architectureCheckers =
                new GlobalIncomingConstraint(
                    contextEnsembleSymbol,
                    sourceEnsembleSymbols.toSeq
                ) :: architectureCheckers
        }

        def allows_incoming_dependencies_from(sourceEnsembleSymbols: Symbol*): Unit = {
            architectureCheckers =
                new GlobalIncomingConstraint(
                    contextEnsembleSymbol,
                    sourceEnsembleSymbols.toSeq
                ) :: architectureCheckers
        }

        def is_only_allowed_to(
            dependencyTypes: Set[DependencyType],
            targetEnsembles: Symbol*
        ): Unit = {
            architectureCheckers =
                new LocalOutgoingOnlyAllowedConstraint(
                    dependencyTypes,
                    contextEnsembleSymbol,
                    targetEnsembles.toSeq
                ) :: architectureCheckers
        }

        def is_not_allowed_to(
            dependencyTypes: Set[DependencyType],
            targetEnsembles: Symbol*
        ): Unit = {
            architectureCheckers =
                new LocalOutgoingNotAllowedConstraint(
                    dependencyTypes,
                    contextEnsembleSymbol,
                    targetEnsembles.toSeq
                ) :: architectureCheckers
        }

        def every_element_should_be_annotated_with(
            annotationPredicate: AnnotationPredicate
        ): Unit = {
            architectureCheckers =
                new LocalOutgoingAnnotatedWithConstraint(
                    contextEnsembleSymbol,
                    Seq(annotationPredicate)
                ) :: architectureCheckers
        }

        def every_element_should_be_annotated_with(
            property:             String,
            annotationPredicates: Seq[AnnotationPredicate],
            matchAny:             Boolean                  = false
        ): Unit = {
            architectureCheckers =
                new LocalOutgoingAnnotatedWithConstraint(
                    contextEnsembleSymbol,
                    annotationPredicates,
                    property,
                    matchAny
                ) :: architectureCheckers
        }

        def every_element_should_implement_method(
            methodPredicate: SourceElementPredicate[Method]
        ): Unit = {
            architectureCheckers =
                new LocalOutgoingShouldImplementMethodConstraint(
                    contextEnsembleSymbol,
                    methodPredicate
                ) :: architectureCheckers
        }

        def every_element_should_extend(targetEnsembles: Symbol*): Unit = {
            architectureCheckers =
                new LocalOutgoingShouldExtendConstraint(
                    contextEnsembleSymbol,
                    targetEnsembles.toSeq
                ) :: architectureCheckers
        }
    }

    protected implicit def EnsembleSymbolToSpecificationElementFactory(
        ensembleSymbol: Symbol
    ): SpecificationFactory = {
        SpecificationFactory(ensembleSymbol)
    }

    protected implicit def EnsembleToSourceElementMatcher(
        ensembleSymbol: Symbol
    ): SourceElementsMatcher = {
        if (!ensembles.contains(ensembleSymbol))
            throw SpecificationError(s"the ensemble: $ensembleSymbol is not yet defined")

        ensembles(ensembleSymbol)._1
    }

    /**
     * Returns a textual representation of an ensemble.
     */
    def ensembleToString(ensembleSymbol: Symbol): String = {
        val (sourceElementsMatcher, extension) = ensembles(ensembleSymbol)
        s"$ensembleSymbol{"+
            s"$sourceElementsMatcher  "+
            {
                if (extension.isEmpty)
                    "/* NO ELEMENTS */ "
                else {
                    extension.tail.foldLeft("\n\t//"+extension.head.toString+"\n")((s, vse) => s+"\t//"+vse.toJava+"\n")
                }
            }+"}"
    }

    /**
     * Can be called after the evaluation of the extents of the ensembles to print
     * out the current configuration.
     */
    def ensembleExtentsToString: String = {
        val s = new mutable.StringBuilder()
        for ((ensemble, (_, elements)) <- theEnsembles) {
            s ++= s"$ensemble\n"
            for (element <- elements) {
                s ++= s"\t\t\t${element.toJava}\n"
            }
        }
        s.result()
    }

    def analyze(): Set[SpecificationViolation] = {
        val dependencyStore = time {
            project.get(DependencyStoreWithoutSelfDependenciesKey)
        } { ns => logProgress("2.1. preprocessing dependencies took "+ns.toSeconds) }

        logInfo("Dependencies between source elements: "+dependencyStore.dependencies.size)
        logInfo("Dependencies on primitive types: "+dependencyStore.dependenciesOnBaseTypes.size)
        logInfo("Dependencies on array types: "+dependencyStore.dependenciesOnArrayTypes.size)

        time {
            for {
                (source, targets) <- dependencyStore.dependencies
                (target, dTypes) <- targets
            } {
                allSourceElements += source
                allSourceElements += target

                theOutgoingDependencies.update(source, targets)

                for { dType <- dTypes } {
                    theIncomingDependencies.update(
                        target,
                        theIncomingDependencies.getOrElse(target, immutable.Set.empty) +
                            ((source, dType))
                    )
                }
            }
        } { ns => logProgress("2.2. postprocessing dependencies took "+ns.toSeconds) }
        logInfo("Number of source elements: "+allSourceElements.size)
        logInfo("Outgoing dependencies: "+theOutgoingDependencies.size)
        logInfo("Incoming dependencies: "+theIncomingDependencies.size)

        // Calculate the extension of the ensembles
        //
        time {
            val instantiatedEnsembles =
                theEnsembles.par map { ensemble =>
                    val (ensembleSymbol, (sourceElementMatcher, _)) = ensemble
                    // if a sourceElementMatcher is reused!
                    sourceElementMatcher.synchronized {
                        val extension = sourceElementMatcher.extension(project)
                        if (extension.isEmpty && sourceElementMatcher != NoSourceElementsMatcher)
                            logWarn(s"   $ensembleSymbol (${extension.size})")
                        else
                            logInfo(s"   $ensembleSymbol (${extension.size})")

                        spec.synchronized { matchedSourceElements ++= extension }
                        (ensembleSymbol, (sourceElementMatcher, extension))
                    }
                }
            theEnsembles = mutable.Map.from(instantiatedEnsembles.seq)

            unmatchedSourceElements = allSourceElements --= matchedSourceElements

            logInfo("   => Matched source elements: "+matchedSourceElements.size)
            logInfo("   => Other source elements: "+unmatchedSourceElements.size)
        } { ns =>
            logProgress("3. determing the extension of the ensembles took "+ns.toSeconds)
        }

        // Check all rules
        //
        time {
            val result =
                for { architectureChecker <- architectureCheckers.par } yield {
                    logProgress("   checking: "+architectureChecker)
                    for (violation <- architectureChecker.violations()) yield violation
                }
            Set.empty ++ (result.filter(_.nonEmpty).flatten)
        } { ns =>
            logProgress("4. checking the specified dependency constraints took "+ns.toSeconds)
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

        Project.JavaClassFileReader().ClassFiles(file)
    }

    def ProjectJAR(jarName: String): Seq[(ClassFile, URL)] = {
        val file = new java.io.File(jarName)
        if (!file.exists)
            throw SpecificationError("the specified directory does not exist: "+jarName)
        if (!file.canRead)
            throw SpecificationError("cannot read the specified JAR: "+jarName)
        if (file.isDirectory)
            throw SpecificationError("the specified jar file is a directory: "+jarName)

        OPALLogger.info("creating project", s"loading $jarName")(GlobalLogContext)

        Project.JavaClassFileReader().ClassFiles(file)
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

        OPALLogger.info("creating project", s"loading library $jarName")(GlobalLogContext)

        Project.JavaLibraryClassFileReader.ClassFiles(file)
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
        fileName:          String,
        pathSeparatorChar: Char   = java.io.File.pathSeparatorChar
    ): Iterable[String] = {
        processSource(Source.fromFile(new java.io.File(fileName))) { s =>
            s.getLines().map(_.split(pathSeparatorChar)).flatten.toSet
        }
    }

    /**
     * Returns a list of paths that matches the given
     * regular expression from the given list of paths.
     */
    def PathToJARs(paths: Iterable[String], jarName: Regex): Iterable[String] = {
        val matchedPaths = paths.collect { case p @ (jarName(_)) => p }
        if (matchedPaths.isEmpty)
            throw SpecificationError(s"no path is matched by: $jarName.");
        matchedPaths
    }

    /**
     * Returns a list of paths that match the given list of
     * regular expressions from the given list of paths.
     */
    def PathToJARs(paths: Iterable[String], jarNames: Iterable[Regex]): Iterable[String] = {
        jarNames.foldLeft(Set.empty[String])((c, n) => c ++ PathToJARs(paths, n))
    }
}
