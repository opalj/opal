/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.io.File
import java.net.URL
import org.opalj.log.OPALLogger
import org.opalj.log.StandardLogContext
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.common.SimpleAIKey

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * A template for implementing 3-address code based analyses.
 *
 * @author Michael Eichberg
 */
object TACTemplate {

    /** Description of the command-line parameters. */
    def usage: String = {
        "Usage: java …TACTemplate \n"+
            "{-cp <JAR file/Folder containing class files>}*\n"+
            "[{-libCP <JAR file/Folder containing library class files>}*] (generally required to get precise/correct type information)\n"+
            "[-libJDK] (the JDK is added to the project as a library)\n"+
            "[-class <class file name>] (filters the set of classes)\n"+
            "[-method <method name/signature using Java notation; e.g., \"int hashCode()\">] (filters the set of methods)\n"+
            "[-domain <class name of the domain>]\n"+
            "Example:\n\tjava …TACTemplate -cp /Library/jre/lib/rt.jar -class java.util.ArrayList -method toString"
    }

    /** Prints the errors message and then the usage information. */
    def error(message: String): String = s"Error: $message \n$usage"

    /** Prints the error message, usage information and then quits the application. */
    def handleError(message: String): Nothing = {
        Console.err.println(error(message))
        sys.exit(-1)
    }

    def main(args: Array[String]): Unit = {

        // 1.
        // Declaration of analysis specific parameters
        //
        var cp = List.empty[String] // files and folders containing the classes of the used libraries
        var libcp = List.empty[String] // files and folders containing the classes of the used libraries
        var className: Option[String] = None
        var methodSignature: Option[String] = None
        var domainName: String = "org.opalj.ai.domain.l0.PrimitiveTACAIDomain"
        // Alternative, readily available domains are, e.g.:
        // org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
        // org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse

        // 2.
        // 2.1.
        // Parsing command line parameters
        //
        var i = 0
        def readNextArg(): String = {
            i += 1
            if (i < args.length) {
                args(i)
            } else {
                handleError(s"missing argument: ${args(i - 1)}")
            }
        }
        while (i < args.length) {
            args(i) match {
                case "-cp"     => cp ::= readNextArg()
                case "-libJDK" => libcp ::= JRELibraryFolder.toString
                case "-libcp"  => libcp ::= readNextArg()
                case "-class"  => className = Some(readNextArg().replace('/', '.'))
                case "-method" => methodSignature = Some(readNextArg())
                case "-domain" => domainName = readNextArg() // overwrites default domain
                case unknown   => handleError(s"unknown parameter: $unknown")
            }
            i += 1
        }
        // 2.2.
        // Validating command line parameters
        //
        if (cp.isEmpty) handleError("missing parameters")

        // 3.
        // Instantiate the Project
        //
        //      Given that we may use a context-sensitive domain (e.g., ...domain.l2.DefaultDomain),
        //      we also completely load the library code and not just the public API.
        implicit val logContext = new StandardLogContext()
        OPALLogger.register(logContext, OPALLogger.globalLogger())
        val reader = Project.JavaClassFileReader(logContext)
        val p = Project(
            reader.AllClassFiles(cp.map(new File(_))),
            reader.AllClassFiles(libcp.map(new File(_))), libraryClassFilesAreInterfacesOnly = false
        )
        // 4.
        // Finish the configuration of the (underlying data-flow) analyses that will be used.
        val domainClass = Class.forName(domainName)
        OPALLogger.info("analysis configuration", s"using $domainClass for ai")(p.logContext)
        val constructor = domainClass.getConstructor(classOf[Project[URL]], classOf[Method])
        p.getOrCreateProjectInformationKeyInitializationData(
            SimpleAIKey,
            (m: Method) => constructor.newInstance(p, m).asInstanceOf[Domain with RecordDefUse]
        )
        p.get(SimpleAIKey) // used by the LazyTACUsingAIKey
        val tac = p.get(LazyTACUsingAIKey)

        // 5.
        // Perform analysis
        // As part of this template, we just demonstrate how to print the virtual methods
        // calls of a selected set of methods.
        for {
            cf <- p.allProjectClassFiles.par // OPAL is generally, thread safe and facilitates parallelization
            if className.isEmpty || cf.thisType.toJava.contains(className.get)
            m <- cf.methods
            if m.body.isDefined
            if methodSignature.isEmpty || m.signature.toJava.contains(methodSignature.get)
            c = tac(m)
            VirtualFunctionCallStatement(VirtualFunctionCall(pc, declaringClass: ObjectType, _, name, descriptor, receiver, _)) <- c.stmts
        } {
            println(m.toJava(s"$pc: virtual function call of $receiver.${descriptor.toJava(declaringClass.toJava, name)}"))
        }

        println("Done.")
    }
}
