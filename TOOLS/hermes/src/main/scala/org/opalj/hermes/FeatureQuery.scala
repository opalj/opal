/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import java.net.URL

import scala.io.Source
import scala.io.Codec

import com.github.rjeschke.txtmark.Processor

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.SimpleLongProperty

import org.opalj.io.processSource
import org.opalj.br.analyses.Project

/**
 * Extracts a feature/a set of closely related features of a given project.
 *
 * @author Michael Eichberg
 */
abstract class FeatureQuery(implicit hermes: HermesConfig) {

    /**
     * Queries should regularly check if they are interrupted using this method.
     */
    final def isInterrupted(): Boolean = Thread.currentThread().isInterrupted()

    // ================================ ABSTRACT FUNCTIONALITY ================================

    /**
     * The unique ids of the extracted features.
     */
    def featureIDs: Seq[String]

    /**
     * The function which analyzes the project and extracts the feature information.
     *
     * @param  project A representation of the project. To speed up queries, intermediate
     *         information that may also be required by other queries can/should be stored in the
     *         project using the [[org.opalj.fpcf.PropertyStore]] or using a
     *         [[org.opalj.br.analyses.ProjectInformationKey]].
     * @param  rawClassFiles A direct 1:1 representation of the class files. This makes it possible
     *         to write queries that need to get an understanding of an unprocessed class file; e.g.
     *         that need to analyze the constant pool in detail.
     * @note   '''Every query should regularly check that its thread is not interrupted
     *         using `isInterrupted`'''.
     */
    def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IterableOnce[Feature[S]]

    // =================================== DEFAULT FUNCTIONALITY ===================================
    // ==================================== (can be overridden) ====================================

    /**
     * A short descriptive name; by default the simple name of `this` class.
     */
    val id: String = {
        val simpleClassName = this.getClass.getSimpleName
        val dollarPosition = simpleClassName.indexOf('$')
        if (dollarPosition == -1)
            simpleClassName
        else
            simpleClassName.substring(0, dollarPosition)
    }

    /**
     * Returns an explanation of the feature (group) using Markdown as its formatting language.
     *
     * By default the name of the class is used to lookup the resource "className.markdown"
     * which is expected to be found along the extractor.
     */
    protected def mdDescription: String = {
        var descriptionResourceURL = this.getClass.getResource(s"$id.markdown")
        if (descriptionResourceURL == null)
            descriptionResourceURL = this.getClass.getResource(s"$id.md")
        try {
            processSource(Source.fromURL(descriptionResourceURL)(Codec.UTF8)) { _.mkString }
        } catch {
            case t: Throwable => s"not available: $descriptionResourceURL; ${t.getMessage}"
        }
    }

    /**
     * Returns an HTML description of this feature query that is targeted at end users; by default
     * it calls `mdDescription` to try to find a markdown document that describes this feature and
     * then uses TxtMark to convert the document. If a document is returned the web engine's
     * user style sheet is set to [[org.opalj.hermes.FeatureQueries.MDCSS]]; in case of an URL no
     * stylesheet is set.
     *
     * @return An HTML document/a link to an HTML document that describes this query.
     */
    val htmlDescription: Either[String, URL] = Left(Processor.process(mdDescription))

    // =============================== HERMES INTERNAL FUNCTIONALITY ===============================

    /**
     * The time it took to evaluate the query across all projects in nanoseconds.
     */
    private[hermes] val accumulatedAnalysisTime: LongProperty = new SimpleLongProperty()

    private[hermes] def createInitialFeatures[S]: Seq[ObjectProperty[Feature[S]]] = {
        featureIDs.map(fid => new SimpleObjectProperty(Feature[S](fid)))
    }

}

abstract class DefaultFeatureQuery(
        implicit
        hermes: HermesConfig
) extends FeatureQuery {

    def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]]

    final def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IterableOnce[Feature[S]] = {
        val locations = evaluate(projectConfiguration, project, rawClassFiles)
        for { (featureID, featureIDIndex) <- featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, locations(featureIDIndex))
        }
    }
}

abstract class DefaultGroupedFeaturesQuery(
        implicit
        hermes: HermesConfig
) extends DefaultFeatureQuery {

    def groupedFeatureIDs: Seq[Seq[String]]

    def evaluateFeatureGroups[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IndexedSeq[IterableOnce[LocationsContainer[S]]]

    final def featureIDs: Seq[String] = groupedFeatureIDs.flatten

    final override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        evaluateFeatureGroups(projectConfiguration, project, rawClassFiles).flatten
    }

}

/**
 * Common constants related to feature queries.
 *
 * @author Michael Eichberg
 */
object FeatureQueries {

    /**
     * The URL of the CSS file which used to style the HTML document.
     */
    final val MDCSS: URL = this.getClass.getResource("Queries.css")

}
