## Fan-In and Fan-Out Relations

This file describes the fan-in as well as the fan-out relation of classes that are located within
this package. Changes to this structures have to be done carefully since that will influence
existing _test cases_ (see `DEVELOPING_OPAL/tools/.../Hermes`).

### metrics.fanin_fanout.Expression

___Fan-Out___: `java.lang.Object` (1)

___Fan-In___: `fanin_fanout.Constant`, `fanin_fanout.AddExpression`, `fanin_fanout.ExpressionInterpreter` (3)

`3/1 = 3`

### metrics.fanin_fanout.Constant

___Fan-Out___: `java.lang.Object`, `java.lang.Integer`, `fanin_fanout.Expression` (3)

___Fan-In___: `fanin_fanout.ExpressionInterpreter` (1)

`3/1 = 3`

### metrics.fanin_fanout.AddExpression

___Fan-Out___: `java.lang.Object`, `fanin_fanout.Expression` (2)

___Fan-In___: `fanin_fanout.ExpressionInterpreter` (1)
`2/1 = 1`

### metrics.fanin_fanout.ExpressionInterpreter

___Fan-Out___: `java.lang.Object`, `java.lang.Integer`, `fanin_fanout.Expression`, `fanin_fanout.AddExpression`, `fanin_fanout.Constant`, `java.lang.String`, `java.lang.System` (7)

___Fan-In___: `fanin_fanout.ExpressionInterpreter` (1)

`6/1 = 6`
