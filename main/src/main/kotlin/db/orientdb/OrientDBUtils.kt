package db.orientdb

import org.json.JSONArray
import org.json.JSONObject

/*************************************************
 * Helper functions for OrientDB database adapter*
 ************************************************/

object OrientDBUtils {
    /*
     * Helper method returns first row from OrientDatabase query response
     * @param responseJSON:
     */
    fun getFirstResult(responseJSON: JSONObject?): JSONObject? {
        if (responseJSON==null) return null
        if (!responseJSON.has("result")) return null
        val responseArray = responseJSON["result"] as? JSONArray ?: return null
        if (responseArray.length()==0) return null
        return responseArray[0] as? JSONObject ?: return null
    }
}