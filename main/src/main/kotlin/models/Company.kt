package models

import i18n.t
import utils.ValidationResult
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

    init {
        errorDescriptions["name"] = hashMapOf(
            ValidationResult.EMPTY_VALUE to t("Не указано наименование")
        )
        errorDescriptions["address"] = hashMapOf(
            ValidationResult.EMPTY_VALUE to t("Не указан адрес")
        )
        errorDescriptions["inn"] = hashMapOf(
            ValidationResult.EMPTY_VALUE to t("Не указан ИНН"),
            ValidationResult.INCORRECT_VALUE to t("Указан некорректный ИНН"),
            ValidationResult.DUPLICATE_VALUE to t("Организация с таким ИНН уже есть в базе")
        )
        errorDescriptions["kpp"] = hashMapOf(
                ValidationResult.EMPTY_VALUE to t("Не указан КПП"),
                ValidationResult.INCORRECT_VALUE to t("Указана некорректный КПП")
        )
        errorDescriptions["type"] = hashMapOf(
                ValidationResult.EMPTY_VALUE to t("Не указан тип организации"),
                ValidationResult.INCORRECT_VALUE to t("Указана некорректный тип организации")
        )
    }

    override val modelName:String
        get() = "Companies"

    override fun validate(isNew:Boolean,user_id:String?): HashMap<String,Any>? {
        super.validate(isNew, user_id)
        validateName();validateInn(isNew,user_id);validateAddress()
        val type = validateType() ?: return getValidationResult()
        validateKpp(type)
        return getValidationResult()
    }

    private fun validateName() {
        errors["name"] = validator.getErrorMessage("name", validator.validateString(this["name"]))
    }

    private fun validateAddress() {
        errors["address"] = validator.getErrorMessage("address", validator.validateString(this["address"]))
    }

    private fun validateInn(isNew:Boolean,user_id:String?) {
        errors["inn"] = validator.getErrorMessage("inn",validator.validateLong(this["inn"]))
        if (errors["inn"].toString().isNotEmpty()) return
        errors["inn"] = validator.getErrorMessage("inn",
                validator.validateDuplicate("inn",this["inn"],isNew,user_id))
    }

    private fun validateType(): Int? {
        errors["type"] = validator.getErrorMessage("type",validator.validateLong(this["type"]))
        if (errors["type"].toString().isNotEmpty()) return null
        val type = this["type"].toString().toInt()
        errors["type"] = validator.getErrorMessage("type",validator.validateInList(type, CompanyTypes as HashMap<Any, Any>))
        return type
    }

    private fun validateKpp(type:Int) {
        when (type) {
            1 -> if (this["kpp"].toString().isNotEmpty()) errors["kpp"] = errorDescriptions["kpp"]!![ValidationResult.INCORRECT_VALUE]!!
            2 -> errors["kpp"] = validator.getErrorMessage("kpp",validator.validateLong(this["kpp"]))
        }
    }
}

/**
 * List of allowed spending types
 */
var CompanyTypes = hashMapOf(
        1 to t("Индивидуальный предприниматель"),
        2 to t("Общество с ограниченной ответственностью")
)