import java.net.URL

import org.opalj.br.ClassFile
import org.opalj.br.Field
import org.opalj.br.ObjectType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS

/* LATTICE (ClassImmutability) */

sealed trait ClassImmutabilityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = ClassImmutability
}

sealed trait ClassImmutability extends ClassImmutabilityPropertyMetaInformation with OrderedProperty {
    def meet(other: ClassImmutability): ClassImmutability = {
        (this, other) match {
            case (TransitivelyImmutableClass, _)       => other
            case (_, TransitivelyImmutableClass)       => this
            case (MutableClass, _) | (_, MutableClass) => MutableClass
            case (_, _)                                => this
        }
    }

    override def checkIsEqualOrBetterThan(e: Entity, other: ClassImmutability): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }

    final def key: PropertyKey[ClassImmutability] = ClassImmutability.key
}

case object TransitivelyImmutableClass extends ClassImmutability

case object NonTransitivelyImmutableClass extends ClassImmutability

case object MutableClass extends ClassImmutability

object ClassImmutability extends ClassImmutabilityPropertyMetaInformation {
    final val key: PropertyKey[ClassImmutability] = PropertyKey.create(
        "ClassImmutability",
        MutableClass
    )
}

/* ANALYSIS */

class ClassImmutabilityAnalysis(val project: SomeProject) extends FPCFAnalysis {
    def analyzeClassImmutability(classFile: ClassFile): ProperPropertyComputationResult = {
        var immutability: ClassImmutability = TransitivelyImmutableClass
        var dependencies = Map.empty[Entity, EOptionP[Entity, Property]]

        def checkSuperclass(value: EOptionP[ClassFile, ClassImmutability]): Unit = {
            dependencies -= value.e
            if (value.hasUBP)
                immutability = immutability.meet(value.ub)
            if (value.isRefinable)
                dependencies += value.e -> value
        }

        val superclassType = classFile.superclassType

        if (superclassType.isDefined && superclassType.get != ObjectType.Object) {
            val superclass = project.classFile(superclassType.get)
            if (superclass.isEmpty)
                return Result(classFile, MutableClass)
            val superclassImmutability = propertyStore(superclass.get, ClassImmutability.key)
            checkSuperclass(superclassImmutability)
        }

        def checkField(value: EOptionP[Field, FieldImmutability]): Unit = {
            dependencies -= value.e
            if (value.hasUBP)
                value.ub match {
                    case TransitivelyImmutableField    => /* Nothing to do here */
                    case NonTransitivelyImmutableField => immutability = NonTransitivelyImmutableClass
                    case MutableField                  => immutability = MutableClass
                }
            if (value.isRefinable)
                dependencies += value.e -> value
        }

        val instanceFields = classFile.fields.filter(!_.isStatic)
        val fieldImmutabilities = propertyStore(instanceFields, FieldImmutability.key)
        fieldImmutabilities.foreach(checkField)

        def continuation(updatedValue: SomeEPS): ProperPropertyComputationResult = {
            updatedValue.e match {
                case _: ClassFile => checkSuperclass(updatedValue.asInstanceOf[EOptionP[ClassFile, ClassImmutability]])
                case _: Field     => checkField(updatedValue.asInstanceOf[EOptionP[Field, FieldImmutability]])
            }

            result()
        }

        def result(): ProperPropertyComputationResult = {
            if (dependencies.isEmpty || immutability == MutableClass)
                Result(classFile, immutability)
            else
                InterimResult.forUB(classFile, immutability, dependencies.valuesIterator.toSet, continuation)
        }

        result()
    }

    def lazilyAnalyzeClassImmutability(entity: Entity): ProperPropertyComputationResult = {
        entity match {
            case classfile: ClassFile => analyzeClassImmutability(classfile)
            case _                    => throw new IllegalArgumentException("Class Immutability Analysis can only process classfiles!")
        }
    }
}

/* SCHEDULERS */

trait ClassImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {
    def derivedProperty: PropertyBounds = PropertyBounds.ub(ClassImmutability)

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(ClassImmutability), PropertyBounds.ub(FieldImmutability))
}

object EagerClassImmutabilityAnalysis extends ClassImmutabilityAnalysisScheduler with BasicFPCFEagerAnalysisScheduler {
    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new ClassImmutabilityAnalysis(project)
        propertyStore.scheduleEagerComputationsForEntities(project.allClassFiles)(analysis.analyzeClassImmutability)
        analysis
    }
}

object LazyClassImmutabilityAnalysis extends ClassImmutabilityAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {
    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new ClassImmutabilityAnalysis(project)
        propertyStore.registerLazyPropertyComputation(ClassImmutability.key, analysis.lazilyAnalyzeClassImmutability)
        analysis
    }
}

/* RUNNER */

object ClassImmutabilityRunner extends ProjectAnalysisApplication {
    override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {
        val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(
            EagerClassImmutabilityAnalysis,
            LazyFieldImmutabilityAnalysis
        )

        val transitivelyImmutableClasses = propertyStore.finalEntities(TransitivelyImmutableClass).size
        val nonTransitivelyImmutableClasses = propertyStore.finalEntities(NonTransitivelyImmutableClass).size
        val mutableClasses = propertyStore.finalEntities(MutableClass).size

        BasicReport(
            "Results of class immutability analysis: \n"+
                s"Transitively Immutable classes:     $transitivelyImmutableClasses \n"+
                s"Non-Transitively Immutable classes: $nonTransitivelyImmutableClasses \n"+
                s"Mutable classes:                    $mutableClasses"
        )
    }
}

/* FieldImmutability Lattice & Analysis (Simplified) */

sealed trait FieldImmutabilityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = FieldImmutability
}

sealed trait FieldImmutability extends FieldImmutabilityPropertyMetaInformation with OrderedProperty {
    def meet(other: FieldImmutability): FieldImmutability = {
        (this, other) match {
            case (TransitivelyImmutableField, _)       => other
            case (_, TransitivelyImmutableField)       => this
            case (MutableField, _) | (_, MutableField) => MutableField
            case (_, _)                                => this
        }
    }

    override def checkIsEqualOrBetterThan(e: Entity, other: FieldImmutability): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }

    final def key: PropertyKey[FieldImmutability] = FieldImmutability.key
}

case object TransitivelyImmutableField extends FieldImmutability

case object NonTransitivelyImmutableField extends FieldImmutability

case object MutableField extends FieldImmutability

object FieldImmutability extends FieldImmutabilityPropertyMetaInformation {
    final val key: PropertyKey[FieldImmutability] = PropertyKey.create(
        "FieldImmutability",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case f: Field =>
                    if (f.isFinal) NonTransitivelyImmutableField else MutableField
                case x =>
                    throw new IllegalArgumentException(s"$x is not a Field")
            }
        }
    )
}

class FieldImmutabilityAnalysis(val project: SomeProject) extends FPCFAnalysis {
    def analyzeFieldImmutability(field: Field): ProperPropertyComputationResult = {
        val immutability =
            if (field.isFinal) field.fieldType match {
                case bt if bt.isBaseType =>
                    TransitivelyImmutableField
                case wt: ObjectType if ObjectType.isPrimitiveTypeWrapper(wt) =>
                    TransitivelyImmutableField
                case st if st == ObjectType.String =>
                    TransitivelyImmutableField
                case _ =>
                    NonTransitivelyImmutableField
            }
            else MutableField

        Result(field, immutability)
    }

    def lazilyAnalyzeFieldImmutability(entity: Entity): ProperPropertyComputationResult = {
        entity match {
            case field: Field => analyzeFieldImmutability(field)
            case _            => throw new IllegalArgumentException("Field Immutability Analysis can only process Fields!")
        }
    }
}

trait FieldImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {
    def derivedProperty: PropertyBounds = PropertyBounds.ub(FieldImmutability)

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(FieldImmutability), PropertyBounds.ub(FieldImmutability))
}

object EagerFieldImmutabilityAnalysis extends FieldImmutabilityAnalysisScheduler with BasicFPCFEagerAnalysisScheduler {
    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new FieldImmutabilityAnalysis(project)
        propertyStore.scheduleEagerComputationsForEntities(project.allFields)(analysis.analyzeFieldImmutability)
        analysis
    }
}

object LazyFieldImmutabilityAnalysis extends FieldImmutabilityAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {
    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new FieldImmutabilityAnalysis(project)
        propertyStore.registerLazyPropertyComputation(FieldImmutability.key, analysis.lazilyAnalyzeFieldImmutability)
        analysis
    }
}