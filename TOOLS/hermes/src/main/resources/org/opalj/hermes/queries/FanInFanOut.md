# Fan-In and Fan-Out

The query computes for each class the *Fan-In*, *Fan-Out* and the ratio between both.

## Configuration

### Configuration Options

All three feature kinds are configurable over two dimensions. The first dimension is the number of catergories which also determines the number of features per metric. (Each category relates to a feature.) The second dimension is the cardinality of each category. It determines how many values belong to a single feature.

Lets assume we have 3 categories with a cardinality of 2. This will result in the following categories/features w.r.t. the fan in/out:

Category 1: `Fan In/Out < 2`

Category 2: `2 ≤ Fan In/Out < 4`

Category 3: `Fan In/Out ≥ 4`

### Default Configuration

    org.opalj.hermes.queries.FanInFanOut {
        fanin.categories = 6
        fanin.categorySize = 3
        fanout.categories = 6
        fanout.categorySize = 2
        ratio.categories = 4
        ratio.categorySize = 0.25
    }
