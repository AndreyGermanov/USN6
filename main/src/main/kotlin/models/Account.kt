package models

import i18n.t
import java.io.Serializable


@Suppress("UNCHECKED_CAST")
/**
 * Class defines Account model. Inherits and overrides methods of Model class and works
 * specifically for account data.
 */
class Account: Model() {

    // Database types of all fields in model
    override val fieldTypes: HashMap<String, Serializable>
        get() = hashMapOf(
                "uid" to hashMapOf("type" to "String"),
                "number" to hashMapOf("type" to "String"),
                "bik" to hashMapOf("type" to "String"),
                "ks" to hashMapOf("type" to "String"),
                "bank_name" to hashMapOf("type" to "String"),
                "company" to hashMapOf("type" to "Link","display_field" to "name")
        )

    override val modelName:String
        get() = "Accounts"

    override fun validate(isNew:Boolean,user_id:String?): HashMap<String,Any>? {
        super.validate(isNew, user_id)
        validateBankName()
        validateBik()
        validateKs()
        validateNumber(isNew,user_id)
        validateCompany(user_id)
        return getValidationResult()
    }

    private fun validateBankName() {
        if (this["bank_name"] == null || this["bank_name"].toString().trim().isEmpty())
            errors["bank_name"] = t("Не указано наименование банка")
    }

    private fun validateNumber(isNew:Boolean,user_id:String?) {
        if (this["number"] == null || this["number"].toString().isEmpty()) {
            errors["number"] = t("Не указан номер счета")
            return
        }
        var condition = "number='${this["number"]}'"
        if (!isNew) {
            condition += " AND @rid!=${this["uid"].toString().replace("_",":")}"
        }
        val items = getList(hashMapOf("condition" to condition),user_id)
        if (items.size>0) errors["number"] = t("Счет с таким номером уже есть в базе")

    }

    private fun validateKs() {
        if (this["ks"] == null || this["ks"].toString().isEmpty())
            errors["ks"] = t("Не указан корр. счет")
    }

    private fun validateBik() {
        if (this["bik"] == null || this["bik"].toString().isEmpty()) {
            errors["bik"] = t("Не указан БИК")
            return
        }
        try {
            val bik = this["bik"].toString().toLong()
            if (bik <= 0) errors["bik"] = t("Указан некорректный БИК")
        } catch (e: Exception) {
            errors["bik"] = t("Указан некорректный БИК")
        }
    }
}
