/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import java.util.concurrent.ConcurrentHashMap

import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.da.ClassFile

/**
 * Groups test case features that perform a polymorhpic method calls that are related to Java 8
 * interfaces. I.e., method calls to an interface's default method.
 *
 * @note The features represent the __Java8PolymorphicCalls__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class Java8PolymorphicCalls(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    case class CacheKey(otID: Int, method: String, opcode: Int)
    val relInvokeCache = new ConcurrentHashMap[CacheKey, Boolean]()

    override def featureIDs: Seq[String] = {
        Seq( /* IDM = interface default method */
            "J8PC1", /* 0 --- call on interface which must be resolved to an IDM */
            "J8PC2", /* 1 --- call on interface (with IDM) that must not be resolved to it */
            "J8PC3", /* 2 --- call on class which transitively calls an method that potentially could target an IDM */
            "J8PC4", /* 3 --- call on class type which must be resolved to an IDM */
            "J8PC5", /* 4 --- call that's dispatched to IDM where the class inherits from multiple interfaces with that idm (sig. wise) */
            "J8PC6" /* 5 --- call to static interface method */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        val instructionsLocations = Array.fill(6)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            callerType = classFile.thisType
            method @ MethodWithBody(body) ← classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation ← body collect {
                case iv: INVOKEVIRTUAL if isPotentialCallOnDefaultMethod(iv, project)   ⇒ iv
                case ii: INVOKEINTERFACE if isPotentialCallOnDefaultMethod(ii, project) ⇒ ii
                case is: INVOKESTATIC if is.isInterface                                 ⇒ is
            }
        } {
            val pc = pcAndInvocation.pc
            val invokeKind = pcAndInvocation.value

            val l = InstructionLocation(methodLocation, pc)

            val kindID = invokeKind match {
                case ii @ INVOKEINTERFACE(dc, name, md) ⇒ {
                    val subtypes = project.classHierarchy.allSubtypes(dc.asObjectType, false)
                    val hasDefaultMethodTarget = subtypes.exists { ot ⇒
                        val target = project.instanceCall(callerType, ot, name, md)
                        if (target.hasValue) {
                            val definingClass = target.value.asVirtualMethod.classType.asObjectType
                            project.classFile(definingClass).exists(_.isInterfaceDeclaration)
                        } else
                            false
                    }

                    if (hasDefaultMethodTarget)
                        0 /* a default method can be a target */
                    else {
                        1 /* interface has default method, but it's always overridden */
                    }
                }
                case iv @ INVOKEVIRTUAL(dc, name, md) ⇒ {
                    val subtypes = project.classHierarchy.allSubtypes(dc.asObjectType, true)
                    var subtypeWithMultipleInterfaces = false
                    val hasDefaultMethodTarget = subtypes.exists { ot ⇒
                        val target = project.instanceCall(callerType, ot, name, md)
                        if (target.hasValue) {
                            val definingClass = target.value.asVirtualMethod.classType.asObjectType
                            val isIDM = project.classFile(definingClass).exists(_.isInterfaceDeclaration)
                            if (isIDM) {
                                // if the method is resolved to an IDM we have to check whether there are multiple options
                                // in order to check the linearization order
                                val typeInheritMultipleIntWithSameIDM = project.classHierarchy.allSuperinterfacetypes(ot, false).count { it ⇒
                                    val cf = project.classFile(it)
                                    if (cf.nonEmpty) {
                                        val method = cf.get.findMethod(name, md)
                                        method.exists(_.body.nonEmpty)
                                    } else {
                                        false
                                    }
                                }
                                subtypeWithMultipleInterfaces |= typeInheritMultipleIntWithSameIDM > 1
                            }
                            isIDM
                        } else
                            false
                    }

                    if (hasDefaultMethodTarget) {
                        if (subtypeWithMultipleInterfaces)
                            4 /* the type inherits multiple interface with IDMs */
                        else
                            3 /* a default method can be a target */
                    } else {
                        2 /* interface has default method, but it's always overridden */
                    }

                }
                case _: INVOKESTATIC ⇒ 5 /* call of a static interface method */
            }

            if (kindID >= 0) {
                instructionsLocations(kindID) += l
            }
        }

        instructionsLocations;
    }

    // This method determines whether the called interface method might be dispatched to a default method.
    private[this] def isPotentialCallOnDefaultMethod[S](
        mii: MethodInvocationInstruction, project: Project[S]
    ): Boolean = {

        val t = mii.declaringClass
        if (!t.isObjectType)
            return false;

        val ot = t.asObjectType
        val methodName = mii.name
        val methodDescriptor = mii.methodDescriptor

        val invokeID = CacheKey(ot.id, methodDescriptor.toJava(methodName), mii.opcode)
        relInvokeCache.containsKey(invokeID)
        if (relInvokeCache.containsKey(invokeID))
            return relInvokeCache.get(invokeID);

        val ch = project.classHierarchy
        var relevantInterfaces = ch.allSuperinterfacetypes(ot, true)
        ch.allSubclassTypes(ot, false).foreach { st ⇒
            relevantInterfaces = relevantInterfaces ++ ch.allSuperinterfacetypes(st, false)
        }

        val isRelevant = relevantInterfaces.exists { si ⇒
            val cf = project.classFile(si)
            if (cf.isDefined) {
                val method = cf.get.findMethod(methodName, methodDescriptor)
                method.isDefined && method.get.body.isDefined && method.get.isPublic
            } else {
                false
            }
        }

        relInvokeCache.put(invokeID, isRelevant)
        isRelevant
    }
}