# Metrics
Extracts basic metric information of a given project. Each given metric is repesented in different feature categories.

<table>
  <tr>
    <th>name</th>
    <th>description</th>
  </tr>
  <tr>
    <td>fields per class (FPC)</td>
    <td>Extracts how often a class has 0, between 1 and 3, between 4 and 10, or more than 10 fields.</td>
  </tr>
  <tr>
    <td> methods per class (MPC)</td>
    <td>Extracts how often a class has 0, between 1 and 3, between 4 and 10, or more than 10 methods.</td>
  </tr>
  <tr>
    <td>classes per package (CPP)</td>
    <td>Extracts how often a package has between 1 and 3, between 4 and 10, or more than 10 classes.</td>
  </tr>
  <tr>
      <td>McCabe</td>
      <td>Extracts how often a method has a complexity of 1 (linear), between 2 and 3, between 4 and 10, or more than 10.
      Please note that this analysis also takes exceptions into account.</td>
    </tr>
  <tr>
      <td>number of children (NOC)</td>
      <td>Extracts how often a class has 0, between 1 and 3, between 4 and 10, or more than 10 children. 
      This metric is the number of direct descendants (subclasses) for each class. Classes with large number of children are
       considered to be difficult to modify and usually require more testing because of the effects 
       on changes on all the children. They are also considered more complex and fault-prone because
        a class with numerous children may have to provide services in a larger number of contexts
        and therefore must be more flexible [1].</td>
    </tr>
</table>

[1] Basili, V. R., Briand, L. C., and Melo, W. L., "A Validation of Object Orient Design Metrics as
Quality Indicators," IEEE Transactions on Software Engineering, vol. 21, pp. 751-761, 1996.