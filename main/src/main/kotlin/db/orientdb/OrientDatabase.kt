package db.orientdb

import db.Database
import khttp.structures.authorization.BasicAuthorization
import models.Model
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

@Suppress("UNCHECKED_CAST")
/**
 * Database Adapter for "OrientDB" database
 * @param dburl: Path to OrientDB database instance to connect to
 * @param dbname: Database name to open
 * @param dblogin: Login
 * @param dbpassword: Password
 */
class OrientDatabase(// URL to database, including host and port

        var dburl: String, // Name of database
        val dbname: String, // Login name to access database
        private val dblogin: String, // Password to access database
        private val dbpassword: String): Database {

    /**
     * Method returns URL to access database
     * @return URL, to use in REST requests to database server
     */
    fun getServerUrl(): String = "http://$dburl"

    /**
     * Method constructs BASIC Authentication header to send in REST HTTP
     * requests to database server
     */
    fun getAuth(): BasicAuthorization = BasicAuthorization(dblogin,dbpassword)

    /**
     * Method used to send request to database server and receive response as JSON Object
     * @param query: SQL query string
     * @param options: Options which should be applied to request: condition, limit, fields etc
     * @return: Either JSONObject or null if error
     */
    fun execQueryJSON(query:String,options:HashMap<String,Any>?=null):JSONObject? {
        return execQuery(query,options)
    }

    /**
     * Method used to send request to database server and receive response
     * @param query: SQL query string
     * @param options: Options which should be applied to request: condition, limit, fields etc
     * @return: Either JSONObject or null if error
     */
    fun execQuery(query:String,options:HashMap<String,Any>?=null):JSONObject? {
        return Request(this).execQueryJSON(query,options)
    }

    /*******************************************************************
     * Implementation of CRUD methods, defined in "Database" interface *
     *******************************************************************/
    override fun getCount(model:Model,options: HashMap<String,Any>,user_id:String?): Int {
        return ListQueryBuilder(this,model,options,user_id).getCount()
    }

    override fun getList(model:Model,options: HashMap<String,Any>,user_id:String?): ArrayList<Model> {
        return ListQueryBuilder(this,model,options,user_id).getList()
    }

    override fun getItem(model: Model,user_id:String?): Model? {
        return CrudRunner(this,model,user_id).getItem()
    }

    override fun postItem(model: Model,user_id:String?): Model? {
        return CrudRunner(this,model,user_id).postItem()
    }

    override fun putItem(model: Model,user_id:String?): Model? {
        return CrudRunner(this,model,user_id).putItem()
    }

    override fun deleteItems(model:Model,ids:String,user_id:String?): Boolean {
        return CrudRunner(this,model,user_id).deleteItems(ids)
    }

    override fun auth(name:String, password:String): String? {
        return Authenticator(this).auth(name,password)
    }

    override fun tokenAuth(token:String): String? {
        return Authenticator(this).tokenAuth(token)
    }
}