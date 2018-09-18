package models

import i18n.t
import utils.ValidationResult
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

    init {
        errorDescriptions["number"] = hashMapOf(
                ValidationResult.EMPTY_VALUE to t("Не указан номер счета"),
                ValidationResult.INCORRECT_VALUE to t("Указан некорректный номер счета"),
                ValidationResult.DUPLICATE_VALUE to t("Счет с таким номером уже есть в базе")
        )
        errorDescriptions["bik"] = hashMapOf(
                ValidationResult.EMPTY_VALUE to t("Не указан БИК"),
                ValidationResult.INCORRECT_VALUE to t("Указан некорректный БИК")
        )
        errorDescriptions["ks"] = hashMapOf(
                ValidationResult.EMPTY_VALUE to t("Не указан корр.счет"),
                ValidationResult.INCORRECT_VALUE to t("Указан некорректный корр.счет")
        )
        errorDescriptions["bank_name"] = hashMapOf(
                ValidationResult.EMPTY_VALUE to t("Не указано наименование банка")
        )
    }

    override val modelName:String
        get() = "Accounts"

    override fun validate(isNew:Boolean,user_id:String?): HashMap<String,Any>? {
        super.validate(isNew, user_id)
        validateBankName();validateBik();validateKs();validateNumber(isNew,user_id);validateCompany(user_id)
        return getValidationResult()
    }

    private fun validateNumber(isNew:Boolean,user_id:String?) {
        errors["number"] = validator.getErrorMessage("number",validator.validateString(this["number"]))
        if (errors["number"].toString().isNotEmpty()) return
        errors["number"] = validator.getErrorMessage("number",
                validator.validateDuplicate("number",this["number"],isNew,user_id))
    }

    private fun validateBik() {
        errors["bik"] = validator.getErrorMessage("bik", validator.validateLong(this["bik"]))
    }

    private fun validateKs() {
        errors["ks"] = validator.getErrorMessage("ks",validator.validateString(this["ks"]))
    }

    private fun validateBankName() {
        errors["bank_name"] = validator.getErrorMessage("bank_name",
                validator.validateString(this["bank_name"]))
    }
}