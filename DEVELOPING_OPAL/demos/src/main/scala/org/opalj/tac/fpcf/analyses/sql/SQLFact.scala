
package org.opalj.tac.fpcf.analyses.sql

import org.opalj.tac.fpcf.properties.TaintFact

trait SQLFact extends TaintFact

case class StringValue(index: Int, values: Set[String], taintStatus: Boolean) extends SQLFact with TaintFact
