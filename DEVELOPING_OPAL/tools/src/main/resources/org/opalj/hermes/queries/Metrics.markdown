# Metrics
Extracts basic metric information of a given project

<table>
  <tr>
    <th>name</th>
    <th>description</th>
  </tr>
  <tr>
    <td>FPC</td>
    <td>Minimum, maximum, and an average of fields per class.</td>
  </tr>
  <tr>
    <td>MPC</td>
    <td>Minimum, maximum, and an average of methods per class.</td>
  </tr>
  <tr>
    <td>CPP</td>
    <td>Minimum, maximum, and an average of classes per package.</td>
  </tr>
  <tr>
    <td>DIP</td>
    <td>Minimum, maximum, and an average of the depth of inheritance tree. </td>
  </tr>
  <tr>
      <td>NOC</td>
      <td>Minimum, maximum, and an average of the number of children. This metric is the number of
      direct descendants (subclasses) for each class. Classes with large number of children are
       considered to be difficult to modify and usually require more testing because of the effects 
       on changes on all the children. They are also considered more complex and fault-prone because
        a class with numerous children may have to provide services in a larger number of contexts
        and therefore must be more flexible [1].</td>
    </tr>
</table>

[1] Basili, V. R., Briand, L. C., and Melo, W. L., "A Validation of Object Orient Design Metrics as
Quality Indicators," IEEE Transactions on Software Engineering, vol. 21, pp. 751-761, 1996.