/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.android

import org.opalj.br.ClassFile

/*
 * Classes used for parsing AndroidManifest.xml, including components and intent filters.
 */

case class IntentFilter(actions: Seq[String], categories: Seq[String])

sealed abstract class AndroidComponent {
    val cls: ClassFile
    val intentFilters: Seq[IntentFilter]
}

case class Activity(
    cls:           ClassFile,
    intentFilters: Seq[IntentFilter]
) extends AndroidComponent {
    def isLauncherActivity: Boolean = intentFilters.exists(filter =>
        filter.actions.contains(AndroidManifestKey.ACTION_MAIN)
            && filter.categories.contains(AndroidManifestKey.CATEGORY_LAUNCHER)
    )
}

case class Service(
    cls:           ClassFile,
    intentFilters: Seq[IntentFilter]
) extends AndroidComponent

case class BroadcastReceiver(
    cls:           ClassFile,
    intentFilters: Seq[IntentFilter]
) extends AndroidComponent

case class ContentProvider(
    cls:           ClassFile,
    intentFilters: Seq[IntentFilter]
) extends AndroidComponent

case class AndroidManifest(
    packageName:        String,
    activities:         Seq[Activity],
    services:           Seq[Service],
    broadcastReceivers: Seq[BroadcastReceiver],
    contentProviders:   Seq[ContentProvider]
)
