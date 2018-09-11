package models

import db.DBManager
import db.Database
import i18n.t
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.io.Serializable

/**
 * Base class for database models
 */
@Suppress("UNCHECKED_CAST", "FunctionName")
open class Model {
    // Actual data record, which this model represents
    private var item = ConcurrentHashMap<String,Any>()

    /**
     * Name of model in database
     * @return Name of model
     */
    open val modelName: String
        get() = ""

    /**
     * Array of field names, which record of this model can hold. All them are temporary
     * @return Array of field names
     */
    open val fields: Array<String>
        get() = fieldTypes.keys.toTypedArray()

    open val fieldTypes: HashMap<String,Serializable>
        get() = hashMapOf("uid" to hashMapOf("type" to "String"))

    open var uid: String
        get() = getFieldValue("uid") as? String ?: ""
        set(value) = setFieldValue("uid",value)

    // Determines if this model depends on "user_id", e.g. only authenticated user can work with it
    // and only with records, belongs to this "user_id
    open val isUserDependent = true

    /**
     * Method used to set data record
     * @param record: Record to set
     */
    open fun setRecord(record:HashMap<String,Any>) {
        for (key in record.keys) {
            item[key] = record[key]!!
        }
    }

    /**
     * Method returns record data
     * @return HashMap with record data
     */
    open fun getRecord(): HashMap<String,Any> {
        val result = HashMap<String,Any>()
        for (key in item.keys()) {
            result[key] = item[key]!!
        }
        return result
    }

    /**
     * Method returns value of specified field of this model
     * @param fieldName - name of field to return
     * @return - either value of specified field or null if field does not exists
     */
    open fun getFieldValue(fieldName:String): Any? {
        if (!item.containsKey(fieldName)) {
            return null
        }
        return item[fieldName]
    }

    /**
     * Method used to set value for field with specified [fieldName]
     * @param fieldName - name of field to set
     * @param fieldValue - which value to set
     */
    open fun setFieldValue(fieldName:String,fieldValue:Any) {
        if (fields.contains(fieldName)) {
            item[fieldName] = fieldValue
        }
    }

    open operator fun get(fieldName:String):Any? {
        return getFieldValue(fieldName)
    }

    open operator fun set(fieldName:String,fieldValue:Any) {
        setFieldValue(fieldName,fieldValue)
    }

    /**
     * Method returns record data ready to save to database
     * @return HashMap with record data, preprocessed to contain only values, which are ready to send to DB
     */
    open fun getRecordForDB(): HashMap<String,Any> {
        val result = HashMap<String,Any>()
        for (key in item.keys()) {
            result[key] = item[key]!!.toString()
        }
        return result
    }
    /**
     * Method returns list of models. It queries database and returns
     * result according to rules inside "options" param. If not provided,
     * just returns all models
     * @param options: Options which defines filter, sort order, which fields to get for model
     * @param user_id: ID of user to which affected records should belong
     * @return: ArrayList of models
     */
    fun getList(options:HashMap<String,Any>? = null,user_id:String?=null): ArrayList<Model> {
        var opts = HashMap<String,Any>()
        var result = ArrayList<Model>()
        if (options != null) {
            opts = options
        }
        if (!opts.containsKey("fields")) {
            opts["fields"] = fields
        }
        opts["field_types"] = fieldTypes
        val db = DBManager.getDB()
        if (db != null) {
            result = db.getList(this,opts,user_id)
        }
        return result
    }

    /**
     * Method returns number of models in collection, after applying filters defined in
     * [options] argument. If no argument provided, returns count of all items in collection
     * @param options: Options which defines filter
     * @param user_id: ID of user to which affected records should belong
     * @return Number of models in collection
     */
    fun getCount(options: HashMap<String,Any>? = null,user_id:String?=null): Int {
        val result = 0
        var opts = HashMap<String,Any>()
        if (options != null) {
            opts = options
        }
        val db = DBManager.getDB() ?: return result
        return db.getCount(this,opts,user_id)
    }

    /**
     * Method used to get model by ID
     * @param id: ID to search
     * @param user_id: ID of user to which affected record should belong
     * @return Found model or null if model not found
     */
    fun getItem(id:String,user_id:String?=null): Model? {
        val db: Database? = DBManager.getDB() ?: return null
        this.uid = id
        return db!!.getItem(this,user_id)
    }

    /**
     * Method returns first item which meets provided condition
     * @param condition: SQL condition
     * @param user_id: ID of user to which affected record should belong
     * @return Found model or null if model not found
     */
    open fun getItemByCondition(condition:String,user_id:String?=null): Model? {
        val list = this.getList(hashMapOf("condition" to condition),user_id)
        if (list.isNotEmpty())
            return list[0]
        return null
    }

    /**
     * Method used to post new model in database
     * @param user_id: ID of user to which affected record should belong
     * @return: Either HashMap with errors or null if no errors
     */
    fun postItem(user_id:String?=null): HashMap<String,Any> {
        val result = validate(true,user_id)
        if (result != null) {
            return result
        }
        val db = DBManager.getDB() ?: return hashMapOf("general" to t("Ошибка подключения к базе данных"))
        val model = db.postItem(this,user_id)
        return model?.getRecord() ?: hashMapOf("general" to t("Внутренняя ошибка при записи в базу данных")) as HashMap<String, Any>
    }

    /**
     * Method used to update this model in database
     * @param user_id: ID of user to which affected records should belong
     * @return HashMap which is either updated record or information about validation or system errors
     */
    fun putItem(user_id:String?=null): HashMap<String,Any> {
        val result = validate(false,user_id)
        if (result != null) {
            return result
        }
        val db = DBManager.getDB() ?: return hashMapOf("general" to t("Ошибка подключения к базе данных"))
        val model = db.putItem(this,user_id)
        return model?.getRecord() ?: hashMapOf("general" to t("Внутренняя ошибка при записи в базу данных")) as HashMap<String, Any>

    }

    /**
     * Method used to delete this model from database
     * @param user_id: ID of user to which affected records should belong
     * @return HashMap which is either removed record or information about validation or system errors
     */
    fun deleteItems(ids:String,user_id:String?=null): HashMap<String,Any>? {
        val db = DBManager.getDB() ?: return hashMapOf("errors" to hashMapOf("general" to t("Ошибка подключения к базе данных")))
        return db.deleteItems(this,ids,user_id)
    }

    /**
     * Method used to get fields of model from JSONObject and return as HashMap
     * @param params: JSONObject to get fields from
     * @return HashMap with extracted fields
     */
    fun JSONToHashMap(params: JSONObject): HashMap<String,Any> {
        val result = HashMap<String,Any>()
        for (field in fields) {
            if (params.has(field)) {
                result[field] = if (params[field].toString() == "null") "" else params[field]
            }
        }
        if (params.has("@rid")) {
            result["@rid"] = params["@rid"]!!
            result.remove("uid")
        }
        return result
    }

    /**
     * Method used to populate model fields from provided HashMap
     * @param params: source hashmap
     */
    fun populate(params: HashMap<String,Any>) {
        for (fieldName in params.keys) {
            if (fieldName === "uid" && params.keys.contains("@rid")) {
                continue
            }
            if (fieldName === "@rid") {
                this["uid"] = params[fieldName]!!
            } else {
                this[fieldName] = params[fieldName]!!
            }
        }
    }

    /**
     * Method used to validate current model before posting to database
     * @param isNew: Do we check a new item
     * @return: Either Hashmap<String,String> with errors or null if no errors
     */
    open fun validate(isNew:Boolean = false,user_id:String?=null):HashMap<String,Any>? {
        return null
    }
}