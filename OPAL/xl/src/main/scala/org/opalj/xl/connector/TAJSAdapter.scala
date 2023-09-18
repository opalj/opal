/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.connector

import dk.brics.tajs.analysis.xl.translator.JavaTranslator
import dk.brics.tajs.lattice.Value
import org.opalj.xl.translator.JavaJavaScriptTranslator

import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Type
import org.opalj.tac.common.DefinitionSite

class TAJSAdapter(project:SomeProject) extends JavaTranslator {

    override def queryFunctionValue(javaFullClassName: String, javaFunctionName: String): Value = {
        val classFileOption = project.classFile(ObjectType(javaFullClassName.replace(".", "/")))
            val returnType = classFileOption.get.methods.filter(_.name == javaFunctionName).head.returnType
        JavaJavaScriptTranslator.
            Java2JavaScript("", Map(returnType.asFieldType -> Set.empty[DefinitionSite]), None, project)._2;
    }

    override def queryPropertyValue(
                              javaFullClassName: String,
                              javaPropertyName: String
                          ): Value = {
        val classFileOption = project.classFile(ObjectType(javaFullClassName.replace(".", "/")))
        val fields = classFileOption.get.fields.filter(field => field.name == javaPropertyName)
        val m = Map(fields(0).fieldType.asInstanceOf[Type] -> Set.empty[DefinitionSite])
        JavaJavaScriptTranslator.Java2JavaScript("", m, None, project)._2
    }
}
