package org.opalj.Commandline_base.commandlines

trait OpalCommand {
    def parse[T](arg: T): Any
}
