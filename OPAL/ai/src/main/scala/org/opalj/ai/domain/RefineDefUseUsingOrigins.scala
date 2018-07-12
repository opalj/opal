/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Collects/refines the abstract interpretation time definition/use information using the domain
 * values' origin information if available.
 *
 * @note ReturnAddressValues are ignored by this domain; however, the parent domain
 *       [[RecordDefUse]] has appropriate handling.
 *
 * @author Michael Eichberg
 */
trait RefineDefUseUsingOrigins extends RecordDefUse {
    defUseDomain: Domain with TheCode with TheClassHierarchy with Origin ⇒

    override protected[this] def originsOf(domainValue: DomainValue): Option[ValueOrigins] = {
        domainValue match {
            case vo: ValueWithOriginInformation ⇒ Some(vo.origins)
            case _                              ⇒ super.originsOf(domainValue)
        }
    }
}
