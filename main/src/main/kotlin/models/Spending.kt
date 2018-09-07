package models

import i18n.t
import java.io.Serializable


@Suppress("UNCHECKED_CAST")
/**
 * Class defines Spending document model. Inherits and overrides methods of Model class and works
 * specifically for it data.
 */
class Spending: Model() {

    // Database types of all fields in model
    override val fieldTypes: HashMap<String, Serializable>
        get() = hashMapOf(
                "uid" to hashMapOf("type" to "String"),
                "number" to hashMapOf("type" to "Long"),
                "date" to hashMapOf("type" to "Long"),
                "description" to hashMapOf("type" to "String"),
                "amount" to hashMapOf("type" to "Decimal"),
                "type" to hashMapOf("type" to "Enum", "values" to SpendingTypes),
                "period" to hashMapOf("type" to "String"),
                "company" to hashMapOf("type" to "Link","display_field" to "name")
        )

    override val modelName:String
        get() = "Spendings"

    override fun validate(isNew:Boolean,user_id:String?): HashMap<String,Any>? {
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

        if (this["period"] == null) {
            errors["period"] = t("Не указан период расходов")
        } else {
            if (this["period"].toString().trim().isEmpty()) {
                errors["period"] = "Не указан период расходов"
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
                println(e.message)
                errors["amount"] = t("Указана некорректная сумма")
            }
        }

        if (this["type"] == null) {
            errors["type"] = t("Не указан тип расходов")
        } else {
            try {
                val type = this["type"] as Long
                if (type < 1 || type > 7) errors["type"] = t("Указан некорректный тип расходов")
            } catch (e: Exception) {
                errors["type"] = t("Указан некорректный тип расходов")
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
        val items = Company().getList(options as? HashMap<String, Any>,user_id)
        if (items.size == 0) errors["company"] = t("Выбрана некорректная организация")

        return if (errors.keys.size > 0) errors else null
    }

}

/**
 * List of allowed spending types
 */
var SpendingTypes = hashMapOf(
        1 to t("Оплата налога УСН"),
        2 to t("Оплата торгового сбора"),
        3 to t("Страховые взносы на обязательное пенсионное страхование"),
        4 to t("Страховые взносы на обязательное социальное страхование на случай временной нетрудоспособности и в связи с материнством"),
        5 to t("Страховые взносы на обязательное медицинское страхование"),
        6 to t("Страховые взносы на обязательное социальное страхование от несчастных случаев на производстве и профессиональных заболеваний"),
        7 to t("Расходы по выплате пособия по временной нетрудоспособности"),
        8 to t("Платежи (взносы) по договорам добровольного личного страхования")
)
