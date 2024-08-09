/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package logger

import scala.collection.mutable.HashMap
import scala.collection.mutable.Set

import org.opalj.br.Field

object PointsToInteractionLogger {
    val javaToNativeCalls = new scala.collection.mutable.HashMap[String, Array[Long]]()

    val nativeToJavaCalls = new scala.collection.mutable.HashMap[String, Array[Long]]()

    val nativeToJavaFieldWrites = new HashMap[Field, Array[Long]]()

    val nativeToJavaFieldReads = Set.empty[Field]
}
