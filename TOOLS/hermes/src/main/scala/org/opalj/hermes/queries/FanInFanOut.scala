/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import scala.annotation.tailrec
import scala.collection.mutable

import com.typesafe.config.Config

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import org.opalj.log.LogContext
import org.opalj.br.FieldType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.da.CONSTANT_Class_info
import org.opalj.da.CONSTANT_Fieldref_info
import org.opalj.da.CONSTANT_InterfaceMethodref_info
import org.opalj.da.CONSTANT_MethodHandle_info
import org.opalj.da.CONSTANT_Methodref_info
import org.opalj.da.Constant_Pool
import org.opalj.da.CONSTANT_NameAndType_info
import org.opalj.da.CONSTANT_Utf8_info
import org.opalj.da.ClassFile
import org.opalj.da.Constant_Pool_Entry

/**
 *  This metric computes the Fan-In and Fan-Out of a class.
 *
 * @author Michael Reif
 */
class FanInFanOut(implicit hermes: HermesConfig) extends FeatureQuery {

    /// Configuration Keys
    final val FanInFanOutConfigPrefix = "org.opalj.hermes.queries.FanInFanOut"

    ///// ###############################################################
    ///// ############### Config Parsing Data Structures ################
    ///// ###############################################################

    case class FeatureConfiguration(
            featureName:   String,
            numCategories: Int,
            categorySize:  Double,
            offset:        Int
    ) {

        private[this] lazy val _maxFeatureIndex = numCategories - 1

        def featureIndex(value: Int): Int = {
            Math.min(value / categorySize.toInt, _maxFeatureIndex) + offset
        }

        def featureIndex(value: Double): Int = {
            Math.min((value / numCategories).toInt, _maxFeatureIndex) + offset
        }
    }

    object FeatureConfiguration {

        implicit def logContext: LogContext = GlobalLogContext

        final val logCategory = "Hermes - fan-in/fan-out query"

        def apply(
            featureName:         String,
            categoriesKey:       String,
            categorySizeKey:     String,
            categoriesDefault:   Int,
            categorySizeDefault: Double,
            offset:              Int
        ): FeatureConfiguration = {
            implicit val config = hermes.Config.getConfig(FanInFanOutConfigPrefix)
            val numCategories = parseNumCategories(categoriesKey).getOrElse(categoriesDefault)
            val categorySize = parseCategorySize(categorySizeKey).getOrElse(categorySizeDefault)

            FeatureConfiguration(featureName, numCategories, categorySize, offset)
        }

        private[this] def parseNumCategories(
            categoriesKey: String
        )(
            implicit
            config: Config
        ): Option[Int] = {
            val numCategories = config.getInt(categoriesKey)
            if (numCategories > 0)
                Some(numCategories)
            else {
                val message =
                    FanInFanOutConfigPrefix + categoriesKey+
                        " setting invalid - value <= 0; category size has been set to default"
                OPALLogger.warn(logCategory, message)
                None
            }
        }

        private[this] def parseCategorySize(
            categorySizeKey: String
        )(
            implicit
            config: Config
        ): Option[Double] = {
            val categorySize = config.getDouble(categorySizeKey)
            if (categorySize > 0) {
                Some(categorySize)
            } else {
                val message =
                    FanInFanOutConfigPrefix + categorySizeKey+
                        " setting invalid - value <= 0; category size has been set to default"
                OPALLogger.warn(logCategory, message)
                None
            }
        }
    }

    ///// ###############################################################
    ///// ####################### CONFIGURATION #########################
    ///// ###############################################################

    val fanoutFeatureName = "FanOut"
    val fanoutCategories = "fanout.categories"
    val fanoutCategorySize = "fanout.categorySize"
    val DEFAULT_fanoutCategories = 4
    val DEFAULT_fanoutCategorySize = 1d

    val faninFeatureName = "FanIn"
    val faninCategories = "fanin.categories"
    val faninCategorySize = "fanin.categorySize"
    val DEFAULT_faninCategories = 4
    val DEFAULT_faninCategorySize = 1d

    val ratioFeatureName = "FanIn/FanOut"
    val ratioCategories = "ratio.categories"
    val ratioCategorySize = "ratio.categorySize"
    val DEFAULT_ratioCategories = 4
    val DEFAULT_ratioCategorySize = 0.5d

    private[this] val fanoutFeature = FeatureConfiguration(
        fanoutFeatureName,
        fanoutCategories,
        fanoutCategorySize,
        DEFAULT_fanoutCategories,
        DEFAULT_fanoutCategorySize,
        offset = 0
    )

    private[this] val faninFeature = FeatureConfiguration(
        faninFeatureName,
        faninCategories,
        faninCategorySize,
        DEFAULT_faninCategories,
        DEFAULT_faninCategorySize,
        offset = fanoutFeature.numCategories
    )

    private[this] val ratioFeature = FeatureConfiguration(
        ratioFeatureName,
        ratioCategories,
        ratioCategorySize,
        DEFAULT_ratioCategories,
        DEFAULT_ratioCategorySize,
        offset = fanoutFeature.numCategories + faninFeature.numCategories
    )

    private[this] lazy val _featureInfo: Seq[FeatureConfiguration] = Seq(
        fanoutFeature,
        faninFeature,
        ratioFeature
    )

    // Initializes the featureIds from the configuration file.
    private[this] lazy val _featureIDs: Seq[String] = {

        val seqBuilder = Seq.newBuilder[String]

        _featureInfo.foreach { featureKind =>
            val FeatureConfiguration(featureName, numCategories, _, _) = featureKind
            var i = 1
            while (i <= numCategories) {
                seqBuilder += s"$featureName - Category $i"
                i += 1
            }
        }

        seqBuilder.result()
    }

    ///// ###############################################################
    ///// ################## QUERY INTERFACE METHODS ####################
    ///// ###############################################################

    override def featureIDs: Seq[String] = _featureIDs

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IterableOnce[Feature[S]] = {

        val features = Array.fill(featureIDs.size)(new LocationsContainer[S])

        val fanOutMap = mutable.Map.empty[Int, Int]
        val fanInMap = mutable.Map.empty[Int, Int]

        @inline def getObjectTypeID = project.classHierarchy.getObjectType _

        for {
            (classFile, source) <- rawClassFiles
            classFileType = classFile.thisType.asJVMType
            objectTypeId = ObjectType(classFileType).id
            location = ClassFileLocation(Some(source), classFileType)
        } {
            implicit val constantPool = classFile.constant_pool
            val cpEntryPredicate: PartialFunction[Constant_Pool_Entry, Constant_Pool_Entry] = {
                case CONSTANT_Fieldref_info(_, name_and_type_index) => constantPool(name_and_type_index)
                case CONSTANT_NameAndType_info(_, descriptor_index) => constantPool(descriptor_index)
                case CONSTANT_MethodHandle_info(_, reference_index) => constantPool(reference_index)
                case CONSTANT_Methodref_info(_, class_index)        => constantPool(class_index)
                case CONSTANT_Class_info(name_index)                => constantPool(name_index)
            }

            @inline def cpEntries = classFile.constant_pool.filter(cpEntryPredicate.isDefinedAt)

            val referencedTypes = mutable.Set.empty[Int]
            cpEntries.foreach { cpEntry =>
                val typeInfo = getTypeInfo(cpEntry)(constantPool)
                if (typeInfo.charAt(0) == '(') {
                    val md = MethodDescriptor(typeInfo)
                    referencedTypes ++= md.parameterTypes.foldLeft(Set.empty[Int])((res, p) =>
                        if (p.isObjectType)
                            res + p.asObjectType.id
                        else res)
                    if (md.returnType.isObjectType)
                        referencedTypes += md.returnType.asObjectType.id
                } else {
                    try {
                        val ft = FieldType(typeInfo)
                        if (ft.isObjectType)
                            referencedTypes += ft.asObjectType.id
                    } catch {
                        case iae: IllegalArgumentException =>
                            referencedTypes += ObjectType(typeInfo).id
                    }
                }
            }

            val fanOut = referencedTypes.size - 1
            val fanOutIndex = fanoutFeature.featureIndex(fanOut)
            features(fanOutIndex) += location

            fanOutMap += (objectTypeId -> fanOut)

            referencedTypes.foreach { otId =>
                if (otId != objectTypeId) {
                    val newRefCount = fanInMap.getOrElse(otId, 0) + 1
                    fanInMap += ((otId, newRefCount))
                }
            }
        }

        fanInMap.foreach { entry =>
            val (otID, fanIn) = entry
            val fanOut = fanOutMap.getOrElse(otID, 1)
            val fanInFanOut = fanIn.toDouble / fanOut.toDouble
            val l = ClassFileLocation(project, getObjectTypeID(otID))

            val fanInFeatureIndex = faninFeature.featureIndex(fanIn)
            val ratioFeatureIndex = ratioFeature.featureIndex(fanInFanOut)

            features(fanInFeatureIndex) += l
            features(ratioFeatureIndex) += l
        }

        for { (featureID, featureIDIndex) <- featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, features(featureIDIndex))
        }
    }

    @tailrec private[this] def getTypeInfo(
        constant_Pool_Entry: Constant_Pool_Entry
    )(implicit constant_pool: Constant_Pool): String = {
        constant_Pool_Entry match {
            case CONSTANT_Class_info(name_index) =>
                getTypeInfo(constant_pool(name_index))
            case CONSTANT_Fieldref_info(_, name_and_type_index) =>
                getTypeInfo(constant_pool(name_and_type_index))
            case CONSTANT_MethodHandle_info(_, reference_index) =>
                getTypeInfo(constant_pool(reference_index))
            case CONSTANT_Methodref_info(_, class_index) =>
                getTypeInfo(constant_pool(class_index))
            case CONSTANT_InterfaceMethodref_info(_, class_index) =>
                getTypeInfo(constant_pool(class_index))
            case CONSTANT_NameAndType_info(_, descriptor_index) =>
                getTypeInfo(constant_pool(descriptor_index))
            case CONSTANT_Utf8_info(_, value) => value
        }
    }
}

