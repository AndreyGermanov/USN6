package models

import utils.isValidEmail
import i18n.t
import utils.ValidationResult
import java.io.Serializable
import kotlin.reflect.jvm.internal.ReflectProperties


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

    init {
        errorDescriptions["type"] = hashMapOf(
            ValidationResult.EMPTY_VALUE to t("Не указан тип отчета"),
            ValidationResult.INCORRECT_VALUE to t("Указан некорректный тип отчета")
        )
        errorDescriptions["email"] = hashMapOf(
            ValidationResult.INCORRECT_VALUE to t("Указан некорректный email")
        )
        errorDescriptions["period"] = hashMapOf(
            ValidationResult.EMPTY_VALUE to t("Не указан период отчета"),
            ValidationResult.INCORRECT_VALUE to t("Указан некорректный период отчета")
        )
    }

    override val modelName:String
        get() = "Reports"

    override fun validate(isNew:Boolean,user_id:String?): HashMap<String,Any>? {
        super.validate(isNew, user_id)
        validateDate();validatePeriod();validateType();validateCompany(user_id);validateEmail()
        return getValidationResult()
    }

    private fun validateType() {
        errors["type"] = validator.getErrorMessage("type",validator.validateString(this["type"]))
        if (errors["type"].toString().isNotEmpty()) return
        errors["type"] = validator.getErrorMessage("type",
                validator.validateInList(this["type"]!!,ReportTypes as HashMap<Any,Any>))
    }

    private fun validateEmail() {
        if (this["email"] !== null && this["email"].toString().isNotEmpty()) {
            if (!isValidEmail(this["email"].toString())) {
                errors["email"] = t("Указан некорректный email")
            }
        }
    }

    private fun validatePeriod() {
        errors["period"] = validator.getErrorMessage("period",validator.validateLong(this["period"]))
    }
}

/**
 * List of allowed Report types
 */
var ReportTypes = hashMapOf(
        "kudir" to t("Книга учета доходов и расходов")
)
