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
@Table(name = "User")
@SerialVersionUID(100L)
class User extends AbstractEntity {

    @Id
    @Column(name = "id", nullable = false)
    var id: Int = _

    @Column(name = "first_name", nullable = false)
    var firstName: String = ""

    @Column(name = "last_name", nullable = false)
    var lastName: String = ""

    @Column(name = "password", nullable = false)
    var password: String = ""

    var address: Address = null

    @Transient
    def getFullName(): String = {
        this.firstName+" "+this.lastName
    }

}