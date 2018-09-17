package utils

import com.google.gson.Gson

import db.DBManager
import db.orientdb.OrientDatabase
import db.orientdb.OrientDBUtils
import khttp.responses.Response
import kotlinx.coroutines.experimental.launch
import models.User
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import system.ConfigManager
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class ResponseParser(private val response:Response) {
    fun asObject():JSONObject {
        val parser = JSONParser()
        return try {
            parser.parse(response.text) as JSONObject
        } catch (e:Exception) {
            JSONObject()
        }
    }
    fun asArray():JSONArray {
        val parser = JSONParser()
        return try {
            parser.parse(response.text) as JSONArray
        } catch (e:Exception) {
            JSONArray()
        }
    }
}

object TestEnvironment {

    private fun setupEnvironment() {
        val defaultConfig = hashMapOf(
                "web" to hashMapOf(
                        "host" to "localhost",
                        "port" to 8086,
                        "root" to "/home/andrey/IdeaProjects/WAB3/public",
                        "cache_path" to "/home/andrey/WAB3_cache"
                ),
                "db" to hashMapOf(
                        "host" to "localhost",
                        "port" to 2480,
                        "name" to "test2",
                        "login" to "admin",
                        "password" to "admin"
                ),
                "mail" to hashMapOf(
                        "host" to "smtp.gmail.com",
                        "port" to "465",
                        "login" to "germanovzce@gmail.com",
                        "password" to "",
                        "from" to "6@usn.ru"
                )
        )
        try {
            Files.createDirectory(Paths.get("/tmp/WAB3_tests"))
        } catch (e:Exception) {}
        ConfigManager.configFilePath = "/tmp/WAB3_tests/config.json"
        val parser = JSONParser()
        val gson = Gson()
        ConfigManager.config = parser.parse(gson.toJson(defaultConfig)) as JSONObject
        ConfigManager.saveConfig()
        launch {
            Application.run()
        }.start()
        Thread.sleep(2000)
    }

    private fun createInitialUser() {
        val db = DBManager.getDB() as OrientDatabase
        db.execQuery("INSERT INTO users set name='test', password='${HashUtils.sha512("test")}',active=1," +
                "activation_token='${HashUtils.sha512("testtest@test.com")}',email='test@test.com'")
    }

    fun createEnvironment() {
        setupEnvironment()
        createInitialUser()
    }

    fun getTestCompanyUid():String {
        return this.getRid("name='Test'","companies",true) ?: ""
    }

    fun getUserRid(condition:String):String {
        return this.getRid(condition,"users") ?: ""
    }

    fun getRid(condition:String,model:String,clean:Boolean=false):String? {
        val query = "SELECT @rid from $model where $condition"
        val db = DBManager.getDB() as OrientDatabase
        val result = db.execQueryJSON(query)
        val row = OrientDBUtils.getFirstResult(result)
        if (row!=null && row.has("@rid")) {
            return if (clean)
                this.cleanRid(row["@rid"].toString())
            else
                row["@rid"].toString()
        }
        return null
    }

    fun cleanRid(rid:String):String {
        return rid.replace("#","").replace(":","_")
    }

    fun uncleadRid(rid:String):String {
        return "#${rid.replace("#","").replace("_",":")}"
    }

    fun addTestCompany(user_id:String="") {
        val query = "INSERT INTO companies SET name='Test',inn='312311006772',kpp='123456789',type=2," +
                "address='Test addr',user=$user_id"
        (DBManager.getDB() as OrientDatabase).execQuery(query)
    }

    fun destroyEnvironment() {
        val db = DBManager.getDB() as OrientDatabase
        db.execQuery("DELETE FROM companies")
        db.execQuery("DELETE FROM accounts")
        db.execQuery("DELETE FROM income")
        db.execQuery("DELETE FROM spendings")
        db.execQuery("DELETE FROM reports")
        db.execQuery("DELETE FROM users")
        try {
            Files.delete(Paths.get("/tmp/WAB3/config.json"))
            Files.delete(Paths.get("/tmp/WAB3"))
        } catch (e:Exception) {}
    }

    fun createTestUser(): User {
        val user = User()
        user["name"] = "test2"
        user["email"] = "andrey@it-port.ru"
        user["active"] = 0
        user["password"] = "111111"
        user["activation_token"] = HashUtils.sha512("test2andrey@it-port.ru")
        user.postItem()
        return user
    }

    fun parseResponse(response: Response,asArray:Boolean = false):JSONObject? {
        return ResponseParser(response).asObject()
    }

    fun getError(response:JSONObject,field_name:String):String? {
        val errors = response["errors"] as JSONObject
        return errors[field_name] as? String
    }

    fun getAuthToken(login:String,password:String):String {
        return String(Base64.getEncoder().encode("$login:$password".toByteArray()))
    }

    fun getBasicAuthHeader(login:String,password:String):Map<String,String> {
        return mapOf("Authorization" to "Basic ${getAuthToken(login,password)}")
    }


}

