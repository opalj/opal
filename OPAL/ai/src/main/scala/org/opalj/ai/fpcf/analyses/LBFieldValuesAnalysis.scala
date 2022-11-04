/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package analyses

import scala.collection.mutable

import org.opalj.log.OPALLogger
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EOptionPSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LBProperties
import org.opalj.fpcf.MultiResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.SinglePropertiesBoundType
import org.opalj.fpcf.SomeEPS
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile
import org.opalj.br.Code
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.Field
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.ai.domain
import org.opalj.ai.fpcf.analyses.FieldValuesAnalysis.ignoredFields
import org.opalj.ai.fpcf.domain.RefinedTypeLevelFieldAccessInstructions
//import org.opalj.ai.fpcf.domain.RefinedTypeLevelInvokeInstructions
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.fpcf.properties.FieldValue
import org.opalj.ai.fpcf.properties.TypeBasedFieldValueInformation
import org.opalj.ai.fpcf.properties.ValueBasedFieldValueInformation

/**
 * Computes for each private field an approximation of the type of values stored in the field.
 *
 * Basically, we perform an abstract interpretation of all methods that assign a value
 * to the field to compute the type of the values stored in the field.
 *
 * This analysis is deliberately optimized for performance and, given that we don't have precise
 * call-graph information and also don't track the origin of values, we can't track
 * the overall nullness property of the values stored in a field. E.g., even if we always
 * see that a field is only written in the constructor we still don't know if that happens w.r.t.
 * `this` or some other instance and whether the field was actually read before that!
 *
 * @note This is a very, very shallow analysis and improvements w.r.t. the
 *       precision are easily imaginable.
 *
 * @note
 * ADDITIONALLY, WE HAVE TO IGNORE THOSE FIELDS WHICH SEEMS TO BE ALWAYS NULL BECAUSE  THESE
 * FIELDS ARE OFTEN INITIALZED - AT RUNTIME - BY SOME CODE OUTSIDE THE SCOPE OF "PURE" JAVA BASED
 * ANALYSES.
 *
 * E.G., WE IGNORE THE FOLLOWING FIELDS FROM JAVA 8:
 *  - [BY NAME] java.util.concurrent.FutureTask{ runner:null // Originaltype: java.lang.Thread }
 *  - [BY NAME] java.nio.channels.SelectionKey{ attachment:null // Originaltype: java.lang.Object }
 *  - [BY SETTER] java.lang.System{ err:null // Originaltype: java.io.PrintStream }
 *  - [BY SETTER] java.lang.System{ in:null // Originaltype: java.io.InputStream }
 *  - [BY SETTER] java.lang.System{ out:null // Originaltype: java.io.PrintStream }
 *  - [BY CONSTRUCTOR] java.net.InterfaceAddress{ address:null // Originaltype: java.net.InetAddress }
 *
 * '''[UPDATE BY NATIVE CODE...] sun.nio.ch.sctp.ResultContainer{ value:null // Originaltype: java.lang.Object }'''
 *
 * THE FOLLOWING FIELDS ARE "REALLY" NULL in JAVA 8 (1.8.0 - 1.8.0_25):
 *  - [OK] java.util.TimeZone{ NO_TIMEZONE:null // Originaltype: java.util.TimeZone }
 *  - [OK - DEPRECATED] java.security.SecureRandom{ digest:null // Originaltype: java.security.MessageDigest }
 *
 * The reason/purpose is not 100% clear:
 *  - [OK] javax.swing.JList.AccessibleJList.AccessibleJListChild{ accessibleContext:null }
 *  - [OK] javax.swing.JList.AccessibleJList.AccessibleJListChild{ component:null }
 *  - [OK] com.sun.corba.se.impl.io.IIOPInputStream{ abortIOException:null }
 *  - [OK] com.sun.corba.se.impl.orb.ORBImpl{ codeBaseIOR:null }
 *  - [OK - ACCIDENTIALLY CREATED?] com.sun.org.apache.xpath.internal.jaxp.XPathImpl{ d:null }
 *  - [OK - LEGACY CODE?] javax.swing.JPopupMenu{ margin:null }
 *  - [OK - LEGACY CODE?] sun.audio.AudioDevice{ mixer:null }
 *  - [OK - RESERVED FOR FUTURE USAGE] com.sun.corba.se.impl.corba.ServerRequestImpl{ _ctx:null }
 *  - [OK - LEGACY CODE?] com.sun.java.swing.plaf.motif.MotifPopupMenuUI{ border:null }
 *  - [OK - LEGACY CODE?] com.sun.media.sound.SoftSynthesizer{ testline:null }
 *  - [OK - LEGACY CODE?] com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader{ fSymbolTable:null }
 *  - [OK - LEGACY CODE] com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl{ _piParams:null }
 *  - [OK - RESERVED FOR FUTURE USAGE?] com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBase{ m_namespaceLists:null }
 *  - [OK - LEGACY CODE?] sun.awt.motif.MFontConfiguration{ fontConfig:null }
 *  - [OK - LEGACY CODE?] sun.print.PSStreamPrintJob{ reader:null }
 *
 * @author Michael Eichberg
 */
class LBFieldValuesAnalysis private[analyses] (
        val project: SomeProject
) extends FPCFAnalysis { analysis =>

    final val fieldAccessInformation = project.get(FieldAccessInformationKey)

    /**
     * Computes the set of the fields which are potentially refinable w.r.t. type information.
     */
    def relevantFieldsIterable(classFile: ClassFile): Iterable[Field] = {
        val thisClassType = classFile.thisType
        for {
            field <- classFile.fields
            if field.fieldType.isObjectType

            // Test that the initialization can be made by the declaring class only:
            // (Please note, that we don't track the nullness of domains; we are really
            // just interested in the type!)
            if field.isFinal || field.isPrivate // || TODO <the field is package visible and the package is closed ... requires additional support below to find all relevant methods> || ...

            // we don't want to ignore the field for some special reason
            if ignoredFields.get(thisClassType).forall(!_.contains(field.name))

            // TODO XXX FIXME We don't think that the field is accessed via unsafe or reflection

            // If the type is final, then the type is already necessarily precise...
            if !classHierarchy.isKnownToBeFinal(field.fieldType.asReferenceType)

        } yield {
            field
        }
    }

    /**
     * The domain used for analyzing the values stored in a field.
     *
     * One instance of this domain is used to analyze all methods of the respective
     * class. Only after the analysis of all methods, the information returned by
     * [[FieldValuesAnalysisDomain#fieldInformation]] is guaranteed to be correct.
     *
     * @author Michael Eichberg
     */
    class FieldValuesAnalysisDomain private (
            val classFile: ClassFile,
            val dependees: EOptionPSet[Entity, Property]
    ) extends CorrelationalDomain
        with domain.TheProject
        with domain.TheCode
        with domain.DefaultSpecialDomainValuesBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        // We don't use: "domain.l0.DefaultTypeLevelIntegerValues" because we want constant
        // propagation which helps in case of "ifs" related to type tests etc.::
        with domain.l1.DefaultIntegerValues
        with domain.l0.DefaultTypeLevelLongValues
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelPrimitiveValuesConversions
        with domain.l0.TypeLevelLongValuesShiftOperators
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l0.TypeLevelDynamicLoads
        // IT HAST TO BE L0 - we can't deal with "null" values!
        with domain.l0.DefaultReferenceValuesBinding
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        //with RefinedTypeLevelInvokeInstructions
        with RefinedTypeLevelFieldAccessInstructions {

        final val thisClassType: ObjectType = classFile.thisType

        // Map of fieldNames (that are potentially relevant) and the (refined) value information
        var fieldInformation: Map[Field, Option[DomainValue]] = null

        // Map between the method and the ones called by the method which could successfully
        // be resolved.
        val calledMethods: mutable.Map[Method, mutable.Set[Method]] = mutable.Map.empty

        def this(
            classFile:      ClassFile,
            relevantFields: IterableOnce[Field],
            dependees:      EOptionPSet[Entity, Property] = EOptionPSet.empty
        ) = {
            this(classFile, dependees)
            fieldInformation = relevantFields.iterator.map[(Field, Option[DomainValue])](_ -> None).toMap
        }

        final override val UsedPropertiesBound: SinglePropertiesBoundType = LBProperties

        final override implicit def project: SomeProject = analysis.project

        def hasCandidateFields: Boolean = fieldInformation.nonEmpty
        def candidateFields: Iterable[Field] = fieldInformation.keys

        private[this] var currentMethod: Method = null
        private[this] var currentCode: Code = null

        /**
         * Sets the method that is currently analyzed. This method '''must not be called'''
         * during the abstract interpretation of a method. It is must be called
         * before this domain is used for the first time and immediately before the
         * interpretation of the next method (code block) starts.
         */
        def setMethodContext(method: Method): Unit = {
            currentMethod = method
            currentCode = method.body.get
        }

        override def code: Code = currentCode

        /*
        override protected[this] def doInvokeWithRefinedReturnValue(
            calledMethod: Method,
            result:       MethodCallResult
        ): MethodCallResult = {
            calledMethods.getOrElseUpdate(currentMethod, mutable.Set.empty) += calledMethod
            super.doInvokeWithRefinedReturnValue(calledMethod, result)
        }
        */

        private def updateFieldInformation(
            value:              DomainValue,
            declaringClassType: ObjectType,
            name:               String,
            fieldType:          FieldType
        ): Unit = {
            if (declaringClassType ne thisClassType)
                return ;

            classFile.findField(name, fieldType).foreach { field =>
                if (fieldInformation.contains(field)) {
                    fieldInformation(field) match {
                        case Some(previousValue) =>
                            if (previousValue ne value) {
                                previousValue.join(Int.MinValue, value) match {
                                    case SomeUpdate(newValue) =>
                                        // IMPROVE Remove "irrelevant fields" to check if we can stop the overall process...
                                        fieldInformation += (field -> Some(newValue))
                                    case NoUpdate => /*nothing to do*/
                                }
                            }
                        case None =>
                            fieldInformation += (field -> Some(value))
                    }
                }
            }
        }

        override def putfield(
            pc:                 PC,
            objectref:          DomainValue,
            value:              DomainValue,
            declaringClassType: ObjectType,
            name:               String,
            fieldType:          FieldType
        ): Computation[Nothing, ExceptionValue] = {
            updateFieldInformation(value, declaringClassType, name, fieldType)
            super.putfield(pc, objectref, value, declaringClassType, name, fieldType)
        }

        override def putstatic(
            pc:                 PC,
            value:              DomainValue,
            declaringClassType: ObjectType,
            name:               String,
            fieldType:          FieldType
        ): Computation[Nothing, Nothing] = {
            updateFieldInformation(value, declaringClassType, name, fieldType)
            super.putstatic(pc, value, declaringClassType, name, fieldType)
        }
    }

    private[this] def analyzeRelevantMethods(
        classFile: ClassFile,
        domain:    FieldValuesAnalysisDomain
    ): Unit = {
        val relevantMethods =
            domain.fieldInformation.keys.foldLeft(Set.empty[Method]) { (ms, field) =>
                ms ++ fieldAccessInformation.writeAccesses(field).map(_._1)
            }
        relevantMethods.foreach { method =>
            domain.setMethodContext(method)
            BaseAI(method, domain)
            // The state is implicitly accumulated in the domain, which only works, because
            // the domain is so simple!
        }
    }

    private[analyses] def analyze(classFile: ClassFile): PropertyComputationResult = {
        val relevantFields = relevantFieldsIterable(classFile)
        if (relevantFields.isEmpty) {
            return MultiResult(
                classFile.fields.iterator map { f =>
                    FinalEP(f, TypeBasedFieldValueInformation(f.fieldType))
                }
            )
        }
        val domain = new FieldValuesAnalysisDomain(classFile, relevantFields)
        analyzeRelevantMethods(classFile, domain)
        val allFieldsIterator = classFile.fields.iterator
        val results = allFieldsIterator.map[ProperPropertyComputationResult] { (f: Field) =>
            domain.fieldInformation.get(f) match {

                case Some(None) =>
                    // relevant field, but obviously no writes were found...
                    val dv = domain.DefaultValue(-1, f.fieldType)
                    val fv = ValueBasedFieldValueInformation(dv)
                    // println(f.toJava+"!!!!!!>>>>>> "+fv)
                    Result(FinalEP(f, fv))

                case Some(Some(domain.DomainReferenceValueTag(dv))) =>
                    if ( /* ultimate refinements => */ dv.isNull.isYes || classHierarchy.isKnownToBeFinal(dv.leastUpperType.get)) {
                        val vi = ValueBasedFieldValueInformation(dv.toCanonicalForm)
                        // println(f.toJava+"!!!!!!>>>>>> "+vi)
                        Result(FinalEP(f, vi))
                    } else {
                        // IMPROVE Consider using the CFG to determine if the read fields are relevant at all; currently the analysis is flow insensitive.

                        // 1. get the dependees that are relevant... i.e., fields read in
                        //    methods that also write the field for which we compute more
                        //    precise information or methods called by the methods that
                        //    write the field.
                        val methodsWithFieldWrites =
                            fieldAccessInformation.writeAccesses(f).map(_._1).toSet
                        val relevantDependees = domain.dependees.filter { eOptionP =>
                            eOptionP match {
                                case EOptionP(readField: Field, _) =>
                                    fieldAccessInformation.readAccesses(readField).exists { mPCs =>
                                        methodsWithFieldWrites.contains(mPCs._1)
                                    }
                                case EOptionP(calledMethod: Method, _) =>
                                    // Please note, that – if we get more precise type
                                    // information while carrying out the analysis – the set
                                    // of method dependees may increase after this initial
                                    // filtering, because we may be able to resolve further
                                    // method calls that are initially not resolved.
                                    // e.g.
                                    // {{{
                                    //  val o = Foo.m() // initially returns "some object"
                                    //  o.toString // no precisely resolvables
                                    //  // if now, m() is known to return only String objects
                                    //  // o.toString becomes resolvable!
                                    // }}}
                                    methodsWithFieldWrites.exists { m =>
                                        val methodsCalledByM = domain.calledMethods.get(m)
                                        methodsCalledByM.nonEmpty &&
                                            methodsCalledByM.get.contains(calledMethod)
                                    }
                                case _ => throw new MatchError(eOptionP)
                            }
                        }

                        def c(eps: SomeEPS): ProperPropertyComputationResult = {
                            // println("\nCONTINUATION:")
                            // print(f.toJava+"\n\t\t"+relevantDependees+"\n\t\t")
                            val newDependees = relevantDependees.clone()
                            newDependees.updateAll()
                            val domain = new FieldValuesAnalysisDomain(classFile, List(f), newDependees)
                            analyzeRelevantMethods(classFile, domain)
                            val dvOption = domain.fieldInformation(f)
                            if (dvOption.isEmpty) {
                                // the field is no longer written...
                                OPALLogger.error(
                                    "analysis state",
                                    s"the field values analysis for ${f} failed miserably: "
                                )(project.logContext)
                            }
                            val domain.DomainReferenceValueTag(dv) = dvOption.get
                            val vi = ValueBasedFieldValueInformation(dv.toCanonicalForm)
                            // println("======>>>>>>\n\t\t"+vi+"\n\t\t"+relevantDependees)
                            if (newDependees.isEmpty ||
                                dv.isNull.isYes ||
                                classHierarchy.isKnownToBeFinal(dv.leastUpperType.get)) {
                                Result(FinalEP(f, vi))
                            } else {
                                InterimResult.forLB(f, vi, newDependees.toSet, c)
                            }
                        }

                        val vi = ValueBasedFieldValueInformation(dv.toCanonicalForm)
                        if (relevantDependees.isEmpty) {
                            //   println(f.toJava+"!!!!!!>>>>>> "+vi)
                            Result(FinalEP(f, vi))
                        } else {
                            //   println(f.toJava+"======>>>>>>\n\t\t"+vi+"\n\t\t"+relevantDependees)
                            InterimResult.forLB(f, vi, relevantDependees.toSet, c)
                        }
                    }

                case _ =>
                    Result(FinalEP(f, TypeBasedFieldValueInformation(f.fieldType)))
            }
        }

        /* ONLY RESULTS FOR REFERENCE-TYPED FIELDS FOR WHICH WE COULD GET BETTER RESULTS:
            var results: List[FinalEP[Field, FieldValue]] = Nil
            domain.fieldInformation foreach { e =>
                val (field, domainValueOption: Option[IsReferenceValue @unchecked]) = e
                domainValueOption.foreach { domainValue =>
                    if (domainValue.isPrecise || domainValue.isNull.isYesOrNo ||
                        // when we reach this point:
                        //      value.isNull == Unknown &&
                        //      the type is not precise
                        domainValue.leastUpperType.get != field.fieldType) {
                        val vi = ValueBasedFieldValueInformation(domainValue.toCanonicalForm)
                        results ::= FinalEP(field, vi)
                    }
                }
            }
            */
        Results(results)
    }

}

object FieldValuesAnalysis {

    /**
     * The following (final) fields are directly initialized by the JVM or some native code.
     * I.e., the initialization is not visible and if we don't ignore the fields, we would derive
     * wrong information.
     */
    // IMPROVE Move to configuration file.
    final val ignoredFields: Map[ObjectType, Set[String]] = Map(
        ObjectType.System -> Set("in", "out", "err"),
        ObjectType("java/net/InterfaceAddress") -> Set("address"),
        ObjectType("java/util/concurrent/FutureTask") -> Set("runner"),
        ObjectType("java/nio/channels/SelectionKey") -> Set("attachment")
    )

}

object EagerLBFieldValuesAnalysis extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(FieldAccessInformationKey)

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        // To ensure that subsequent analyses are able to pick-up the results of this
        // analysis, we state that the domain that has to be used when computing
        // the AIResult has to use the (partial) domain: RefinedTypeLevelFieldAccessInstructions.
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey)(
            i => i.getOrElse(Set.empty) + classOf[RefinedTypeLevelFieldAccessInstructions]
        )
        null
    }

    override def uses: Set[PropertyBounds] = Set()

    def derivedProperty: PropertyBounds = PropertyBounds.lb(FieldValue.key)

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new LBFieldValuesAnalysis(p)
        val classFiles =
            if (!p.libraryClassFilesAreInterfacesOnly)
                p.allClassFiles
            else
                p.allProjectClassFiles
        ps.scheduleEagerComputationsForEntities(classFiles)(analysis.analyze)
        analysis
    }
}
