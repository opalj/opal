/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.br.DeclaredMethod
import org.opalj.br.ElementReferenceType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results

/**
 * Introduces additional allocation sites for serialization methods.
 *
 * @author Dominik Helm
 */
abstract class SerializationAllocationsAnalysis(
    final val project: SomeProject
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis { self =>

    override final val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.ObjectInputStream,
        "",
        ObjectType.ObjectInputStream,
        "readObject",
        MethodDescriptor.JustReturnsObject
    )

    trait PointsToBase extends AbstractPointsToBasedAnalysis {
        override protected[this] type ElementType = self.ElementType
        override protected[this] type PointsToSet = self.PointsToSet
        override protected[this] type DependerType = self.DependerType

        override protected[this] val pointsToPropertyKey: PropertyKey[PointsToSet] =
            self.pointsToPropertyKey

        override protected[this] def emptyPointsToSet: PointsToSet = self.emptyPointsToSet

        override protected[this] def createPointsToSet(
            pc:            Int,
            callContext:   ContextType,
            allocatedType: ReferenceType,
            isConstant:    Boolean,
            isEmptyArray:  Boolean
        ): PointsToSet = {
            self.createPointsToSet(
                pc,
                callContext.asInstanceOf[self.ContextType],
                allocatedType,
                isConstant,
                isEmptyArray
            )
        }

        @inline override protected[this] def getTypeOf(element: ElementType): ReferenceType = {
            self.getTypeOf(element)
        }

        @inline override protected[this] def getTypeIdOf(element: ElementType): Int = {
            self.getTypeIdOf(element)
        }

        @inline override protected[this] def isEmptyArray(element: ElementType): Boolean = {
            self.isEmptyArray(element)
        }
    }

    override def processNewCaller(
        calleeContext:  ContextType,
        callerContext:  ContextType,
        pc:             Int,
        tac:            TACode[TACMethodParameter, V],
        receiverOption: Option[Expr[V]],
        params:         Seq[Option[Expr[V]]],
        tgtVarOption:   Option[V],
        isDirect:       Boolean
    ): ProperPropertyComputationResult = {
        implicit val state: State =
            new PointsToAnalysisState[ElementType, PointsToSet, ContextType](callerContext, null)

        if (tgtVarOption.isDefined) {
            handleOISReadObject(callerContext, tgtVarOption.get, pc, tac.stmts)
        } else {
            state.addIncompletePointsToInfo(pc)
        }

        Results(createResults(state))
    }

    private[this] def handleOISReadObject(
        context:   ContextType,
        targetVar: V,
        pc:        Int,
        stmts:     Array[Stmt[V]]
    )(
        implicit state: State
    ): Unit = {
        val defSite = getDefSite(pc)

        var foundCast = false
        for {
            use <- targetVar.usedBy
        } stmts(use) match {
            case Checkcast(_, _, ElementReferenceType(castType)) =>
                foundCast = true

                // for each subtype of the cast type we add an allocation
                for {
                    t <- ch.allSubtypes(castType, reflexive = true)
                    cf <- project.classFile(t) // we ignore cases were no class file exists
                    if !cf.isInterfaceDeclaration
                    if ch.isSubtypeOf(t, ObjectType.Serializable)
                } {
                    state.includeSharedPointsToSet(
                        defSite,
                        createPointsToSet(pc, context, t, isConstant = false),
                        PointsToSetLike.noFilter
                    )
                }
            case _ =>
        }

        if (!foundCast) {
            state.addIncompletePointsToInfo(pc)
        }
    }
}

trait SerializationAllocationsAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {
    def propertyKind: PropertyMetaInformation
    def createAnalysis: SomeProject => SerializationAllocationsAnalysis

    override def requiredProjectInformation: ProjectInformationKeys =
        AbstractPointsToBasedAnalysis.requiredProjectInformation :+ DeclaredMethodsKey

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callers, propertyKind)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(propertyKind)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = createAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(_ => Results(analysis.registerAPIMethod()))
        analysis
    }
}

object TypeBasedSerializationAllocationsAnalysisScheduler extends SerializationAllocationsAnalysisScheduler {
    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => SerializationAllocationsAnalysis =
        new SerializationAllocationsAnalysis(_) with TypeBasedAnalysis
}

object AllocationSiteBasedSerializationAllocationsAnalysisScheduler extends SerializationAllocationsAnalysisScheduler {
    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => SerializationAllocationsAnalysis =
        new SerializationAllocationsAnalysis(_) with AllocationSiteBasedAnalysis
}
