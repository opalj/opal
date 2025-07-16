/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac2bc

import java.io.File

class SingleClassFileTACtoBCTest extends TACtoBCTest {

    val dirName: String = "single"
    val packageName: String = "org/opalj/tac2bc/single"

    describe("TACtoBC Single Class File Generation") {
        executeTest("original")
    }

    def getSourceDir(originalFileName: String): String = ""

    def description(originalFileName: String, testFileName: String): String = {
        s"should compile and generate class file for $testFileName, create the TAC representation of it, convert it back to bytecode, generate a new .class file and compare the output of both original and generated .class files"
    }

    def getTestFiles(file: File, fileName: String): IterableOnce[(String, String)] = {
        if (fileName.endsWith(".java"))
            Iterator((fileName, fileName))
        else Iterator.empty
    }
}
