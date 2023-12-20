/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package apk

import scala.jdk.CollectionConverters.ListHasAsScala

import org.opalj.apk.ApkComponentType.ApkComponentType

import com.typesafe.config.Config

/**
 * Component of an APK. Each component is a potential entry point. A component is either an
 * Activity, Service, Broadcast Receiver or Content Provider. Parsed from AndroidManifest.xml.
 *
 * The entry point functions were collected up to API level 33 (Android 13). They also include
 * all deprecated functions.
 *
 * @param componentType the type of the component.
 * @param clazz the class name of the component.
 * @param intentActions list of intent actions that trigger this component / entry point.
 * @param intentCategories list of intent categories that trigger this component / entry point.
 *
 * @author Nicolas Gross
 */
class ApkComponent(
        val componentType:    ApkComponentType,
        val clazz:            String,
        val intentActions:    Seq[String],
        val intentCategories: Seq[String]
)(implicit config: Config) {

    private val ActivityEntryPoints = Seq.from(
        config.getStringList(ConfigKeyPrefix + "APKComponent.ActivityEntryPoints").asScala
    )

    private val ServiceEntryPoints = Seq.from(
        config.getStringList(ConfigKeyPrefix + "APKComponent.ServiceEntryPoints").asScala
    )

    private val ReceiverEntryPoints = Seq.from(
        config.getStringList(ConfigKeyPrefix + "APKComponent.ReceiverEntryPoints").asScala
    )

    private val ProviderEntryPoints = Seq.from(
        config.getStringList(ConfigKeyPrefix + "APKComponent.ProviderEntryPoints").asScala
    )

    /**
     * Returns the list of functions that might be called as entry points for this component.
     */
    def entryFunctions(): Seq[String] = {
        componentType match {
            case ApkComponentType.Activity          => ActivityEntryPoints
            case ApkComponentType.Service           => ServiceEntryPoints
            case ApkComponentType.BroadcastReceiver => ReceiverEntryPoints
            case ApkComponentType.ContentProvider   => ProviderEntryPoints
        }
    }

    override def toString: String = {
        val ls = System.lineSeparator()
        s"$clazz: $componentType$ls\tactions: $intentActions$ls\tcategories: $intentCategories$ls"
    }
}
