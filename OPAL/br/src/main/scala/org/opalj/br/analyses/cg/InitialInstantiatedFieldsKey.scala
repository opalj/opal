package org.opalj.br.analyses.cg

import net.ceedubs.ficus.Ficus._
import org.opalj.br.{Field, ReferenceType}
import org.opalj.br.analyses.{ProjectInformationKey, ProjectInformationKeys, SomeProject}
import org.opalj.collection.immutable.UIDSet

object InitialInstantiatedFieldsKey extends ProjectInformationKey[Iterable[(Field, UIDSet[ReferenceType])], Nothing]{

    final val ConfigKeyPrefix = "org.opalj.br.analyses.cg.InitialInstantiatedFieldsKey."

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq.empty

    override def compute(project: SomeProject): Iterable[(Field, UIDSet[ReferenceType])] = {
        val key = ConfigKeyPrefix + "analysis"
        val configuredAnalysis = project.config.as[Option[String]](key)
        if(configuredAnalysis.isEmpty) {
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
