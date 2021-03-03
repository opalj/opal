/* BSD 2-Clause License - see OPAL/LICENSE for details. */
import sbt._

import scala.xml.NodeSeq

/**
 * Definition of the tasks and settings to support exporting OPAL to Maven.
 *
 * @author Michael Eichberg
 * @author Simon Leischnig
 */
object MavenPublishing {

  // method populates sbt.publishTo = SettingKey[Option[Resolver]]
  def publishTo(isSnapshot: Boolean): Option[Resolver] = {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }

  def pomNodeSeq(): NodeSeq = {
    <scm>
        <url>git@github.com:stg-tud/opal.git</url>
        <connection>scm:git:git@github.com:stg-tud/opal.git</connection>
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
        <developer>
            <id>errt</id>
            <name>Dominik Helm</name>
        </developer>
        <developer>
          <id>kuebler</id>
          <name>Florian Kübler</name>
        </developer>
        <developer>
          <id>roth</id>
          <name>Tobias Roth</name>
        </developer>
    </developers>
  }

}
