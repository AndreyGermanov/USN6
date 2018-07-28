package models

import i18n.t
import java.io.Serializable


@Suppress("UNCHECKED_CAST")
/**
 * Class defines Income document model. Inherits and overrides methods of Model class and works
 * specifically for it data.
 */
class Income: Model() {

    // Database types of all fields in model
    override val fieldTypes: HashMap<String, Serializable>
        get() = hashMapOf(
                "uid" to hashMapOf("type" to "String"),
                "number" to hashMapOf("type" to "Long"),
                "date" to hashMapOf("type" to "Long"),
                "description" to hashMapOf("type" to "String"),
                "amount" to hashMapOf("type" to "Decimal"),
                "company" to hashMapOf("type" to "Link","display_field" to "name")
        )

    override val modelName:String
        get() = "Income"

    override fun validate(isNew:Boolean): HashMap<String,Any>? {
        val errors = HashMap<String,Any>()
        if (this["number"] == null) {
            errors["number"] = t("Не указан номер документа")
        } else {
            try {
                val number = this["number"] as Long
                if (number <= 0) errors["number"] = t("Указан некорректный номер документа")
            } catch (e: Exception) {
                errors["number"] = t("Указан некорректный номер документа")
            }
        }

        if (this["date"] == null) {
            errors["date"] = t("Не указана дата документа")
        } else {
            try {
                val date = this["date"] as Long
                if (date <= 0) errors["date"] = t("Указана некорректная дата документа")
            } catch (e: Exception) {
                errors["date"] = t("Указана некорректная дата документа")
            }
        }


        if (this["description"] == null) {
            errors["description"] = t("Не указано описание операции")
        } else {
            if (this["description"].toString().trim().isEmpty()) {
                errors["description"] = "Не указано описание операции"
            }
        }

        if (this["amount"] == null) {
            errors["amount"] = t("Не указана сумма")
        } else {
            try {
                if (this["amount"] is Long) {
                    val amount = this["amount"] as Long
                    if (amount <= 0) errors["amount"] = t("Указана некорректная сумма")
                } else {
                    val amount = this["amount"] as Double
                    if (amount <= 0) errors["amount"] = t("Указана некорректная сумма")
                }
            } catch (e: Exception) {
                errors["amount"] = t("Указана некорректная сумма")
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

        val condition = "@rid=${this["company"]}"
        val options = hashMapOf("condition" to condition)
        val items = Company().getList(options as? HashMap<String, Any>)
        if (items.size == 0) errors["company"] = t("Выбрана некорректная организация")

        return if (errors.keys.size > 0) errors else null
    }

}
