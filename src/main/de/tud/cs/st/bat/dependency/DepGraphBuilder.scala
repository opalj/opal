package de.tud.cs.st.bat.dependency
import java.lang.Integer

trait DepGraphBuilder {
  type EdgeType >: { val id: Int; val descr: String }

  def getID(identifier: String): Int
  def addEdge(src: Int, trgt: Int, eType: EdgeType)

  def addEdge(src: Integer, trgt: Integer, eType: EdgeType) {
    if (src != null && trgt != null) {
      addEdge(src.intValue(), trgt.intValue(), eType)
    }
  }
}