/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContexts

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class InnerClassesEntry(
        inner_class_info_index:   Int,
        outer_class_info_index:   Int,
        inner_name_index:         Int,
        inner_class_access_flags: Int
) {

    def toXHTML(definingClass: FieldTypeInfo)(implicit cp: Constant_Pool): Node = {

        val definingClassFQN = definingClass.asJava
        val accessFlags =
            AccessFlags.toString(inner_class_access_flags, AccessFlagsContexts.INNER_CLASS)

        // IMPROVE Use ObjectTypeInfo
        val definedType = cp(inner_class_info_index).toString
        val outerClassFQN =
            if (outer_class_info_index != 0)
                cp(outer_class_info_index).toString
            else
                ""

        if (definingClassFQN == outerClassFQN && inner_name_index != 0) {
            val innerName = cp(inner_name_index).toString
            <div class="inner_class">
                ...
                {{
                <span class="access_flags">{ accessFlags } </span>
                <span class="sn tooltip">
                    { innerName }
                    <span>
                        Defined Type:
                        <span class="fqn">{ definedType }</span>
                    </span>
                </span>
                }}
            </div>
        } else {
            val innerName =
                if (inner_name_index != 0) cp(inner_name_index).toString else ""
            val outerName =
                if (outerClassFQN == "") {
                    if (innerName == "")
                        "..." // <= anonymous inner type
                    else {
                        // named inner type of an anonymous outer type
                        val outerName =
                            definedType.substring(0, definedType.length() - innerName.length() + 1)

                        if (outerName.length < definingClassFQN.length)
                            outerName.substring(0, outerName.length() - 1)
                        else if (outerName.length == definingClassFQN.length)
                            "..."
                        else
                            "..."+outerName.substring(definingClassFQN.length(), outerName.length() - 1)
                    }
                } else
                    outerClassFQN

            <div class={ "inner_class"+{ if (definedType == definingClassFQN) " selfref" else "" } }>
                <span class="fqn">
                    { outerName }
                    {{
                    <span class="access_flags">{ accessFlags } </span>
                    {
                        if (innerName != "") {
                            <span class="sn tooltip">
                                { innerName }
                                <span class="fqn">{ definedType }</span>
                            </span>
                        } else {
                            <span class="fqn tooltip">
                                { definedType }
                                <span>Anonymous Type</span>
                            </span>
                        }
                    }
                    }}
                </span>
            </div>

        }
    }
}
