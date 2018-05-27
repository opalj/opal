/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj.hermes
package queries

import org.opalj.collection.immutable.Chain
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

    override val apiFeatures: Chain[APIFeature] = {

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

        Chain(

            // PROCESS

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Runtime, "exec"),
                    InstanceAPIMethod(ProcessBuilder, "start")
                ),
                "Process"
            ),

            // JVM EXIT

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Runtime, "exit"),
                    InstanceAPIMethod(Runtime, "halt"),
                    StaticAPIMethod(System, "exit")
                ),
                "JVM exit"
            ),

            // NATIVE LIBRARIES

            APIFeatureGroup(
                Chain(
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
                Chain(
                    StaticAPIMethod(System, "getenv")
                ),
                "Environment"
            ),

            // SOUND

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Sound.Clip, "start"),
                    InstanceAPIMethod(Sound.DataLine, "start"),
                    InstanceAPIMethod(Sound.TargetDataLine, "start"),
                    InstanceAPIMethod(Sound.SourceDataLine, "start"),
                    InstanceAPIMethod(Sound.MediaPlayer, "play")
                ), "Sound"
            ),

            // NETWORK

            APIFeatureGroup(
                Chain(
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
