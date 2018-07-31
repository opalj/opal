/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package dataflow
package spec

import scala.collection.{Map, Set}
import br._
import br.analyses._
//import br.instructions._

//import domain._
//import domain.l0._

/**
 * Support methods to facilitate the definition of data-flow constraints.
 *
 * @author Michael Eichberg and Ben Hermann
 */
trait SourcesAndSinks {

    //
    // Storing/Managing Sources
    //
    private[this] var sourceMatchers: List[AValueLocationMatcher] = Nil
    def sources(vlm: AValueLocationMatcher): Unit = sourceMatchers = vlm :: sourceMatchers

    private[this] var theSourceValues: Map[Method, Set[ValueOrigin]] = _
    def sourceValues: Map[Method, Set[ValueOrigin]] = theSourceValues

    def sources(
        filter:  Function[ClassFile, Boolean],
        matcher: PartialFunction[Method, Set[ValueOrigin]]
    ): Unit = {

        sourceMatchers =
            new AValueLocationMatcher {

                def apply(project: SomeProject) = {
                    val map = scala.collection.mutable.AnyRefMap.empty[Method, Set[ValueOrigin]]
                    for {
                        classFile ← project.allProjectClassFiles
                        if filter(classFile)
                        method ← classFile.methods
                        if method.body.isDefined
                    } {
                        if (matcher.isDefinedAt(method)) {
                            val matchedValues = matcher(method)
                            if (matchedValues.nonEmpty)
                                map.update(method, matchedValues)
                        }
                    }
                    map.repack
                    map
                }
            } :: sourceMatchers
    }

    //
    // Storing/Managing Sinks
    //
    private[this] var sinkMatchers: List[AValueLocationMatcher] = Nil
    def sinks(vlm: AValueLocationMatcher): Unit = sinkMatchers = vlm :: sinkMatchers

    private[this] var theSinkInstructions: Map[Method, Set[PC]] = _
    def sinkInstructions: Map[Method, Set[PC]] = theSinkInstructions

    //
    // Instantiating the problem
    //

    protected[this] def initializeSourcesAndSinks(project: SomeProject): Unit = {
        import scala.collection.immutable.HashMap

        val sources = sourceMatchers map ((m: AValueLocationMatcher) ⇒ m(project))
        this.theSourceValues = sources.foldLeft(HashMap.empty[Method, Set[ValueOrigin]])(_ ++ _)

        val sinks = sinkMatchers map ((m: AValueLocationMatcher) ⇒ m(project))
        this.theSinkInstructions = sinks.foldLeft(HashMap.empty[Method, Set[PC]])(_ ++ _)
    }
}
