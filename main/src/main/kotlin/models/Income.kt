package models

import i18n.t
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

    override val modelName:String
        get() = "Income"

    override fun validate(isNew:Boolean,user_id:String?): HashMap<String,Any>? {
        super.validate(isNew,user_id)
        validateNumber()
        validateDate()
        validateDescription()
        validateAmount()
        validateCompany(user_id)
        return getValidationResult()
    }

    private fun validateDescription() {
        if (this["description"] == null || this["description"].toString().trim().isEmpty())
            errors["description"] = t("Не указано описание операции")
    }
}
