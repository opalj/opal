/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package entity.impl

import entity.AbstractEntity
import entity.annotation.Entity
import entity.annotation.Table
import entity.annotation.Column
import entity.annotation.Transient
import entity.annotation.Id

/**
 * @author Marco Torsello
 */
@Entity
@Table(name = "Address")
@SerialVersionUID(100L)
class Address extends AbstractEntity {

    @Id
    @Column(name = "id", nullable = false)
    var id: Int = _

    @Column(name = "street", nullable = false)
    var street: String = ""

    @Column(name = "city", nullable = false)
    var city: String = ""

    @Column(name = "user_id", nullable = true)
    var userId: Integer = null

}