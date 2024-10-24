package org.opalj.Commandline_base.commandlines

object ThreadsNumCommand extends OpalPlainCommand[Int] {
    override var name: String = "num of threads"
    override var argName: String = "threadsNum"
    override var description: String = "number of threads to be used"
    override var defaultValue: Option[Int] = None
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = null
}
