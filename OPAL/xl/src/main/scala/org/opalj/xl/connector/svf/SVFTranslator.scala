/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package connector
package svf

import scala.collection.mutable


class SVFTranslator[PointsToSet](mapping: mutable.Map[Long, PointsToSet], emptyPointsToSet: PointsToSet) {
  def getPTS(svfId: Long) : PointsToSet = {
    if(mapping.contains(svfId))
        mapping(svfId)
    else
        emptyPointsToSet
  }
  def storePointsToSet(svfId: Long, pts: PointsToSet) = {
      mapping.addOne(svfId -> pts)
  }
}
