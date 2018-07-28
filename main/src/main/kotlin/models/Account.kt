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

    override fun validate(isNew:Boolean): HashMap<String,Any>? {
        val errors = HashMap<String,Any>()
        if (this["bank_name"] == null) {
            errors["bank_name"] = t("Не указано наименование банка")
        } else {
            if (this["bank_name"].toString().trim().isEmpty()) {
                errors["bank_name"] = "Не указано наименование банка"
            }
        }
        if (this["number"] == null) {
            errors["number"] = t("Не указан номер счета")
        }
        if (this["ks"] == null) {
            errors["ks"] = t("Не указан корр. счет")
        }
        if (this["bik"] == null) {
            errors["bik"] = t("Не указан БИК")
        } else {
            try {
                val bik = this["bik"].toString().toLong()
                if (bik <= 0) errors["bik"] = t("Указан некорректный БИК")
            } catch (e: Exception) {
                errors["bik"] = t("Указан некорректный БИК")
            }
        }
        if (this["company"] == null) {
            errors["company"] = t("Не указана организация")
        } else {
            if (this["company"].toString().trim().isEmpty()) {
                errors["company"] = "Не указана организация"
            }
        }

        if (errors.keys.size != 0) {
            return errors
        }
        var condition = "number='${this["number"]}'"
        if (!isNew) {
            condition += " AND @rid!=#${this["uid"].toString().replace("_",":")}"
        }
        var options = hashMapOf(
                "condition" to condition
        )
        var items = getList(options as? HashMap<String, Any>)
        if (items.size>0) errors["number"] = t("Счет с таким номером уже есть в базе")

        condition = "@rid=${this["company"]}"
        options = hashMapOf("condition" to condition)
        items = Company().getList(options as? HashMap<String, Any>)
        if (items.size == 0) errors["company"] = t("Выбрана некорректная организация")

        return if (errors.keys.size > 0) errors else null
    }

}
