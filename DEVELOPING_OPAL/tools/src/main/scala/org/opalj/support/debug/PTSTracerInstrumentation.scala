/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.debug

import org.opalj.ba.InsertionPosition
import org.opalj.ba.LabeledCode
import org.opalj.ba.toDA
import org.opalj.bc.Assembler
import org.opalj.bi.Java15Version
import org.opalj.br.{IntegerType, MethodDescriptor, ObjectType, PCAndInstruction, VoidType}
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.{ALOAD, DUP, GETFIELD, GETSTATIC, INVOKESPECIAL, INVOKESTATIC, LDC, LoadFloat, LoadInt, MethodInvocationInstruction, NEW, POP, SIPUSH}
import org.opalj.util.InMemoryClassLoader

import java.io.{FileOutputStream, PrintWriter}
import java.net.{URL, URLClassLoader}
import java.nio.file.Files
import java.nio.file.Paths
import scala.collection.immutable.ArraySeq
import scala.collection.mutable
/**
 * instrument possible def sites to log instance. TODO:
 */
object PTSTracerInstrumentation {

    def main(args: Array[String]): Unit = {
        import Console.RED
        import Console.RESET
        if (args.length != 2) {
            println("You have to specify the code that should be analyzed.")
            println("\t1: directory containing class files      e.g.  /home/julius/IdeaProjects/opal/DEVELOPING_OPAL/validate/target/scala-2.13/test-classes")
            println("\t2: destination of instrumented class files e.g /home/julius/IdeaProjects/opal/DEVELOPING_OPAL/validate/target/scala-2.13/instrumented")
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
        // clear trace.xml file
      val fw = new PrintWriter(new FileOutputStream("trace.xml", false));
      fw.write("<trace>\n<methods>\n")
      //implicit val ps: PropertyStore = project.get(PropertyStoreKey)
        //val tacai = (m: Method) => { val FinalP(taCode) = ps(m, TACAI.key); taCode.tac }
        val classloaderScriptEngineJars = new URLClassLoader(
          Array(
            new URL("file:///Users/tobiasroth/Downloads/asm-all-5.2.jar"),
            new URL("file:///Users/tobiasroth/Downloads/nashorn-core-15.4.jar")
          ),
          ClassLoader.getSystemClassLoader)
        val PTSLoggerType = ObjectType("org/opalj/fpcf/fixtures/PTSLogger")
        val ptsClassFile = PTSTracerInstrumentation.readFile(inputClassPath+"/org/opalj/fpcf/fixtures/PTSLogger.class")

        // TODO: copy all attributes etc.
        val testCases = mutable.Set[(String, Array[Byte])]()
        var methodId = 1
        for (cf <- project.allProjectClassFiles.filter(_.fqn.startsWith("org/opalj/fpcf/fixtures/xl/js/"))) {
            var hasMain = false
            val newMethods =
                for (m <- cf.methods) yield {
                    if (m.name.equals("main")) hasMain = true
                    methodId = (methodId.+)(1)
                    println(s"instrumenting ${m.fullyQualifiedSignature}")
                    val tag = (s"<method fullyqualified=\"" + m.fullyQualifiedSignature+ "\" id=\"" + methodId + "\"/>")
                  println(tag)
                  fw.write(tag + "\n")
                    m.body match {
                        case None =>
                            m.copy() // methods which are native and abstract ...
                          
                        case Some(code) =>

                            val lCode = LabeledCode(code)
                            var modified = false
                            var lastNew: Option[(Int, ObjectType)] = Option.empty
                          if (!m.isStatic) {
                            // log this parameter last (in case of constructor)
                            // it won't change anyways
                            modified = true
                            var maxPC = 0
                            code.programCounters.foreach(p => maxPC = maxPC.max(p))
                            lCode.insert(
                              maxPC, InsertionPosition.Before,
                              Seq(
                                ALOAD(0),
                                SIPUSH(methodId),
                                SIPUSH(-1),
                                INVOKESTATIC(PTSLoggerType, isInterface = false, "logParameterInstance", MethodDescriptor(ArraySeq(ObjectType.Object, IntegerType, IntegerType), VoidType))
                              )
                            )
                          }
                          var index = 0
                          val paramOffsetOnStack = if (m.isStatic) 0 else 1
                            for (param <- m.descriptor.parameterTypes) {
                               if (!param.isBaseType ) {
                                modified = true
                                lCode.insert(
                                  0, InsertionPosition.After,
                                  Seq(
                                    ALOAD(paramOffsetOnStack + index),
                                    SIPUSH(methodId),
                                    SIPUSH(index),
                                    INVOKESTATIC(PTSLoggerType, isInterface = false, "logParameterInstance", MethodDescriptor(ArraySeq(ObjectType.Object, IntegerType, IntegerType), VoidType))
                                  )
                                )
                              }
                              index += 1
                            }
                            for {
                                PCAndInstruction(pc, inst) <- code
                            } {
                                var insertDefsiteLog = false
                                var pcToLog = pc
                                inst match {
                                    case NEW(objectType) => {
                                        lastNew = Option((pc, objectType))
                                    }
                                    case INVOKESPECIAL(objectType, isInterface, name, methodDescriptor) => {
                                        lastNew match {
                                            case Some(pcAndType) => {
                                              pcToLog = pcAndType._1
                                              modified = true
                                              lCode.insert(
                                                pc, InsertionPosition.After,
                                                Seq(
                                                  DUP,
                                                  SIPUSH(methodId),
                                                  SIPUSH(pcToLog),
                                                  INVOKESTATIC(PTSLoggerType, isInterface = false, "logAllocsiteInstance", MethodDescriptor(ArraySeq(ObjectType.Object, IntegerType, IntegerType), VoidType))
                                                )
                                              )


                                                lastNew = Option.empty
                                            }
                                            case None =>
                                        }
                                    }
                                    case GETFIELD(objectType, name, fieldType) => {
                                      if (!fieldType.isBaseType)
                                        insertDefsiteLog = true
                                    }
                                    case GETSTATIC(cls, name, fieldType) => {
                                      if (!fieldType.isBaseType)
                                        insertDefsiteLog = true
                                    }
                                    case load : LDC[_]  => {
                                      load match {
                                        case _ : LoadInt =>
                                        case _ : LoadFloat =>
                                        case _ => insertDefsiteLog = true
                                      }
                                    }
                                    case MethodInvocationInstruction(refType, isInterface, name, methodDescriptor) => {
                                        if (!methodDescriptor.returnType.isVoidType && !methodDescriptor.returnType.isBaseType) {
                                            // don't log if return value POP'd immediately
                                            val nextInstPC = code.pcOfNextInstruction(pc)
                                            val nextInst = code.find(_.pc == nextInstPC)
                                            nextInst match {
                                                case Some(PCAndInstruction(_, POP)) =>
                                                case Some(_)                        => insertDefsiteLog = true
                                                case None                           =>
                                            }
                                        }
                                    }
                                    case _ => {

                                    }
                                }
                                if (insertDefsiteLog) {
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
            if (hasMain) testCases add (cf.thisType.toJava, newRawCF)
        }
      fw.write("</methods>\n")
      fw.write("<events>\n")
        fw.close()
      val ContainerClassType = ObjectType("org/opalj/fpcf/fixtures/xl/js/testpts/SimpleContainerClass")
      val ContainerClassTypeFile = PTSTracerInstrumentation.readFile(outputClassPath + "/org/opalj/fpcf/fixtures/xl/js/testpts/SimpleContainerClass.class")

      for ((className, code) <- testCases) {

            // cannot use inmemoryclassloader, because scriptengine will still use
            val cl = new InMemoryClassLoader(Map(
                (className, code),
                (PTSLoggerType.toJava, ptsClassFile),
                (ContainerClassType.toJava, ContainerClassTypeFile)
            ), parent = classloaderScriptEngineJars)
            Thread.currentThread().setContextClassLoader(cl);

            val newClass = cl.findClass(className)
            println(newClass)

            //val instance = newClass.getDeclaredConstructor().newInstance()
            val main = newClass.getMethod("main", (Array[String]().getClass()))
            if (main != null) {
                try {
                    main.invoke(null, Array[String]())
                } catch {
                    case e: Exception => e.printStackTrace()
                }
            }
        }

    }
    def readFile(path: String) = {
        Files.readAllBytes(Paths.get(path))
    }

//  def isLoadRefType[T: TypeTag](load: LDC[T]): Boolean = {
//    typeOf[T] match {
//      case t if t =:= typeOf[ReferenceType] => true
//      case _ => false
//    }
//  }

}