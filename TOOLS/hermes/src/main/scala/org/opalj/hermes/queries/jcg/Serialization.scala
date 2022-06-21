/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import org.opalj.value.ValueInformation
import org.opalj.da.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.MethodWithBody
import org.opalj.br.ReferenceType
import org.opalj.br.VoidType
import org.opalj.br.MethodDescriptor
import org.opalj.br.IntegerType
import org.opalj.br.ClassHierarchy
import org.opalj.br.MethodCallMethodHandle
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.MethodDescriptor.WriteObjectDescriptor
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.Instruction
import org.opalj.br.MethodDescriptor.ReadObjectDescriptor
import org.opalj.br.MethodDescriptor.JustReturnsObject
import org.opalj.tac.LazyTACUsingAIKey
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.DUVar
import org.opalj.tac.Assignment
import org.opalj.tac.Stmt
import org.opalj.tac.Checkcast
import org.opalj.tac.ExprStmt
import org.opalj.tac.InvokedynamicFunctionCall

import scala.collection.immutable.ArraySeq

/**
 * Groups test case features that perform serialization.
 *
 * @note The features represent the __Serialization__ test cases from the Call Graph Test Project
 *       (JCG).
 *
 * @author Dominik Helm
 */
class Serialization(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    // TODO Add Serialization.md from JCG to resources once it is fixed

    type V = DUVar[ValueInformation]

    // required types and descriptors
    val OOS = ObjectType("java/io/ObjectOutputStream")
    val OIS = ObjectType("java/io/ObjectInputStream")
    val OOutput = ObjectType("java/io/")

    val OOSwriteObject = MethodDescriptor.JustTakes(ObjectType.Object)
    val OISregisterValidation = MethodDescriptor(
        ArraySeq(ObjectType("java/io/ObjectInputValidation"), IntegerType),
        VoidType
    )
    val writeExternal = MethodDescriptor.JustTakes(ObjectType("java/io/ObjectOutput"))
    val readExternal = MethodDescriptor.JustTakes(ObjectType("java/io/ObjectInput"))

    override def featureIDs: Seq[String] = {
        Seq(
            "Ser1", /* 0 --- Trivial writeObject */
            "Ser2", /* 1 --- Local non-trivial writeObject */
            "Ser3", /* 2 --- Non-local writeObject */
            "Ser4", /* 3 --- readObject without cast */
            "Ser5", /* 4 --- readObject with casts */
            "Ser6", /* 5 --- writeReplace */
            "Ser7", /* 6 --- readResolve */
            "Ser8", /* 7 --- registerValidation */
            "Ser9", /* 8 --- general deserialization of Serializiable (not Externalizable) classes */
            "ExtSer1", /* 9 --- writeExternal */
            "ExtSer2+ExtSer3", /* 10 -- readExternal & general deserialization of Externalizable classes */
            "SerLam1+SerLam2" /* 11 --- (de-)serialization of Lambdas */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        implicit val locations: Array[LocationsContainer[S]] =
            Array.fill(featureIDs.size)(new LocationsContainer[S])

        val tacai = project.get(LazyTACUsingAIKey)

        implicit val p: Project[_] = project
        implicit val classHierarchy: ClassHierarchy = project.classHierarchy

        val serializableTypes: Set[ObjectType] =
            classHierarchy.allSubtypes(ObjectType.Serializable, reflexive = false)

        val externalizableTypes: Set[ObjectType] =
            classHierarchy.allSubtypes(ObjectType.Externalizable, reflexive = false)

        val notExternalizableTypes = serializableTypes diff externalizableTypes

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) <- classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation <- body collect ({
                case i @ INVOKEVIRTUAL(declClass, "writeObject", OOSwriteObject) if classHierarchy.isSubtypeOf(declClass, OOS)               => i
                case i @ INVOKEVIRTUAL(declClass, "readObject", JustReturnsObject) if classHierarchy.isSubtypeOf(declClass, OIS)             => i
                case i @ INVOKEVIRTUAL(declClass, "registerValidation", OISregisterValidation) if classHierarchy.isSubtypeOf(declClass, OIS) => i
            }: PartialFunction[Instruction, INVOKEVIRTUAL])
            tac = tacai(method)
        } {
            val pc = pcAndInvocation.pc
            val l = InstructionLocation(methodLocation, pc)

            val invocation = tac.stmts(tac.properStmtIndexForPC(pc))

            if (invocation.astID == VirtualMethodCall.ASTID) {
                if (invocation.asVirtualMethodCall.name == "writeObject")
                    handleWriteObject(
                        invocation.asVirtualMethodCall,
                        l,
                        tac.stmts,
                        serializableTypes,
                        externalizableTypes,
                        notExternalizableTypes
                    )
                else // registerValidation
                    locations(7) += l
            } else {
                invocation.astID match {
                    case Assignment.ASTID =>
                        handleReadObject(
                            invocation.asAssignment,
                            l,
                            tac.stmts,
                            serializableTypes,
                            externalizableTypes,
                            notExternalizableTypes
                        )
                    case ExprStmt.ASTID =>
                        handleReadObjectWithoutCasts(
                            l,
                            serializableTypes,
                            externalizableTypes,
                            notExternalizableTypes
                        )
                }
            }
        }

        ArraySeq.unsafeWrapArray(locations)
    }

    def handleWriteObject[S](
        invocation:             VirtualMethodCall[V],
        l:                      Location[S],
        stmts:                  Array[Stmt[V]],
        serializableTypes:      Set[ObjectType],
        externalizableTypes:    Set[ObjectType],
        notExternalizableTypes: Set[ObjectType]
    )(
        implicit
        locations:      Array[LocationsContainer[S]],
        project:        SomeProject,
        classHierarchy: ClassHierarchy
    ): Unit = {
        val paramVar = invocation.params.head.asVar
        val param = paramVar.value.asReferenceValue

        if (paramVar.definedBy.exists { defSite =>
            if (defSite >= 0) {
                val expr = stmts(defSite).asAssignment.expr
                expr.astID == InvokedynamicFunctionCall.ASTID && isLambdaMetafactoryCall(expr.asInvokedynamicFunctionCall)
            } else false
        }) {
            locations(11) += l
        } else if (param.isPrecise) {
            if (param.isNull.isNo &&
                hasMethod(param.asReferenceType, "writeObject", WriteObjectDescriptor) &&
                !classHierarchy.isSubtypeOf(param.asReferenceType, ObjectType.Externalizable))
                locations(0) += l
            else if (param.isNull.isNo &&
                classHierarchy.isSubtypeOf(param.asReferenceType, ObjectType.Externalizable))
                locations(9) += l

            if (param.isNull.isNo &&
                hasMethod(param.asReferenceType, "writeReplace", JustReturnsObject))
                locations(5) += l

        } else if (param.allValues.forall(_.isPrecise)) {
            if (param.allValues.exists { pt =>
                pt.isNull.isNo &&
                    hasMethod(pt.asReferenceType, "writeObject", WriteObjectDescriptor) &&
                    !classHierarchy.isSubtypeOf(pt.asReferenceType, ObjectType.Externalizable)
            })
                locations(1) += l

            if (param.allValues.exists { pt =>
                pt.isNull.isNo &&
                    hasMethod(pt.asReferenceType, "writeReplace", JustReturnsObject)
            })
                locations(5) += l

            if (param.allValues.exists { pt =>
                pt.isNull.isNo &&
                    classHierarchy.isSubtypeOf(pt.asReferenceType, ObjectType.Externalizable)
            })
                locations(9) += l
        } else {
            if (notExternalizableTypes.exists { subtype =>
                hasMethod(subtype, "writeObject", WriteObjectDescriptor)
            })
                locations(2) += l

            if (serializableTypes.exists { subtype =>
                hasMethod(subtype, "writeReplace", JustReturnsObject)
            })
                locations(5) += l

            if (externalizableTypes.nonEmpty)
                locations(9) += l
        }
    }

    def isLambdaMetafactoryCall(invokedynamic: InvokedynamicFunctionCall[V]): Boolean = {
        val handle = invokedynamic.bootstrapMethod.handle
        handle.isInstanceOf[MethodCallMethodHandle] &&
            handle.asInstanceOf[MethodCallMethodHandle].receiverType == ObjectType.LambdaMetafactory
    }

    def handleReadObject[S](
        invocation:             Assignment[V],
        l:                      Location[S],
        stmts:                  Array[Stmt[V]],
        serializableTypes:      Set[ObjectType],
        externalizableTypes:    Set[ObjectType],
        notExternalizableTypes: Set[ObjectType]
    )(
        implicit
        locations:      Array[LocationsContainer[S]],
        project:        SomeProject,
        classHierarchy: ClassHierarchy
    ): Unit = {
        val ret = invocation.targetVar

        val castTypes = ret.usedBy.iterator.collect {
            case index if stmts(index).astID == Checkcast.ASTID => stmts(index).asCheckcast.cmpTpe
        }.toSeq

        if (castTypes.isEmpty) {
            handleReadObjectWithoutCasts(
                l,
                serializableTypes,
                externalizableTypes,
                notExternalizableTypes
            )
        } else {
            if (castTypes.exists { cType =>
                hasMethod(cType, "readObject", ReadObjectDescriptor) &&
                    !classHierarchy.isSubtypeOf(cType, ObjectType.Externalizable)
            })
                locations(4) += l

            if (castTypes.exists { cType =>
                hasMethod(cType, "readResolve", JustReturnsObject)
            })
                locations(6) += l

            if (castTypes.exists { cType =>
                !classHierarchy.isSubtypeOf(cType, ObjectType.Externalizable)
            })
                locations(8) += l

            if (castTypes.exists { cType =>
                classHierarchy.isSubtypeOf(cType, ObjectType.Externalizable) ||
                    cType.isObjectType &&
                    classHierarchy.allSubclassTypes(cType.asObjectType, reflexive = false).exists {
                        classHierarchy.isSubtypeOf(_, ObjectType.Externalizable)
                    }
            }) {
                locations(10) += l
            }
        }
    }

    def handleReadObjectWithoutCasts[S](
        l:                      Location[S],
        serializableTypes:      Set[ObjectType],
        externalizableTypes:    Set[ObjectType],
        notExternalizableTypes: Set[ObjectType]
    )(
        implicit
        locations: Array[LocationsContainer[S]],
        project:   SomeProject
    ): Unit = {
        if (notExternalizableTypes.exists { subtype =>
            hasMethod(subtype, "readObject", ReadObjectDescriptor)
        })
            locations(3) += l

        if (serializableTypes.exists { subtype =>
            hasMethod(subtype, "readResolve", JustReturnsObject)
        })
            locations(6) += l

        if (notExternalizableTypes.nonEmpty)
            locations(8) += l

        if (externalizableTypes.nonEmpty) {
            locations(10) += l
            locations(11) += l
        }
    }

    def hasMethod(
        refType:    ReferenceType,
        name:       String,
        descriptor: MethodDescriptor
    )(implicit p: SomeProject): Boolean = {
        if (refType.isObjectType) {
            val objectType = refType.asObjectType
            val classFileO = p.classFile(objectType)
            classFileO.isDefined && classFileO.get.findMethod(name, descriptor).isDefined ||
                p.instanceCall(objectType, objectType, name, descriptor).hasValue
        } else false
    }
}
