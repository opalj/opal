/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l2

import java.net.URL

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * This system test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of all methods using the l2.DefaultDomain. It basically
 * tests if we can load and process a large number of different classes without exceptions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultDomainTest extends DomainTestInfrastructure("l2.DefaultDomain") {

    type AnalyzedDomain = l2.DefaultDomain[URL]

    def Domain(project: Project[URL], method: Method): AnalyzedDomain = {
        new l2.DefaultDomain(project, method)
    }

}
