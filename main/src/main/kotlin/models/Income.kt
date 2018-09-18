package models

import i18n.t
import utils.ValidationResult
import java.io.Serializable


@Suppress("UNCHECKED_CAST")
/**
 * Class defines Income document model. Inherits and overrides methods of Model class and works
 * specifically for it data.
 */
class Income: Document() {

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

    init {
        errorDescriptions["description"] = hashMapOf(
                ValidationResult.EMPTY_VALUE to t("Не указано описание")
        )
    }

    override val modelName:String
        get() = "Income"

    override fun validate(isNew:Boolean,user_id:String?): HashMap<String,Any>? {
        super.validate(isNew,user_id)
        validateNumber();validateDate();validateDescription();validateAmount();validateCompany(user_id)
        return getValidationResult()
    }

    private fun validateDescription() {
        errors["description"] = validator.getErrorMessage("description",validator.validateString(this["description"]))
    }
}
