/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac2bc

import java.io.File
import java.nio.file.Path

class MutatedClassFileTACtoBCTest extends TACtoBCTest {

    val dirName: String = "mutation"

    describe("TACtoBC Mutation Testing") {
        executeTest("mutated")
    }

    def getSourceDir(originalFileName: String): String = s"/${originalFileName.replace(".java", "")}"

    def description(originalFileName: String, testFileName: String): String = {
        s"should compile $originalFileName, generate TAC, convert back to bytecode," +
            s" and compare with the mutated class file $testFileName"
    }

    def getTestFiles(file: File, fileName: String): IterableOnce[(String, String)] = {
        if (file.isDirectory) {
            for {
                subFile <- file.listFiles()
                subFileName = subFile.getName
                if subFileName.endsWith(".java") && subFileName.startsWith(s"${fileName}_mutation")
            } yield (subFileName, s"$fileName.java")
        } else Iterator.empty
    }

    override def extraFilesToLoad(
        testClassFileName: String,
        testInputDir:      String
    ): List[Path] = {
        val baseFileName = testClassFileName.replace(".class", "$")
        val directory = new File(testInputDir)

        val extraFiles = if (directory.exists && directory.isDirectory) {
            directory.listFiles((_, name) => name.startsWith(baseFileName))
        } else {
            Array.empty
        }

        extraFiles.iterator.map(file => file.toPath).toList
    }
}
