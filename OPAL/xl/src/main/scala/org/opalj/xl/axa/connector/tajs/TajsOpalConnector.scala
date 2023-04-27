/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.axa.connector.tajs

import dk.brics.tajs.analysis.axa.connector.IConnector
import dk.brics.tajs.lattice.Value
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.PropertyStore

class TajsOpalConnector(project: SomeProject) extends IConnector{

  val propertyStore: PropertyStore = project.get(PropertyStoreKey)

  override def queryFunctionValue(javaFullClassName: String, javaFunctionName: String): Value =
    Value.makeStr("<Function Value from Connector>") // for debugging purpose

  override def queryPropertyValue(javaFullClassName: String, javaPropertyName: String): Value = {
    /*val classFileOption = project.classFile(ObjectType(javaFullClassName))
      if(classFileOption.isDefined){
        val fields = classFileOption.get.fields.filter(field=>field.name==javaPropertyName)
          if (fields.size>0)
            return new TajsTranslator(project, new File("")).OpalToTajs(Set(fields.head.fieldType.asReferenceType), Set.empty[DefinitionSite]);
      } */
    Value.makeStr("<Property Value from Connector>") // for debugging purpose
  }

  override def resume(code: String): Unit = {}
}
