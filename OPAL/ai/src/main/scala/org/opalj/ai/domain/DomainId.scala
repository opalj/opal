/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Common super trait of all domains have an id to identify them.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait DomainId {

    /**
     * The type which is used to identify the domain or the domain's context.
     * E.g., if a new domain is created to analyze a called method it may be
     * associated with the instruction caused its creation. It can, however,
     * also just identify the method (by means of, e.g., the pair `(classFile,method)`
     * that it is used for.
     */
    type Id

    /**
     * Returns the value that identifies this domain (usually it is loosely
     * connected to the analyzed method).
     *
     * This value may subsequently be used to identify/track object instances but – if
     * so – this happens at the sole responsibility of the domain. OPAL-AI does
     * not require any kind of tracking.
     */
    def id: Id

}
