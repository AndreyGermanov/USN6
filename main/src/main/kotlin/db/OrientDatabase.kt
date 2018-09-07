package db

import Utils.HashUtils
import com.google.gson.Gson
import khttp.structures.authorization.BasicAuthorization
import models.Model
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.io.Serializable

@Suppress("UNCHECKED_CAST")
/**
 * Database Adapter for "OrientDB" database
 * @param dburl: Path to OrientDB database instance to connect to
 * @param dbname: Database name to open
 * @param dblogin: Login
 * @param dbpassword: Password
 */
class OrientDatabase(// URL to database, including host and port
        private var dburl: String, // Name of database
        private val dbname: String, // Login name to access database
        private val dblogin: String, // Password to access database
        private val dbpassword: String): Database {


    /**
     * Method returns URL to access database
     * @return URL, to use in REST requests to database server
     */
    private fun getServerUrl(): String {
        return "http://$dburl"
    }

    /**
     * Method constructs BASIC Authentication header to send in REST HTTP
     * requests to database server
     */
    private fun getAuth(): BasicAuthorization {
        return BasicAuthorization(dblogin,dbpassword)
    }

    /**
     * Base Method used to send request to database server
     * @param query: SQL query string
     * @param options: Options which should be applied to request: condition, limit, fields etc
     * @param resultType: In which format result should be returned: JSON(default) or CSV
     * @return: Either JSONObject if result type is JSON, String if result type is CSV, or null if error
     */
    fun execQuery(query:String,options:HashMap<String,Any>?=null,resultType:OrientDBResultType = OrientDBResultType.JSON): Any? {
        var limit = "100"
        if (options != null && options.containsKey("limit")) {
            limit = options["limit"].toString()
        }
        val headers = HashMap<String,String>()
        if (resultType == OrientDBResultType.CSV) {
            headers["Accept"] = "text/csv"
        }
        var query_line = "/command/$dbname/sql/$limit"
        if (options!=null && options.containsKey("function")) {
            query_line = "/function/$dbname$query"
        }
        val url = "${getServerUrl()}$query_line"
        try {
            val response = khttp.post(url, data=query, auth=getAuth(),
                    headers = headers)
            if (response.statusCode != 200) {
                return null
            }
            if (resultType == OrientDBResultType.JSON) {
                return response.jsonObject
            } else if (resultType == OrientDBResultType.CSV) {
                return response.text
            }
        } catch (e:Exception) {}
        return null
    }

    /**
     * Method used to send request to database server and receive response as JSON Object
     * @param query: SQL query string
     * @param options: Options which should be applied to request: condition, limit, fields etc
     * @return: Either JSONObject or null if error
     */
    fun execQueryJSON(query:String,options:HashMap<String,Any>?=null):JSONObject? {
        val result = execQuery(query,options,OrientDBResultType.JSON) ?: return null
        return result as JSONObject
    }

    /**
     * Method used to send request to database server and receive response as CSV string
     * @return: Either String or null if error
    fun execQueryCSV(query:String,options:HashMap<String,Any>):String? {
        val result = execQuery(query,options,OrientDBResultType.CSV) ?: return null
        return result as String
    }
    */

    /**
     * Method returns SQL expression for specified field based on this type
     * Used to construct SQL SELECT query
     * @param field: Field name
     * @param field_options: Field options, which include type of field and other related options
     * @param alias: Should add "as <field_name>" to the end of generated expression
     * @return: String SQL expression for SELECT query
     */
    private fun getFieldSelectSql(field:String,field_options:HashMap<String,Any>,alias:Boolean=true):String {
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

    /*******************************************************************
     * Implementation of CRUD methods, defined in "Database" interface *
     *******************************************************************/
    override fun getCount(model:Model,options: HashMap<String,Any>,user_id:String?): Int {
        var query = "SELECT count(*) as number FROM ${model.modelName}"
        var condition = ""
        if (options.containsKey("condition")) {
            condition = options["condition"].toString().trim()
        }
        if (options.containsKey("filter_fields") && options.containsKey("filter_value")) {
            val filter_fields = options["filter_fields"].toString().split(",")
            val filter_field_conditions = ArrayList<String>()
            val filter_value = options["filter_value"].toString()
            for (filter_field in filter_fields) {
                filter_field_conditions.add("$filter_field.toLowerCase() like '$filter_value%'")
            }
            val filter_field_condition = "(${filter_field_conditions.toArray().joinToString(" OR ")})"
            if (condition.isNotEmpty()) {
                condition += " AND $filter_field_condition"
            } else {
                condition = filter_field_condition
            }
        }
        if (model.isUserDependent) {
            if (user_id === null)
                return 0
            if (condition.isNotEmpty())
                condition += " AND user=$user_id"
            else
                condition = "user=$user_id"
        }
        if (condition.isNotEmpty()) {
            query += " WHERE $condition"
        }
        val result = 0
        val responseJSON = execQueryJSON(query,options) ?: return result
        if (!responseJSON.has("result")) return result
        val resultsArray: JSONArray = responseJSON.optJSONArray("result")
        if (resultsArray.length()==0) return result
        val item = resultsArray[0] as? JSONObject ?: return result
        if (!item.has("number")) return result
        return Integer.valueOf(item["number"].toString())
    }

    override fun getList(model:Model,options: HashMap<String,Any>,user_id:String?): ArrayList<Model> {
        val fields = options["fields"] as Array<String>

        val field_types = options["field_types"] as HashMap<String,Serializable>
        val fields_sql = ArrayList<String>()
        for (field in fields) {
            if (field_types.containsKey(field)) {
                fields_sql.add(getFieldSelectSql(field,field_types[field] as HashMap<String,Any>))
            } else {
                fields_sql.add(field)
            }
        }
        val result = ArrayList<Model>()
        var query = "SELECT ${fields_sql.joinToString(",")} FROM ${model.modelName}"
        var condition = ""
        if (options.containsKey("condition")) {
            condition = options["condition"].toString().trim()
        }
        if (options.containsKey("filter_fields") && options.containsKey("filter_value")) {
            val filter_fields = options["filter_fields"].toString().split(",")
            val filter_field_conditions = ArrayList<String>()
            val filter_value = options["filter_value"].toString()
            for (filter_field in filter_fields) {
                filter_field_conditions.add(
                        "${getFieldSelectSql(filter_field,
                                field_types[filter_field] as HashMap<String,Any>,
                                false)}.toLowerCase() like '$filter_value%'")
            }
            val filter_field_condition = "(${filter_field_conditions.toArray().joinToString(" OR ")})"
            if (condition.isNotEmpty()) {
                condition += " AND $filter_field_condition"
            } else {
                condition = filter_field_condition
            }
        }
        if (model.isUserDependent) {
            if (user_id === null)
                return result
            if (condition.isNotEmpty())
                condition += " AND user=$user_id"
            else
                condition = "user=$user_id"
        }
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
        val responseJSON = execQueryJSON(query,options) ?: return result
        if (!responseJSON.has("result")) {
            return result
        }
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

    override fun getItem(model: Model,user_id:String?): Model? {
        var uid = model.uid
        if (!uid.startsWith("#")) uid = "#${uid.replace("_",":")}"
        var query = "SELECT * FROM ${model.modelName} WHERE @rid=$uid"
        if (model.isUserDependent) {
            if (user_id === null)
                return null
            query += " AND user=$user_id"
        }
        val response = execQueryJSON(query,hashMapOf())
        if (response == null || !response.has("result")) {
            return null
        }

        val resultArray = response["result"] as JSONArray
        if (resultArray.length()==0) {
            return null
        }
        val foundItem = resultArray[0] as JSONObject
        model.populate(model.JSONToHashMap(foundItem))
        return model
    }

    override fun postItem(model: Model,user_id:String?): Model? {
        val record = model.getRecordForDB()
        if (record.contains("uid")) {
            return null
        }
        val gson = Gson()
        if (model.isUserDependent) {
            if (user_id ===  null)
                return null
            record["user"] = user_id
        }
        val data = gson.toJson(record)
        val query = "INSERT INTO ${model.modelName} CONTENT $data"
        val response = execQueryJSON(query,hashMapOf()) ?: return null
        if (!response.has("result")) return null
        val resultArray = response["result"] as JSONArray
        if (resultArray.length()==0) return null
        val rec = resultArray[0] as? JSONObject ?: return null
        record["uid"] = rec["@rid"].toString()
        model.setRecord(record)
        return model
    }

    override fun putItem(model: Model,user_id:String?): Model? {
        val record = model.getRecordForDB()
        if (!record.contains("uid")) {
            return null
        }
        var uid = record["uid"].toString()
        if (!uid.startsWith("#")) uid = "#${uid.replace("_",":")}"
        if (model.isUserDependent) {
            if (user_id  == null) {
                return null
            }
            record["user"] = user_id
        }
        val gson = Gson()
        val data = gson.toJson(record)
        val query = "UPDATE ${model.modelName} CONTENT $data WHERE @rid=$uid"

        execQueryJSON(query,hashMapOf()) ?: return null
        return model
    }

    override fun deleteItems(model:Model,ids:String,user_id:String?): HashMap<String,Any>? {
        val items_list = ids.split(",")
        if (items_list.isEmpty()) return null
        val items_to_delete = ArrayList<String>()
        for (item in items_list) {
            var value = item
            execQueryJSON("/deleteReferences/$value",hashMapOf("function" to true))
            if (!value.startsWith("#")) value = "#${value.replace("_",":")}"
            items_to_delete.add(""+value+"")
        }
        var query = "DELETE FROM ${model.modelName} WHERE @rid IN [${items_to_delete.toArray().joinToString(",")}]"
        if (model.isUserDependent) {
            if (user_id === null)
                return null
            query += " AND user=$user_id"
        }
        execQueryJSON(query,hashMapOf()) ?: return null
        return null
    }

    override fun auth(name:String, password:String): String? {
        if (name.isEmpty() || password.isEmpty()) {
            return null
        }
        val password_hash = HashUtils.sha512(password)
        val query = "SELECT @rid as uid FROM users WHERE name='$name' AND password='$password_hash'"
        val result = execQueryJSON(query,hashMapOf()) ?: return null
        if (!result.has("result")) {
            return null
        }
        val users = result["result"] as? JSONArray ?: return null
        if (users.length()==0) {
            return null
        }
        val user = users[0] as? JSONObject ?: return null
        return user["uid"].toString()
    }

    override fun tokenAuth(token:String): String? {
        val authToken = String(Base64.getDecoder().decode(token))
        val credentials = authToken.split(":")
        if (credentials.size == 2) {
            return this.auth(credentials[0], credentials[1])
        }
        return null
    }

    /********************
     * Helper functions *
     ********************/
    /**
     * Helper method returns first row from OrientDatabase query response
     * @param responseJSON:
     */
    fun getFirstResult(responseJSON:JSONObject?):JSONObject? {
        if (responseJSON==null) return null
        if (!responseJSON.has("result")) return null
        val responseArray = responseJSON["result"] as? JSONArray ?: return null
        if (responseArray.length()==0) return null
        val item = responseArray[0] as? JSONObject ?: return null
        return item
    }

    /**
     * Helper method returns rows of OrientDatabase query response as ArrayList of HashMaps
     */
    fun getResultsHashMap(responseJSON:JSONObject?): ArrayList<HashMap<String,String>>? {
        if (responseJSON==null) return null
        if (!responseJSON.has("result")) return null
        val responseArray = responseJSON["result"] as? JSONArray ?: return null
        if (responseArray.length()==0) return null
        val result = ArrayList<HashMap<String,String>>()
        for (column in responseArray) {
            val item = column as? JSONObject ?: continue
            var row = HashMap<String,String>()
            for (key in item.keys()) {
                row[key] = item[key].toString()
            }
            result.add(row)
        }
        return result
    }

}

/**
 * Types of result, which can be returned from
 * database server
 */
enum class OrientDBResultType {
    JSON, CSV
}