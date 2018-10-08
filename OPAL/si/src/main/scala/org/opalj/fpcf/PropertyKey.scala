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

    // TODO additionally pass the cycle to the cycle resolution strategy to enable decisions about the "correct" value
    type CycleResolutionStrategy[E <: Entity, P <: Property] = (PropertyStore, EPS[E, P]) ⇒ P

    private[this] val propertyKeyNames = new AtomicReferenceArray[String](SupportedPropertyKinds)

    /*
     * @note [[PropertyKey]]s of simple properties don't have fallback property computations.
     *       This fact is also used to distinguish these two property kinds.
     */
    private[this] val fallbackPropertyComputations = {
        new AtomicReferenceArray[(PropertyStore, FallbackReason, Entity) ⇒ Property](
            SupportedPropertyKinds
        )
    }

    private[this] val notComputedProperties = {
        new AtomicReferenceArray[Property](SupportedPropertyKinds)
    }

    /*
     * @note [[PropertyKey]]s of simple properties don't have fastrack property computations.
     */
    private[this] val fastTrackPropertyComputations = {
        new AtomicReferenceArray[(PropertyStore, Entity) ⇒ Option[Property]](
            SupportedPropertyKinds
        )
    }

    /*
     * @note [[PropertyKey]]s of simple properties don't have cycle resolution strategies.
     */
    private[this] val cycleResolutionStrategies = {
        new AtomicReferenceArray[CycleResolutionStrategy[_ <: Entity, _ <: Property]](
            SupportedPropertyKinds
        )
    }

    private[this] val lastKeyId = new AtomicInteger(-1)

    private[this] def nextKeyId(): Int = {
        val nextKeyId = this.lastKeyId.incrementAndGet()
        if (nextKeyId >= PropertyKind.SupportedPropertyKinds) {
            throw new IllegalStateException(
                s"maximum number of property keys ($SupportedPropertyKinds) "+
                    "exceeded; increase PropertyKind.SupportedPropertyKinds"
            );
        }
        nextKeyId
    }

    private[this] def setKeyName(keyId: Int, name: String): Unit = {
        propertyKeyNames.set(keyId, name)
        var i = 0
        while (i < keyId) {
            if (propertyKeyNames.get(i) == name)
                throw new IllegalArgumentException(s"the property name $name is already used");
            i += 1
        }
    }

    /**
     * Creates a new [[PropertyKey]] for a simple property.
     *
     * Simple properties are only to be used for properties for which a (meaningful) lower bound
     * does not exists or is very hard to compute and is – in particular – never of interest for
     * clients. Additionally, a basic analysis which derives the property has to exist; if no
     * analysis is scheduled the client would not be able to distinguish between this case where
     * no analysis is run and the case where an analysis does not derive a value for a specific
     * entity (e.g., because the entity is unused).
     *
     * @param notComputedProperty This property is used as the "fallback" when the property is
     *        not computed for a specific entity.
     */
    def forSimpleProperty[P <: Property](
        name:                String,
        notComputedProperty: P
    ): PropertyKey[P] = {
        val thisKeyId = nextKeyId()
        setKeyName(thisKeyId, name)
        notComputedProperties.set(thisKeyId, notComputedProperty)
        new PropertyKey(thisKeyId)
    }

    /**
     * Creates a new [[PropertyKey]] object that is to be shared by all regular properties that
     * belong to the same category.
     *
     * @param name  The unique name associated with the property. To ensure
     *              uniqueness it is recommended to prepend (parts of) the package name of property.
     *              Properties defined by OPAL start with "opalj."
     *
     * @param fallbackPropertyComputation A function that returns the property that will be
     *              associated with those entities for which the property is not explicitly
     *              computed. This is generally the bottom value of the lattice. However, if an
     *              analysis was scheduled, but a property was not computed, a special (alternative)
     *              value can be used. This is in particular relevant for properties which depend
     *              on the reachable code.
     *
     * @param cycleResolutionStrategy The strategy that will be used to resolve unfinished cyclic
     *              computations. In the vast majority of cases it is sufficient to just commit
     *              the given value.
     *
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
        val thisKeyId = nextKeyId()
        setKeyName(thisKeyId, name)

        fallbackPropertyComputations.set(
            thisKeyId,
            fallbackPropertyComputation.asInstanceOf[(PropertyStore, FallbackReason, Entity) ⇒ Property]
        )
        fastTrackPropertyComputations.set(
            thisKeyId,
            fastTrackPropertyComputation.asInstanceOf[(PropertyStore, Entity) ⇒ Option[Property]]
        )
        cycleResolutionStrategies.set(
            thisKeyId,
            cycleResolutionStrategy.asInstanceOf[CycleResolutionStrategy[Entity, Property]]
        )

        new PropertyKey(thisKeyId)
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

    def isPropertyKindForSimpleProperty(pk: PropertyKind): Boolean = {
        fallbackPropertyComputations.get(pk.id) == null
    }

    def isPropertyKeyForSimpleProperty(pk: SomePropertyKey): Boolean = {
        fallbackPropertyComputations.get(pk.id) == null
    }

    def isPropertyKeyForSimplePropertyBasedOnPKId(pkId: Int): Boolean = {
        fallbackPropertyComputations.get(pkId) == null
    }

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
        fallbackPropertyBasedOnPKId(ps, fr, e, pk.id).asInstanceOf[P]
    }

    private[fpcf] def fallbackPropertyBasedOnPKId(
        ps:   PropertyStore,
        fr:   FallbackReason,
        e:    Entity,
        pkId: Int
    ): Property = {
        val fallbackComputation = fallbackPropertyComputations.get(pkId)
        if (fallbackComputation == null)
            throw new IllegalArgumentException(
                "no fallback computation exists (for simple properties of kind): "+name(pkId)
            )
        fallbackComputation(ps, fr, e)
    }

    /**
     * @note This method is intended to be called by the framework.
     */
    def notComputedProperty[P <: Property](pk: PropertyKey[P]): P = {
        notComputedPropertyBasedOnPKId(pk.id).asInstanceOf[P]
    }

    private[fpcf] def notComputedPropertyBasedOnPKId(pkId: Int): Property = {
        val notComputedProperty = notComputedProperties.get(pkId)
        if (notComputedProperty == null)
            throw new IllegalArgumentException(
                "no property exists which models the case that no property is computed: "+name(pkId)
            )
        notComputedProperty
    }

    /**
     * @note This method is intended to be called by the framework.
     */
    def fastTrackProperty[P <: Property](
        ps: PropertyStore,
        e:  Entity,
        pk: PropertyKey[P]
    ): Option[P] = {
        fastTrackPropertyBasedOnPKId(ps, e, pk.id).asInstanceOf[Option[P]]
    }
    private[fpcf] def fastTrackPropertyBasedOnPKId(
        ps:   PropertyStore,
        e:    Entity,
        pkId: Int
    ): Option[Property] = {
        val fastTrackPropertyComputation = fastTrackPropertyComputations.get(pkId)
        if (fastTrackPropertyComputation == null)
            throw new IllegalArgumentException(
                "no fast track computation exists (for simple properties of kind): "+name(pkId)
            )
        fastTrackPropertyComputation(ps, e)
    }

    /**
     * @note This method is intended to be called by the framework.
     */
    def resolveCycle[E <: Entity, P <: Property](ps: PropertyStore, eps: EPS[E, P]): P = {
        val pkId = eps.pk.id
        val cycleResolutionStrategy = cycleResolutionStrategies.get(pkId)
        if (cycleResolutionStrategy == null)
            throw new IllegalArgumentException(
                "no cycle resolution strategy exists (for simple properties of kind): "+name(pkId)
            )
        cycleResolutionStrategy.asInstanceOf[CycleResolutionStrategy[E, P]](ps, eps)
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
