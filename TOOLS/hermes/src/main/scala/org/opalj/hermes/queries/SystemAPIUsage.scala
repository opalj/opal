/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br.ClassType
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
            val Clip = ClassType("javax/sound/sampled/Clip")
            val DataLine = ClassType("javax/sound/sampled/DataLine")
            val TargetDataLine = ClassType("javax/sound/sampled/TargetDataLine")
            val SourceDataLine = ClassType("javax/sound/sampled/SourceDataLine")
            val MediaPlayer = ClassType("javafx/scene/media/MediaPlayer")
        }

        object Network {
            val Socket = ClassType("java/net/Socket")
            val SSLSocket = ClassType("javax/net/ssl/SSLSocket")
            val ServerSocket = ClassType("java/net/ServerSocket")
            val SSLServerSocket = ClassType("javax/net/ssl/SSLServerSocket")
            val DatagramSocket = ClassType("javax/net/DatagramSocket")
            val MulticastSocket = ClassType("javax/net/MulticastSocket")

            val DatagramPacket = ClassType("java/net/DatagramPacket")
            val InetAddress = ClassType("java/net/InetAddress")

            val URL = ClassType("java/net/URL")
            val URI = ClassType("java/net/URI")
            val URLConnection = ClassType("java/net/URLConnection")
        }

        val constructor = "<init>"

        val Runtime = ClassType("java/lang/Runtime")
        val System = ClassType("java/lang/System")
        val ProcessBuilder = ClassType("java/lang/ProcessBuilder")

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
                ),
                "Sound"
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
