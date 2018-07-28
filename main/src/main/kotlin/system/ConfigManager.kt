package system

import com.google.gson.Gson
import i18n.t
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Object used to manage system configuration
 */
object ConfigManager {
    // Path to configuration file
    var configFilePath = System.getProperty("user.dir")+"/config.json"
    // Configuration file object
    var config = JSONObject()
    // Web path of config
    val webConfig: JSONObject
        get() = config["web"] as? JSONObject ?: JSONObject()
    // Database path of config
    val dbConfig: JSONObject
        get() = config["db"] as? JSONObject ?: JSONObject()
    val mailConfig: JSONObject
        get() = config["mail"] as? JSONObject ?: JSONObject()
    // Default configuration
    var defaultConfig = hashMapOf(
            "web" to hashMapOf(
                    "host" to "localhost",
                    "port" to 8085,
                    "root" to "public",
                    "cache_path" to "/tmp/WAB3_cache"
            ),
            "db" to hashMapOf(
                    "host" to "localhost",
                    "port" to 2480,
                    "name" to "wab3",
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

    /**
     * Method used to load config from configuration file
     */
    fun loadConfig() {
        val parser = JSONParser()
        try {
            val stream = InputStreamReader(FileInputStream(configFilePath))
            val data = stream.readText()
            val loaded_config = parser.parse(data) as JSONObject
            val validationResult = validateConfig(loaded_config)
            if (validationResult!==null) {
                println(validationResult)
                System.exit(1)
            }
            config = loaded_config
        } catch (e: java.io.FileNotFoundException) {
            println("${t("Config file not found")}: $configFilePath. ${t("Loading default config file")}")
            val gson = Gson()
            config = parser.parse(gson.toJson(defaultConfig)) as JSONObject
            saveConfig()
        } catch (e: ParseException) {
            println("${t("Could not parse configuration file")} $configFilePath. ${t("Please check and fix errors.")}")
            System.exit(1)
        }
    }

    /**
     * Method used to save config to configuration ifle
     * @param src: Configuration to save (if not provided, will save current active config)
     */
    fun saveConfig(src:JSONObject?=null) {
        var configToSave = config
        if (src !== null) {
            configToSave = src
        }
        val validationResult = validateConfig(configToSave)
        if (validationResult !== null) {
            println("${t("Could not save config due to error")}: $validationResult")
            return
        }
        val content = org.json.JSONObject(configToSave.toJSONString()).toString(4)
        val outputStream = OutputStreamWriter(FileOutputStream(configFilePath))
        outputStream.write(content)
        outputStream.flush()
    }

    /**
     * Method used to validate config.
     * @param src: Configuration to validate
     * @return: Error message or null, if no errors
     */
    fun validateConfig(src:JSONObject?=null):String? {
        var configToCheck = config
        if (src !== null) {
            configToCheck = src
        }
        if (!configToCheck.containsKey("web"))
            return t("Config file does not have 'web' section")
        val web = configToCheck["web"] as? JSONObject ?: return t("Incorrect format of 'web' section in config file")
        if (!web.containsKey("host") || web["host"].toString().isEmpty())
            return t("Incorrect 'host' in 'web' section of config file")
        if (!web.containsKey("port") || web["port"].toString().isEmpty())
            return t("Incorrect 'port' in 'web' section of config file")
        if (!web.containsKey("root") || web["root"].toString().isEmpty())
            return t("Incorrect 'root' path in 'web' section of config file")
        if (!Files.exists(Paths.get(web["root"].toString()))) {
            return t("Provided 'root' path in 'web' section of config file does not exist")
        }
        if (!configToCheck.containsKey("db"))
            return t("Config file does not have 'db' section")
        val db = configToCheck["db"] as? JSONObject ?: return t("Incorrect format of 'db' section in config file")
        if (!db.containsKey("host") || db["host"].toString().isEmpty())
            return t("Incorrect 'host' in 'db' section of config file")
        if (!db.containsKey("port") || db["port"].toString().isEmpty())
            return t("Incorrect 'port' in 'db' section of config file")
        if (!db.containsKey("name") || db["name"].toString().isEmpty())
            return t("Incorrect 'name' in 'db' section of config file")
        if (!db.containsKey("login") || db["login"].toString().isEmpty())
            return t("Incorrect 'login' in 'db' section of config file")
        if (!db.containsKey("password") || db["password"].toString().isEmpty())
            return t("Incorrect 'password' in 'db' section of config file")
        return null
    }
}