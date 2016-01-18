package org.opalj.fpcf.analysis.methods

import org.opalj.br.Method
import org.opalj.fpcf.{PropertyKey, PropertyMetaInformation, Property}

/**
 * This is the common super trait for the call-by-signature information which is necessary
 * to build library call graphs.
 *
 * @author Michael Reif
 */
trait CallBySignatureTargets extends Property {

    final def key = CallBySignatureTargets.key
}

object CallBySignatureTargets extends PropertyMetaInformation {

    // unsave default while analyzing software libraries in isolation
    final val key = PropertyKey.create("CallBySignatureTargets", NoResolution)
}

case class CbsTargets(cbsTargets: Set[Method]) extends CallBySignatureTargets {
    final val isRefineable: Boolean = true
}

case object NoResolution extends CallBySignatureTargets {
    final val isRefineable: Boolean = false
}