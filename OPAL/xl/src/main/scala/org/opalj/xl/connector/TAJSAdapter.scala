/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.connector

import dk.brics.tajs.analysis.xl.translator.TajsAdapter
import dk.brics.tajs.lattice.Value
import org.opalj.xl.translator.JavaJavaScriptTranslator

import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Type

class TAJSAdapter(project:SomeProject) extends TajsAdapter {

    override def queryFunctionValue(javaFullClassName: String, javaFunctionName: String): Value = {
        val classFileOption = project.classFile(ObjectType(javaFullClassName.replace(".", "/")))
            val returnType = classFileOption.get.methods.filter(_.name == javaFunctionName).head.returnType
        JavaJavaScriptTranslator.
            Java2JavaScript("", List(returnType.asFieldType), None)._2;
    }

    override def queryPropertyValue(
                              javaFullClassName: String,
                              javaPropertyName: String
                          ): Value = {
        val classFileOption = project.classFile(ObjectType(javaFullClassName.replace(".", "/")))
        val fields = classFileOption.get.fields.filter(field => field.name == javaPropertyName)
        val m = List(fields(0).fieldType.asInstanceOf[Type])
        JavaJavaScriptTranslator.Java2JavaScript("", m, None)._2
    }
}
