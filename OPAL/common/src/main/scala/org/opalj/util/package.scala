/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import scala.annotation.nowarn
import scala.annotation.tailrec

import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import scala.util.Properties.versionNumberString
import scala.util.Using

import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.error

/**
 * Utility methods.
 *
 * @author Michael Eichberg
 */
package object util {

    // Used in the context of the implementation of "collect(PartialFunction)" methods.
    final val AnyToAnyThis: Function1[Any, Any] = {
        new Function1[Any, Any] { def apply(x: Any): Any = this }
    }

    val ScalaMajorVersion: String = {
        // TODO Simplify this again once Scala 3 uses a corresponding runtime library

        @tailrec
        def getVersionNumber(res: java.util.Enumeration[java.net.URL]): String =
            if (!res.hasMoreElements)
                versionNumberString.split('.').take(2).mkString(".") // e.g. 2.10, 2.11
            else {
                val manifest = new java.util.Properties()
                Using(res.nextElement().openStream()) { stream => manifest.load(stream) }
                manifest.getProperty("Specification-Title") match {
                    case "scala3-library-bootstrapped" => manifest.getProperty("Implementation-Version")
                    case _                             => getVersionNumber(res)
                }
            }

        getVersionNumber(this.getClass.getClassLoader.getResources("META-INF/MANIFEST.MF"))
    }

    def avg(ts: IterableOnce[Nanoseconds]): Nanoseconds = {
        val iterator = ts.iterator
        if (iterator.isEmpty)
            return Nanoseconds.None;

        Nanoseconds(iterator.map(_.timeSpan).sum / iterator.size)
    }

    /**
     * Converts the specified number of bytes into the corresponding number of megabytes
     * and returns a textual representation.
     */
    def asMB(bytesCount: Long): String = {
        val mbs = bytesCount / 1024.0d / 1024.0d
        f"$mbs%.2f MB" // String interpolation
    }

    /**
     * Converts the specified number of nanoseconds into milliseconds.
     */
    final def ns2ms(nanoseconds: Long): Double = nanoseconds.toDouble / 1000.0d / 1000.0d

    /**
     *  Tries its best to run the garbage collector and to wait until all objects are also
     *  finalized.
     */
    @nowarn("msg=method getObjectPendingFinalizationCount in trait MemoryMXBean is deprecated")
    final def gc(
        memoryMXBean: MemoryMXBean = ManagementFactory.getMemoryMXBean,
        maxGCTime:    Milliseconds = new Milliseconds(333)
    )(
        implicit logContext: Option[LogContext] = None
    ): Unit = {
        val startTime = System.nanoTime()
        var run = 0
        while {
            if (logContext.isDefined) {
                val pendingCount = memoryMXBean.getObjectPendingFinalizationCount()
                OPALLogger.info(
                    "performance",
                    s"garbage collection run $run (pending finalization: $pendingCount)"
                )(using logContext.get)
            }
            // In general it is **not possible to guarantee** that the garbage collector is really
            // run, but we still do our best.
            memoryMXBean.gc()
            if (memoryMXBean.getObjectPendingFinalizationCount() > 0) {
                // It may be the case that some finalizers (of just gc'ed object) are still running
                // and -- therefore -- some further objects are freed after the gc run.
                Thread.sleep(50)
                memoryMXBean.gc()
            }
            run += 1

            memoryMXBean.getObjectPendingFinalizationCount() > 0 && ns2ms(
                System.nanoTime() - startTime
            ) < maxGCTime.timeSpan
        } do ()
    }

    def renderConfig(config: Config, withComments: Boolean = true): String = {
        val renderingOptions =
            ConfigRenderOptions.defaults().setOriginComments(false).setComments(withComments).setJson(false)
        val opalConf = config.withOnlyPath("org")
        opalConf.root().render(renderingOptions)
    }

    /**
     * Reflectively retrieves an object.
     *
     * @note Make sure to supply the expected object type parameter A to avoid potential issues.
     */
    def getObjectReflectively[A](fqn: String, source: AnyRef, category: String = "configuration")(implicit
        logContext: LogContext = GlobalLogContext
    ): Option[A] = {
        import scala.reflect.runtime.universe._
        try {
            val mirror = runtimeMirror(source.getClass.getClassLoader)
            val module = mirror.staticModule(fqn)
            Some(mirror.reflectModule(module).instance.asInstanceOf[A])
        } catch {
            case sre: ScalaReflectionException =>
                error(category, s"Cannot find object $fqn", sre)
                None
            case cce: ClassCastException =>
                error(category, "Reflected object is invalid", cce)
                None
        }
    }

}
