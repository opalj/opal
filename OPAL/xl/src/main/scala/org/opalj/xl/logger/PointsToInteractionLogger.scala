/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package logger

object PointsToInteractionLogger {
    val javaToNativeCalls = new scala.collection.mutable.HashMap[String, Array[Long]]()

    val nativeToJavaCalls = new scala.collection.mutable.HashMap[String, Array[Long]]()
}
