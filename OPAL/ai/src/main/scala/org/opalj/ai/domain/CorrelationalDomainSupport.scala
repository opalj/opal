/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Provides basic support for tracking the correlation between domain values stored in
 * different registers/in different stack slots.
 *
 * @author Michael Eichberg
 */
trait CorrelationalDomainSupport
    extends JoinStabilization
    with IdentityBasedCorrelationChangeDetection

