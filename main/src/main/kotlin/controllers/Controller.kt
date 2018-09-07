@file:Suppress("UNCHECKED_CAST", "EXPERIMENTAL_FEATURE_WARNING")

package controllers

import Utils.paramsToHashMap
import Utils.queryStringToJSON
import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authentication
import io.ktor.content.default
import io.ktor.content.files
import io.ktor.content.static
import io.ktor.content.staticRootFolder
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.*
import org.json.simple.parser.JSONParser
import models.Model
import org.json.JSONObject
import system.ConfigManager
import java.io.File

/**
 * Interface which all CRUD controllers must implement
 */
interface CRUDControllerInterface {

    /**
     * Method returns number of items in collection
     * @param options: Filtering options which affect to result
     * @param user_id: ID of user, which is used to limit results
     * @return Number of items in collection
     */
    fun getCount(options:HashMap<String,Any>? = null,user_id:String?=null):Int

    /**
     * Method returns list of items
     * @param options: Options which affect to result: (filtering,ordering,limit, pagination, fields)
     * @param user_id: ID of user, which is used to limit results
     * @return Array of items, which meet the options
     */
    fun getList(options:HashMap<String,Any>? = null,user_id:String?=null):ArrayList<HashMap<String, Any>>

    /**
     * Method used to clean item of list before return
     * @param item: Input item. It can be either Model or HashMap<String,Any>
     * @return: Output record with fields after cleanup
     */
    fun cleanListItem(item:Any): HashMap<String,Any>
    /**
     * Method used to get record for item with specified ID
     * @param id: ID of item to search
     * @param user_id: ID of user, which is used to limit results
     * @return HashMap with record fields
     */
    fun getItem(id:String,user_id:String?=null):HashMap<String,Any>?
    /**
     * Method used to add new record of item to database
     * @param params: POST parameters, which includes all fields of record to add
     * @param user_id: ID of user, which is used to limit results
     * @return HashMap of operation result. If success, contains JSON object for inserted item,
     * or error information otherwise
     */
    fun postItem(params: String,user_id:String?=null):HashMap<String,Any>
    /**
     * Method used to update record of item with specified ID in database
     * @param id: ID of record to update
     * @param user_id: ID of user, which is used to limit results
     * @return HashMap of operation result. If success, contains JSON object for updated item,
     * or error information otherwise
     */
    fun putItem(id:String,params:String,user_id:String?=null): HashMap<String,Any>
    /**
     * Method used to delete records of item with specified IDs from database
     * @param ids: IDs of record to remove
     * @param user_id: ID of user, which is used to limit results
     * @return HashMap of operation result. If error, contains JSON object with error descriptions,
     * or error information otherwise
     */
    fun deleteItems(ids:String,user_id:String?=null):HashMap<String,Any>?

    /**
     * Method returns new instance of model type, which is managed by this controller
     */
    fun getModelInstance(): Model


}

open class Controller:CRUDControllerInterface {

    init {
        Application.registerController(this)
    }

    override fun getCount(options: HashMap<String, Any>?,user_id:String?): Int {
        return getModelInstance().getCount(options)
    }

    override fun getList(options:HashMap<String,Any>?,user_id:String?):ArrayList<HashMap<String, Any>> {

        val rows =  ArrayList<HashMap<String, Any>>()
        val models = getModelInstance().getList(options,user_id)
        for (model in models) {
            rows.add(cleanListItem(model))
        }
        return rows
    }

    override fun cleanListItem(item: Any): HashMap<String, Any> {
        return when (item) {
            is Model -> item.getRecord()
            is HashMap<*, *> -> item as? HashMap<String,Any> ?: HashMap()
            else -> HashMap()
        }
    }

    override fun getItem(id:String,user_id:String?):HashMap<String,Any>? {
        val result = getModelInstance().getItem(id,user_id)
        if (result != null) {
            return cleanListItem(result)
        }
        return null
    }

    override fun postItem(params: String,user_id:String?):HashMap<String,Any> {
        val item = getModelInstance()
        val parser = JSONParser()
        val text = queryStringToJSON(params)
        val json = parser.parse(text) as org.json.simple.JSONObject
        val obj = JSONObject()
        for (p in json.keys) {
            obj.put(p.toString(),json[p])
        }
        item.populate(item.JSONToHashMap(obj))
        val result = item.postItem(user_id)
        return cleanListItem(result)
    }

    override fun putItem(id:String,params:String,user_id:String?): HashMap<String,Any> {
        val item = getModelInstance().getItem(id,user_id)
        if (item === null) {
            return hashMapOf("general" to "Could not find object to delete")
        }
        val text = queryStringToJSON(params)
        val parser = JSONParser()
        val json = parser.parse(text) as org.json.simple.JSONObject
        val obj = JSONObject()
        for (p in json.keys) {
            obj.put(p.toString(),json[p])
        }
        item.populate(item.JSONToHashMap(obj))
        return cleanListItem(item.putItem(user_id))
    }

    override fun deleteItems(ids:String,user_id:String?):HashMap<String,Any>? {
        if (ids.isEmpty()) {
            return hashMapOf("general" to "Could not find items to delete")
        }
        val model = getModelInstance()
        return model.deleteItems(ids,user_id)
    }

    override fun getModelInstance():Model {
        return Model()
    }
}

/**
 * Basic routes, which webserver should serve
 */
fun Routing.root() {
    get("/api") {
        call.respond(HttpStatusCode.OK)
    }
    static("/") {
        val config = ConfigManager.webConfig
        var path = config["root"].toString()
        if (!path.startsWith("/")) {
            path = System.getProperty("user.dir")+"/"+path
        }
        staticRootFolder = File(path)
        static("static") {
            files("static")
        }
        default("index.html")
    }
}

/**
 * Method defines typical API endpoints for CRUD operations to model
 * @param modelName: Name of database model
 * @param ctrl: Instance of controller, used to implement actions for routes
 */
fun Routing.crud(modelName:String,ctrl:CRUDControllerInterface) {
    val gson = Gson()
    Application.registerController(ctrl)
    get("/api/$modelName/{id?}") {
        val user_id = call.authentication.principal<UserIdPrincipal>()?.name
        if (call.parameters["id"] !== null) {
            if (call.parameters["id"].toString() == "count") {
                val count = ctrl.getCount(paramsToHashMap(call.request.queryParameters),user_id).toString()
                call.respondText(count)
            } else {
                call.respondText(gson.toJson(ctrl.getItem(call.parameters["id"]!!,user_id)))
            }
        } else {
            call.respondText(gson.toJson(ctrl.getList(paramsToHashMap(call.request.queryParameters),user_id)))
        }
    }
    post("/api/$modelName") {
        val user_id = call.authentication.principal<UserIdPrincipal>()?.name
        try {
            val result = ctrl.postItem(call.receiveText(),user_id)
            if (result.containsKey("uid")) {
                call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "ok", "result" to result)))
            } else {
                call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "error", "errors" to result)))
            }
        } catch (E:Exception) {
            call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "error",
                    "errors" to hashMapOf("general" to E.message))))
        }
    }
    put("/api/$modelName/{id}") {
        val user_id = call.authentication.principal<UserIdPrincipal>()?.name
        try {
            val result = ctrl.putItem(call.parameters["id"]!!, call.receiveText(),user_id)
            if (result.containsKey("uid")) {
                call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "ok", "result" to result)))
            } else {
                call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "error", "errors" to result)))
            }
        } catch(E:Exception) {
            call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "error",
                    "errors" to hashMapOf("general" to E.message))))
        }
    }
    delete("/api/$modelName/{id}") {
        val user_id = call.authentication.principal<UserIdPrincipal>()?.name
        try {
            val result = ctrl.deleteItems(call.parameters["id"]!!,user_id)
            if (result == null || !result.containsKey("errors")) {
                call.respond(HttpStatusCode.OK,gson.toJson(hashMapOf("status" to "ok")))
            } else {
                call.respond(HttpStatusCode.OK,gson.toJson(hashMapOf("status" to "error", "errors" to result["errors"]!!)))
            }
        } catch(E:Exception) {
            call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "error",
                    "errors" to hashMapOf("general" to E.message))))
        }
    }
}