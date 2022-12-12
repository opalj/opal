package org.opalj.xl

//import org.opalj.xl.common.Entity

///trait PropertyComputationResult
///case class InterimResult() extends PropertyComputationResult
///case class FinalResult() extends PropertyComputationResult
/*
case class Property(key: Any)

case class Analysis() {
  def processProperty(e: Entity, p: Property, r: PropertyComputationResult): Option[PropertyComputationResult]
}

trait Language {
  type State
}

trait XState[LSrc <: Language, LTrg <: Language] {
  def embed(src: LSrc, trg: LTrg)(s: src.State): trg.State
}

/**
 * XLang interactions
 * 1. requested property result changes: suspend + resume (may be called multiple times)
 * 2. shared state changes: update/propagate
 *
 */



class XLang {

  // Java -> JS
  //def xLangCall(srcLang: Language, trgLang: Language, trgEntity: Entity, prop: Property, srcAnalysis: Analysis)
  //         : PropertyComputationResult = {

    // JS.foo() -> Untainted
    // Java.global = source()
    // val r = JS.foo() -> Tainted

    // JS.global.tainted = Java.global.tainted

    // state transformer
    // PropertyStore.addDependency(Java.global, TaintProp, _ => js.walaState.set(JS.global))
    // PropertyStore.set(JS.foo(), TaintProp, js.analyze(JS.foo()))

    //val existingResult: PropertyComputationResult = ??? // propStore.get(trgEntity, prop.key)
    //srcAnalysis.processProperty(trgEntity, prop, existingResult) match {
    //  case Some(res) => return res
    //  case None => // nothing
    //}


    //   case LUB(...)
    //   case FinalResult(r) => return FinalResult(r)
    //
  //}

  /*
    1 statische Funktion: Funktionsname, Tip STring, Liste von Java Argumenten
    OPAl called Sturdy und fragt nach dem Ergebnis
    State Sharing
    Static Program Analysis
   */

}*/