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

    override fun validate(isNew:Boolean,user_id:String?): HashMap<String,Any>? {
        super.validate(isNew, user_id)
        validateName()
        validateInn(isNew,user_id)
        validateAddress()
        val type = validateType() ?: return getValidationResult()
        validateKpp(type)
        return getValidationResult()
    }

    private fun validateName() {
        if (this["name"] == null || this["name"].toString().trim().isEmpty())
            errors["name"] = t("Не указано наименование")
    }

    private fun validateAddress() {
        if (this["address"] == null || this["address"].toString().trim().isEmpty())
            errors["address"] = "Не указан адрес"
    }

    private fun validateInn(isNew:Boolean,user_id:String?) {
        if (this["inn"] == null) {
            errors["inn"] = t("Не указан ИНН")
            return
        }
        try {
            if (this["inn"].toString().toLong() <= 0) {
                errors["inn"] = t("Указан некорректный ИНН")
                return
            }
        } catch (e: Exception) {
            errors["inn"] = t("Указан некорректный ИНН")
            return
        }

        var condition = "inn='${this["inn"]}'"
        if (!isNew) {
            condition += " AND @rid!=${this["uid"].toString().replace("_",":")}"
        }
        val options = hashMapOf(
                "condition" to condition
        )
        val items = getList(options as? HashMap<String, Any>,user_id)
        if (items.size>0) {
            errors["inn"] = t("Организация с таким ИНН уже есть в базе")
        }
    }

    private fun validateType(): Int? {
        if (this["type"] == null) {
            errors["type"] = t("Не указан тип организации")
            return null
        }
        try {
            val type =  Integer.valueOf((this["type"].toString().toLong()).toString())
            if (type != 1 && type != 2) {
                errors["type"] = t("Указан некорректный тип организации")
                return null
            }
            return type
        } catch (e: Exception) {
            errors["type"] = t("Указан некорректный тип организации")
            return null
        }
    }

    private fun validateKpp(type:Int) {
        if (this["kpp"] == null && type.equals(2)) {
            errors["kpp"] = t("Не указан КПП")
            return
        }
        if (this["kpp"] != null && !type.equals(1)) {
            try {
                if (this["kpp"].toString().toLong() <= 0) errors["kpp"] = t("Указан некорректный КПП")
                return
            } catch (e:Exception) {
                errors["kpp"] = t("Указан некорректный КПП")
                return
            }
        }
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