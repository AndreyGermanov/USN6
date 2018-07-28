package models

import Utils.HashUtils
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
                "surname" to hashMapOf("type" to "String"),
                "password" to hashMapOf("type" to "String")
        )

    override operator fun set(fieldName:String,fieldValue:Any) {
        if (fieldName == "password" && fieldValue is String) {
            setFieldValue("password",HashUtils.sha512(fieldValue))
        } else {
            super.setFieldValue(fieldName,fieldValue)
        }
    }

    override val modelName:String
        get() = "Users"

}
