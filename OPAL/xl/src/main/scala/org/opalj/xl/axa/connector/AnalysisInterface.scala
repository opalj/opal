/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package connector

import dk.brics.tajs.analysis.axa.connector.IConnector

import java.io.File

trait AnalysisInterface[State, K, V] {
    def analyze(
        file:            File,
        propertyChanges: scala.collection.mutable.Map[K, V],
        connector:       IConnector
    ): State

    def resume(file: File, propertyChanges: scala.collection.mutable.Map[K, V]): State

}