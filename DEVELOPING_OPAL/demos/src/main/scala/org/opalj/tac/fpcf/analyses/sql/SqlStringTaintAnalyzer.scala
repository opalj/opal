
package org.opalj.tac.fpcf.analyses.sql

object SqlStringTaintAnalyzer {


  var taintMemory = new SqlTaintMemory(Set("TAINTED_VALUE","'TAINTED_VALUE'"))

  val debugMode = true

  def doAnalyze(string:String, taintMemorySQL: SqlTaintMemory): Boolean ={
    taintMemory = taintMemorySQL;
    doAnalyze(string)
  }

  def doAnalyze(string: String) = {


    if(string.isBlank) println("yes blank")
    if(debugMode)println("String to analyze: " + string)

    val normalizedString = if(!string.isEmpty) preprozesing(string) else ""
    val insertPatter = raw"(INSERT).*;.*".r
    val updatePatter = raw"(UPDATE).*;.*".r
    val selectPatter = raw"(SELECT).*;.*".r


    normalizedString match {
      case i@insertPatter(_*) =>
        analyzeInsertStatement(i)

      case u@updatePatter(_*) =>
        analyzeUpdateStatement(u)

      case s@selectPatter(_*) =>
        analyzeSelectStatment(s)

      case _ =>
        if(debugMode) println("Result of SqlAnalyzer: Ana does not support: "+ string)
        false
    }
  }

  def preprozesing(string: String) = {
    val hasCorrectSyntax = checkSQLStringSyntax(string)
    val normalizedString = if (hasCorrectSyntax) normalizeSQLString(string) else ""
    if(debugMode && !hasCorrectSyntax ){
      println("Wrong Syntax :" + string)
    }
    normalizedString
  }

  def checkSQLStringSyntax(string: String) = {
    val word = raw"(\w+|'\w+'|`\w+`)"
    val specialWord = raw"($word|(\w+(-|\.|\w+)+\w+)|('\w+(-|\.|\w+)+\w+')|(`\w+(-|\.|\w+)+\w+`))"
    val wordlist = raw"$specialWord(\s*,\s*$specialWord)*"
    val insertColumn = raw"\(\s*$wordlist\s*\)"
    val values = raw"\(\s*$wordlist\s*\)(\s*,\s*\(\s*$wordlist\s*\))*"


//Valid form := INSERT IGNORE* INTO* 'TableName' ( list of Column) VALUES
    val insertPattern = raw"((INSERT|insert)\s+((IGNORE|ignore)\s+)?(INTO|into)\s+)($specialWord)\s+$insertColumn\s+(VALUES|values)\s+($values)\s*;\s*".r
    val selectPattern = raw"(SELECT|select)\s+($wordlist|\*)\s+(FROM|from)\s+($specialWord)\s*(\s+(WHERE|where)\s+.+)?;".r
    val updatePattern = raw"(UPDATE|update)\s+($specialWord)\s+(SET|set)\s+($specialWord\s+=\s+$specialWord)(\s*,\s*$specialWord\s+=\s+$specialWord)*(\s+(WHERE|where) .+)?\s*;\s*".r

    string match {
      case insertPattern(_*) => true
      case selectPattern(_*) => true
      case updatePattern(_*) => true
      case _ => false
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
    if(debugMode) println("String normalized to: " + nStr)
    nStr
  }

  def extractInformationOfInsert(normalizedString: String) = {

    val insertPatter01 = raw"(INSERT (IGNORE\s)?INTO) (.+) (\( .+ \)) (VALUES) (\(.+\)) ; ".r

    var extractedTableName = ""
    var extractedColumnNames: Seq[String] = Seq()
    var extractedValueGroups: Set[Array[String]] = Set(Array())


    normalizedString match {
      case insertPatter01(_, _, tableName, columnNames, _, values) =>
        extractedTableName = tableName
        extractedColumnNames = columnNames.replaceAll(raw"(\(|\))", "").split(",").map(_.trim).toSeq
        val removedBrackets = values.split(raw"\) , \(").map(x => x.replaceAll(raw"(\(|\))", ""))
        extractedValueGroups = removedBrackets.map(x => x.split(",").map(_.trim)).toSet
      case _ =>
    }

    (extractedTableName, extractedColumnNames, extractedValueGroups)

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
    if(debugMode) println("Result of analyzeInsertStatement:  \n"
      + "  analyzed String: " + normalizedString + " \n  "
      + taintedColumns.mkString("(", ",", ")") + " of table " + table + " will be tainted \n")

    taintedColumns.nonEmpty

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
    if(debugMode)println("Result of analyzeSelectStatment: \n"
      + "  analyzed String: " + normalizedString + " \n  "
      + taintedColumns.mkString("(", ",", ")") + " of table " + table + " where tainted \n")

    hastaints
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

  def analyzeUpdateStatement(normalizedString: String) = {
    val (table, columnAndValues) = extractInformationOfUpdate(normalizedString)
    var taintColumns: Set[String] = Set()

    for ((colmn, value) <- columnAndValues) {
      if (taintMemory.isTainted(value) || taintMemory.isTainted(colmn) || taintMemory.isTainted(table)) {
        taintColumns += colmn
      }
    }

    if (taintColumns.nonEmpty) taintMemory.taintTableAndColums(table, taintColumns)
    if(debugMode)println("Result of analyzeUpdateStatment:  \n"
      + "  analyzed String: " + normalizedString + " \n  "
      + taintColumns.mkString("(", ",", ")") + " of table " + table + " will be tainted")

    taintColumns.nonEmpty

  }
}
