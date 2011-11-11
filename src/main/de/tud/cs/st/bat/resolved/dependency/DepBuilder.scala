package de.tud.cs.st.bat.resolved
package dependency
import java.lang.Integer
import DependencyType._

trait DepBuilder {

  def getID(identifier: String): Int
  def addDep(src: Int, trgt: Int, dType: DependencyType)

  def addDep(src: Option[Int], trgt: Option[Int], dType: DependencyType) {
    if (src != None && trgt != None) {
      addDep(src.get, trgt.get, dType)
    }
  }
}