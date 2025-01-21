/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import scala.annotation.tailrec

import org.opalj.collection.IntIterator
import org.opalj.fpcf.AnalysisScenario.AnalysisAutoConfigKey
import org.opalj.graphs.Graph
import org.opalj.graphs.sccs
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.util.PerformanceEvaluation.time

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

    private[this] var scheduleBatches: List[PhaseConfiguration[A]] = List.empty

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

        time {
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

            // TODO ....
            val scheduleStrategyConfig = ScheduleConfig.getConfig
            val scheduleStrategy = scheduleStrategyConfig.getStrategy
            val scheduleLazyTransformerInAllenBatches =
                scheduleStrategyConfig.isLazyTransformerInMultipleBatches

            scheduleStrategy match {
                case ScheduleStrategy.SPS =>
                    this.scheduleBatches = List(computePhase(ps, allCS, Set.empty))

                case ScheduleStrategy.MPS | ScheduleStrategy.IPMS | ScheduleStrategy.OPMS =>
                    val computationSpecificationMap: Map[ComputationSpecification[A], Int] = allCS.zipWithIndex.toMap
                    var scheduleGraph: Map[Int, Set[Int]] = Map.empty

                    def getAllCSFromPropertyBounds(
                        properties: Set[PropertyBounds]
                    ): Set[ComputationSpecification[A]] = {
                        def containsProperty(cs: ComputationSpecification[A], property: PropertyBounds): Boolean =
                            cs.derivesLazily.contains(property) ||
                                cs.derivesCollaboratively.contains(property) ||
                                cs.derivesEagerly.contains(property)

                        allCS.filter(cs => properties.exists(containsProperty(cs, _)))
                    }

                    def mapCSToNum(specifications: Set[ComputationSpecification[A]]): Set[Int] = {
                        specifications.flatMap(computationSpecificationMap.get)
                    }

                    computationSpecificationMap.foreach { csID =>
                        scheduleGraph += (csID._2 -> mapCSToNum(getAllCSFromPropertyBounds(csID._1.uses(ps))))
                    }

                    if (!scheduleLazyTransformerInAllenBatches) {
                        scheduleGraph.foreach { node =>
                            if (computationSpecificationMap.find(_._2 == node._1).map(
                                    _._1
                                ).head.computationType.toString.contains("Lazy") || computationSpecificationMap.find(
                                    _._2 == node._1
                                ).map(
                                    _._1
                                ).head.computationType.toString.contains("Transformer")
                            ) {
                                scheduleGraph.foreach { subNode =>
                                    if (subNode._2.contains(node._1)) {
                                        scheduleGraph =
                                            scheduleGraph +
                                                (node._1 -> (scheduleGraph.get(node._1).head ++ Set(subNode._1)))
                                    }
                                }
                            }
                        }
                    }

                    def edgeFunctionForSCCS(node: Int): IntIterator = {
                        val edges = scheduleGraph.getOrElse(node, Set.empty).iterator
                        new IntIterator {
                            def hasNext: Boolean = edges.hasNext
                            def next(): Int = edges.next()
                        }
                    }

                    def getAllUses(css: List[Int]): Set[PropertyBounds] = {
                        var allUses: Set[PropertyBounds] = Set.empty
                        css.foreach { cs =>
                            allUses = allUses ++ computationSpecificationMap.find(_._2 == cs).map(_._1).head.uses(ps)
                        }
                        allUses
                    }

                    var aCyclicGraph = sccs(scheduleGraph.size, edgeFunctionForSCCS)
                        .map(batch => batch -> mapCSToNum(getAllCSFromPropertyBounds(getAllUses(batch))))
                        .toMap

                    var visited: List[List[Int]] = List.empty
                    @tailrec
                    def setLazyInAllBatches(map: Map[List[Int], Set[Int]], firstElement: List[Int]): Unit = {
                        if (firstElement.forall(csID =>
                                computationSpecificationMap.find(_._2 == csID).map(
                                    _._1
                                ).head.computationType.toString.contains("Lazy") ||
                                    computationSpecificationMap.find(_._2 == csID).map(
                                        _._1
                                    ).head.computationType.toString.contains("Transformer")
                            )
                        ) {
                            var existInSomeBatch = false
                            map.foreach { batch =>
                                if (batch._2.toList.intersect(firstElement).nonEmpty && batch._1 != firstElement) {
                                    aCyclicGraph = aCyclicGraph + ((batch._1 ++ firstElement) -> mapCSToNum(
                                        getAllCSFromPropertyBounds(getAllUses(batch._1 ++ firstElement))
                                    ).diff((batch._1 ++ firstElement).toSet))
                                    aCyclicGraph = aCyclicGraph - batch._1
                                    existInSomeBatch = true
                                }
                            }
                            if (existInSomeBatch) {
                                aCyclicGraph = aCyclicGraph - firstElement
                                setLazyInAllBatches(aCyclicGraph, aCyclicGraph.head._1)
                            } else {
                                visited = visited :+ firstElement
                                val keyList = aCyclicGraph.keys.toSet -- visited
                                if (keyList.nonEmpty) {
                                    setLazyInAllBatches(aCyclicGraph, keyList.head)
                                }
                            }
                        } else {
                            visited = visited :+ firstElement
                            val keyList = aCyclicGraph.keys.toSet -- visited
                            if (keyList.nonEmpty) {
                                setLazyInAllBatches(aCyclicGraph, keyList.head)
                            }
                        }
                    }

                    if (scheduleLazyTransformerInAllenBatches) {
                        setLazyInAllBatches(aCyclicGraph, aCyclicGraph.head._1)
                    }

                    val preparedGraph = aCyclicGraph.map { case (nodes, deps) =>
                        nodes -> (deps -- nodes).toList
                    }

                    var transformingMap: Map[Int, List[Int]] = Map.empty
                    var counter = 0
                    preparedGraph.foreach { node =>
                        transformingMap = transformingMap + (counter -> node._1)
                        counter = counter + 1
                    }

                    var preparedGraph2 = preparedGraph.map { case (node, deps) =>
                        var dependencies: List[Int] = List.empty
                        transformingMap.foreach { tuple =>
                            if (tuple._2.intersect(deps).nonEmpty) {
                                dependencies = dependencies :+ tuple._1
                            }
                        }
                        transformingMap.find(_._2 == node).map(_._1).head -> dependencies
                    }

                    if (scheduleStrategy == ScheduleStrategy.IPMS || scheduleStrategy == ScheduleStrategy.OPMS) {
                        def mergeIndependentBatches(graph: Map[Int, List[Int]]): Map[Int, List[Int]] = {
                            var allUses: Set[Int] = Set.empty
                            def getUses(batch: Int): Set[Int] = {
                                val uses = graph.get(batch).head
                                allUses = allUses ++ uses

                                uses.foreach { otherBatch => getUses(otherBatch) }

                                val returnUses = allUses
                                returnUses
                            }

                            var map: Map[Int, Set[Int]] = Map.empty
                            graph.foreach { batch =>
                                val tempUses = getUses(batch._1)
                                map = map + (batch._1 -> tempUses)
                                allUses = Set.empty
                            }

                            var couldBeMerged: List[(Int, Int)] = List.empty
                            map.foreach { batch =>
                                map.foreach { subBatch =>
                                    if (subBatch != batch) {
                                        if ((!subBatch._2.contains(batch._1)) && (!batch._2.contains(subBatch._1))) {
                                            if (!couldBeMerged.contains((subBatch._1, batch._1))) {
                                                couldBeMerged = couldBeMerged :+ (batch._1, subBatch._1)
                                            }

                                        }
                                    }

                                }

                            }

                            var updatedGraph: Map[Int, List[Int]] = graph
                            if (couldBeMerged.nonEmpty && scheduleStrategy == ScheduleStrategy.IPMS) {
                                val tempTransformation_2 =
                                    (transformingMap.get(couldBeMerged.head._1).head ++
                                        transformingMap.get(couldBeMerged.head._2).head).distinct
                                transformingMap =
                                    transformingMap - couldBeMerged.head._1 - couldBeMerged.head._2
                                transformingMap = transformingMap + (counter -> tempTransformation_2)

                                val tempGraph_2: List[Int] = (graph.get(couldBeMerged.head._1).head ++
                                    graph.get(couldBeMerged.head._2).head).distinct
                                updatedGraph = updatedGraph - couldBeMerged.head._1 - couldBeMerged.head._2
                                updatedGraph = updatedGraph + (counter -> tempGraph_2)

                                def replaceIdInMap(oldId: Int, newId: Int): Unit = {
                                    updatedGraph = updatedGraph.map { case (key, values) =>
                                        key -> values.map(v => if (v == oldId) newId else v)
                                    }
                                }

                                replaceIdInMap(couldBeMerged.head._1, counter)
                                replaceIdInMap(couldBeMerged.head._2, counter)
                                counter = counter + 1
                                updatedGraph = mergeIndependentBatches(updatedGraph)

                            } else if (couldBeMerged.nonEmpty && scheduleStrategy == ScheduleStrategy.OPMS) {
                                def checkForLeastAmountOfAnalysis(): (Int, Int) = {
                                    var twoBatchesWithLeastAmountOfAnalysis = (0, 0)
                                    var otherSize = 0
                                    couldBeMerged.foreach { tuple =>
                                        if (otherSize == 0) {
                                            twoBatchesWithLeastAmountOfAnalysis = tuple
                                            otherSize = transformingMap.get(tuple._1).head.size + transformingMap.get(
                                                tuple._2
                                            ).head.size
                                        } else if (transformingMap.get(tuple._1).head.size + transformingMap.get(
                                                       tuple._2
                                                   ).head.size < otherSize
                                        ) {
                                            twoBatchesWithLeastAmountOfAnalysis = tuple
                                            otherSize = transformingMap.get(tuple._1).head.size + transformingMap.get(
                                                tuple._2
                                            ).head.size
                                        }

                                    }
                                    twoBatchesWithLeastAmountOfAnalysis
                                }

                                val toBeMerged = checkForLeastAmountOfAnalysis()

                                val tempTransformation_2 = (transformingMap.get(toBeMerged._1).head ++
                                    transformingMap.get(toBeMerged._2).head).distinct
                                transformingMap =
                                    transformingMap - toBeMerged._1 - toBeMerged._2
                                transformingMap = transformingMap + (counter -> tempTransformation_2)

                                val tempGraph_2: List[Int] = (graph.get(toBeMerged._1).head ++
                                    graph.get(toBeMerged._2).head).distinct
                                updatedGraph = updatedGraph - toBeMerged._1 - toBeMerged._2
                                updatedGraph = updatedGraph + (counter -> tempGraph_2)

                                def replaceIdInMap(oldId: Int, newId: Int): Unit = {
                                    updatedGraph = updatedGraph.map { case (key, values) =>
                                        key -> values.map(v => if (v == oldId) newId else v)
                                    }
                                }

                                replaceIdInMap(toBeMerged._1, counter)
                                replaceIdInMap(toBeMerged._2, counter)
                                counter = counter + 1
                                updatedGraph = mergeIndependentBatches(updatedGraph)
                            }
                            updatedGraph
                        }
                        preparedGraph2 = mergeIndependentBatches(preparedGraph2)
                    }

                    def topologicalSort(graph: Map[Int, List[Int]]): List[Int] = {
                        var sortedNodes: List[Int] = List.empty
                        var permanent: Set[Int] = Set.empty
                        var temporary: Set[Int] = Set.empty

                        val preparedGraph = graph.map { case (node, deps) =>
                            node -> deps.filter(_ != node)
                        }

                        def visit(node: Int): Unit = {
                            if (!permanent.contains(node)) {
                                if (temporary.contains(node)) {
                                    throw new IllegalStateException("Graph contains a cycle")
                                }
                                temporary = temporary + node

                                preparedGraph.get(node).head.foreach { otherNode => visit(otherNode) }

                                permanent = permanent + node
                                temporary = temporary - node

                                sortedNodes = sortedNodes :+ node
                            }

                        }
                        for (node <- preparedGraph.keys) {
                            visit(node)
                        }

                        sortedNodes
                    }

                    val batchOrder = topologicalSort(preparedGraph2)

                    var alreadyScheduledCS: Set[ComputationSpecification[A]] = Set.empty
                    batchOrder.foreach { batch =>
                        var scheduledInThisPhase: Set[ComputationSpecification[A]] = Set.empty
                        transformingMap.get(batch).head.foreach { csID =>
                            scheduledInThisPhase =
                                scheduledInThisPhase + computationSpecificationMap.find(_._2 == csID).map(_._1).head
                        }
                        this.scheduleBatches = this.scheduleBatches :+ computePhase(
                            ps,
                            scheduledInThisPhase,
                            (allCS -- scheduledInThisPhase -- alreadyScheduledCS)
                        )
                        alreadyScheduledCS = alreadyScheduledCS ++ scheduledInThisPhase
                    }
                case _ => throw new IllegalStateException(s"Invalid scheduler configuration: $scheduleStrategy");
            }

            OPALLogger.info("scheduler", s"scheduling strategy ${scheduleStrategy} is selected")
        } { t => OPALLogger.info("scheduler", s"initialization of Scheduler took ${t.toSeconds}") }

        Schedule(
            scheduleBatches,
            initializationData
        )
    }

    /**
     * Computes the configuration for a specific batch; this method can only handle the situation
     * where all analyses can be executed in the same phase.
     */
    private def computePhase(
        propertyStore:     PropertyStore,
        phaseAnalysis:     Set[ComputationSpecification[A]],
        nextPhaseAnalysis: Set[ComputationSpecification[A]]
    ): PhaseConfiguration[A] = {

        // 1. compute the phase configuration; i.e., find those properties for which we must
        //    suppress interim updates.
        var suppressInterimUpdates: Map[PropertyKind, Set[PropertyKind]] = Map.empty
        // Interim updates have to be suppressed when an analysis uses a property for which
        // the wrong bounds/not enough bounds are computed.
        transformersCS foreach { cs => suppressInterimUpdates += (cs.derivesLazily.get.pk -> cs.uses(ps).map(_.pk)) }

        def extractPropertyKinds(analyses: Set[ComputationSpecification[A]]): Set[PropertyKind] = {
            analyses.flatMap { analysis =>
                (analysis.derivesLazily.toSet ++
                    analysis.derivesEagerly ++
                    analysis.derivesCollaboratively ++
                    analysis.derives.toSet)
                    .map(_.pk)
            }
        }

        val propertyKindsFromPhaseAnalysis = extractPropertyKinds(phaseAnalysis)
        val propertyKindsFromNextPhaseAnalysis = extractPropertyKinds(nextPhaseAnalysis)

        // 3. create the batch
        val batchBuilder = List.newBuilder[ComputationSpecification[A]]
        batchBuilder ++= phaseAnalysis

        // FIXME...

        // Interim updates can be suppressed when the depender and dependee are not in a cyclic
        // relation; however, this could have a negative impact on the effect of deep laziness -
        // once we are actually implementing it. For the time being, suppress notifications is always
        // advantageous.

        val phase1Configuration = PropertyKindsConfiguration(
            propertyKindsComputedInThisPhase = propertyKindsFromPhaseAnalysis,
            suppressInterimUpdates = suppressInterimUpdates,
            propertyKindsComputedInLaterPhase = propertyKindsFromNextPhaseAnalysis
        )

        PhaseConfiguration(phase1Configuration, batchBuilder.result())
    }
}

/**
 * Factory to create an [[AnalysisScenario]].
 */
object AnalysisScenario {

    final val AnalysisAutoConfigKey = "org.opalj.fpcf.AnalysisScenario.AnalysisAutoConfig"
    final val AnalysisScheduleStrategy = "org.opalj.fpcf.AnalysisScenario.ScheduleStrategy"
    final val AnalysisScheduleLazyTransformerInMultipleBatches =
        "org.opalj.fpcf.AnalysisScenario.AnalysisScheduleLazyTransformerInMultipleBatches"

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
