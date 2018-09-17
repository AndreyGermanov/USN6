package models

import utils.isValidEmail
import i18n.t
import java.io.Serializable


@Suppress("UNCHECKED_CAST")
/**
 * Class defines Report model. Inherits and overrides methods of Model class and works
 * specifically for it data.
 */
class Report: Document() {

    // Database types of all fields in model
    override val fieldTypes: HashMap<String, Serializable>
        get() = hashMapOf(
                "uid" to hashMapOf("type" to "String"),
                "date" to hashMapOf("type" to "Long"),
                "period" to hashMapOf("type" to "Long"),
                "type" to hashMapOf("type" to "Enum", "values" to ReportTypes),
                "company" to hashMapOf("type" to "Link","display_field" to "name"),
                "email" to hashMapOf("type" to "String")
        )

    override val modelName:String
        get() = "Reports"

    override fun validate(isNew:Boolean,user_id:String?): HashMap<String,Any>? {
        super.validate(isNew, user_id)
        validateDate()
        validatePeriod()
        validateType()
        validateCompany(user_id)
        validateEmail()
        return getValidationResult()
    }

    private fun validateType() {
        if (this["type"] == null) {
            errors["type"] = t("Не указан тип отчета")
        } else if (!ReportTypes.containsKey(this["type"])) {
            errors["type"] = t("Указан некорректный тип отчета")
        }
    }

    private fun validateEmail() {
        if (this["email"] !== null && this["email"].toString().isNotEmpty()) {
            if (!isValidEmail(this["email"].toString())) {
                errors["email"] = t("Указан некорректный email")
            }
        }
    }

    private fun validatePeriod() {
        if (this["period"] == null) {
            errors["period"] = t("Не указан период отчета")
        } else {
            try {
                val period = this["period"].toString().toLong()
                if (period <= 0) errors["period"] = t("Указана некорректный период отчета")
            } catch (e: Exception) {
                errors["period"] = t("Указан некорректный период отчета")
            }
        }
    }
}

/**
 * List of allowed Report types
 */
var ReportTypes = hashMapOf(
        "kudir" to t("Книга учета доходов и расходов")
)
