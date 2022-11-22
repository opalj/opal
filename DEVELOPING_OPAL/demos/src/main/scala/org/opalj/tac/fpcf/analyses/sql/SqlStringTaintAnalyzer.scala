
package org.opalj.tac.fpcf.analyses.sql

object SqlStringTaintAnalyzer {


  var taintMemory = new TaintMemorySQL("TAINTED_VALUE")
  val debugMode = true

  def doAnalyze(string: String) = {

    val normalizedString = preprozesing(string)
    val insertPatter = raw"(INSERT).*".r
    val updatePatter = raw"(UPDATE).*".r
    val selectPatter = raw"(SELECT).*".r

    normalizedString match {
      case i@insertPatter(_*) =>
        val result = analyzeInsertStatement(i)
        if(debugMode) println(result._2)
        result._1

      case u@updatePatter(_*) =>
        val result = analyzeUpdateStatment(u)
        if(debugMode) println(result._2)
        result._1
      case s@selectPatter(_*) =>
        val result = analyzeSelectStatment(s)
        if(debugMode) println(result._2)
        result._1
      case _ =>
        if(debugMode) println("nothing analyzed")
        false
    }
  }

  def preprozesing(string: String) = {
    val (hasCorrectSyntax, sqlString) = filterSQLString(string)
    var normalizedString = sqlString

    if (hasCorrectSyntax) {
      normalizedString = normalizeSQLString(sqlString)
    }
    normalizedString
  }

  def filterSQLString(string: String) = {
    val word = raw"(\w+|'\w+'|`\w+`)"
    val specialWord = raw"($word|(\w+(-|\.|\w+)+\w+)|('\w+(-|\.|\w+)+\w+')|(`\w+(-|\.|\w+)+\w+`))"
    val wordlist = raw"$specialWord(\s*,\s*$specialWord)*"
    val insertColumn = raw"\(\s*$wordlist\s*\)"
    val values = raw"\(\s*$wordlist\s*\)(\s*,\s*\(\s*$wordlist\s*\))*"

    val insertPattern = raw"((INSERT|insert)\s+((IGNORE|ignore)\s+)?(INTO|into)\s+)($specialWord)\s+$insertColumn\s+(VALUES|values)\s+($values)\s*;\s*".r
    val selectPattern = raw"(SELECT|select)\s+($wordlist|\*)\s+(FROM|from)\s+($specialWord)\s*(\s+(WHERE|where)\s+.+)?;".r
    val updatePattern = raw"(UPDATE|update)\s+($specialWord)\s+(SET|set)\s+($specialWord\s+=\s+$specialWord)(\s*,\s*$specialWord\s+=\s+$specialWord)*(\s+(WHERE|where) .+)?\s*;\s*".r


    string match {
      case i@insertPattern(_*) => (true, i)
      case s@selectPattern(_*) => (true, s)
      case u@updatePattern(_*) => (true, u)
      case _ => (false, "")
    }
  }

  def normalizeSQLString(sqlString: String): String = {
    var nStr = sqlString.replaceAll("  ", " ")

    nStr = nStr.replaceAll(",", " , ")
    nStr = nStr.replaceAll(";", " ; ")
    nStr = nStr.replaceAll("=", " = ")
    nStr = nStr.replaceAll(raw"\(", " ( ")
    nStr = nStr.replaceAll(raw"\)", " ) ")
    nStr = nStr.replace("select", "SELECT")
    nStr = nStr.replace("from", "FROM")
    nStr = nStr.replace("where", "WHERE")
    nStr = nStr.replace("values", "VALUES")
    nStr = nStr.replace("insert", "INSERT")
    nStr = nStr.replace("into", "INTO")
    nStr = nStr.replace("update", "UPDATE")
    nStr = nStr.replace("set", "SET")

    while (nStr.contains("  ")) {
      nStr = nStr.replaceAll("  ", " ")
    }
    nStr
  }

  def extractInformationOfInsert(normalizedString: String) = {

    val insertPatter01 = raw"(INSERT (IGNORE\s)?INTO) (.+) (\( .+ \)) (VALUES) (\(.+\)) ; ".r

    var tableNameReturn = ""
    var columnNamesReturn: Seq[String] = Seq()
    var valuesGroupsReturn: Set[Array[String]] = Set(Array())


    normalizedString match {
      case insertPatter01(_, _, tableName, columnNames, _, values) =>
        tableNameReturn = tableName
        columnNamesReturn = columnNames.replaceAll(raw"(\(|\))", "").split(",").map(_.trim).toSeq
        val removedBrackets = values.split(raw"\) , \(").map(x => x.replaceAll(raw"(\(|\))", ""))
        valuesGroupsReturn = removedBrackets.map(x => x.split(",").map(_.trim)).toSet
      case _ =>
    }

    (tableNameReturn, columnNamesReturn, valuesGroupsReturn)

  }

  def analyzeInsertStatement(normalizedString: String) = {
    val (table, columns, valueGroups) = extractInformationOfInsert(normalizedString)
    var taintedColumns: Set[String] = columns.filter(column => taintMemory.isTainted(column)).toSet

    valueGroups.foreach(array => {
      for (i <- array.indices) {
        if (taintMemory.isTainted(array(i))) taintedColumns += columns(i)
      }
    })

    if (taintMemory.isTainted(table)) taintedColumns ++= columns
    if (taintedColumns.nonEmpty) taintMemory.taintTableAndColums(table, taintedColumns)

    (taintedColumns.nonEmpty,
      "\nResult of analyzeInsertStatement:  \n"
        + "  analyzed String: " + normalizedString + " \n  "
        + taintedColumns.mkString("(", ",", ")") + " of table " + table + " will be tainted")

  }

  def extractInformationOfSelect(normalizedString: String) = {

    val selectPatter01 = raw"(SELECT) (.+) FROM ((\w|-|'|´)+) (WHERE (.+))?; ".r
    var tableNameReturn = ""
    var columnNamesReturn: Seq[String] = Seq()

    normalizedString match {
      case selectPatter01(_, columns, tableName, _, _, condition) =>
        columnNamesReturn = columns.split(",").map(_.trim).toSeq
        tableNameReturn = tableName
      case _ =>
    }
    (tableNameReturn, columnNamesReturn)
  }

  def analyzeSelectStatment(normalizedString: String) = {
    val (table, columns) = extractInformationOfSelect(normalizedString)
    var tempColumn: Set[String] = columns.toSet

    if (columns.contains("*")) {

      taintMemory.taintedTableAndColumns.get(table) match {
        case Some(x) => tempColumn = x
        case None =>
      }
    }

    if (taintMemory.isTainted(table)) taintMemory.taintTableAndColums(table, tempColumn)
    val (hastaints, taintedColumns) = taintMemory.columnsAreTainted(table, tempColumn)

    (hastaints, "\nResult of analyzeSelectStatment: \n"
      + "  analyzed String: " + normalizedString + " \n  "
      + taintedColumns.mkString("(", ",", ")") + " of table " + table + " where tainted")
  }

  def extractInformationOfUpdate(normalizedString: String) = {

    val updatePattern = raw"(UPDATE) (.+) SET (.+) ; ".r
    val valueAndConditionPattern = raw"('?\w+'? = '?\w+'? (, '?\w+'? = '?\w+'? )*)(WHERE .+)?".r

    var table = ""
    var temp03: Set[(String, String)] = Set()

    normalizedString match {
      case updatePattern(_, tableName, valuesAndCondition) =>
        table = tableName

        valuesAndCondition match {
          case valueAndConditionPattern(a, _*) =>
            val temp = a.split(",").map(_.trim).toSet
            temp03 = temp.map(str => str.split("=").map(_.trim)).map(array => (array(0), array(1)))
          case _ =>
        }
      case _ =>
    }

    (table, temp03)
  }

  def analyzeUpdateStatment(normalizedString: String) = {
    val (table, columnAndValues) = extractInformationOfUpdate(normalizedString)
    var taintColumns: Set[String] = Set()

    for ((colmn, value) <- columnAndValues) {
      if (taintMemory.isTainted(value) || taintMemory.isTainted(colmn) || taintMemory.isTainted(table)) {
        taintColumns += colmn
      }
    }

    if (taintColumns.nonEmpty) taintMemory.taintTableAndColums(table, taintColumns)

    (taintColumns.nonEmpty,
      "\nResult of analyzeUpdateStatment:  \n"
        + "  analyzed String: " + normalizedString + " \n  "
        + taintColumns.mkString("(", ",", ")") + " of table " + table + " will be tainted")

  }
}
