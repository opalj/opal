/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.solver

import scala.annotation.tailrec

import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.Entity
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.problem.FlowRecorderModes
import org.opalj.ide.problem.FlowRecorderModes.FlowRecorderMode
import org.opalj.ide.problem.FlowRecordingIDEProblem
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEProblem
import org.opalj.ide.problem.IDEValue

/**
 * Wrapper class for a normal IDE analysis for debugging purposes. Records the flow paths the IDE solver takes for a
 * given base problem as graph and writes it to a file in DOT format.
 * DOT files can either be viewed with a suitable local program or online e.g. at
 * [[https://dreampuf.github.io/GraphvizOnline]].
 * @param baseProblem the base problem that defines the flows and edge functions that should be analyzed
 * @param path the location to write the resulting DOT file (either a file ending with `.dot` or a directory)
 * @param uniqueFlowsOnly whether to drop or to keep duplicated flows
 * @param recordEdgeFunctions whether to record edge functions too or just stick with the flow
 */
class FlowRecordingIDEAnalysis[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity](
        project:                 SomeProject,
        baseProblem:             IDEProblem[Fact, Value, Statement, Callable],
        propertyMetaInformation: IDEPropertyMetaInformation[Statement, Fact, Value],
        path:                    Option[Path]     = None,
        recorderMode:            FlowRecorderMode = FlowRecorderModes.NODE_AS_STMT_AND_FACT,
        uniqueFlowsOnly:         Boolean          = true,
        recordEdgeFunctions:     Boolean          = true
) extends IDEAnalysis(
        project,
        new FlowRecordingIDEProblem(baseProblem, recorderMode, uniqueFlowsOnly, recordEdgeFunctions),
        propertyMetaInformation
    ) {
    /**
     * The passed IDE problem
     */
    val flowRecordingProblem: FlowRecordingIDEProblem[Fact, Value, Statement, Callable] =
        problem.asInstanceOf[FlowRecordingIDEProblem[Fact, Value, Statement, Callable]]

    /**
     * Get the file to write the graph to. If [[path]] references a file then this method will return [[path]]. If
     * [[path]] references a directory, a new filename is created (s.t. no file gets overwritten).
     */
    private def getFile: File = {
        lazy val className = {
            val classFQN = project.projectClassFilesWithSources.head._1.thisType.fqn
            classFQN.substring(classFQN.lastIndexOf('/') + 1)
        }

        @tailrec
        def getNextFreeWithBasePath(basePath: Path, index: Int = 1): Path = {
            val pathToCheck =
                if (index == 0) {
                    basePath.resolve(s"$className-flow-recording.dot")
                } else {
                    basePath.resolve(s"$className-flow-recording-$index.dot")
                }

            if (Files.exists(pathToCheck)) {
                getNextFreeWithBasePath(basePath, index + 1)
            } else {
                pathToCheck
            }
        }

        val completePath = path match {
            case Some(p) =>
                if (p.toUri.getPath.endsWith(".dot")) {
                    p
                } else {
                    getNextFreeWithBasePath(p)
                }
            case None => getNextFreeWithBasePath(Paths.get("."))
        }

        completePath.toFile
    }

    def startRecording(): Unit = {
        val writer = new FileWriter(getFile)

        flowRecordingProblem.startRecording(writer)
    }

    def stopRecording(): Unit = {
        val writer = flowRecordingProblem.stopRecording()

        writer.close()
    }
}
