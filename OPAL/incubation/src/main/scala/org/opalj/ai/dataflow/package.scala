/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import scala.collection.{Map, Set}

import br.analyses.SomeProject
import br.Method

/**
 * Supports the specification and solving of data-flow problems.
 *
 * =Goal=
 * To be able to express data-flow problems at a very high-level of abstraction.
 * I.e., that some information flows or not-flows
 * from a well-identified source to a well-identified sink.
 *
 * =Usage Scenario=
 *  - We want to avoid that information is stored in the database/processed by the
 *      backend without being sanitized.
 *  -  We want to specify that certain information is not allowed to flow from
 *      ''one module'' to ''another'' module.
 *
 * =Concept=
 *
 *  1. Select sources
 *      1. Sources are parameters passed to methods (e.g., doPost(session : Session)
 *      (This covers the main method as well as typical callback methods.)
 *      1. Values returned by methods (e.g., System.in.read) (here, we identify the call site)
 *  1. Select sinks
 *      1. A sink is either a field (in which the value is stored)
 *      1. a method (parameter) which is passed the value
 *  1. Filtering (Terminating) data-flows
 *      1. If a specific operation was performed, e.g.,
 *      1. If a comparison (e.g., against null, > 0 , ...)
 *      1. An instanceOf/a checkcast
 *      1. A mathematical operation (e.g. +.-,...)
 *      1. [OPTIMIZATION] If the value was passed to a specific method (e.g., check(x : X) - throws Exception if the check fails)
 *      1. [OPTIMIZATION] If the value was returned by a well-identified method (e.g., String sanitized = s.replace(...,...))
 *
 *  4. Extending data-flows (Side Channels)
 *      - OPEN:   What would be the general strategy if a value influences another value?
 *      - [SIDE CHANNELS?] What happens if the value is stored in a field of an object and that object is used?
 *      - [SIDE CHANNELS?] What happens if the value is used during the computation, but does not (directly) influence the output.
 *          (e.g., if(x == 0) 1; else 2;
 *
 * Furthermore, the framework will automatically handle taint propagation and aliasing.
 * I.e., a tainted value that is stored in a field automatically marks the respective
 * field as tainted.
 *
 * @author Michael Eichberg and Ben Hermann
 */
package object dataflow {

    type AValueLocationMatcher = Function1[SomeProject, Map[Method, Set[ValueOrigin]]]

}

