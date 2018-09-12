package models

import utils.HashUtils
import i18n.t
import org.json.simple.JSONObject
import java.io.Serializable

/**
 * Class defines User model. Inherits and overrides methods of Model class and works
 * specifically for users data.
 */
class User: Model() {

    override val fieldTypes:HashMap<String,Serializable>
        get() = hashMapOf(
                "uid" to hashMapOf("type" to "String"),
                "name" to hashMapOf("type" to "String"),
                "password" to hashMapOf("type" to "String"),
                "email" to hashMapOf("type" to "String"),
                "active" to hashMapOf("type" to "Integer"),
                "activation_token" to hashMapOf("type" to "String")
        )

    override operator fun set(fieldName:String,fieldValue:Any) {
        if (fieldName == "password" && fieldValue is String) {
            setFieldValue("password",HashUtils.sha512(fieldValue))
        } else {
            super.setFieldValue(fieldName,fieldValue)
        }
    }

    /**
     * Method used to validate model fields before create or update model in database
     * @param fields: Fields to validate
     * @param isNew: Is validated item is new
     * @param user_id: ID of user to which affected record should belong
     * @returns HashMap of errors or null if no validation errors
     */
    fun validate(fields: JSONObject, isNew:Boolean=false,user_id:String?=null):HashMap<String,Any>? {
        val errors = HashMap<String,Any>()
        if (!fields.containsKey("name"))
            errors["name"] = t("Не указано имя пользователя")

        if (this.validatePassword(fields)!==null)
            errors["password"] = this.validatePassword(fields)!!

        if (!fields.containsKey("email"))
            errors["email"] = t("Не указан email")

        if (errors.size>0)
            return errors

        var condition = "name='${fields["name"]}'"
        if (!isNew) {
            condition += " AND @rid!=#${this["uid"].toString().replace("_",":")}"
        }
        var options = hashMapOf(
                "condition" to condition
        )
        var items = getList(options as? HashMap<String, Any>,user_id)
        if (items.size>0) {
            errors["name"] = t("Пользователь с таким именем уже есть в базе")
            return errors
        }

        condition = "email='${fields["email"]}'"
        if (!isNew) {
            condition += " AND @rid!=#${this["uid"].toString().replace("_",":")}"
        }
        options = hashMapOf(
                "condition" to condition
        )
        items = getList(options as? HashMap<String, Any>,user_id)
        if (items.size>0) {
            errors["email"] = t("Пользователь с таким email уже есть в базе")
            return errors
        }

        return null
    }

    fun validatePassword(fields:JSONObject):String? {
        if (!fields.containsKey("password"))
            return t("Не указан пароль")
        else if (fields["password"].toString() != fields["confirm_password"].toString())
            return t("Пароли должны совпадать")
        return null
    }

    /**
     * Method used to get model from database which meets specified condition
     * @param condition: SQL condition to match
     * @param user_id: ID of user to which affected record should belong
     * @returns User model or null if not found
     */
    override fun getItemByCondition(condition:String,user_id:String?):User? {
        val model = super.getItemByCondition(condition, user_id) ?: return null
        val user = User()
        user.setRecord(model.getRecord())
        return user
    }

    override val modelName:String
        get() = "Users"

    // Determines if this model depends on "user_id", e.g. only authenticated user can work with it
    // and only with records, belongs to this "user_id
    override val isUserDependent = false
}
