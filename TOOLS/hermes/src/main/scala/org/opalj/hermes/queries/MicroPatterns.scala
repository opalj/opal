/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.Field
import org.opalj.br.PC
import org.opalj.br.ObjectType
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ArrayLoadInstruction
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.LoadConstantInstruction
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.ReturnValueInstruction
import org.opalj.br.instructions.VirtualMethodInvocationInstruction
import org.opalj.ai.BaseAI
import org.opalj.ai.CorrelationalDomain
import org.opalj.ai.domain
import org.opalj.br.LongType

/**
 * Counts which kinds of micro patterns are actually available.
 *
 * @author Leonid Glanz
 */
class MicroPatterns(implicit hermes: HermesConfig) extends FeatureQuery {

    override val featureIDs: List[String] = {
        List(
            /*0*/ "Designator",
            /*1*/ "Taxonomy",
            /*2*/ "Joiner",
            /*3*/ "Pool",
            /*4*/ "Function Pointer",
            /*5*/ "Function Object",
            /*6*/ "Cobol Like",
            /*7*/ "Stateless",
            /*8*/ "Common State",
            /*9*/ "Immutable",
            /*10*/ "Restricted Creation",
            /*11*/ "Sampler",
            /*12*/ "Box",
            /*13*/ "Compound Box",
            /*14*/ "Canopy",
            /*15*/ "Record",
            /*16*/ "Data Manager",
            /*17*/ "Sink",
            /*18*/ "Outline",
            /*19*/ "Trait",
            /*20*/ "State Machine",
            /*21*/ "Pure Type",
            /*22*/ "Augmented Type",
            /*23*/ "Pseudo Class",
            /*24*/ "Implementor",
            /*25*/ "Overrider",
            /*26*/ "Extender"
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(org.opalj.da.ClassFile, S)]
    ): IterableOnce[Feature[S]] = {
        implicit val theProject = project

        val fa = project.get(FieldAccessInformationKey)

        val microPatternLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])
        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
        } {
            val location = ClassFileLocation(source, classFile)

            if (isDesignator(classFile)) microPatternLocations(0) += location
            if (isTaxonomy(classFile)) microPatternLocations(1) += location
            if (isJoiner(classFile)) microPatternLocations(2) += location
            if (isPool(classFile)) microPatternLocations(3) += location
            if (isFunctionPointer(classFile)) microPatternLocations(4) += location
            if (isFunctionObject(classFile)) microPatternLocations(5) += location
            if (isCobolLike(classFile)) microPatternLocations(6) += location
            if (isStateless(classFile)) microPatternLocations(7) += location
            if (isCommonState(classFile)) microPatternLocations(8) += location
            if (isImmutable(classFile, fa)) microPatternLocations(9) += location
            if (isRestrictedCreation(classFile)) microPatternLocations(10) += location
            if (isSampler(classFile)) microPatternLocations(11) += location
            if (isBox(classFile, fa)) microPatternLocations(12) += location
            if (isCompoundBox(classFile, fa)) microPatternLocations(13) += location
            if (isCanopy(classFile, fa)) microPatternLocations(14) += location
            if (isRecord(classFile)) microPatternLocations(15) += location
            if (isDataManager(classFile)) microPatternLocations(16) += location
            if (isSink(classFile)) microPatternLocations(17) += location
            if (isOutline(classFile)) microPatternLocations(18) += location
            if (isTrait(classFile)) microPatternLocations(19) += location
            if (isStateMachine(classFile)) microPatternLocations(20) += location
            if (isPureType(classFile)) microPatternLocations(21) += location
            if (isAugmentedType(classFile)) microPatternLocations(22) += location
            if (isPseudoClass(classFile)) microPatternLocations(23) += location
            if (isImplementor(classFile, project)) microPatternLocations(24) += location
            if (isOverrider(classFile, project)) microPatternLocations(25) += location
            if (isExtender(classFile)) microPatternLocations(26) += location
        }
        for { (featureID, featureIDIndex) <- featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, microPatternLocations(featureIDIndex))
        }
    }

    def hasExplicitSuperType(cl: ClassFile): Boolean = cl.superclassType.exists(_ ne ObjectType.Object)

    /**
     * [From the paper] Thus, a Designator micro pattern is an interface which does not
     *  declare any methods, does not define any static fields or methods, and does not
     *  inherit such members from any of its superinterfaces.
     *
     *  A class can also be Designator if its definition, as well as the definitions of
     *  all of its ancestors (other than Object), are empty.
     *
     * A class can also be Designator if its definition, as well as the definitions of
     * all of its ancestors (other than Object), are empty.
     */
    def isDesignator(cl: ClassFile)(implicit project: SomeProject): Boolean = {
        if (cl.thisType == ObjectType.Object)
            return false;

        // IMPROVE Cache the results of super interfaces to avoid recomputations or compute it top-down starting with top-level interfaces.

        def isDesignatorType(ot: ObjectType): Boolean = {
            project.classFile(ot).exists(this.isDesignator)
        }

        cl.fields.isEmpty && {
            if (cl.isInterfaceDeclaration)
                cl.methods.isEmpty && cl.interfaceTypes.forall(isDesignatorType)
            else
                // we have to filter the always present default constructor (compiler generated,
                // if not user defined)
                cl.methods.size == 1 && cl.methods.head.descriptor == MethodDescriptor.NoArgsAndReturnVoid &&
                    cl.interfaceTypes.forall(isDesignatorType) &&
                    isDesignatorType(cl.superclassType.get)
        }

    }

    /**
     * [From the paper:] An empty interface which extends a single interface is
     * called a Taxonomy, since it is included, in the subtyping sense, in its parent,
     * but otherwise identical to it.
     *
     * There are also classes which are Taxonomy. Such a class must similarly be empty,
     * i.e., add no fields nor methods to its parent. Since constructors are not inherited,
     * an empty class may contain constructors. A Taxonomy class may not implement any interfaces.
     */
    def isTaxonomy(cl: ClassFile): Boolean = {
        cl.fields.isEmpty && {
            if (cl.isInterfaceDeclaration) {
                cl.interfaceTypes.size == 1 && cl.methods.isEmpty
            } else {
                cl.thisType != ObjectType.Object /*this test is not necessary, but is fast */ &&
                    cl.interfaceTypes.isEmpty &&
                    cl.methods.forall(_.isInitializer)
            }
        }
    }

    /**
     * [From the paper:] An empty interface which extends more than one interface is called a
     * Joiner, since in effect, it joins together the sets of members of its parents.
     *
     * An empty class which implements one or more interfaces is also a Joiner.
     * ''Here, empty means that the we can have constructors and (optionally) a serialVersionUID
     * field.''
     */
    def isJoiner(cl: ClassFile): Boolean = {
        if (cl.isInterfaceDeclaration) {
            cl.interfaceTypes.size > 1 && cl.fields.isEmpty && cl.methods.isEmpty
        } else {
            cl.interfaceTypes.nonEmpty &&
                cl.methods.forall(m => m.isInitializer) && (
                    cl.fields match {
                        case Seq() | Seq(Field(_, "serialVersionUID", LongType)) => true
                        case _                                                   => false
                    }
                )
        }
    }

    /**
     * [From the paper:] The most degenerate classes are those which have neither state
     * nor behavior. Such a class is distinguished by the requirement that it declares
     * no instance fields. Moreover, all of its declared static fields must be final.
     * Another requirement is that the class has no methods (other than those inherited
     * from Object, or automatically generated constructors).
     */
    def isPool(cl: ClassFile): Boolean = {
        cl.fields.nonEmpty && cl.fields.forall(f => f.isFinal && f.isStatic) &&
            // We also (have to) accept a static initializer, because that one will
            // initialize the final static fields!
            cl.methods.forall(m => m.isInitializer && m.descriptor.parametersCount == 0)
    }

    private final val javaLangObjectMethods: Set[String] = Set(
        "hashCode", "equals",
        "notify", "notifyAll", "wait",
        "getClass", "clone", "toString", "finalize"
    )

    def isObjectMethod(method: Method): Boolean = {
        // TODO This just checks the name, why don't we check the full signature?
        javaLangObjectMethods.contains(method.name)
    }

    def isFunctionPointer(cl: ClassFile): Boolean = {
        !cl.isInterfaceDeclaration && !cl.isAbstract && cl.methods.count { m =>
            !isInitMethod(m)
        } == 1 && cl.methods.count { m =>
            m.isPublic
        } == 1 && !cl.methods.exists(m => m.isStatic &&
            !m.isStaticInitializer) && cl.fields.isEmpty
    }

    def isFunctionObject(cl: ClassFile): Boolean = {
        cl.fields.nonEmpty &&
            cl.fields.forall { f => !f.isStatic } &&
            cl.methods.count { m => !isInitMethod(m) && m.isPublic } == 1 &&
            !cl.methods.filter(m => !isInitMethod(m)).exists(m => m.isStatic)
    }

    def isCobolLike(cl: ClassFile): Boolean = {
        !cl.methods.exists { m =>
            !isInitMethod(m)
        } && cl.methods.count { m =>
            !isInitMethod(m) &&
                m.isStatic
        } == 1 &&
            !cl.fields.exists { f => !f.isStatic } && cl.fields.exists(f => f.isStatic)
    }

    def isStateless(cl: ClassFile): Boolean = {
        !cl.isInterfaceDeclaration && !cl.isAbstract &&
            !cl.fields.exists { f => !(f.isFinal && f.isStatic) } &&
            cl.methods.count(m => !isInitMethod(m) && !isObjectMethod(m)) > 1
    }

    def isCommonState(cl: ClassFile): Boolean = {
        !cl.isInterfaceDeclaration &&
            cl.fields.nonEmpty && cl.fields.forall { f => f.isStatic } &&
            cl.fields.exists { f => !f.isFinal }
    }

    def isImmutable(cl: ClassFile, fa: FieldAccessInformation): Boolean = {
        !cl.isInterfaceDeclaration &&
            !cl.isAbstract && cl.fields.count { f => !f.isStatic } > 1 &&
            cl.fields.forall(f => f.isPrivate && !f.isStatic) &&
            cl.fields.forall { f =>
                !fa.allWriteAccesses.contains(f) ||
                    fa.allWriteAccesses(f).forall(p => isInitMethod(p._1))
            }
    }

    def isRestrictedCreation(cl: ClassFile): Boolean = {
        !cl.isInterfaceDeclaration &&
            cl.fields.exists { f => f.isStatic && !f.isFinal && f.fieldType == cl.thisType } &&
            cl.methods.filter { m => m.isConstructor }.forall { m => m.isPrivate }
    }

    def isSampler(cl: ClassFile): Boolean = {
        !cl.isInterfaceDeclaration &&
            cl.methods.filter { m => m.isConstructor }.exists { m => m.isPublic } &&
            cl.fields.exists { f =>
                f.isStatic &&
                    f.fieldType.toJava.equals(cl.thisType.toJava)
            }
    }

    def isBox(cl: ClassFile, fa: FieldAccessInformation): Boolean = {
        !cl.isInterfaceDeclaration &&
            cl.fields.count { f => !f.isStatic } == 1 &&
            cl.fields.count { f => !f.isFinal } == 1 &&
            cl.fields.exists(f => fa.allWriteAccesses.contains(f) &&
                fa.allWriteAccesses(f).exists(t => cl.methods.contains(t._1)))
    }

    def isCompoundBox(cl: ClassFile, fa: FieldAccessInformation): Boolean = {
        !cl.isInterfaceDeclaration &&
            cl.fields.count(f => f.fieldType.isReferenceType && !f.isStatic &&
                !f.isFinal && fa.allWriteAccesses.contains(f) &&
                fa.allWriteAccesses(f).exists(t => cl.methods.contains(t._1))) == 1 &&
            cl.fields.count(f => !f.isStatic && !f.fieldType.isReferenceType) + 1 == cl.fields.size
    }

    def isCanopy(cl: ClassFile, fa: FieldAccessInformation): Boolean = {
        !cl.isInterfaceDeclaration &&
            cl.fields.count { f => !f.isStatic } == 1 &&
            cl.fields.count { f => !f.isStatic && !f.isPublic } == 1 &&
            cl.fields.exists { f =>
                !f.isStatic && fa.allWriteAccesses.contains(f) &&
                    fa.allWriteAccesses(f).forall(p => isInitMethod(p._1))
            }
    }

    def isRecord(cl: ClassFile): Boolean = {
        !cl.isInterfaceDeclaration && cl.fields.nonEmpty &&
            cl.fields.forall { f => f.isPublic } && cl.fields.exists(f => !f.isStatic) && cl.methods.forall(m => isInitMethod(m) || isObjectMethod(m))
    }

    def isSink(cl: ClassFile): Boolean = {
        !cl.isInterfaceDeclaration &&
            cl.methods.exists { m => !isInitMethod(m) } &&
            cl.methods.forall { m =>
                m.body.isEmpty ||
                    m.body.get.instructions.filter(i => i.isInstanceOf[MethodInvocationInstruction]).forall { i =>
                        !i.asInstanceOf[MethodInvocationInstruction].declaringClass.isObjectType ||
                            i.asInstanceOf[MethodInvocationInstruction].declaringClass.asObjectType.equals(cl.thisType)
                    }
            }
    }

    def isOutline(cl: ClassFile): Boolean = {
        !cl.isInterfaceDeclaration && cl.isAbstract && cl.methods.count { m =>
            m.body.isDefined &&
                m.body.get.instructions.exists { i =>
                    i.isInstanceOf[VirtualMethodInvocationInstruction] &&
                        i.asInstanceOf[VirtualMethodInvocationInstruction].declaringClass.equals(cl.thisType) &&
                        cl.methods.filter { x => x.isAbstract }.exists { x =>
                            x.name.equals(i.asInstanceOf[VirtualMethodInvocationInstruction].name) &&
                                x.descriptor.equals(i.asInstanceOf[VirtualMethodInvocationInstruction].methodDescriptor)
                        }
                }
        } > 1
    }

    def isTrait(cl: ClassFile): Boolean = {
        !cl.isInterfaceDeclaration && cl.isAbstract &&
            cl.fields.isEmpty &&
            cl.methods.exists(m => m.isAbstract)
    }

    def isStateMachine(cl: ClassFile): Boolean = {
        cl.methods.count(m => !isInitMethod(m)) > 1 &&
            cl.fields.nonEmpty &&
            cl.methods.forall { m => m.descriptor.parametersCount == 0 }
    }

    def isPureType(cl: ClassFile): Boolean = {
        (
            (
                cl.isAbstract && cl.methods.nonEmpty &&
                cl.methods.forall { m => m.isAbstract && !m.isStatic }
            ) ||
                (
                    cl.isInterfaceDeclaration && cl.methods.nonEmpty && cl.methods.forall { m => !m.isStatic }
                )
        ) &&
                    cl.fields.isEmpty
    }

    def isAugmentedType(cl: ClassFile): Boolean = {
        !cl.isInterfaceDeclaration && cl.isAbstract &&
            cl.methods.forall { m => m.isAbstract } && cl.fields.size >= 3 &&
            cl.fields.forall { f => f.isFinal && f.isStatic } &&
            cl.fields.map { f => f.fieldType }.toSet.size == 1
    }

    def isPseudoClass(cl: ClassFile): Boolean = {
        !cl.isInterfaceDeclaration &&
            cl.fields.forall { f => f.isStatic } && cl.methods.nonEmpty &&
            cl.methods.forall { m => m.isAbstract || m.isStatic }
    }

    def isImplementor[S](cl: ClassFile, theProject: Project[S]): Boolean = {
        !cl.isInterfaceDeclaration &&
            !cl.isAbstract && cl.methods.exists { m =>
                m.isPublic &&
                    !isInitMethod(m)
            } && cl.methods.forall { m =>
                isInitMethod(m) || !m.isPublic ||
                    (theProject.resolveMethodReference(cl.thisType, m.name, m.descriptor) match {
                        case Some(a) => (a.isAbstract || a.body.isEmpty) && (
                            (hasExplicitSuperType(cl) && a.classFile != null &&
                                a.classFile.thisType == cl.superclassType.get) ||
                                cl.interfaceTypes.exists(it => a.classFile != null && a.classFile.thisType == it)
                        )
                        case None => false
                    })
            }
    }

    def isInitMethod(method: Method): Boolean = method.isInitializer

    def isOverrider[S](cl: ClassFile, theProject: Project[S]): Boolean = {
        !cl.isInterfaceDeclaration && !cl.isAbstract &&
            cl.methods.exists { m =>
                !isInitMethod(m)
            } && cl.methods.forall { m =>
                m.isInitializer ||
                    (theProject.resolveMethodReference(cl.thisType, m.name, m.descriptor) match {
                        case Some(a) => (!a.isAbstract && a.body.isDefined && m.body.nonEmpty) && (
                            (hasExplicitSuperType(cl) && a.classFile != null &&
                                a.classFile.thisType == cl.superclassType.get) ||
                                cl.interfaceTypes.exists(it => a.classFile != null && a.classFile.thisType == it)
                        )
                        case None => false
                    })
            }
    }

    def isExtender[S](cl: ClassFile)(implicit theProject: Project[S]): Boolean = {
        !cl.isInterfaceDeclaration &&
            cl.methods.exists(m => !isInitMethod(m)) && hasExplicitSuperType(cl) && cl.methods.forall { m =>
                isInitMethod(m) ||
                    theProject.resolveMethodReference(cl.thisType, m.name, m.descriptor).isEmpty
            }
    }

    def isDataManager[S](cl: ClassFile)(implicit theProject: Project[S]): Boolean = {
        !cl.isInterfaceDeclaration &&
            !cl.isAbstract &&
            cl.fields.nonEmpty &&
            cl.methods.count(m => !isInitMethod(m) && !isObjectMethod(m)) > 1 &&
            cl.methods.filter(m => !isInitMethod(m) && !isObjectMethod(m)).forall { m =>
                isSetter(m) || isGetter(m)
            }
    }

    class AnalysisDomain[S](val project: Project[S], val method: Method)
        extends CorrelationalDomain
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l0.TypeLevelDynamicLoads
        with domain.l1.DefaultReferenceValuesBinding
        with domain.l1.DefaultIntegerRangeValues
        with domain.l1.DefaultLongValues
        with domain.l1.ConcretePrimitiveValuesConversions
        with domain.l1.LongValuesShiftOperators
        with domain.TheProject
        with domain.TheMethod
        with domain.RecordDefUse

    def isGetter[S](method: Method)(implicit theProject: Project[S]): Boolean = {
        if (!method.isPublic || method.returnType.isVoidType || method.body.isEmpty ||
            !method.body.get.instructions.exists { i => i.isInstanceOf[FieldReadAccess] }) {
            return false
        }
        val instructions = method.body.get.foldLeft(Map.empty[PC, Instruction])((m, pc, i) => m + ((pc, i)))
        val result = BaseAI(method, new AnalysisDomain(theProject, method))
        val returns = instructions.filter(i => i._2.isInstanceOf[ReturnValueInstruction])

        returns.forall(r => result.domain.operandOrigin(r._1, 0).forall { u =>
            instructions.contains(u) && (instructions(u).isInstanceOf[FieldReadAccess] ||
                instructions(u).isInstanceOf[ArrayLoadInstruction] ||
                instructions(u).isInstanceOf[LoadConstantInstruction[_]])
        })
    }

    def isSetter[S](method: Method)(implicit theProject: Project[S]): Boolean = {
        if (!method.isPublic || !method.returnType.isVoidType ||
            method.descriptor.parametersCount == 0 || method.body.isEmpty ||
            (method.body.isDefined && method.body.isEmpty) ||
            !method.body.get.instructions.exists { i => i.isInstanceOf[FieldWriteAccess] }) {
            return false
        }
        val instructions = method.body.get.foldLeft(Map.empty[PC, Instruction])((m, pc, i) => m + ((pc, i)))
        val result = BaseAI(method, new AnalysisDomain(theProject, method))
        val puts = instructions.filter(i => i._2.isInstanceOf[FieldWriteAccess])

        puts.forall(p => (p._2.isInstanceOf[PUTSTATIC] &&
            result.domain.operandOrigin(p._1, 0).forall { x =>
                x < 0 || (instructions.contains(x) &&
                    instructions(x).isInstanceOf[LoadConstantInstruction[_]])
            }) ||
            (p._2.isInstanceOf[PUTFIELD] &&
                result.domain.operandOrigin(p._1, 1).forall { x =>
                    x < 0 || (instructions.contains(x) &&
                        instructions(x).isInstanceOf[LoadConstantInstruction[_]])
                }))
    }

}
