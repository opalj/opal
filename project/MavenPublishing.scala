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
import sbt._

import scala.xml.NodeSeq

/**
 * Definiton of the tasks and settings to support exporting OPAL to Maven.
 *
 * @author Michael Eichberg
 * @author Simon Leischnig
 */
object MavenPublishing {

    // method populates sbt.publishTo = SettingKey[Option[Resolver]]
    def publishTo(isSnapshot: Boolean): Option[Resolver] = {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot)
            Some("snapshots" at nexus+"content/repositories/snapshots")
        else
            Some("releases" at nexus+"service/local/staging/deploy/maven2")
    }

    def pomNodeSeq(): NodeSeq = {
        <scm>
            <url>git@bitbucket.org:delors/opal.git</url>
            <connection>scm:git:git@bitbucket.org:delors/opal.git</connection>
        </scm>
        <developers>
            <developer>
                <id>eichberg</id>
                <name>Michael Eichberg</name>
                <url>http://www.michael-eichberg.de</url>
            </developer>
            <developer>
                <id>reif</id>
                <name>Michael Reif</name>
            </developer>
        </developers>
    }

}
