/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br.analyses.Project
import org.opalj.tac.cg.{AndroidCallGraphKey, RTACallGraphKey}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.net.URL

class AndroidSchedulerTest extends AnyFunSpec with Matchers {

    // new File("C:\\Users\\Tom\\Desktop\\android.jar")
    //final val testProject: Project[URL] = Project(new File("C:\\Users\\Tom\\Desktop\\testJar\\test.jar"), new File("C:\\Users\\Tom\\Desktop\\android.jar"))
    final val testProject: Project[URL] = Project(new File("C:\\Users\\Tom\\Desktop\\öffi\\Oeffi-enjarify.jar"), new File("C:\\Users\\Tom\\Desktop\\android.jar"))
    //val k = new AndroidCallGraphKey(cgk = RTACallGraphKey, "C:\\Users\\Tom\\Desktop\\testJar\\AndroidManifest.xml")
    val k = new AndroidCallGraphKey(cgk = RTACallGraphKey, "C:\\Users\\Tom\\Desktop\\öffi\\AndroidManifest.xml")
    val cg = testProject.get(k)
    println("done")
    println(cg.numEdges)

/**
    val aJar = new File("C:\\Users\\Tom\\Desktop\\android.jar")

    val d = new File("C:\\Users\\Tom\\Desktop\\AndroidTests")
    d.listFiles.filter(_.isDirectory).toList.foreach{dir =>
      dir.listFiles.filter(_.isFile).filter(_.getPath.endsWith(".jar")).toList.foreach{f =>
        val p: Project[URL] = Project(f, aJar)
        val k = new AndroidCallGraphKey(cgk = RTACallGraphKey, dir.getPath + "\\AndroidManifest.xml")
        p.get(k)
        val str = f.getPath.split("\\.")(0)
        CallGraphSerializer.writeCG(p.get(k), new File(str + "_Output.json"))(p.get(DeclaredMethodsKey))
      }
    }
*/
}

