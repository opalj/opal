/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses.cg

import org.opalj.br.Field
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.UIDSet

import net.ceedubs.ficus.Ficus._

/**
 * The ProjectInformationKey to iterate all fields that shall be considered to be instantiated by default. Each field
 * is associated with a set of ReferenceTypes to be considered instantiated for this field.
 */
object InitialInstantiatedFieldsKey extends ProjectInformationKey[Iterable[(Field, UIDSet[ReferenceType])], Nothing] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.cg.InitialInstantiatedFieldsKey."

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq.empty

    override def compute(project: SomeProject): Iterable[(Field, UIDSet[ReferenceType])] = {
        val key = ConfigKeyPrefix + "analysis"
        val configuredAnalysis = project.config.as[Option[String]](key)
        if (configuredAnalysis.isEmpty) {
            throw new IllegalArgumentException(
                "No InitialInstantiatedFieldsKey configuration available; Instantiated fields cannot be computed!"
            )
        }

        val fqn = configuredAnalysis.get
        val ifFinder = instantiatedFieldsFinder(fqn)
        ifFinder.collectInstantiatedFields(project)
    }

    private[this] def instantiatedFieldsFinder(fqn: String): InstantiatedFieldsFinder = {
        import scala.reflect.runtime.universe._
        val mirror = runtimeMirror(this.getClass.getClassLoader)
        val module = mirror.staticModule(fqn)
        mirror.reflectModule(module).instance.asInstanceOf[InstantiatedFieldsFinder]
    }

}
