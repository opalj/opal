/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.pts

import org.opalj.br.AnnotationLike
import org.opalj.br.ElementValue
import org.opalj.br.ElementValuePairs
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.analyses.pointsto.longToAllocationSite
import scala.collection.immutable.ArraySeq

import org.opalj.br.fpcf.properties.NoContext

class PointsToSetMatcher extends AbstractPropertyMatcher {

    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val singleAnnotation = ObjectType("org/opalj/fpcf/properties/pts/PointsToSet")
        //val containerAnnotation = ObjectType("org/opalj/fpcf/properties/callgraph/DirectCalls")

        if (a.annotationType == singleAnnotation) {
            validateSingleAnnotation(p, as, entity, a, properties)
        } else {
            Some("Invalid annotation.")
        }
    }

    private def findElement(
        elements:       ElementValuePairs,
        annotationName: String
    ): Option[ElementValue] = {
        elements.find(_.name == annotationName).map(_.value)
    }
    private def validateSingleAnnotation(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val annotationType = a.annotationType.asObjectType

        val variableDefinitionLine = getValue(p, annotationType, a.elementValuePairs, "variableDefinition").asIntValue.value

        val subAnnotationsJava: ArraySeq[AnnotationLike] =
            getValue(p, annotationType, a.elementValuePairs, "expectedJavaAllocSites")
                .asArrayValue.values.map(a => a.asAnnotationValue.annotation)

        // expected allocation sites tuples ( line number, type id)
        val expectedJavaAllocSites = subAnnotationsJava.map(
            allocSiteAnnotation => (
                findElement(allocSiteAnnotation.elementValuePairs, "cf").get.asClassValue.value.asObjectType,
                findElement(allocSiteAnnotation.elementValuePairs, "methodName").get.asStringValue.value,
                findElement(allocSiteAnnotation.elementValuePairs, "methodDescriptor").get.asStringValue.value,
                findElement(allocSiteAnnotation.elementValuePairs, "allocSiteLinenumber").get.asIntValue.value,
                findElement(allocSiteAnnotation.elementValuePairs, "allocatedType").get.asStringValue.value,
            )
        ).toSet

        val subAnnotationsJS: ArraySeq[AnnotationLike] =
            getValue(p, annotationType, a.elementValuePairs, "expectedJavaScriptAllocSites")
                .asArrayValue.values.map(a => a.asAnnotationValue.annotation)
        /*
evalCallSource = JavaScriptAllocationReturn.class,
                    evalCallLineNumber = 34,
                    allocatedType = "java.lang.Object"
 */
        // expected allocation sites tuples ( line number, type id)
        val expectedJSAllocSites = subAnnotationsJS.iterator.
            filter(allocSiteAnnotation => findElement(allocSiteAnnotation.elementValuePairs, "cf").isDefined).map(
                allocSiteAnnotation => (
                    findElement(allocSiteAnnotation.elementValuePairs, "cf").get.asClassValue.value.asObjectType,
                    "JavaScript", "<uml>",
                    -findElement(allocSiteAnnotation.elementValuePairs, "nodeIdTAJS").get.asIntValue.value - 100,
                    findElement(allocSiteAnnotation.elementValuePairs, "allocatedType").get.asStringValue.value,
                )
            ).toSet

        implicit val ps: PropertyStore = p.get(PropertyStoreKey)
        implicit val typeIterator: TypeIterator = p.get(TypeIteratorKey)
        val m = entity.asInstanceOf[Method]
        val methodCode = m.body match {
            case Some(code) => code
            case None       => return Some("Code of call site is not available.");
        }
        val defsitesInMethod = ps.entities(propertyFilter = _.e.isInstanceOf[DefinitionSite]).map(_.asInstanceOf[DefinitionSite]).filter(_.method == m).toSet

        //The last defSite is the one of interest
        val defSite = defsitesInMethod.iterator.filter(ds => methodCode.lineNumber(ds.pc).getOrElse(-1) == variableDefinitionLine).maxByOption(_.pc)
        val defsiteOfInterest = defSite.getOrElse(throw new Exception(s"No definition site found for  ${m.name} , line ${variableDefinitionLine}"))

        val ptsProperties = ps.properties(defsiteOfInterest).map(_.toFinalEP.p)
        val pts = {
            ptsProperties.find(_.isInstanceOf[AllocationSitePointsToSet]).map(_.asInstanceOf[AllocationSitePointsToSet]) match {
                case Some(s) => s
                case None    => /*return Some*/ throw new Exception(s"No points-to-set found for definition  ${m.name} , line ${variableDefinitionLine}")
            }
        }
        val detectedAllocSites = (for {
            (ctx, pc, typeId) <- pts.elements.iterator.map(longToAllocationSite)
        } yield {
            if (ctx != NoContext) {
                (ctx.method.declaringClassType.asObjectType, ctx.method.name, ctx.method.descriptor.toUMLNotation, ctx.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1), ObjectType.lookup(typeId).toJava)
            } else
                (m.classFile.thisType.asObjectType, "JavaScript", "<uml>", pc, ObjectType.lookup(typeId).toJava)
        }).toSet
        println("------------------")
        println(s"detected alloc site: ${detectedAllocSites.map(x => s"${x._1} ${x._2} ${x._3} ${x._4} ${x._5}")}")
        println(s"expected Java alloc sites: ${expectedJavaAllocSites.map(x => s"${x._1} ${x._2} ${x._3} ${x._4} ${x._5}")}")
        println(s"expected JS alloc sites: ${expectedJSAllocSites.map(x => s"${x._1} ${x._2} ${x._3} ${x._4} ${x._5}")}")

        val missingAllocSiteSet = (expectedJavaAllocSites ++ expectedJSAllocSites) diff detectedAllocSites
        println(s"missing alloc site: ${missingAllocSiteSet.map(x => s"${x._1} ${x._2} ${x._3} ${x._4} ${x._5}")}")

        if (missingAllocSiteSet.nonEmpty) {
            Some(s"detected alloc site: ${detectedAllocSites.mkString("\n")}\n${missingAllocSiteSet.size} unresolved alloc sites for variable in  ${m.name} , line ${variableDefinitionLine}. remember to sbt test:compile")
        } else {
            None
        }
    }
}
