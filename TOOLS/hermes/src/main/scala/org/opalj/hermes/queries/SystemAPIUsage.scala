/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.hermes
package queries

import org.opalj.br.ObjectType
import org.opalj.hermes.queries.util.APIFeature
import org.opalj.hermes.queries.util.APIFeatureGroup
import org.opalj.hermes.queries.util.APIFeatureQuery
import org.opalj.hermes.queries.util.InstanceAPIMethod
import org.opalj.hermes.queries.util.StaticAPIMethod

/**
 * Counts the calls to specific JVM are system features.
 *
 * @author Michael Reif
 */
class SystemAPIUsage(implicit hermes: HermesConfig) extends APIFeatureQuery {

    override val apiFeatures: List[APIFeature] = {

        object Sound {
            val Clip = ObjectType("javax/sound/sampled/Clip")
            val DataLine = ObjectType("javax/sound/sampled/DataLine")
            val TargetDataLine = ObjectType("javax/sound/sampled/TargetDataLine")
            val SourceDataLine = ObjectType("javax/sound/sampled/SourceDataLine")
            val MediaPlayer = ObjectType("javafx/scene/media/MediaPlayer")
        }

        object Network {
            val Socket = ObjectType("java/net/Socket")
            val SSLSocket = ObjectType("javax/net/ssl/SSLSocket")
            val ServerSocket = ObjectType("java/net/ServerSocket")
            val SSLServerSocket = ObjectType("javax/net/ssl/SSLServerSocket")
            val DatagramSocket = ObjectType("javax/net/DatagramSocket")
            val MulticastSocket = ObjectType("javax/net/MulticastSocket")

            val DatagramPacket = ObjectType("java/net/DatagramPacket")
            val InetAddress = ObjectType("java/net/InetAddress")

            val URL = ObjectType("java/net/URL")
            val URI = ObjectType("java/net/URI")
            val URLConnection = ObjectType("java/net/URLConnection")
        }

        val constructor = "<init>"

        val Runtime = ObjectType("java/lang/Runtime")
        val System = ObjectType("java/lang/System")
        val ProcessBuilder = ObjectType("java/lang/ProcessBuilder")

        List(

            // PROCESS

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Runtime, "exec"),
                    InstanceAPIMethod(ProcessBuilder, "start")
                ),
                "Process"
            ),

            // JVM EXIT

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Runtime, "exit"),
                    InstanceAPIMethod(Runtime, "halt"),
                    StaticAPIMethod(System, "exit")
                ),
                "JVM exit"
            ),

            // NATIVE LIBRARIES

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Runtime, "load"),
                    InstanceAPIMethod(Runtime, "loadLibrary"),
                    StaticAPIMethod(System, "load"),
                    StaticAPIMethod(System, "loadLibrary")
                ),
                "Native Libraries"
            ),

            // SECURITY_MANAGER

            StaticAPIMethod(System, "getSecurityManager"),
            StaticAPIMethod(System, "setSecurityManager"),

            // ENV

            APIFeatureGroup(
                List(
                    StaticAPIMethod(System, "getenv")
                ),
                "Environment"
            ),

            // SOUND

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Sound.Clip, "start"),
                    InstanceAPIMethod(Sound.DataLine, "start"),
                    InstanceAPIMethod(Sound.TargetDataLine, "start"),
                    InstanceAPIMethod(Sound.SourceDataLine, "start"),
                    InstanceAPIMethod(Sound.MediaPlayer, "play")
                ), "Sound"
            ),

            // NETWORK

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Network.Socket, constructor),
                    InstanceAPIMethod(Network.ServerSocket, constructor),
                    InstanceAPIMethod(Network.DatagramSocket, constructor),
                    InstanceAPIMethod(Network.DatagramPacket, constructor),
                    InstanceAPIMethod(Network.MulticastSocket, constructor),
                    InstanceAPIMethod(Network.SSLSocket, constructor),
                    InstanceAPIMethod(Network.SSLServerSocket, constructor),
                    InstanceAPIMethod(Network.InetAddress, constructor)
                ),
                "Network sockets"
            )
        )
    }
}
