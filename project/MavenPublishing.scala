/* BSD 2-Clause License - see OPAL/LICENSE for details. */
import scala.xml.NodeSeq

/**
 * Definition of the tasks and settings to support exporting OPAL to Maven.
 *
 * @author Michael Eichberg
 * @author Simon Leischnig
 */
object MavenPublishing {

    def pomNodeSeq(): NodeSeq = {
    <scm>
        <url>https://github.com/opalj/opal.git</url>
        <connection>scm:git:git@github.com:opalj/opal.git</connection>
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
        <developer>
            <id>duesing</id>
            <name>Johannes Düsing</name>
        </developer>
    </developers>
    }

}
