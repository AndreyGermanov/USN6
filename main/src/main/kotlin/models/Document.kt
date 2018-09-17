package models

import i18n.t

/**
 * Model for document entities. All models which has date inherits from it
 */
open class Document: Model() {

    protected fun validateNumber() {
        if (this["number"] == null) {
            errors["number"] = t("Не указан номер документа")
            return
        }
        try {
            val number = this["number"].toString().toLong()
            if (number <= 0) errors["number"] = t("Указан некорректный номер документа")
        } catch (e: Exception) {
            errors["number"] = t("Указан некорректный номер документа")
        }
    }

    protected fun validateDate() {
        if (this["date"] == null) {
            errors["date"] = t("Не указана дата документа")
            return
        }
        try {
            val date = this["date"].toString().toLong()
            if (date <= 0) errors["date"] = t("Указана некорректная дата документа")
        } catch (e: Exception) {
            errors["date"] = t("Указана некорректная дата документа")
        }
    }

    protected fun validateAmount() {
        if (this["amount"] == null) {
            errors["amount"] = t("Не указана сумма")
            return
        }
        try {
            if (this["amount"] is Long) {
                if (this["amount"].toString().toLong() <= 0)
                    errors["amount"] = t("Указана некорректная сумма")
            } else {
                if (this["amount"].toString().toDouble() <= 0)
                    errors["amount"] = t("Указана некорректная сумма")
            }
        } catch (e: Exception) {
            errors["amount"] = t("Указана некорректная сумма")
        }
    }

}