/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Writer

import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.properties.Context

/**
 * Provides the functionality to serialize a [[CallGraph]] into a .json file according to the format
 * required by the JCG (https://bitbucket.org/delors/jcg) project.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
object CallGraphSerializer {

    /**
     * Writes the given call graph into the provided file according to the JCG format
     * (https://bitbucket.org/delors/jcg).
     *
     * @note That this implementation explicitly uses a buffered writer and writes the information
     *       directly into the file. Furthermore, it is not pretty printed and spaces are omitted.
     *       This allows large call graphs to be serialized efficiently.
     */
    def writeCG(cg: CallGraph, outFile: File)(implicit declaredMethods: DeclaredMethods): Unit = {
        val writer = new BufferedWriter(new FileWriter(outFile))
        writer.write(s"""{"reachableMethods":[""")
        var firstRM = true
        for {
            rm <- cg.reachableMethods()
            callees = cg.calleesOf(rm.method)
        } {
            if (firstRM) {
                firstRM = false
            } else {
                writer.write(",")
            }
            writer.write("{\"method\":")
            writeMethodObject(rm.method, writer)
            writer.write(",\"callSites\":[")

            writeCallSites(rm.method, callees, writer)

            writer.write("]}")
        }
        writer.write("]}")
        writer.flush()
        writer.close()
    }

    private def writeCallSites(
        method:  DeclaredMethod,
        callees: Iterator[(Int, Iterator[Context])],
        out:     Writer
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        val bodyO = if (method.hasSingleDefinedMethod) method.definedMethod.body else None
        var first = true
        for ((pc, targets) <- callees) {
            bodyO match {
                case None =>
                    for (tgt <- targets) {
                        if (first) first = false
                        else out.write(",")
                        writeCallSite(tgt.method, -1, pc, Iterator(tgt), out)
                    }

                case Some(body) =>
                    val declaredTgtO = body.instructions(pc) match {
                        case MethodInvocationInstruction(dc, _, name, desc) => Some((dc, name, desc))
                        case _                                              => None
                    }

                    val line = body.lineNumber(pc).getOrElse(-1)

                    if (declaredTgtO.isDefined) {
                        val (dc, name, desc) = declaredTgtO.get
                        val declaredType =
                            if (dc.isArrayType)
                                ObjectType.Object
                            else
                                dc.asObjectType

                        val declaredTarget = declaredMethods(
                            declaredType, declaredType.packageName, declaredType, name, desc
                        )

                        val (directCallees, indirectCallees) = targets.partition { callee =>
                            callee.method.name == name && // TODO check descriptor correctly for refinement
                                callee.method.descriptor.parametersCount == desc.parametersCount
                        }

                        for (tgt <- indirectCallees) {
                            if (first) first = false
                            else out.write(",")
                            writeCallSite(tgt.method, line, pc, Iterator(tgt), out)
                        }
                        if (directCallees.nonEmpty) {
                            if (first) first = false
                            else out.write(",")
                            writeCallSite(declaredTarget, line, pc, directCallees, out)
                        }

                    } else {
                        for (tgt <- targets) {
                            if (first) first = false
                            else out.write(",")
                            writeCallSite(tgt.method, line, pc, Iterator(tgt), out)
                        }
                    }
            }
        }
    }

    private def writeCallSite(
        declaredTarget: DeclaredMethod,
        line:           Int,
        pc:             Int,
        targets:        Iterator[Context],
        out:            Writer
    ): Unit = {
        out.write("{\"declaredTarget\":")
        writeMethodObject(declaredTarget, out)
        out.write(",\"line\":")
        out.write(line.toString)
        out.write(",\"pc\":")
        out.write(pc.toString)
        out.write(",\"targets\":[")
        var first = true
        for (tgt <- targets) {
            if (first) first = false
            else out.write(",")
            writeMethodObject(tgt.method, out)
        }
        out.write("]}")
    }

    private def writeMethodObject(
        method: DeclaredMethod,
        out:    Writer
    ): Unit = {
        out.write("{\"name\":\"")
        out.write(method.name)
        out.write("\",\"declaringClass\":\"")
        out.write(method.declaringClassType.toJVMTypeName)
        out.write("\",\"returnType\":\"")
        out.write(method.descriptor.returnType.toJVMTypeName)
        out.write("\",\"parameterTypes\":[")
        if (method.descriptor.parametersCount > 0)
            out.write(method.descriptor.parameterTypes.iterator.map[String](_.toJVMTypeName).mkString("\"", "\",\"", "\""))
        out.write("]}")
    }
}
