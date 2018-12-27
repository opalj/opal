/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.util.concurrent.atomic.AtomicInteger

import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds

/**
 * A value object that identifies a specific kind of properties. Every entity in
 * the [[PropertyStore]] must be associated with at most one property per property kind/key.
 *
 * To create a property key use one of the companion object's [[PropertyKey$]].`create` method.
 *
 * When a phase finishes all values are committed using the current upper bound unless a property
 * only has a lower bound.
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

    private[this] val propertyKeys = new Array[SomePropertyKey](SupportedPropertyKinds)

    private[this] val propertyKeyNames = new Array[String](SupportedPropertyKinds)

    /*
     * @note [[PropertyKey]]s of simple properties don't have fallback property computations.
     *       This fact is also used to distinguish these two property kinds.
     */
    private[this] val fallbackPropertyComputations = {
        new Array[(PropertyStore, FallbackReason, Entity) ⇒ Property](SupportedPropertyKinds)
    }

    /*
     * @note [[PropertyKey]]s of simple properties don't have fastrack property computations.
     */
    private[this] val fastTrackPropertyComputations = {
        new Array[(PropertyStore, Entity) ⇒ Option[Property]](SupportedPropertyKinds)
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
        propertyKeyNames(keyId) = name
        var i = 0
        while (i < keyId) {
            if (propertyKeyNames(i) == name)
                throw new IllegalArgumentException(s"the property name $name is already used");
            i += 1
        }
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
     * @param fastTrackPropertyComputation (Optionally) called by the property store if the property
     *              is queried for the first time (see `PropertyStore.setupPhase`).
     *              This method is expected to either provide
     *              a precise analysis very fast or to not provide a result at all.
     *              I.e., it is expected to derive only those properties that can trivially be
     *              derived precisely. In general, the computation should succeed in at most
     *              a few hundred steps. The computation must NOT query any properties and
     *              must be idempotent.
     *
     * @note This method is '''not thread-safe''' - the setup of the property store (e.g.,
     *       using the [[org.opalj.fpcf.FPCFAnalysesManager]] or an [[AnalysisScenario]] has to
     *       be done by the driver thread and therefore no synchronization is needed.)
     */
    def create[E <: Entity, P <: Property](
        name:                         String,
        fallbackPropertyComputation:  FallbackPropertyComputation[E, P],
        fastTrackPropertyComputation: (PropertyStore, E) ⇒ Option[P]
    ): PropertyKey[P] = {
        val thisKeyId = nextKeyId()
        setKeyName(thisKeyId, name)

        fallbackPropertyComputations(thisKeyId) =
            fallbackPropertyComputation.asInstanceOf[(PropertyStore, FallbackReason, Entity) ⇒ Property]

        fastTrackPropertyComputations(thisKeyId) =
            fastTrackPropertyComputation.asInstanceOf[(PropertyStore, Entity) ⇒ Option[Property]]

        val pk = new PropertyKey(thisKeyId)
        propertyKeys(thisKeyId) = pk

        pk
    }

    def create[E <: Entity, P <: Property](name: String): PropertyKey[P] = {
        create(name, fallbackPropertyComputation = null, fastTrackPropertyComputation = null)
    }

    def create[E <: Entity, P <: Property](
        name:             String,
        fallbackProperty: P
    ): PropertyKey[P] = {
        val fpc = (_: PropertyStore, _: FallbackReason, _: Entity) ⇒ fallbackProperty
        create(name, fpc, fastTrackPropertyComputation = null)
    }

    def create[E <: Entity, P <: Property](
        name:                        String,
        fallbackPropertyComputation: FallbackPropertyComputation[E, P]
    ): PropertyKey[P] = {
        create(name, fallbackPropertyComputation, fastTrackPropertyComputation = null)
    }

    def create[E <: Entity, P <: Property](
        name:                         String,
        fallbackProperty:             P,
        fastTrackPropertyComputation: (PropertyStore, E) ⇒ Option[P]
    ): PropertyKey[P] = {
        val fpc = (_: PropertyStore, _: FallbackReason, _: Entity) ⇒ fallbackProperty
        create(name, fpc, fastTrackPropertyComputation)
    }

    //
    // Query the core properties of each property kind
    // ===============================================
    //

    def key(id: Int): SomePropertyKey = propertyKeys(id)

    /**
     * Returns the unique name of the kind of properties associated with the given key id.
     */
    def name(id: Int): String = propertyKeyNames(id)

    final def name(pKind: PropertyKind): String = name(pKind.id)

    final def name(eOptionP: SomeEOptionP): String = name(eOptionP.pk.id)

    final def hasFallback(propertyKind: PropertyKind): Boolean = {
        hasFallbackBasedOnPKId(propertyKind.id)
    }

    final def hasFallbackBasedOnPKId(pkId: Int): Boolean = {
        fallbackPropertyComputations(pkId) != null
    }

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
        val fallbackComputation = fallbackPropertyComputations(pkId)
        if (fallbackComputation == null)
            throw new IllegalArgumentException("no fallback computation exists: "+name(pkId))
        fallbackComputation(ps, fr, e)
    }

    final def hasFastTrackProperty(propertyKind: PropertyKind): Boolean = {
        hasFallbackBasedOnPKId(propertyKind.id)
    }

    final def hasFastTrackPropertyBasedOnPKId(pkId: Int): Boolean = {
        fastTrackPropertyComputations(pkId) != null
    }

    /**
     * @note This method is intended to be called by the framework. It is defined iff a
     *       fast track property computation was registered.
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
        val fastTrackPropertyComputation = fastTrackPropertyComputations(pkId)
        if (fastTrackPropertyComputation == null)
            throw new IllegalArgumentException("no fast track computation exists: "+name(pkId))
        fastTrackPropertyComputation(ps, e)
    }

    private[fpcf] def computeFastTrackProperty[P <: Property](
        ps: PropertyStore,
        e:  Entity,
        pk: PropertyKey[P]
    ): Option[P] = {
        computeFastTrackPropertyBasedOnPKId(ps, e, pk.id)
    }
    private[fpcf] def computeFastTrackPropertyBasedOnPKId[P <: Property](
        ps:   PropertyStore,
        e:    Entity,
        pkId: Int
    ): Option[P] = {
        val fastTrackPropertyComputation = fastTrackPropertyComputations(pkId)
        if (fastTrackPropertyComputation == null)
            None
        else
            fastTrackPropertyComputation(ps, e).asInstanceOf[Option[P]]
    }

    /**
     * Returns the id associated with the last created property key.
     *
     * The id of the first property kind is `0`; `-1` is returned if no property key is created
     * so far.
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
