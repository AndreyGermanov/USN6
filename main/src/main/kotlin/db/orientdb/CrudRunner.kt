package db.orientdb

import com.google.gson.Gson
import models.Model
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList

/**
 * Class used to run CRUD queries for OrientDB database adapter
 * @param db: Link to underlying OrientDatabase adapter class
 * @param model: Link to database model with which need to work
 * @param user_id: Rid of user which should be owner of all query results (used as a system level filter)
 */
class CrudRunner(val db: OrientDatabase, val model: Model, val user_id:String?) {

    /**
     * Method fetches data from current model from database and returns it
     * @returns: Model or null if no model with specified UID in database
     */
    fun getItem(): Model? {
        var uid = model.uid
        if (!uid.startsWith("#")) uid = "#${uid.replace("_",":")}"
        var query = "SELECT * FROM ${model.modelName} WHERE @rid=$uid"
        if (model.isUserDependent) {
            if (user_id === null)
                return null
            query += " AND user=$user_id"
        }
        val response = db.execQueryJSON(query,hashMapOf())
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

    /**
     * Method posts model to database (creates new record)
     * @returns: Model created or null in case of internal error
     */
    fun postItem(): Model? {
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
        val response = db.execQueryJSON(query,hashMapOf()) ?: return null
        if (!response.has("result")) return null
        val resultArray = response["result"] as JSONArray
        if (resultArray.length()==0) return null
        val rec = resultArray[0] as? JSONObject ?: return null
        record["uid"] = rec["@rid"].toString()
        model.setRecord(record)
        return model
    }

    /**
     * Method puts (updates) model in database
     * @returns: Updated model or null in case of internal error
     */
    fun putItem(): Model? {
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
        db.execQueryJSON(query,hashMapOf()) ?: return null
        return model
    }

    /**
     * Method removes models with specified ID from database
     * @param ids: Comma separated list of model rids
     * @returns: Boolean true if operation successfull or false if internal error occured
     */
    fun deleteItems(ids:String):Boolean {
        val items_list = ids.split(",")
        if (items_list.isEmpty()) return false
        val items_to_delete = ArrayList<String>()
        for (item in items_list) {
            var value = item
            db.execQueryJSON("/deleteReferences/$value",hashMapOf("function" to true))
            if (!value.startsWith("#")) value = "#${value.replace("_",":")}"
            items_to_delete.add(""+value+"")
        }
        var query = "DELETE FROM ${model.modelName} WHERE @rid IN [${items_to_delete.toArray().joinToString(",")}]"
        if (model.isUserDependent) {
            if (user_id === null)
                return false
            query += " AND user=$user_id"
        }
        db.execQueryJSON(query,hashMapOf()) ?: return false
        return true
    }
}