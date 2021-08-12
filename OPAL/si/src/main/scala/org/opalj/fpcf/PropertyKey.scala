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
        new Array[(PropertyStore, FallbackReason, Entity) => Property](SupportedPropertyKinds)
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
     * @note This method is '''not thread-safe''' - the setup of the property store (e.g.,
     *       using the [[org.opalj.br.fpcf.FPCFAnalysesManager]] or an [[AnalysisScenario]] has to
     *       be done by the driver thread and therefore no synchronization is needed.)
     */
    def create[E <: Entity, P <: Property](
        name:                        String,
        fallbackPropertyComputation: FallbackPropertyComputation[E, P]
    ): PropertyKey[P] = {
        val thisKeyId = nextKeyId()
        setKeyName(thisKeyId, name)

        fallbackPropertyComputations(thisKeyId) =
            fallbackPropertyComputation.asInstanceOf[(PropertyStore, FallbackReason, Entity) => Property]

        val pk = new PropertyKey(thisKeyId)
        propertyKeys(thisKeyId) = pk

        pk
    }

    def create[E <: Entity, P <: Property](name: String): PropertyKey[P] = {
        create(name, fallbackPropertyComputation = null)
    }

    def create[E <: Entity, P <: Property](
        name:             String,
        fallbackProperty: P
    ): PropertyKey[P] = {
        val fpc = (_: PropertyStore, _: FallbackReason, _: Entity) => fallbackProperty
        create(name, fpc)
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

    /**
     * Returns the id associated with the last created property key.
     *
     * The id of the first property kind is `0`; `-1` is returned if no property key is created
     * so far.
     */
    private[fpcf] def maxId: Int = lastKeyId.get

}
