/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.debug

import org.opalj.ba.InsertionPosition
import org.opalj.ba.LabeledCode
import org.opalj.ba.toDA
import org.opalj.bc.Assembler
import org.opalj.bi.Java15Version
import org.opalj.br.IntegerType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.PCAndInstruction
import org.opalj.br.VoidType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.instructions.{DUP, INVOKESPECIAL, INVOKESTATIC, LoadString, MethodInvocationInstruction, NEW, POP, SIPUSH}
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.properties.TACAI

import java.nio.file.Files
import java.nio.file.Paths
import scala.collection.immutable.ArraySeq

/**
 * instrument possible def sites to log instance. TODO:
 */
object PTSTracker {

    def main(args: Array[String]): Unit = {
        import Console.RED
        import Console.RESET
        if (args.length != 2) {
            println("You have to specify the method that should be analyzed.")
            println("\t1: directory containing class files ")
            println("\t2: destination of instrumented class files")
            return ;
        }
        val inputClassPath = args(0)
        val outputClassPath = args(1)

        val file = new java.io.File(inputClassPath)
        if (!file.exists()) {
            println(RED+"[error] the file does not exist: "+inputClassPath+"."+RESET)
            return ;
        }

        val project = try { Project(file) } catch {
            case e: Exception =>
                println(RED+"[error] cannot process file: "+e.getMessage+"."+RESET)
                return ;
        }
        implicit val ps: PropertyStore = project.get(PropertyStoreKey)
        val tacai = (m: Method) => { val FinalP(taCode) = ps(m, TACAI.key); taCode.tac }
        val PTSLoggerType = ObjectType("org/opalj/fpcf/fixtures/PTSLogger")
        // TODO: copy all attributes etc.
        var methodId = 1
        for (cf <- project.allProjectClassFiles.filter(_.fqn.startsWith("org/opalj/fpcf/fixtures/xl/js/"))) {

            println(cf.sourceFile)
            val newMethods =
                for (m <- cf.methods) yield {

                    methodId = (methodId.+)(1)
                    m.body match {
                        case None =>
                            m.copy() // methods which are native and abstract ...

                        case Some(code) =>
                            val tac = tacai(m)
                            println(tac)
                            // let's search all "toString" calls
                            val lCode = LabeledCode(code)
                            var modified = false
                            var lastNew: Option[(Int, ObjectType)] = Option.empty

                            for {
                                PCAndInstruction(pc, inst) <- code
                            } {
                                var insertLog = false
                                var pcToLog = pc
                                inst match {
                                    case NEW(objectType) => {
                                        lastNew = Option((pc, objectType))
                                    }
                                    case INVOKESPECIAL(objectType, isInterface, name, methodDescriptor) => {
                                        lastNew match {
                                            case Some(pcAndType) => {
                                                insertLog = true
                                                pcToLog = pcAndType._1
                                                lastNew = Option.empty
                                            }
                                            case None =>
                                        }

                                    }
                                    case LoadString(value) => {
                                        insertLog = true
                                    }
                                    case MethodInvocationInstruction(refType, isInterface, name, methodDescriptor) => {
                                        if (!methodDescriptor.returnType.isVoidType) {
                                            // don't log if return value POP'd immediately
                                            val nextInstPC = code.pcOfNextInstruction(pc)
                                            val nextInst = code.find(_.pc == nextInstPC)
                                            nextInst match {
                                                case Some(PCAndInstruction(_, POP)) =>
                                                case Some(_) => insertLog = true
                                                case None =>
                                            }

                                        }
                                    }
                                    case _ => {

                                    }
                                }
                                if (insertLog) {
                                    modified = true
                                    lCode.insert(
                                        pc, InsertionPosition.After,
                                        Seq(
                                            DUP,
                                            SIPUSH(methodId),
                                            SIPUSH(pcToLog),
                                            INVOKESTATIC(PTSLoggerType, isInterface = false, "logDefsiteInstance", MethodDescriptor(ArraySeq(ObjectType.Object, IntegerType, IntegerType), VoidType))
                                        )
                                    )
                                }

                            }
                            if (modified) {
                                val (newCode, _) =
                                    lCode.result( /*cf.version*/ Java15Version, m)( // We can use the default class hierarchy in this example
                                    // as we only instrument linear methods using linear code,
                                    // hence, we don't need to compute a new stack map table attribute!
                                    )
                                m.copy(body = Some(newCode))
                            } else {
                                m.copy()
                            }
                    }
                }

            val newRawCF = Assembler(toDA(cf.copy(methods = newMethods)))
            //val path = inputClassPath+"/"+cf.fqn+".class"
            //val in = () => new FileInputStream(path)
            val outputPath = Paths.get(outputClassPath+"/"+cf.fqn+".class")
            println(outputPath)
            outputPath.getParent.toFile.mkdirs()
            Files.write(outputPath, newRawCF) //, StandardOpenOption.TRUNCATE_EXISTING)

            // // Let's see the old class file...
            // val odlCFHTML = ClassFile(in).head.toXHTML(None)
            // val oldCFHTMLFile = writeAndOpen(odlCFHTML, "SimpleInstrumentationDemo", ".html")
            // println("original: "+oldCFHTMLFile)
            //
            // // Let's see the new class file...
            // val newCF = ClassFile(() => new ByteArrayInputStream(newRawCF)).head.toXHTML(None)
            // println("instrumented: "+writeAndOpen(newCF, "NewSimpleInstrumentationDemo", ".html"))
            //
            // // Let's test that the new class does what it is expected to do... (we execute the
            // // instrumented method)
            // val cl = new InMemoryClassLoader(Map((cf.thisType.toJava, newRawCF)))
            // val newClass = cl.findClass(cf.thisType.toJava)
            // println(newClass)

            // val instance = newClass.getDeclaredConstructor().newInstance()
            //newClass.getMethod("main", String[]).getClass()).invoke(null)
            // newClass.getMethod("returnsValue", classOf[Int]).invoke(instance, Integer.valueOf(0))
            // newClass.getMethod("returnsValue", classOf[Int]).invoke(instance, Integer.valueOf(1))
        }

    }

}