package utils

import i18n.t
import models.Company
import models.Model
import models.SpendingTypes

/**
 * Class contains set of validations, which used to validate fields of model
 * @param model, to which validations applied
 */
class ModelValidator(val model: Model) {

    fun validateString(value:Any?):ValidationResult {
        if (value == null || value.toString().trim().isEmpty())
            return ValidationResult.EMPTY_VALUE
        return ValidationResult.OK
    }

    fun validateDecimal(value:Any?):ValidationResult {
        if (value == null || value.toString().isEmpty()) return ValidationResult.EMPTY_VALUE
        try {
            if (value is Long) {
                if (value.toString().toLong() <= 0) return ValidationResult.INCORRECT_VALUE
            } else {
                if (value.toString().toDouble() <= 0) return ValidationResult.INCORRECT_VALUE
            }
        } catch (e: Exception) {
            return ValidationResult.INCORRECT_VALUE
        }
        return ValidationResult.OK
    }

    fun validateLong(value:Any?):ValidationResult {
        if (value == null || value.toString().isEmpty()) return ValidationResult.EMPTY_VALUE
        try {
            if (value.toString().toLong() <= 0) return ValidationResult.INCORRECT_VALUE
        } catch (e: Exception) {
            return ValidationResult.INCORRECT_VALUE
        }
        return ValidationResult.OK
    }

    /**
     * Method used to check, if model with specified field already exists in database and returns error if yes.
     * @param fieldName: Validated field name
     * @param value: value of field to check
     * @param isNew: flag, to check if this model is not yet in database
     * @param user_id: User which should be an owner of models to check
     * @returns: DUPLICATE_VALUE error if model with specified field already exists or OK if not
     */
    fun validateDuplicate(fieldName:String,value:Any?,isNew:Boolean=false,user_id:String?):ValidationResult {
        var condition = "$fieldName='$value'"
        if (!isNew) {
            condition += " AND @rid!=${model["uid"].toString().replace("_",":")}"
        }
        val items = model.getList(hashMapOf("condition" to condition),user_id)
        if (items.size>0) return ValidationResult.DUPLICATE_VALUE
        return ValidationResult.OK
    }

    /**
     * Method used to validate if specified value exists as a key of item in specified list
     * @param value: Value to check
     * @param list: Hashmap as a source list to check in
     * @returns: INCORRECT_VALUE if no item with that index in list or OK if it exists
     */
    fun validateInList(value:Any,list:HashMap<Any,Any>):ValidationResult {
        if (!list.containsKey(value)) return ValidationResult.INCORRECT_VALUE
        return ValidationResult.OK
    }

    /**
     * Method used to validate if [model] with specified [value] as ID exists in database
     * @param value: Value which is checked
     * @param model: Model type which is checked
     * @param user_id: Only records, which belongs to specified user_id checked
     * @returns: INCORRECT_VALUE if no model with specified ID exists or OK if model with specified id found
     */
    fun validateModelExist(value:Any?,model:Model,user_id:String?):ValidationResult {
        val items = model.getList(hashMapOf("condition" to "@rid=$value"),user_id)
        if (items.size == 0) return ValidationResult.INCORRECT_VALUE
        return ValidationResult.OK
    }

    /**
     * Method used to return error message, based on validation result for specified field
     * @param fieldName: Validated field name
     * @param validationResult: Validation result
     * @returns: Error message, if it defined for this field in current model, or empty string, if no validation error
     * or error message not defined
     */
    fun getErrorMessage(fieldName:String,validationResult: ValidationResult):String {
        if (validationResult == ValidationResult.OK) return ""
        if (!model.errorDescriptions.containsKey(fieldName)) return ""
        if (!model.errorDescriptions[fieldName]!!.containsKey(validationResult)) return ""
        return model.errorDescriptions[fieldName]!![validationResult]!!
    }
}

enum class ValidationResult {
    EMPTY_VALUE,INCORRECT_VALUE,DUPLICATE_VALUE,OK
}