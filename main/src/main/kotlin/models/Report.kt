package models

import i18n.t
import java.io.Serializable


@Suppress("UNCHECKED_CAST")
/**
 * Class defines Report model. Inherits and overrides methods of Model class and works
 * specifically for it data.
 */
class Report: Model() {

    // Database types of all fields in model
    override val fieldTypes: HashMap<String, Serializable>
        get() = hashMapOf(
                "uid" to hashMapOf("type" to "String"),
                "date" to hashMapOf("type" to "Long"),
                "period" to hashMapOf("type" to "Long"),
                "type" to hashMapOf("type" to "Enum", "values" to ReportTypes),
                "company" to hashMapOf("type" to "Link","display_field" to "name")
        )

    override val modelName:String
        get() = "Reports"

    override fun validate(isNew:Boolean): HashMap<String,Any>? {
        val errors = HashMap<String,Any>()

        if (this["date"] == null) {
            errors["date"] = t("Не указана дата отчета")
        } else {
            try {
                val date = this["date"] as Long
                if (date <= 0) errors["date"] = t("Указана некорректная дата отчета")
            } catch (e: Exception) {
                errors["date"] = t("Указана некорректная дата отчета")
            }
        }


        if (this["period"] == null) {
            errors["period"] = t("Не указан период отчета")
        } else {
            try {
                val period = this["period"] as Long
                if (period <= 0) errors["period"] = t("Указана некорректный период отчета")
            } catch (e: Exception) {
                errors["period"] = t("Указан некорректный период отчета")
            }
        }

        if (this["type"] == null) {
            errors["type"] = t("Не указан тип отчета")
        } else if (!ReportTypes.containsKey(this["type"])) {
            errors["type"] = t("Указан некорректный тип отчета")
        }


        if (this["company"] == null) {
            errors["company"] = t("Не указана организация")
        } else {
            if (this["company"].toString().trim().isEmpty()) {
                errors["company"] = "Не указана организация"
            }
        }

        if (errors.keys.size != 0) {
            return errors
        }

        var condition = "@rid=${this["company"]}"
        val options = hashMapOf("condition" to condition)
        var items = Company().getList(options as? HashMap<String, Any>)
        if (items.size == 0) errors["company"] = t("Выбрана некорректная организация")

        condition = "type=${this["type"]} AND period=${this["period"]}"
        if (!isNew) {
            condition += " AND @rid!=#${this["uid"].toString().replace("_",":")}"
        }

        items = Report().getList(options as? HashMap<String, Any>)
        if (items.size > 0) errors["period"] = t("Отчет данного типа за этот период уже существует")

        return if (errors.keys.size > 0) errors else null
    }

}

/**
 * List of allowed Report types
 */
var ReportTypes = hashMapOf(
        "kudir" to t("Книга учета доходов и расходов")
)
