/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package translator

/*import svfjava.SVFFunction
import svfjava.SVFValue */

import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.Method
//import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
//import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.properties.TheTACAI

class JavaNativeTranslator(project: SomeProject) {
    /*
    def Java2NativeFunctionName(functionName: String, functions: List[SVFFunction]): Option[SVFFunction] = {
        functions.find(_ => true)
    } */

    /*def JavaObject2SvfValue(referenceType: ReferenceType) : SVFValue = {
    null //TODO
  } */

    def Native2JavaMethod(functionName: String): Option[Method] = {
        val javaMethodShortName = functionName.substring(functionName.lastIndexOf("_") + 1, functionName.size) //TODO
        //val javaPackageName = llvmMethodName.substring(0, llvmMethodName.lastIndexOf("_")).replace("_", "/")
        //val javaObjectType = ObjectType(javaPackageName)
        //.classFile(javaObjectType)
        val javaMethod = project.allMethods.find(method => method.name == javaMethodShortName)
        javaMethod
    }

    //def svfvalue2JavaPointsToIdentifier(svfValue:SVFValue): Int =
    //  svfValue.toString.split(" = ").head.replace("%", "").trim.toInt //TODO use a more unique identifier

    def Java2Native[PointsToSet >: Null <: PointsToSetLike[_, _, PointsToSet], ContextType, NativeValue](
        variableName:    String,
        context:         ContextType,
        pointsToSetLike: PointsToSet,
        theTACAI:        TheTACAI,
        tpe:             Option[ObjectType] = None,
        nativeValue:     NativeValue
    ): NativeValue = {

        val defaultValue: NativeValue = nativeValue
        /* TODO */
        defaultValue
    }

    def Native2Java[NativeValue](nativeValue: NativeValue): List[Any] = {
        List.empty[Any]
    }
}
