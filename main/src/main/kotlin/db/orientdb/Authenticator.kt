package db.orientdb

import org.json.JSONArray
import org.json.JSONObject
import utils.HashUtils
import java.util.*

/**
 * Class provides authentication support for OrientDB database adapter
 * @param db: Link to underlying OrientDatabase adapter class
 *
 */
class Authenticator(val db:OrientDatabase) {

    /**
     * Method executes query to authenticate user in database
     * @param: name username
     * @param: password (plain text):
     * @returns: String User Rid or null if authentication failed
     */
    fun auth(name:String, password:String): String? {
        if (name.isEmpty() || password.isEmpty()) {
            return null
        }
        val password_hash = HashUtils.sha512(password)
        val query = "SELECT @rid as uid FROM users WHERE name='$name' AND password='$password_hash'"
        val result = db.execQueryJSON(query,hashMapOf()) ?: return null
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

    /**
     * Method authenticates user by provided token
     * @param: token: HTTP BASIC Auth token
     * @returns: String User Rid of authenticated user or null if authentication failed
     */
    fun tokenAuth(token:String): String? {
        val authToken = String(Base64.getDecoder().decode(token))
        val credentials = authToken.split(":")
        if (credentials.size == 2) {
            return this.auth(credentials[0], credentials[1])
        }
        return null
    }
}