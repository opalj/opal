/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import scala.collection.Map
import org.opalj.br.Field
import org.opalj.br.Method

/**
 * @author Michael Eichberg
 */
package object analyses {

    type FieldValueInformation = Map[Field, Domain#DomainValue]

    type MethodReturnValueInformation = Map[Method, Option[Domain#DomainValue]]

}
