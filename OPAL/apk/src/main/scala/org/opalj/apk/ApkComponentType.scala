/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package apk

/**
 * Represents the four types of APK components.
 *
 * @author Nicolas Gross
 */
object ApkComponentType extends Enumeration {
    type ApkComponentType = Value
    val Activity, Service, BroadcastReceiver, ContentProvider = Value
}
