package org.opalj
package tac2bc

import java.io.File
import java.nio.file.Paths

import org.opalj.bi.TestResources

class SingleClassFileTACtoBCTest extends TACtoBCTest {

    val dirName: String = "single"

    describe("TACtoBC Single Class File Generation") {
        executeTest("original")
    }

    def getSourceDir(originalFileName: String): String = ""

    def description(originalFileName: String, testFileName: String): String = {
        s"should compile and generate class file for $testFileName, create the TAC representation of it, convert it back to bytecode, generate a new .class file and compare the output of both original and generated .class files"
    }

    def getTestFiles(file: File, fileName: String): IterableOnce[(String, String)] = {
        if(fileName.endsWith(".java"))
            Iterator((fileName, fileName))
        else Iterator.empty
    }
}
