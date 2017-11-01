package org.opalj.br

import java.io.File

import org.opalj.br.analyses.SomeProject

object ProjectSerializer {
    // Add main, see HermesCli for arg parsing

    def serialize(p: SomeProject, targetFolder: File /* default temp folder */ ) = {
        // TODO : Overall classfile serialize them to disk
        // BytecodeCreator Assembler.apply()
        // Write "wrote all files to ..."

        // TODO: Create small project that uses INVOKEDYNAMIC resolution -> execute project and
        // test if it works
        //  Into validate test/scala|java/br/fixtures

        // Java Call Graph for Java project resolution test
        // Add to test fixtures
    }

    def classToByte(c: ClassFile): Array[Byte] = {

    }
}
