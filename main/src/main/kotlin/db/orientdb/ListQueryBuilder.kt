package db.orientdb

import models.Model
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.ArrayList
import java.util.HashMap

/**
 * Class used to build and execute SELECT queries for OrientDB database adapter
 * @param db: Link to underlying OrientDatabase adapter class
 * @param model: Link to database model with which need to work
 * @param options: Link to query options (condition filters, sort order, limits etc)
 * @param user_id: Rid of user which should be owner of all query results (used as a system level filter)
 */
class ListQueryBuilder(val db:OrientDatabase,val model: Model,val options:HashMap<String,Any>,val user_id:String?) {

    /**
     * Method returns count of items in query, according to current condition
     * @returns Int number of items in the list
     */
    fun getCount(): Int {
        val result = 0
        if (model.isUserDependent && user_id === null) return result
        var query = "SELECT count(*) as number FROM ${model.modelName}"
        val condition = buildListCondition()
        if (condition.isNotEmpty()) {
            query += " WHERE $condition"
        }
        val responseJSON = db.execQueryJSON(query,options) ?: return result
        if (!responseJSON.has("result")) return result
        val resultsArray: JSONArray = responseJSON.optJSONArray("result")
        if (resultsArray.length()==0) return result
        val item = resultsArray[0] as? JSONObject ?: return result
        if (!item.has("number")) return result
        return Integer.valueOf(item["number"].toString())
    }

    /**
     * Method runs SELECT query and returns result as a list of model
     * @returns: Array of models
     */
    fun getList(): ArrayList<Model> {
        val result = ArrayList<Model>()
        if (model.isUserDependent && user_id === null) return result
        val responseJSON = db.execQueryJSON(buildListQuery(),options) ?: return result
        if (!responseJSON.has("result")) return result
        return getModelsListFromQueryResult(responseJSON)
    }

    /**
     * Internal method used to build SELECT query text from provided parameters
     * @returns String: SQL query
     */
    private fun buildListQuery():String {
        val fields = options["fields"] as? Array<String> ?: arrayOf("*")
        val field_types = options["field_types"] as HashMap<String, Serializable>
        val fields_sql = ArrayList<String>()
        for (field in fields) {
            if (field_types.containsKey(field)) {
                fields_sql.add(buildFieldSelectSql(field,field_types[field] as HashMap<String, Any>))
            } else {
                fields_sql.add(field)
            }
        }
        var query = "SELECT ${fields_sql.joinToString(",")} FROM ${model.modelName}"
        val condition = this.buildListCondition()
        if (condition.isNotEmpty()) {
            query += " WHERE $condition"
        }
        if (options.containsKey("order")) {
            val order = options["order"].toString().trim()
            query += " ORDER BY $order"
        }
        if (options.containsKey("skip")) {
            query +=" SKIP ${options["skip"]}"
        }
        if (options.containsKey("limit")) {
            query += " LIMIT ${options["limit"]}"
        }
        return query
    }

    /**
     * Internal Method returns SQL expression for specified field based on this type
     * Used to construct SQL SELECT query
     * @param field: Field name
     * @param field_options: Field options, which include type of field and other related options
     * @param alias: Should add "as <field_name>" to the end of generated expression
     * @return: String SQL expression for SELECT query
     */
    private fun buildFieldSelectSql(field:String,field_options:HashMap<String,Any>,alias:Boolean=true):String {
        if (field === "uid") {
            return "@rid as uid"
        }
        val alias_string = if (alias) " as $field" else ""
        return when (field_options["type"]) {
            "String" -> "$field.asString()$alias_string"
            "Decimal" -> "$field.asDecimal()$alias_string"
            "Long" -> "$field.asLong()$alias_string"
            "Integer" -> "$field.asInteger()$alias_string"
            "Link" -> {
                val field_name = if (field_options.containsKey("field_name"))
                    field_options["field_name"].toString()
                else "name"
                "$field.$field_name$alias_string"
            }
            else -> "$field.asString()$alias_string"
        }
    }

    /**
     * Internal method used to build "WHERE" expression for SELECT query
     * @returns: String condition
     */
    private fun buildListCondition(): String {
        var condition = ""
        if (options.containsKey("condition")) {
            condition = options["condition"].toString().trim()
        }
        val filter_field_condition = buildListFilterCondition()
        if (filter_field_condition.isNotEmpty()) {
            if (condition.isNotEmpty()) {
                condition += " AND $filter_field_condition"
            } else {
                condition = filter_field_condition
            }
        }
        if (model.isUserDependent && user_id !== null) {
            if (condition.isNotEmpty())
                condition += " AND user=$user_id"
            else
                condition = "user=$user_id"
        }
        return condition
    }

    /**
     * Internal function used to add list filter to WHERE condition of select query
     * @returns String condition
     */
    private fun buildListFilterCondition(): String {
        val field_types = options["field_types"] as? HashMap<String, Serializable> ?: HashMap()
        if (options.containsKey("filter_fields") && options.containsKey("filter_value")) {
            val filter_fields = options["filter_fields"].toString().split(",")
            val filter_field_conditions = ArrayList<String>()
            val filter_value = options["filter_value"].toString()
            for (filter_field in filter_fields) {
                filter_field_conditions.add(
                        "${buildFieldSelectSql(filter_field,
                                field_types[filter_field] as HashMap<String, Any>,
                                false)}.toLowerCase() like '$filter_value%'")
            }
            return "(${filter_field_conditions.toArray().joinToString(" OR ")})"
        }
        return ""
    }

    /**
     * Method used to construct array of models from OrientDB database response
     * @param responseJSON: Response from OrientDB after query in JSON format
     * @returns ArrayList of Models
     */
    private fun getModelsListFromQueryResult(responseJSON: JSONObject): ArrayList<Model> {
        val fields = options["fields"] as? Array<String> ?: arrayOf("*")
        val result = ArrayList<Model>()
        val resultsArray: JSONArray = responseJSON.optJSONArray("result")
        for (item in resultsArray) {
            if (item !is JSONObject) {
                continue
            }
            val model = Model()
            val record = HashMap<String,Any>()
            for (field in fields) {
                if (item.has(field)) {
                    record[field] = if (item[field].toString() == "null") "" else item[field]
                }
            }
            model.setRecord(record)
            result.add(model)
        }
        return result
    }
}