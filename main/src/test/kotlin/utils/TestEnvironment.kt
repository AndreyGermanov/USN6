package utils

import Utils.HashUtils
import com.google.gson.Gson

import db.DBManager
import db.OrientDatabase
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import system.ConfigManager
import java.nio.file.Files
import java.nio.file.Paths

object TestEnvironment {

    fun createEnvironment() {
        var defaultConfig = hashMapOf(
                "web" to hashMapOf(
                        "host" to "localhost",
                        "port" to 8086,
                        "root" to "/home/andrey/IdeaProjects/WAB3/public",
                        "cache_path" to "/home/andrey/WAB3_cache"
                ),
                "db" to hashMapOf(
                        "host" to "localhost",
                        "port" to 2480,
                        "name" to "test",
                        "login" to "admin",
                        "password" to "admin"
                ),
                "mail" to hashMapOf(
                        "host" to "smtp.gmail.com",
                        "port" to "465",
                        "login" to "germanovzce@gmail.com",
                        "password" to "FckMce123",
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
        val db = DBManager.getDB() as OrientDatabase
        db.execQuery("INSERT INTO users set name='test', password='${HashUtils.sha512("test")}',surname='Test'")
    }

    fun getTestCompanyUid():String {
        val query = "SELECT @rid as uid FROM companies where name='Test'"
        val db = DBManager.getDB() as OrientDatabase
        val result = db.execQueryJSON(query)
        val row = db.getFirstResult(result)
        if (row!=null && row.has("uid")) {
            return row["uid"].toString().replace("#","").replace(":","_")
        }
        return ""
    }

    fun addTestCompany() {
        val query = "INSERT INTO companies SET name='Test',inn='312311006772',kpp='123456789',type=2,address='Test addr'"
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


}