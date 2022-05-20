/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import java.util.zip.ZipFile

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

import org.opalj.bi.TestResources.allManagedBITestJARs
import org.opalj.br.reader.Java8Framework.{ClassFile => ClassFileReader}

/**
 * Tests whether all class files contained in the "test/classfiles" directory
 * can be processed by the `DependencyExtractor` without failure.
 * The results themselves will not be verified in these test cases.
 *
 * @author Thomas Schlosser
 * @author Michael Eichberg
 * @author Marco Jacobasch
 */
@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class ExtractDependenciesFromClassFilesTest extends AnyFlatSpec with Matchers {

    val dependencyExtractor = new DependencyExtractor(DependencyProcessorAdapter)

    for (file <- allManagedBITestJARs()) {
        val zipfile = new ZipFile(file)
        val jarName = zipfile.getName
        val zipentries = (zipfile).entries
        while (zipentries.hasMoreElements) {
            val zipentry = zipentries.nextElement
            if (!zipentry.isDirectory && zipentry.getName.endsWith(".class")) {
                val className = zipentry.getName
                it should (s"be able to extract dependencies of $className in $jarName") in {
                    val classFiles = ClassFileReader(() => zipfile.getInputStream(zipentry))
                    classFiles foreach (classFile => dependencyExtractor.process(classFile))
                }
            }
        }
    }
}
