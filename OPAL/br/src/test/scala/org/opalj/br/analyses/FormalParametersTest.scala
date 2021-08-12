/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.opalj.br.TestSupport.biProject

/**
 * Tests the `FormalParameters`.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class FormalParametersTest extends AnyFlatSpec with Matchers {

    //
    //
    // Verify
    //
    //

    behavior of "the FormalParametersKey"

    it should ("add for each method parameter a formal parameter") in {
        val methodsProject = biProject("methods.jar")
        val declaredMethods = methodsProject.get(DeclaredMethodsKey)

        val fps = methodsProject.get(VirtualFormalParametersKey)
        methodsProject.allMethods foreach { m =>
            val dm = declaredMethods(m)
            assert(m.isStatic || fps(dm)(0) != null)
            assert(fps(dm).size >= m.descriptor.parametersCount + (if (m.isStatic) 0 else 1))
        }
    }
}
