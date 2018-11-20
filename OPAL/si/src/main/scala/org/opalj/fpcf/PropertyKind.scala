/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * Identifies the kind of a property.
 *
 * Generally, we distinguish between regular properties and simple properties. The latter
 * are generally only to be used if lower bounds cannot be computed or a very extensive and
 * are never of interest to any potential client. E.g., in case of an IFDS analysis,
 * computing the lower bound is not meaningful; in case of a call graph analysis, the lower
 * bound is usually either prohibitively expensive or is not usefull to any analysis.
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
