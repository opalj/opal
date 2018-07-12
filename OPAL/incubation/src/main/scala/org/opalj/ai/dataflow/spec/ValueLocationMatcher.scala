/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package dataflow
package spec

import org.opalj.collection.immutable.Chain

import scala.collection.{Map, Set}

//import bi.AccessFlagsMatcher

import br._
import br.analyses._
import br.instructions._

//import domain._
//import domain.l0._

trait ValueLocationMatcher extends AValueLocationMatcher {

    def apply(project: SomeProject): Map[Method, Set[PC]] // the PCs of the values that we want to track

}

//case class Methods(
//        properties: PartialFunction[Method, Boolean] = { case m: Method ⇒ true },
//        parameters: PartialFunction[(Int, FieldType), Boolean]) extends ValueLocationMatcher {
//
//    def apply(project: SomeProject): Map[Method, Set[PC]] = {
//        import scala.collection.mutable.{ HashMap, HashSet }
//
//        var result = HashMap.empty[Method, HashSet[PC]]
//        for {
//            classFile ← project.classFiles
//            method @ MethodWithBody(body) ← classFile.methods
//            true ← properties.lift(method)
//            (parameterType, index) ← method.descriptor.parameterTypes.zipWithIndex
//        } {
//            val methodParameterShift = if (method.isStatic) -1 else -2
//            val parameter = (index, parameterType)
//            if (parameters.isDefinedAt(parameter) && parameters(parameter)) {
//                // FIXME
//                result.getOrElseUpdate(method, HashSet.empty) += (-index + methodParameterShift)
//            }
//        }
//        result
//    }
//}

case class Calls(
        properties: PartialFunction[(ReferenceType, String, MethodDescriptor), Boolean]
) extends ValueLocationMatcher {

    def apply(project: SomeProject): Map[Method, Set[PC]] = {
        import scala.collection.mutable

        val result = mutable.HashMap.empty[Method, mutable.HashSet[PC]]
        for {
            classFile ← project.allProjectClassFiles
            method ← classFile.methods
            body ← method.body
            pc ← body.foldLeft(Chain.empty[Int /*PC*/ ]) { (pcs, pc, instruction) ⇒
                instruction match {
                    case MethodInvocationInstruction(
                        receiver,
                        _ /*isInterface*/ ,
                        name,
                        descriptor
                        ) if properties.isDefinedAt((receiver, name, descriptor)) &&
                        properties((receiver, name, descriptor)) ⇒ pc :&: pcs
                    case _ ⇒ pcs
                }
            }
        } {
            result.getOrElseUpdate(method, mutable.HashSet.empty) += pc
        }
        result
    }

}
