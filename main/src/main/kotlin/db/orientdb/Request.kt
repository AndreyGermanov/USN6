package db.orientdb

import khttp.responses.Response
import org.json.JSONObject
import java.util.HashMap

class Request(val db:OrientDatabase) {
    /**
     * Method used to send request to database server and receive response as JSON Object
     * @param query: SQL query string
     * @param options: Options which should be applied to request: condition, limit, fields etc
     * @return: Either JSONObject or null if error
     */
    fun execQueryJSON(query:String,options: HashMap<String, Any>?=null): JSONObject? {
        return execQuery(query,options) as? JSONObject ?: return null
    }

    /**
     * Base Method used to send request to database server
     * @param query: SQL query string
     * @param options: Options which should be applied to request: condition, limit, fields etc
     * @return: Either JSONObject if result type is JSON, String if result type is CSV, or null if error
     */
    private fun execQuery(query:String, options: HashMap<String, Any>?=null): Any? {
        return try {
            processResponse(khttp.post(buildRequest(query,options), data=query, auth=db.getAuth()))
        } catch (e:Exception) {
            null
        }
    }

    private fun buildRequest(query:String,options: HashMap<String, Any>?=null):String {
        var limit = "100"
        if (options != null && options.containsKey("limit")) {
            limit = options["limit"].toString()
        }
        var query_line = "/command/${db.dbname}/sql/$limit"
        if (options!=null && options.containsKey("function")) {
            query_line = "/function/${db.dbname}$query"
        }
        return "${db.getServerUrl()}$query_line"
    }

    private fun processResponse(response: Response):Any? {
        if (response.statusCode != 200) return null
        return response.jsonObject
    }

}