/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.fpcf.AnalysisScenario.AnalysisAutoConfigKey
import org.opalj.graphs.Graph
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 * Provides functionality to determine whether a set of analyses is compatible and to compute
 * a schedule to execute a set of analyses.
 *
 * @author Michael Eichberg
 */
class AnalysisScenario[A](val ps: PropertyStore) {

    private[this] var scheduleComputed: Boolean = false

    private[this] var allCS: Set[ComputationSpecification[A]] = Set.empty

    private[this] var derivedProperties: Set[PropertyBounds] = Set.empty
    private[this] var eagerlyDerivedProperties: Set[PropertyBounds] = Set.empty
    private[this] var collaborativelyDerivedProperties: Set[PropertyBounds] = Set.empty
    private[this] var lazilyDerivedProperties: Set[PropertyBounds] = Set.empty

    private[this] var initializationData: Map[ComputationSpecification[A], Any] = Map.empty

    private[this] var usedProperties: Set[PropertyBounds] = Set.empty

    private[this] var eagerCS: Set[ComputationSpecification[A]] = Set.empty
    private[this] var lazyCS: Set[ComputationSpecification[A]] = Set.empty
    private[this] var triggeredCS: Set[ComputationSpecification[A]] = Set.empty
    private[this] var transformersCS: Set[ComputationSpecification[A]] = Set.empty

    private[this] var derivedBy: Map[PropertyKind, (PropertyBounds, Set[ComputationSpecification[A]])] = {
        Map.empty
    }

    def allProperties: Set[PropertyBounds] = derivedProperties ++ usedProperties

    /**
     * Adds the given computation specification (`cs`) to the set of computation specifications
     * that should be scheduled.
     */
    def +=(cs: ComputationSpecification[A]): this.type = {
        if (scheduleComputed) {
            throw new IllegalStateException("process was already computed");
        }

        cs.computationType match {
            case EagerComputation     => eagerCS += cs
            case TriggeredComputation => triggeredCS += cs
            case LazyComputation      => lazyCS += cs
            case Transformer          => transformersCS += cs
        }

        allCS += cs

        initializationData += cs -> cs.init(ps)

        this
    }

    /**
     * Returns the graph which depicts the dependencies between the computed properties
     * based on the current computation specifications.
     * I.e., a property `d` depends on another property `p` if the algorithm which computes
     * `d` uses the property `p`.
     *
     * @note Can only be called after the schedule method was called! If no schedule could be
     *       computed the result of this method is undefined.
     */
    def propertyComputationsDependencies: Graph[PropertyBounds] = {
        if (!scheduleComputed) {
            throw new IllegalStateException("initialization incomplete; schedule not computed");
        }
        val psDeps = Graph.empty[PropertyBounds]
        allCS foreach { cs =>
            // all derived properties depend on all used properties
            cs.derives foreach { derived =>
                psDeps addVertice derived
                cs.uses(ps) foreach { use => psDeps addEdge (derived, use) }
            }
        }
        psDeps
    }

    /**
     * Returns the dependencies between the computations.
     *
     * @note Can only be called after the schedule method was called! If no schedule could be
     *       computed the result of this method is undefined.
     */
    def computationDependencies: Graph[ComputationSpecification[A]] = {
        if (!scheduleComputed) {
            throw new IllegalStateException("initialization incomplete; schedule not computed");
        }

        val compDeps = Graph.empty[ComputationSpecification[A]]
        val derivedBy: Map[PropertyBounds, Set[ComputationSpecification[A]]] = {
            var derivedBy: Map[PropertyBounds, Set[ComputationSpecification[A]]] = Map.empty
            allCS foreach { cs =>
                cs.derives foreach { derives => derivedBy += derives -> (derivedBy.getOrElse(derives, Set.empty) + cs) }
            }
            derivedBy
        }
        allCS foreach { cs =>
            compDeps addVertice cs
            cs.uses(ps) foreach { usedPK =>
                derivedBy.get(usedPK).iterator.flatten.foreach { providerCS =>
                    if (providerCS ne cs) {
                        compDeps addEdge (cs, providerCS)
                    }
                }
            }
        }
        // let's handle the case that multiple analyses derive a property collaboratively
        derivedBy.valuesIterator.filter(_.size > 1) foreach { css =>
            val cssIt = css.iterator
            val headCS = cssIt.next()
            var lastCS = headCS
            do {
                val nextCS = cssIt.next()
                compDeps addEdge (lastCS -> nextCS)
                lastCS = nextCS
            } while (cssIt.hasNext)
            compDeps addEdge (lastCS -> headCS)
        }

        compDeps
    }

    private[this] def processCS(cs: ComputationSpecification[A]): Unit = {
        // 1. check the most basic constraints
        cs.derivesLazily foreach { lazilyDerivedProperty =>
            if (derivedProperties.contains(lazilyDerivedProperty)) {
                val pkName = PropertyKey.name(lazilyDerivedProperty.pk.id)
                val m = s"can not register $cs: $pkName is already computed by another analysis"
                throw new SpecificationViolation(m)
            }
        }

        cs.derivesCollaboratively foreach { collaborativelyDerivedProperty =>
            if (eagerlyDerivedProperties.contains(collaborativelyDerivedProperty) ||
                lazilyDerivedProperties.contains(collaborativelyDerivedProperty)
            ) {
                val pkName = PropertyKey.name(collaborativelyDerivedProperty.pk.id)
                val m =
                    s"can not register $cs: " +
                        s"$pkName is not computed collaboratively by all analyses"
                throw new SpecificationViolation(m)
            }
        }

        cs.derivesEagerly foreach { eagerlyDerivedProperty =>
            if (derivedProperties.contains(eagerlyDerivedProperty)) {
                val pkName = PropertyKey.name(eagerlyDerivedProperty.pk.id)
                val m = s"can not register $cs: $pkName is already computed by another analysis"
                throw new SpecificationViolation(m)
            }
        }

        // TODO Check inner consistency: that is, if an analysis derives multiple properties, they have to be compatible

        // 2. register the analysis
        def handleDerivedProperties(derivedProperties: Set[PropertyBounds]): Unit = {
            this.derivedProperties ++= derivedProperties
            derivedProperties foreach { derivedProperty =>
                val pk = derivedProperty.pk
                derivedBy.get(pk) match {
                    case None =>
                        derivedBy += ((pk, (derivedProperty, Set(cs))))
                    case Some((`derivedProperty`, css)) =>
                        derivedBy += ((pk, (derivedProperty, css + cs)))
                    case Some((deviatingPropertyBounds, css)) =>
                        val propertyName = PropertyKey.name(pk)
                        throw new IllegalArgumentException(
                            s"different bounds ($deviatingPropertyBounds vs. $derivedProperty) " +
                                s"are computed by $css vs. $cs " +
                                s"for the collaboratively computed property $propertyName"
                        )
                }
            }
        }

        usedProperties ++= cs.uses(ps)

        eagerlyDerivedProperties ++= cs.derivesEagerly
        handleDerivedProperties(cs.derivesEagerly)
        collaborativelyDerivedProperties ++= cs.derivesCollaboratively
        handleDerivedProperties(cs.derivesCollaboratively)
        lazilyDerivedProperties ++= cs.derivesLazily.toList
        handleDerivedProperties(cs.derivesLazily.toSet)
    }

    /**
     * Computes an executable schedule.
     *
     * When executing a schedule, the following steps will be performed:
     *  1. all analyses' init method will be called; this may lead to the initialization
     *     of properties
     *
     *  1. setupPhase is called
     *  1. all lazy analyses and all transformers are registered;
     *     (Immediately before registration, beforeSchedule is called.)
     *  1. all triggered computations are registered; this may trigger computations related
     *     to initial properties
     *     (Immediately before registration, beforeSchedule is called.)
     *  1. all eager analyses are started
     *     (Immediately before registration, beforeSchedule is called.)
     *  1. when the phase has finished, all analyses' afterPhaseCompletion methods are called.
     *
     * @param propertyStore required to determine which properties are already computed!
     */
    def computeSchedule(
        propertyStore:   PropertyStore,
        defaultAnalysis: PropertyBounds => Option[ComputationSpecification[A]] = _ => None
    )(
        implicit logContext: LogContext
    ): Schedule[A] = {
        if (scheduleComputed) {
            throw new IllegalStateException("schedule already computed");
        } else {
            scheduleComputed = true
        }

        allCS.foreach(processCS)

        val alreadyComputedPropertyKinds = propertyStore.alreadyComputedPropertyKindIds.toSet

        // 0. check that a property was not already derived
        allCS.foreach { cs =>
            cs.derives foreach { derivedProperty =>
                if (alreadyComputedPropertyKinds.contains(derivedProperty.pk.id)) {
                    val pkName = PropertyKey.name(derivedProperty.pk.id)
                    val m = s"can not register $cs: $pkName was computed in a previous phase"
                    throw new SpecificationViolation(m)
                }
            }
        }

        // 1. check for properties that are not derived (and which require an analysis)
        def useFallback(underivedProperty: PropertyBounds, propertyName: String) = {
            if (PropertyKey.hasFallback(underivedProperty.pk)) {
                val message = s"no analyses scheduled for: $propertyName; using fallback"
                OPALLogger.warn("analysis configuration", message)
            } else {
                throw new IllegalStateException(s"no analysis scheduled for $propertyName")
            }
        }

        val analysisAutoConfig = BaseConfig.getBoolean(AnalysisAutoConfigKey)
        val underivedProperties = usedProperties -- derivedProperties
        underivedProperties
            .filterNot { underivedProperty => alreadyComputedPropertyKinds.contains(underivedProperty.pk.id) }
            .foreach { underivedProperty =>
                if (!derivedProperties.contains(underivedProperty)) {
                    val propertyName = PropertyKey.name(underivedProperty.pk.id)
                    val defaultCSOpt =
                        if (analysisAutoConfig) defaultAnalysis(underivedProperty) else None
                    if (defaultCSOpt.isDefined) {
                        val defaultCS = defaultCSOpt.get
                        try {
                            processCS(defaultCS)
                            val message = s"no analyses scheduled for: $propertyName; using ${defaultCS.name}"
                            OPALLogger.info("analysis configuration", message)
                        } catch {
                            case _: SpecificationViolation =>
                                useFallback(underivedProperty, propertyName)
                        }
                    } else {
                        useFallback(underivedProperty, propertyName)
                    }
                }
            }

        // TODO check all further constraints (in particular those related to cyclic dependencies between analysis...)

        // 2. assign analyses to different batches if an analysis can only process
        //    final properties (unless it is a transformer, the latter have special paths and
        //    constraints and can always be scheduled in the same batch!)

        // TODO ....

//                --------------------------------- VERSUCH 1 ---------------------------------

//        def findFirstNode(
//            graph: Map[ComputationSpecification[A], List[ComputationSpecification[A]]]
//        ): Set[ComputationSpecification[A]] = {
//            var possibleFirstNode: Set[ComputationSpecification[A]] = Set.empty
//            graph foreach { node =>
//                if (node._1.uses(ps).isEmpty) {
//                    possibleFirstNode = possibleFirstNode + node._1
//                }
//            }
//            possibleFirstNode
//        }
//
//        def makeAcyclicGraph(
//            graph: Map[ComputationSpecification[A], List[ComputationSpecification[A]]],
//            head:  ComputationSpecification[A]
//        ): Map[List[ComputationSpecification[A]], List[ComputationSpecification[A]]] = {
//            var groupedGraph: Map[List[ComputationSpecification[A]], List[PropertyBounds]] =
//                Map.empty
//            var visited: Set[ComputationSpecification[A]] = Set.empty
//            var currentGroup = List.empty[ComputationSpecification[A]]
//
//            def getAllUses(group: List[ComputationSpecification[A]]): List[ComputationSpecification[A]] = {
//                var usesList: Set[ComputationSpecification[A]] = Set.empty
//                group foreach { cs => usesList = usesList ++ graph(cs) }
//                usesList.toList
//            }
//
//            def processNode(
//                node: ComputationSpecification[A]
//            ): Unit = {
//                if (!visited.contains(node)) {
//                    visited += node
//
//                    val startsNewGroup = if (node.computationType.toString.contains("Eager")) {
//                        true
//                    } else {
//                        false
//                    }
//                    if (startsNewGroup) {
//                        if (currentGroup.nonEmpty) {
//                            groupedGraph = groupedGraph + (currentGroup -> getAllUses(currentGroup))
//                        }
//                        currentGroup = List(node)
//                    } else {
//                        currentGroup = currentGroup :+ node
//                    }
//                    val connectedNodes = graph.getOrElse(node, List.empty)
//                    connectedNodes.foreach { nextNode => processNode(nextNode) }
//                }
//            }
//
//            def howardAlgorithm(input: Set[Set[PropertyBounds]]): Set[Set[PropertyBounds]] = {
//                var inputSet = input
//                var output = Set.empty[Set[PropertyBounds]]
//
//                while (inputSet.nonEmpty) {
//                    var first = inputSet.head
//                    var rest = inputSet.tail
//                    var lf = -1
//
//                    while (first.size > lf) {
//                        lf = first.size
//                        var rest2 = Set.empty[Set[PropertyBounds]]
//
//                        for (r <- rest) {
//                            if (first.intersect(r).nonEmpty) {
//                                first = first ++ r
//                            } else {
//                                rest2 = rest2 + r
//
//                            }
//                        }
//
//                        rest = rest2
//                    }
//
//                    output = output + first
//                    inputSet = rest
//                }
//
//                output
//            }
//
//            processNode(head)
//
//            if (currentGroup.nonEmpty) {
//                groupedGraph = groupedGraph + (currentGroup -> getAllUses(currentGroup))
//            }
//
//            groupedGraph
//
//        }
//
//        // directedGraph: Map[ComputerSpecification, List[Outputs]]
//        var directedGraph: Map[ComputationSpecification[A], List[ComputationSpecification[A]]] = Map
//            .empty
//
//        allCS foreach { cs =>
//            var edges: List[ComputationSpecification[A]] = List.empty[ComputationSpecification[A]]
//
//            var csOutput: Set[PropertyBounds] = Set.empty
//            if (cs.derivesCollaboratively.isEmpty) {
//                csOutput = cs.derivesLazily.toSet
//            } else if (cs.derivesLazily.toSet.isEmpty) {
//                csOutput = cs.derivesCollaboratively
//            }
//            allCS foreach { tempCS =>
//                if (tempCS.uses(ps).intersect(csOutput).nonEmpty) {
//                    edges = edges :+ tempCS
//                }
//            }
//            directedGraph = directedGraph + (cs -> edges)
//        }
//
//        val head = findFirstNode(directedGraph).head
//
//        val test = makeAcyclicGraph(directedGraph, head)
//
//        println(test)
//        print("")

//        --------------------------------- VERSUCH 1 ---------------------------------

//        --------------------------------- VERSUCH 2 ---------------------------------
//        allCS foreach { cs => println(cs.toString() + " - " + computationDependencies(cs)).toString }
//
//        collaborativelyDerivedProperties foreach { property =>
//            println(property.toString() + " - " + propertyComputationsDependencies(property)).toString
//        }
//
//        def howardAlgorithm(input: Set[Set[PropertyBounds]]): Set[Set[PropertyBounds]] = {
//            var inputSet = input
//            var output = Set.empty[Set[PropertyBounds]]
//
//            while (inputSet.nonEmpty) {
//                var first = inputSet.head
//                var rest = inputSet.tail
//                var lf = -1
//
//                while (first.size > lf) {
//                    lf = first.size
//                    var rest2 = Set.empty[Set[PropertyBounds]]
//
//                    for (r <- rest) {
//                        if (first.intersect(r).nonEmpty) {
//                            first = first ++ r
//                        } else {
//                            rest2 = rest2 + r
//
//                        }
//                    }
//
//                    rest = rest2
//                }
//
//                output = output + first
//                inputSet = rest
//            }
//
//            output
//        }
//
//        def directedGraph(derivedPropertySets: Set[Set[PropertyBounds]]): Map[
//            Set[ComputationSpecification[A]],
//            Set[PropertyBounds]
//        ] = {
//
//            def getAllUses(css: Set[ComputationSpecification[A]]): Set[PropertyBounds] = {
//                var allUses: Set[PropertyBounds] = Set.empty
//                css foreach { cs => allUses = allUses ++ cs.uses(ps) }
//                allUses
//            }
//
//            def getAllDerivedProperties(css: Set[ComputationSpecification[A]]): Set[PropertyBounds] = {
//                var allDerivedProperties: Set[PropertyBounds] = Set.empty
//                css foreach { cs =>
//                    if (cs.derivesEagerly.nonEmpty) {
//                        allDerivedProperties = allDerivedProperties ++ cs.derivesEagerly
//                    } else if (cs.derivesLazily.nonEmpty) {
//                        allDerivedProperties = allDerivedProperties ++ cs.derivesLazily
//                    } else if (cs.derivesCollaboratively.nonEmpty) {
//                        allDerivedProperties = allDerivedProperties ++ cs.derivesCollaboratively
//                    }
//                }
//                allDerivedProperties
//            }
//
//            var forwardConnectionDirectedCyclicGraph: Map[
//                Set[ComputationSpecification[A]],
//                Set[PropertyBounds]
//            ] = Map.empty
//            var backwardConnectionDirectedCyclicGraph: Map[
//                Set[ComputationSpecification[A]],
//                Set[PropertyBounds]
//            ] = Map.empty
//
//            var alreadyUsedCS: Set[ComputationSpecification[A]] = Set.empty
//            derivedPropertySets foreach { set =>
//                var directedCyclicNode: Set[ComputationSpecification[A]] = Set.empty
//                set foreach { property =>
//                    allCS foreach { cs =>
//                        if (cs.derivesCollaboratively.contains(property) || cs.derivesLazily.contains(property)) {
//                            directedCyclicNode = directedCyclicNode + cs
//                            alreadyUsedCS = alreadyUsedCS + cs
//                        }
//                    }
//                }
//                forwardConnectionDirectedCyclicGraph =
//                    forwardConnectionDirectedCyclicGraph + (directedCyclicNode -> getAllDerivedProperties(
//                        directedCyclicNode
//                    ))
//                backwardConnectionDirectedCyclicGraph =
//                    backwardConnectionDirectedCyclicGraph + (directedCyclicNode -> getAllUses(
//                        directedCyclicNode
//                    ))
//            }
//
//            val toComputeCS = allCS.diff(alreadyUsedCS)
//            println(toComputeCS)
//
//            forwardConnectionDirectedCyclicGraph
//
//        }
//
//        var collaborativelyDerivedPropertySet: Set[Set[PropertyBounds]] = Set.empty
//        collaborativelyDerivedProperties foreach { property =>
//            collaborativelyDerivedPropertySet =
//                collaborativelyDerivedPropertySet + propertyComputationsDependencies(property).toSet
//        }
//
//        collaborativelyDerivedPropertySet = collaborativelyDerivedPropertySet
//
//        val test = directedGraph(howardAlgorithm(collaborativelyDerivedPropertySet))
//        println(test)
//
//              --------------------------------- VERSUCH 2 ---------------------------------

//              --------------------------------- VERSUCH 3 ---------------------------------
//        var cSpecification: Map[String, Set[ComputationSpecification[A]]] = Map.empty
//        var forwardConnection: Map[String, Set[ComputationSpecification[A]]] = Map.empty
//        var backwardConnection: Map[String, Set[ComputationSpecification[A]]] = Map.empty
//        var counter: Int = 1
//
//        def getAllUsesCS(usesProperties: Set[PropertyBounds]): List[ComputationSpecification[A]] = {
//            var convertedUsesInCS: List[ComputationSpecification[A]] = List.empty
//            usesProperties foreach { property =>
//                allCS foreach { cs =>
//                    if (cs.derivesLazily.contains(property) || cs.derivesCollaboratively.contains(
//                            property
//                        ) || cs.derivesEagerly.contains(property)
//                    ) {
//                        convertedUsesInCS = convertedUsesInCS :+ cs
//                    }
//                }
//            }
//            convertedUsesInCS
//        }
//
//        def getAllDerivedPropertiesCS(derivedPropertiesProperties: Set[PropertyBounds]): List[ComputationSpecification[A]] = {
//            var convertedDerivedPropertiesInCS: List[ComputationSpecification[A]] = List.empty
//            derivedPropertiesProperties foreach { property =>
//                allCS foreach { cs =>
//                    if (cs.uses(ps).contains(property)) {
//                        convertedDerivedPropertiesInCS = convertedDerivedPropertiesInCS :+ cs
//                    }
//                }
//            }
//            convertedDerivedPropertiesInCS
//        }
//
//        def addCSpec(spec: ComputationSpecification[A]): Unit = {
//            cSpecification += (counter.toString -> Set(spec))
//            var specBackwardCon: Set[PropertyBounds] = Set.empty
//            if (spec.derivesCollaboratively.nonEmpty) {
//                specBackwardCon = spec.derivesCollaboratively
//            } else if (spec.derivesLazily.nonEmpty) {
//                specBackwardCon = spec.derivesLazily.toSet
//            } else if (spec.derivesEagerly.nonEmpty) {
//                specBackwardCon = spec.derivesEagerly
//            }
//            backwardConnection = backwardConnection + (counter.toString -> getAllUsesCS(spec.uses(ps)).toSet)
//            forwardConnection = forwardConnection + (counter.toString -> getAllDerivedPropertiesCS(specBackwardCon).toSet)
//            counter = counter + 1
//        }
//
//        def addCSpecSet(
//            spec:        Set[ComputationSpecification[A]],
//            backwardCon: List[ComputationSpecification[A]],
//            forwardCon:  List[ComputationSpecification[A]]
//        ): Unit = {
//            cSpecification = cSpecification + (counter.toString -> spec)
//            backwardConnection = backwardConnection + (counter.toString -> backwardCon.toSet)
//            forwardConnection = forwardConnection + (counter.toString -> forwardCon.toSet)
//            counter = counter + 1
//        }
//
//        def howardAlgorithm(input: Set[Set[ComputationSpecification[A]]]): Set[Set[ComputationSpecification[A]]] = {
//            var inputSet = input
//            var output = Set.empty[Set[ComputationSpecification[A]]]
//
//            while (inputSet.nonEmpty) {
//                var first = inputSet.head
//                var rest = inputSet.tail
//                var lf = -1
//
//                while (first.size > lf) {
//                    lf = first.size
//                    var rest2 = Set.empty[Set[ComputationSpecification[A]]]
//
//                    for (r <- rest) {
//                        if (first.intersect(r).nonEmpty) {
//                            first = first ++ r
//                        } else {
//                            rest2 = rest2 + r
//                        }
//                    }
//                    rest = rest2
//                }
//                output = output + first
//                inputSet = rest
//            }
//            output
//        }
//
//        def connectDerivesCollaborativelyCS(): Set[Set[ComputationSpecification[A]]] = {
//            var derivesCollaborativelyCS: Set[Set[ComputationSpecification[A]]] = Set.empty
//            collaborativelyDerivedProperties foreach { property =>
//                var css: Set[ComputationSpecification[A]] = Set.empty
//                allCS foreach { cs =>
//                    if (cs.derivesCollaboratively.contains(property)) {
//                        css = css + cs
//                    }
//                }
//                derivesCollaborativelyCS = derivesCollaborativelyCS + css
//            }
//            howardAlgorithm(derivesCollaborativelyCS)
//        }
//
//        def isAllLazyComputation(specs: Set[ComputationSpecification[A]]): Boolean = {
//            var isNotEager = true
//
//            if (specs.nonEmpty) {
//                specs foreach (spec =>
//                    if (spec.computationType.toString.equals("EagerComputation")) {
//                        isNotEager = false
//                    }
//                )
//            }
//            isNotEager
//        }
//
//        def getPossibleFirstNode: Set[String] = {
//            var possibleFirstNodes: Set[String] = Set.empty
//            backwardConnection foreach { con =>
//                if (con._2.isEmpty) {
//                    possibleFirstNodes += con._1
//                }
//            }
//            possibleFirstNodes
//        }
//
//        def getNodesFromCS(css: Set[ComputationSpecification[A]]): Set[String] = {
//            var connectedNodes: Set[String] = Set.empty
//            cSpecification foreach { cS =>
//                if (cS._2.intersect(css).nonEmpty) {
//                    connectedNodes += cS._1
//                }
//            }
//            connectedNodes
//        }
//
//        var visited: Set[String] = Set.empty
//        def createACyclicGraph(node: Set[String]): Unit = {
//            val firstNodes = node
//            if (firstNodes.nonEmpty) {
//                firstNodes foreach { firstNode =>
//                    if (!visited.contains(firstNode)) {
//                        visited = visited + firstNode
//                        if (isAllLazyComputation(cSpecification.get(firstNode).head)) {
//                            val nodesToBeMerged: Set[String] = getNodesFromCS(
//                                forwardConnection.get(firstNode).head.toSet ++ backwardConnection.get(
//                                    firstNode
//                                ).head.toSet
//                            )
//                            nodesToBeMerged foreach { node =>
//                                var newCSpecification = cSpecification.get(node).head
//                                newCSpecification = newCSpecification ++ cSpecification.get(firstNode).head
//                                cSpecification = cSpecification.updated(node, newCSpecification)
//
//                                var newForwardCon: Set[PropertyBounds] = Set.empty
//                                cSpecification.get(node).head foreach (cs =>
//                                    newForwardCon =
//                                        newForwardCon ++ cs.derivesCollaboratively ++ cs.derivesLazily.toSet ++ cs.derivesEagerly
//                                )
//
//                                forwardConnection =
//                                    forwardConnection.updated(node, getAllDerivedPropertiesCS(newForwardCon).toSet)
//
//                                var newBackwardCon: Set[PropertyBounds] = Set.empty
//                                cSpecification.get(node).head foreach (cs =>
//                                    newBackwardCon = newBackwardCon ++ cs.uses(ps)
//                                )
//                                backwardConnection = backwardConnection.updated(node, getAllUsesCS(newBackwardCon).toSet)
//                            }
//                            cSpecification = cSpecification - firstNode
//                            forwardConnection = forwardConnection - firstNode
//                            backwardConnection = backwardConnection - firstNode
//                            createACyclicGraph(getPossibleFirstNode)
//                        } else {
//                            val nextNodesInCS = forwardConnection.get(firstNode).head
//                            val nextNodes = getNodesFromCS(nextNodesInCS.toSet).diff(visited)
//                            if (nextNodes.nonEmpty) {
//                                createACyclicGraph(nextNodes)
//                            }
//                        }
//                    }
//                }
//            } else {
//                var leftNodes: Set[String] = Set.empty
//                cSpecification.foreach(node => leftNodes = leftNodes + node._1)
//                val nextNodesToVisit = leftNodes.diff(visited)
//                if (nextNodesToVisit.nonEmpty) {
//                    createACyclicGraph(Set(nextNodesToVisit.head))
//                }
//            }
//        }
//
//        val derivedCollaborativelyCSS = connectDerivesCollaborativelyCS()
//        var restCS: Set[ComputationSpecification[A]] = Set.empty
//        derivedCollaborativelyCSS foreach { css =>
//            var forwardCon: Set[ComputationSpecification[A]] = Set.empty
//            var backwardCon: Set[ComputationSpecification[A]] = Set.empty
//            var usesSet: Set[PropertyBounds] = Set.empty
//            var derivesPropertySet: Set[PropertyBounds] = Set.empty
//            css foreach { cs =>
//                usesSet = usesSet ++ cs.uses(ps)
//                derivesPropertySet = derivesPropertySet ++ cs.derivesCollaboratively
//            }
//            usesSet = usesSet.diff(derivesPropertySet)
//            backwardCon = getAllUsesCS(usesSet).toSet
//            forwardCon = getAllDerivedPropertiesCS(derivesPropertySet).toSet
//            addCSpecSet(css, backwardCon.toList, forwardCon.toList)
//            restCS = restCS ++ css
//        }
//        val test = allCS.diff(restCS)
//        test foreach { cs => addCSpec(cs) }
//        println("")
//        createACyclicGraph(getPossibleFirstNode)
//
//        var sortedForwardConnection: Map[String, Set[ComputationSpecification[A]]] = Map.empty
//        var sortedBackwardConnection: Map[String, Set[ComputationSpecification[A]]] = Map.empty
//        forwardConnection foreach {cSs =>
//            println(cSs._2.diff(cSpecification.get(cSs._1).head))
//            sortedForwardConnection = sortedForwardConnection + (cSs._1 -> cSs._2.diff(cSpecification.get(cSs._1).head))
//        }
//        backwardConnection foreach {cSs =>
//            println(cSs._2.diff(cSpecification.get(cSs._1).head))
//            sortedBackwardConnection = sortedBackwardConnection + (cSs._1 -> cSs._2.diff(cSpecification.get(cSs._1).head))
//        }
//
//        println("\ncSpecification")
//        cSpecification foreach { cS => println(cS) }
//        println("\nforwardConnection")
//        sortedForwardConnection foreach { cS => println(cS) }
//        println("\nbackwardConnection")
//        sortedBackwardConnection foreach { cS => println(cS) }

//              --------------------------------- VERSUCH 3 ---------------------------------
        println(" ")

        Schedule(
            if (allCS.isEmpty) List.empty else List(computePhase(propertyStore)),
            initializationData
        )
    }

    /**
     * Computes the configuration for a specific batch; this method can only handle the situation
     * where all analyses can be executed in the same phase.
     */
    private def computePhase(propertyStore: PropertyStore): PhaseConfiguration[A] = {

        // 1. compute the phase configuration; i.e., find those properties for which we must
        //    suppress interim updates.
        var suppressInterimUpdates: Map[PropertyKind, Set[PropertyKind]] = Map.empty
        // Interim updates have to be suppressed when an analysis uses a property for which
        // the wrong bounds/not enough bounds are computed.
        transformersCS foreach { cs => suppressInterimUpdates += (cs.derivesLazily.get.pk -> cs.uses(ps).map(_.pk)) }

        // 3. create the batch
        val batchBuilder = List.newBuilder[ComputationSpecification[A]]
        batchBuilder ++= lazyCS
        batchBuilder ++= transformersCS
        batchBuilder ++= triggeredCS
        batchBuilder ++= eagerCS

        // FIXME...

        // Interim updates can be suppressed when the depender and dependee are not in a cyclic
        // relation; however, this could have a negative impact on the effect of deep laziness -
        // once we are actually implementing it. For the time being, suppress notifications is always
        // advantageous.

        val phase1Configuration = PropertyKindsConfiguration(
            propertyKindsComputedInThisPhase = derivedProperties.map(_.pk),
            suppressInterimUpdates = suppressInterimUpdates
        )

        PhaseConfiguration(phase1Configuration, batchBuilder.result())
    }
}

/**
 * Factory to create an [[AnalysisScenario]].
 */
object AnalysisScenario {

    final val AnalysisAutoConfigKey = "org.opalj.fpcf.AnalysisScenario.AnalysisAutoConfig"

    /**
     * @param analyses The set of analyses that should be executed as part of this analysis scenario.
     */
    def apply[A](
        analyses:      Iterable[ComputationSpecification[A]],
        propertyStore: PropertyStore
    ): AnalysisScenario[A] = {
        val as = new AnalysisScenario[A](propertyStore)
        analyses.foreach(as.+=)
        as
    }

}
