package models

import i18n.t
import java.io.Serializable


@Suppress("UNCHECKED_CAST")
/**
 * Class defines Spending document model. Inherits and overrides methods of Model class and works
 * specifically for it data.
 */
class Spending: Document() {

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
        super.validate(isNew, user_id)
        validateNumber()
        validateDate()
        validateDescription()
        validateAmount()
        validateType()
        validatePeriod()
        validateCompany(user_id)
        return getValidationResult()
    }

    private fun validatePeriod() {
        if (this["period"] == null || this["period"].toString().trim().isEmpty())
            errors["period"] = t("Не указан период расходов")
    }

    private fun validateType() {
        if (this["type"] == null) {
            errors["type"] = t("Не указан тип расходов")
        } else {
            try {
                val type = this["type"].toString().toInt()
                if (SpendingTypes[type] == null ) {
                    errors["type"] = t("Указан некорректный тип расходов")
                }
            } catch (e: Exception) {
                errors["type"] = t("Указан некорректный тип расходов")
            }
        }
    }

    private fun validateDescription() {
        if (this["description"] == null || this["description"].toString().trim().isEmpty())
            errors["description"] = t("Не указано описание операции")
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
