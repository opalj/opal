/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package dataflow
package instance

import java.io.File
import org.opalj.io.process
import org.opalj.bi.AccessFlagsMatcher._
import org.opalj.value.IsReferenceValue
import org.opalj.br._
import org.opalj.br.analyses._
import org.opalj.ai.dataflow.spec._
import org.opalj.ai.dataflow.solver.NaiveSolver

/**
 * Searches for strings that are passed to `Class.forName(_)` calls.
 *
 * @author Michael Eichberg and Ben Hermann
 */
abstract class StringPassedToClassForName[Source]
    extends DataFlowProblemSpecification[Source, (String) ⇒ Boolean] {

    type P = (String) ⇒ Boolean
    val definedInRestrictedPackage = p

    //
    // Specification of the sources and sinks
    //

    //    sources(Methods(
    //        properties = { case Method(PUBLIC___OR___PROTECTED_AND_NOT_FINAL(), _, md) ⇒ md.parametersCount >= 1 },
    //        parameters = { case (_ /*ID*/ , ObjectType.String) ⇒ true }
    //    ))

    sources(
        classFile ⇒ !definedInRestrictedPackage(classFile.thisType.packageName),
        {
            case method @ Method(PUBLIC___OR___PROTECTED_AND_NOT_FINAL(), _, md) ⇒
                md.selectParameter(_ == ObjectType.String).toSet.map(
                    parameterIndexToValueOrigin(method.isStatic, method.descriptor, _: Int)
                )
        }
    )

    sinks(
        Calls {
            case (
                ObjectType.Class, "forName",
                SingleArgumentMethodDescriptor((ObjectType.String, ObjectType.Class))
                ) ⇒ true
        }

    )

    // Scenario: ... s.subString(...)
    call {
        case Invoke(
            ObjectType.String,
            _, // methodName
            MethodDescriptor(_, _ /*rt*/ : ObjectType.String.type), // the called method returns a string
            _, // calling context (Method,...)
            _, // the caller
            receiver @ Tainted(_ /*String*/ ), // receiver type
            param @ _ // parameters,
            ) ⇒
            CallResult(
                receiver, // our string remains tainted
                param, // we don't care // example: r.addTo(Set s)
                ValueIsTainted // the RESULT
            )
    }

    // Scenario: assign a tainted string to a field of some class and mark the class as tainted
    write {
        case FieldWrite(
            _,
            _,
            ObjectType.String,
            _,
            _, // the caller
            Tainted(value: IsReferenceValue), // receiver type
            _ //receiver
            ) if value.isValueASubtypeOf(ObjectType.String).isYesOrUnknown ⇒
            ValueIsTainted
    }

}

object StringPassedToClassForName extends DataFlowProblemFactory with DataFlowProblemRunner {

    type P = (String) ⇒ Boolean

    //
    // Handling for the specified "java.security" file.
    //

    override def title: String = "StringPassedToClassForName"

    override def description: String = "Finds calls to Class.forName from non-privliged code"

    final val javaSecurityParameter = "-java.security="

    override def analysisSpecificParametersDescription: String =
        javaSecurityParameter+"<JRE/JDK Security Policy File>"

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Seq[String] = {
        if (parameters.size == 0)
            Seq("missing parameter: -java.security")
        else if (parameters.size > 1)
            Seq("too many parameters: "+parameters.mkString(" "))
        else if (!parameters.head.startsWith(javaSecurityParameter))
            Seq("unknown parameter: "+parameters.head)
        else if (!{
            val securityFileParameter = parameters.head
            val securityFile = securityFileParameter.substring(javaSecurityParameter.length())
            new File(securityFile).exists()
        })
            Seq("the specified security file is not valid: "+parameters.head)
        else
            Seq.empty

    }

    override def processAnalysisParameters(parameters: Seq[String]): P = {
        val javaSecurityFile = parameters.head.substring(javaSecurityParameter.length())

        val restrictedPackages = process(new java.io.FileInputStream(javaSecurityFile)) { in ⇒
            val properties = new java.util.Properties()
            properties.load(in)
            properties.getProperty("package.access", "").
                split(",").
                map(_.trim.replace('.', '/'))
        }
        def definedInRestrictedPackage(packageName: String): Boolean =
            restrictedPackages.exists(packageName.startsWith(_))

        definedInRestrictedPackage
    }

    //
    // Factory method
    //

    override def create[Source](
        theProject: Project[Source],
        theP:       P
    ): DataFlowProblem[Source, P] = {
        object StringPassedToClassForNameWithSimpleSolver extends {
            // early definition block
            final val project = theProject
            final val p = theP
        } with StringPassedToClassForName[Source] with NaiveSolver[Source, P]
        StringPassedToClassForNameWithSimpleSolver
    }

}
