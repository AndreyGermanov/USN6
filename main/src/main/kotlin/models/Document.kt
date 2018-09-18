package models

import i18n.t
import utils.ValidationResult

/**
 * Model for document entities. All models which has date inherits from it
 */
open class Document: Model() {

    init {
        errorDescriptions["amount"] = hashMapOf(
                ValidationResult.EMPTY_VALUE to t("Не указана сумма"),
                ValidationResult.INCORRECT_VALUE to t("Указана некорректная сумма")
        )
        errorDescriptions["number"] = hashMapOf(
                ValidationResult.EMPTY_VALUE to t("Не указан номер документа"),
                ValidationResult.INCORRECT_VALUE to t("Указан некорректный номер документа")
        )
        errorDescriptions["date"] = hashMapOf(
                ValidationResult.EMPTY_VALUE to t("Не указана дата документа"),
                ValidationResult.INCORRECT_VALUE to t("Указана некорректная номер документа")
        )
    }

    protected fun validateNumber() {
        errors["number"] = validator.getErrorMessage("number",validator.validateLong(this["number"]))
    }

    protected fun validateDate() {
        errors["date"] = validator.getErrorMessage("date",validator.validateLong(this["date"]))
    }

    protected fun validateAmount() {
        errors["amount"] = validator.getErrorMessage("amount",validator.validateDecimal(this["amount"]))
    }
}