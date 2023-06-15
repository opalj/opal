/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package connector

import dk.brics.tajs.analysis.axa.connector.IConnector

import java.io.File

trait AnalysisInterface[State, K, V] {
    def analyze(files: List[File], propertyChanges: scala.collection.mutable.Map[K, V], connector: IConnector): State
}