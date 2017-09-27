## Direct Class Extensibility

This package contains classes/interfaces/enums/annotations that are either directly extensible or final.

___Transitive extensibility can't be tested yet.___

The following table gives an overview over the types in that package and their extensibility w.r.t.
open and closed packages.

<table>
    <th> Name </th>
    <th> Visibility </th>
    <th> isFinal </th>
    <th> isExtensible (open package) </th>
    <th> isExtensible (closed package) </th>
    <tr>
        <td>PublicInterface</td>
        <td>public</td>
        <td>✘</td>
        <td>✔</td>
        <td>✔</td>
    </tr>
    <tr>
        <td>Interface</td>
        <td>package visible</td>
        <td>✘</td>
        <td>✔</td>
        <td>✘</td>
    </tr>
    <tr>
        <td>PublicClass</td>
        <td>public</td>
        <td>✘</td>
        <td>✔</td>
        <td>✔</td>
    </tr>
    <tr>
        <td>PublicFinalClass</td>
        <td>public</td>
        <td>✔</td>
        <td>✘</td>
        <td>✘</td>
    </tr>
    <tr>
        <td>Class</td>
        <td>pacakge visible</td>
        <td>✘</td>
        <td>✔</td>
        <td>✘</td>
    </tr>
    <tr>
        <td>FinalClass</td>
        <td>package visible</td>
        <td>✔</td>
        <td>✘</td>
        <td>✘</td>
    </tr>
    <tr>
        <td>PublicClassWithPrivateConstrutor</td>
        <td>public</td>
        <td>✔ (effectively)</td>
        <td>✘</td>
        <td>✘</td>
    </tr>
    <tr>
        <td>PublicEnum</td>
        <td>public</td>
        <td>✔</td>
        <td>✘</td>
        <td>✘</td>
    </tr>
    <tr>
        <td>Enum</td>
        <td>pacakge visible</td>
        <td>✔</td>
        <td>✘</td>
        <td>✘</td>
    </tr>
    <tr>
        <td>PublicAnnotation</td>
        <td>public</td>
        <td>✔ </td>
        <td>✘</td>
        <td>✘</td>
    </tr>
    <tr>
        <td>Annotation</td>
        <td>pacakge visible</td>
        <td>✔</td>
        <td>✘</td>
        <td>✘</td>
    </tr>
</table>
