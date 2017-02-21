# Method Types
Extracts the information about the type of the specified methods.

<table>
  <tr>
    <th></th>
    <th>native</th>
    <th>synthetic</th>
    <th>bridge</th>
    <th>synchronized</th>
    <th>varargs</th>
  </tr>
  <tr>
    <td>static</td>
    <td>✓</td>
    <td>✓</td>
    <td>✗</td>
    <td>✓</td>
    <td>✓</td>
  </tr>
  <tr>
    <td>static { }<br>&lt;clinit&gt;</td>
    <td>✗</td>
    <td>✗</td>
    <td>✗</td>
    <td>✗</td>
    <td>✗</td>
  </tr>
  <tr>
    <td>instance</td>
    <td>✓</td>
    <td>✓<br>(except of Enum.values/valueOf)</td>
    <td>✓</td>
    <td>✓</td>
    <td>✓</td>
  </tr>
  <tr>
    <td>constructor</td>
    <td>✗</td>
    <td>✓<br>(the default constructor is never synthetic)</td>
    <td>✗</td>
    <td>✗</td>
    <td>✓</td>
  </tr>
</table>
