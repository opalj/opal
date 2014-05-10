import de.tud.cs.st.bat.resolved.ai.AIResultBuilder
import de.tud.cs.st.bat.resolved.ai.domain.DefaultDomain

object PathDependencies {
  val result = AIResultBuilder.completed(null,new DefaultDomain)(null,null)
                                                  //> result  : de.tud.cs.st.bat.resolved.ai.AIResult[theDomain.type] = de.tud.cs.
                                                  //| st.bat.resolved.ai.AIResultBuilder$$anon$2@330878fe
  
  val oa = result.operandsArray                   //> oa  : Array[List[theDomain.DomainValue]] = null
  val la = result.localsArray                     //> la  : Array[Array[theDomain.DomainValue]] = null
  
  
}