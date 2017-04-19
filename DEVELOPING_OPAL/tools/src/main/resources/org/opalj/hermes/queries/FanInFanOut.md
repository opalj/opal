# Fan-In and Fan-Out

The query computes the Fan-In, Fan-Out and the ratio between Fan-In and Fan-Out of a class.
 
## Configuration

All three feature kinds are configurable over two dimensions. The first dimension is the number of
available catergories that determines the number of features per metric. (Each category relates
to a feature.) The second dimension is the size of each category. The size of category determines
the range of values that belong to a single feature. 

Lets assume we have 3 categories with a size of 2. That relates to the following:

Category 1: `x < 2`

Category 2: `x ≤ 2 < 4`

Category 3: `x ≥ 4`

####default configuration: 
```
org.opalj.hermes.queries.FanInFanOut {
   fanin.categories = 6
   fanin.categorySize = 3
   fanout.categories = 6
   fanout.categorySize = 2
   ratio.categories = 4
   ratio.categorySize = 0.25
} 
```