/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * Identifies the kind of a property.
 */
trait PropertyKind extends Any /* we now have a universal trait */ {

    /**
     * The id uniquely identifies this property's category. All property objects of the
     * same kind have to use the same id which is guaranteed since they share the same
     * `PropertyKey`
     */
    def id: Int
}

object PropertyKind {

    /**
     * The maximum number of Property Kinds that is (currently!) supported. Increasing this
     * number is necessary iff a related exception is thrown.
     */
    private[fpcf] final val SupportedPropertyKinds /*: Int*/ = 72

}
