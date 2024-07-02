/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.connector.svf
object GlobalJNIMapping {
  var mapping: Any = null
}
class SVFTranslator[PointsToSet] {
  def getPTS(svfId: Long) : PointsToSet = {
    getExistingMapping()(svfId)
  }
  def addPTS(svfId: Long, pts: PointsToSet) = {
    val newMapping: Map[Long, PointsToSet] = getExistingMapping() + (svfId -> pts)
    GlobalJNIMapping.mapping =  newMapping
  }

  private def getExistingMapping() : Map[Long, PointsToSet] = {
    if (GlobalJNIMapping.mapping == null){
      GlobalJNIMapping.mapping = Map[Long, PointsToSet]()
    }
    GlobalJNIMapping.mapping.asInstanceOf[Map[Long, PointsToSet]]
  }
}
