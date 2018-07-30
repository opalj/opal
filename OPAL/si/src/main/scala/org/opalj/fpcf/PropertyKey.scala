/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import scala.language.existentials

import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.AtomicInteger

import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds

/**
 * A value object that identifies a specific kind of properties. Every entity in
 * the [[PropertyStore]] must be associated with at most one property per property kind/key.
 *
 * To create a property key use one of the companion object's [[PropertyKey$]].`create` method.
 *
 * @author Michael Eichberg
 */
final class PropertyKey[+P] private[fpcf] (val id: Int) extends AnyVal with PropertyKind {

    override def toString: String = s"PK(${PropertyKey.name(id)},id=$id)"
}

/**
 * Factory and registry for [[PropertyKey]] objects.
 *
 * @author Michael Eichberg
 */
object PropertyKey {

    // TODO pass the cycle to the cycle resolution strategy to enable decisions about the "correct" value
    type CycleResolutionStrategy[E <: Entity, P <: Property] = (PropertyStore, EPS[E, P]) ⇒ P

    private[this] val propertyKeyNames = new AtomicReferenceArray[String](SupportedPropertyKinds)

    private[this] val fallbackPropertyComputations = {
        new AtomicReferenceArray[(PropertyStore, FallbackReason, Entity) ⇒ Property](SupportedPropertyKinds)
    }

    private[this] val fastTrackPropertyComputations = {
        new AtomicReferenceArray[(PropertyStore, Entity) ⇒ Option[Property]](SupportedPropertyKinds)
    }

    private[this] val cycleResolutionStrategies = {
        new AtomicReferenceArray[CycleResolutionStrategy[_ <: Entity, _ <: Property]](SupportedPropertyKinds)
    }

    private[this] val lastKeyId = new AtomicInteger(-1)

    /**
     * Creates a new [[PropertyKey]] object that is to be shared by all properties that belong to
     * the same category.
     *
     * @param name The unique name associated with the property. To ensure
     *              uniqueness it is recommended to prepend (parts of) the package name of property.
     *              Properties defined by OPAL start with "opalj."
     *
     * @param fallbackPropertyComputation A function that returns the property that will be
     *              associated with those entities for which the property is not explicitly
     *              computed. This is generally the bottom value of the lattice.
     *
     * @param cycleResolutionStrategy The strategy that will be used to resolve unfinished cyclic
     *              computations. In the vast majority of cases it is sufficient to just commit
     *              the given value.
     * @param fastTrackPropertyComputation (Optionally) called by the property store if the property
     *              is computed in the current phase and is queried the first time
     *              (see `PropertyStore.setupPhase`). This method is expected to either provide
     *              a precise analysis very fast or to not provide a result at all.
     *              I.e., it is expected to derive only those properties that can trivially be
     *              derived precisely.
     */
    def create[E <: Entity, P <: Property](
        name:                         String,
        fallbackPropertyComputation:  FallbackPropertyComputation[E, P],
        cycleResolutionStrategy:      CycleResolutionStrategy[E, P],
        fastTrackPropertyComputation: (PropertyStore, E) ⇒ Option[P]
    ): PropertyKey[P] = {
        val lastKeyId = this.lastKeyId.incrementAndGet()
        if (lastKeyId >= PropertyKind.SupportedPropertyKinds) {
            throw new IllegalStateException(
                s"maximum number of property keys ($SupportedPropertyKinds) "+
                    "exceeded; increase PropertyKind.SupportedPropertyKinds"
            );
        }
        propertyKeyNames.set(lastKeyId, name)
        var i = 0
        while (i < lastKeyId) {
            if (propertyKeyNames.get(i) == name)
                throw new IllegalArgumentException(s"the property name $name is already used");
            i += 1
        }
        fallbackPropertyComputations.set(
            lastKeyId,
            fallbackPropertyComputation.asInstanceOf[(PropertyStore, FallbackReason, Entity) ⇒ Property]
        )
        fastTrackPropertyComputations.set(
            lastKeyId,
            fastTrackPropertyComputation.asInstanceOf[(PropertyStore, Entity) ⇒ Option[Property]]
        )
        cycleResolutionStrategies.set(
            lastKeyId,
            cycleResolutionStrategy.asInstanceOf[CycleResolutionStrategy[Entity, Property]]
        )

        new PropertyKey(lastKeyId)
    }

    def create[E <: Entity, P <: Property](
        name:                         String,
        fallbackProperty:             P,
        cycleResolutionStrategy:      CycleResolutionStrategy[E, P]  = (_: PropertyStore, eps: EPS[E, P]) ⇒ eps.ub,
        fastTrackPropertyComputation: (PropertyStore, E) ⇒ Option[P] = (_: PropertyStore, _: Entity) ⇒ None
    ): PropertyKey[P] = {
        create(
            name,
            (_: PropertyStore, _: FallbackReason, _: Entity) ⇒ fallbackProperty,
            cycleResolutionStrategy,
            fastTrackPropertyComputation
        )
    }

    /**
     * Updates the (default) cycle resolution strategy associated with a specific kind of
     * property. Updating the strategy is typically done by analyses that require a different
     * strategy than the one defined by the property. For example, an analysis, which just
     * computes a lower bound, generally has to overwrite the default strategy which picks
     * the upper bound in case of a closed strongly connected component.
     *
     * @return The old strategy.
     */
    def updateCycleResolutionStrategy[E <: Entity, P <: Property](
        key:                     PropertyKey[P],
        cycleResolutionStrategy: CycleResolutionStrategy[E, P]
    ): CycleResolutionStrategy[E, P] = {
        val oldStrategy = cycleResolutionStrategies.get(key.id)
        cycleResolutionStrategies.set(key.id, cycleResolutionStrategy)
        oldStrategy.asInstanceOf[CycleResolutionStrategy[E, P]]
    }

    //
    // Query the core properties of each property kind
    // ===============================================
    //

    /**
     * Returns the unique name of the kind of properties associated with the given key id.
     */
    def name(id: Int): String = propertyKeyNames.get(id)

    final def name(pKind: PropertyKind): String = name(pKind.id)

    /**
     * @note This method is intended to be called by the framework.
     */
    def fallbackProperty[P <: Property](
        ps: PropertyStore,
        fr: FallbackReason,
        e:  Entity,
        pk: PropertyKey[P]
    ): P = {
        fallbackPropertyBasedOnPkId(ps, fr, e, pk.id).asInstanceOf[P]
    }

    private[fpcf] def fallbackPropertyBasedOnPkId(
        ps:   PropertyStore,
        fr:   FallbackReason,
        e:    Entity,
        pkId: Int
    ): Property = {
        fallbackPropertyComputations.get(pkId)(ps, fr, e)
    }

    /**
     * @note This method is intended to be called by the framework.
     */
    def fastTrackProperty[P <: Property](
        ps: PropertyStore,
        e:  Entity,
        pk: PropertyKey[P]
    ): Option[P] = {
        fastTrackPropertyBasedOnPkId(ps, e, pk.id).asInstanceOf[Option[P]]
    }
    private[fpcf] def fastTrackPropertyBasedOnPkId(
        ps:   PropertyStore,
        e:    Entity,
        pkId: Int
    ): Option[Property] = {
        fastTrackPropertyComputations.get(pkId)(ps, e)
    }

    /**
     * @note This method is intended to be called by the framework.
     */
    def resolveCycle[E <: Entity, P <: Property](ps: PropertyStore, eps: EPS[E, P]): P = {
        cycleResolutionStrategies.get(eps.pk.id).asInstanceOf[CycleResolutionStrategy[E, P]](ps, eps)
    }

    /**
     * Returns the id associated with the last created property key.
     * The id associated with the first property kind is `0`;
     * `-1` is returned if no property key is created so far.
     */
    private[fpcf] def maxId: Int = lastKeyId.get

}

/**
 * Specifies the reason why a fallback is used.
 */
sealed trait FallbackReason {
    def propertyIsNotComputedByAnyAnalysis: Boolean
    def propertyIsNotDerivedByPreviouslyExecutedAnalysis: Boolean
}
/**
 * The fallback is used, because the property was queried, but was not explicitly computed in the
 * past, is not computed now and will also not be computed in the future.
 */
case object PropertyIsNotComputedByAnyAnalysis extends FallbackReason {
    def propertyIsNotComputedByAnyAnalysis: Boolean = true
    def propertyIsNotDerivedByPreviouslyExecutedAnalysis: Boolean = false
}

/**
 * The fallback is used, because the property was queried/is required, but the property was
 * not computed for the specific entity though an analysis is scheduled/executed.
 *
 * @note This may happen for properties associated with dead code/code that is no used by the
 *       current project. E.g., the callers property of an unused library method is most
 *       likely not computed. If it is queried, then this is the Property that should be returned.
 */
case object PropertyIsNotDerivedByPreviouslyExecutedAnalysis extends FallbackReason {
    def propertyIsNotComputedByAnyAnalysis: Boolean = false
    def propertyIsNotDerivedByPreviouslyExecutedAnalysis: Boolean = true
}
