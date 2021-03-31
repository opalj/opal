/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import scala.language.existentials
import scala.collection.JavaConverters._
import org.scalatest.funsuite.AnyFunSuite
import java.lang.{Boolean ⇒ JBoolean}
import java.util.concurrent.ConcurrentLinkedQueue

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.opalj.log.StandardLogContext
import org.opalj.log.OPALLogger
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.analyses.SomeProject
import org.opalj.br.reader.InvokedynamicRewriting.StringConcatNameRegEx

/**
 * Infrastructure to load a project containing Jars.
 *
 * @author Arne Lottmann
 * @author Dominik Helm
 */
abstract class InvokedynamicRewritingTest extends AnyFunSuite {

    protected def isProxyFactoryCall(instruction: INVOKESTATIC): Boolean = {
        isProxyFactoryCall(instruction.declaringClass.fqn)
    }

    protected def isProxyFactoryCall(declaringClassFQN: String): Boolean = {
        declaringClassFQN.matches(InvokedynamicRewriting.LambdaNameRegEx)
    }

    protected def proxyFactoryCalls(project: SomeProject): Iterable[INVOKESTATIC] = {
        val factoryCalls = new ConcurrentLinkedQueue[INVOKESTATIC]()
        project.parForeachMethodWithBody() { mi ⇒
            factoryCalls.addAll(
                mi.method.body.get.collectInstructions {
                    case i: INVOKESTATIC if isProxyFactoryCall(i) ⇒ i
                }.asJava
            )
            /*
            for {
                (_,i @ INVOKESTATIC(declaringClass,_,_,_)) <- mi.method.body.get
                if isProxyFactoryCall(declaringClass.fqn)
            } {
                factoryCalls.add(i)
            }
            */
        }
        info(s"found ${factoryCalls.size} lambda proxy factory method calls")
        factoryCalls.asScala

    }

    protected def stringConcatFactoryCalls(project: SomeProject): Iterable[INVOKESTATIC] = {
        val factoryCalls = new ConcurrentLinkedQueue[INVOKESTATIC]()
        project.parForeachMethodWithBody() { mi ⇒
            factoryCalls.addAll(
                mi.method.body.get.collectInstructions {
                    case i: INVOKESTATIC if i.name.matches(StringConcatNameRegEx) ⇒ i
                }.asJava
            )
        }
        info(s"found ${factoryCalls.size} string concat factory method calls")
        factoryCalls.asScala

    }

    /**
     * Loads the library and checks if at least one call to a proxy factory method is found.
     */
    protected def project(libraryPath: java.io.File): (SomeProject, Iterable[INVOKESTATIC]) = {
        val baseConfig: Config = ConfigFactory.load()
        val rewritingConfigKey = InvokedynamicRewriting.InvokedynamicRewritingConfigKey
        val logLambdaConfigKey = InvokedynamicRewriting.LambdaExpressionsLogRewritingsConfigKey
        val logConcatConfigKey = InvokedynamicRewriting.StringConcatLogRewritingsConfigKey
        val config = baseConfig.
            withValue(rewritingConfigKey, ConfigValueFactory.fromAnyRef(JBoolean.TRUE)).
            withValue(logLambdaConfigKey, ConfigValueFactory.fromAnyRef(JBoolean.FALSE)).
            withValue(logConcatConfigKey, ConfigValueFactory.fromAnyRef(JBoolean.FALSE)) /*.
            withValue(SynthesizedClassFiles., ConfigValueFactory.fromAnyRef(JBoolean.FALSE))
            */

        val logContext = new StandardLogContext
        OPALLogger.register(logContext)
        val project = Project(libraryPath, logContext, config)
        val proxyFactoryCalls = this.proxyFactoryCalls(project)
        assert(proxyFactoryCalls.nonEmpty, "there should be calls to the proxy factories")

        stringConcatFactoryCalls(project)

        (project, proxyFactoryCalls)
    }

    protected def checkForMissingProxyClassFiles(
        project:           SomeProject,
        proxyFactoryCalls: Iterable[INVOKESTATIC]
    ): Unit = {
        val missingProxyClassFiles = for {
            proxyFactoryCall ← proxyFactoryCalls
            proxy = project.classFile(proxyFactoryCall.declaringClass)
            if proxy.isEmpty
        } yield {
            (proxy, proxyFactoryCall)
        }

        if (missingProxyClassFiles.nonEmpty) {
            val failures = missingProxyClassFiles.size
            val data = missingProxyClassFiles.mkString(
                "missing proxy ClassFiles for the following instructions:\n\t", "\n\t", "\n"
            )
            val logFile = io.writeAndOpen(data, "MissingProxyClassFiles", ".txt")
            val msg = s"missing $failures proxy ClassFiles for lambdas; see $logFile for details"
            fail(msg)
        }
    }

    protected def load(libraryPath: java.io.File): SomeProject = {
        val (project, proxyFactoryCalls) = this.project(libraryPath)
        checkForMissingProxyClassFiles(project, proxyFactoryCalls)
        project
    }
}
