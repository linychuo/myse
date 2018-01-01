package gui

import javafx.beans.property.Property
import javafx.beans.property.StringProperty
import tornadofx.ItemViewModel
import tornadofx.getProperty
import tornadofx.property
import java.time.LocalDate

class Customer {
    var name: String by property<String>()
    fun nameProperty() = getProperty(Customer::name)

    var birthday: LocalDate by property<LocalDate>()
    fun birthdayProperty() = getProperty(Customer::birthday)

    var street: String by property<String>()
    fun streetProperty() = getProperty(Customer::street)

    var zip: String by property<String>()
    fun zipProperty() = getProperty(Customer::zip)

    var city: String by property<String>()
    fun cityProperty() = getProperty(Customer::city)

    override fun toString() = name
}

class CustomerModel : ItemViewModel<Customer>(Customer()) {
    val name: StringProperty = bind { item?.nameProperty() }
    val birthday: Property<LocalDate> = bind { item?.birthdayProperty() }
    val street: StringProperty = bind { item?.streetProperty() }
    val zip: StringProperty = bind { item?.zipProperty() }
    val city: StringProperty = bind { item?.cityProperty() }
}