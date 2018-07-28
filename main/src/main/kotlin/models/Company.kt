package models

import i18n.t
import java.io.Serializable

@Suppress("UNCHECKED_CAST")
/**
 * Class defines Company model. Inherits and overrides methods of Model class and works
 * specifically for company data.
 */
class Company: Model() {

    // Database types of all fields in model
    override val fieldTypes: HashMap<String, Serializable>
        get() = hashMapOf(
                "uid" to hashMapOf("type" to "String"),
                "name" to hashMapOf("type" to "String"),
                "inn" to hashMapOf("type" to "String"),
                "kpp" to hashMapOf("type" to "String"),
                "type" to hashMapOf("type" to "Long"),
                "address" to hashMapOf("type" to "String")
        )

    override val modelName:String
        get() = "Companies"

    override fun validate(isNew:Boolean): HashMap<String,Any>? {
        val errors = HashMap<String,Any>()
        if (this["name"] == null) {
            errors["name"] = t("Не указано наименование")
        } else {
            if (this["name"].toString().trim().isEmpty()) {
                errors["name"] = "Не указано наименование"
            }
        }
        if (this["inn"] == null) {
            errors["inn"] = t("Не указан ИНН")
        } else {
            try {
                val inn = this["inn"].toString().toLong()
                if (inn <= 0) errors["inn"] = t("Указан некорректный ИНН")
            } catch (e: Exception) {
                errors["inn"] = t("Указан некорректный ИНН")
            }
        }
        var type = 0
        if (this["type"] == null) {
            errors["type"] = t("Не указан тип организации")
        } else {
            try {
                type =  Integer.valueOf((this["type"] as Long).toString())
                if (type != 1 && type != 2) errors["type"] = t("Указан некорректный тип организации")
            } catch (e: Exception) {
                errors["type"] = t("Указан некорректный тип организации")
            }
        }
        if (this["kpp"] == null && type.equals(2)) {
            errors["kpp"] = t("Не указан КПП")
        } else if (this["kpp"] != null && !type.equals(1)) {
            try {
                val kpp = this["kpp"].toString().toLong()
                if (kpp <= 0) errors["kpp"] = t("Указан некорректный КПП")
            } catch (e:Exception) {
                errors["kpp"] = t("Указан некорректный КПП")
            }
        }
        if (this["address"] == null) {
            errors["address"] = "Не указан адрес"
        } else {
            if (this["address"].toString().trim().isEmpty()) {
                errors["address"] = "Не указан адрес"
            }
        }
        if (errors.keys.size != 0) {
            return errors
        }
        var condition = "inn='${this["inn"]}'"
        if (!isNew) {
            condition += " AND @rid!=#${this["uid"].toString().replace("_",":")}"
        }
        val options = hashMapOf(
                "condition" to condition
        )
        val items = getList(options as? HashMap<String, Any>)
        if (items.size>0) {
            errors["inn"] = t("Организация с таким ИНН уже есть в базе")
        }
        return if (errors.keys.size > 0) errors else null
    }

}

enum class CompanyTypes(code:Int) {
    IP(1),OOO(2);
    fun getCode(type:CompanyTypes):Int {
        return when (type) {
            IP -> 1
            OOO -> 2
        }
    }
}