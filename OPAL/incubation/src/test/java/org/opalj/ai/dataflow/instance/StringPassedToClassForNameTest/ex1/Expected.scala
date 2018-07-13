/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package dataflow
package instance
package StringPassedToClassForNameTest.ex1

import scala.collection.Set
import org.opalj.ai.dataflow.spec._
import org.opalj.br._

object Expected extends ExpectedDataFlowsSpecification {

    paths(new SourcesAndSinks {
        sources(
            classFile ⇒ classFile.thisType.packageName == "org/opalj/ai.dataflow/instance/StringPassedToClassForNameTest/ex1",
            {
                case method @ Method(_, "violation", _) ⇒ Set( /*-1 == "this"*/ -2)
            }
        )

        sinks(Calls(
            { case (ObjectType.Class, "forName", SingleArgumentMethodDescriptor((ObjectType.String, ObjectType.Object))) ⇒ true }
        ))
    })

    // We can have multiple "paths" specifications.
}
