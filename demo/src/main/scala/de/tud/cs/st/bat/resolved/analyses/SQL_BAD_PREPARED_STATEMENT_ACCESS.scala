package de.tud.cs.st.bat.resolved.analyses

/**
 *
 * Author: Ralf Mitschke
 * Date: 06.08.12
 * Time: 15:16
 *
 */
object SQL_BAD_PREPARED_STATEMENT_ACCESS
{

    val methodSuffixes =
        List("Array",
            "AsciiStream",
            "BigDecimal",
            "BinaryStream",
            "Blob", "Boolean",
            "Byte",
            "Bytes",
            "CharacterStream",
            "Clob",
            "Date",
            "Double",
            "Float",
            "Int",
            "Long",
            "Object",
            "Ref",
            "RowId",
            "Short",
            "String",
            "Time",
            "Timestamp",
            "UnicodeStream",
            "URL")

    /*

    // Fixed list of suffixes for get/set/update methods
    private static final Set<String> dbFieldTypesSet = new HashSet<String>() {
        static final long serialVersionUID = -3510636899394546735L;
        {
            add("Array");
            add("AsciiStream");
            add("BigDecimal");
            add("BinaryStream");
            add("Blob");
            add("Boolean");
            add("Byte");
            add("Bytes");
            add("CharacterStream");
            add("Clob");
            add("Date");
            add("Double");
            add("Float");
            add("Int");
            add("Long");
            add("Object");
            add("Ref");
            add("RowId");
            add("Short");
            add("String");
            add("Time");
            add("Timestamp");
            add("UnicodeStream");
            add("URL");
        }
    };

    //RM: Bug reported on seeing invoke_interface
    public void sawOpcode(int seen) {

        if (seen == INVOKEINTERFACE) {
            String methodName = getNameConstantOperand();
            String clsConstant = getClassConstantOperand();
            if ((clsConstant.equals("java/sql/ResultSet") && ((methodName.startsWith("get") && dbFieldTypesSet
                    .contains(methodName.substring(3))) || (methodName.startsWith("update") && dbFieldTypesSet
                    .contains(methodName.substring(6)))))
                    || ((clsConstant.equals("java/sql/PreparedStatement") && ((methodName.startsWith("set") && dbFieldTypesSet
                            .contains(methodName.substring(3))))))) {
                String signature = getSigConstantOperand();
                int numParms = PreorderVisitor.getNumberArguments(signature);
                if (stack.getStackDepth() >= numParms) {
                    OpcodeStack.Item item = stack.getStackItem(numParms - 1);

                    if ("I".equals(item.getSignature()) && item.couldBeZero()) {
                        bugReporter.reportBug(new BugInstance(this,
                                clsConstant.equals("java/sql/PreparedStatement") ? "SQL_BAD_PREPARED_STATEMENT_ACCESS"
                                        : "SQL_BAD_RESULTSET_ACCESS", item.mustBeZero() ? HIGH_PRIORITY : NORMAL_PRIORITY)
                                .addClassAndMethod(this).addSourceLine(this));
                    }
                }
            }
        }

    }
     */
}