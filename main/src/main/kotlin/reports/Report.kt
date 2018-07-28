package reports

import org.json.JSONObject
import system.ConfigManager
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Base interface which all reports should implement
 */
interface Report {
    /// Method used to query data from database and populate data for report
    fun getData(options:HashMap<String,String>):JSONObject
    /// Method used to produce report HTML, using data
    fun generate(companyRid:String,period:Long):String
    /// Method used to produce PDF report, using data
    fun generatePdf(companyRid:String,period:Long,token:String): String
}

/**
 * Base class of all reports. Contains basic members. Other reports inherits from it
 */
open class BaseReport: Report {
    override fun getData(options: HashMap<String, String>): JSONObject {
        return JSONObject()
    }

    override fun generate(companyRid: String, period: Long): String {
        return ""
    }

    override fun generatePdf(companyRid: String, period: Long, token: String): String {
        return ""
    }

    /**
     * Method used to prepare cache path which report uses to create temporary files.
     * @return Created path as String
     */
    fun prepareRequestPath():String {
        val request_id = UUID.randomUUID().toString()
        val cache_path = ConfigManager.webConfig["cache_path"].toString()
        if (!Files.exists(Paths.get(cache_path))) {
            Files.createDirectory(Paths.get(cache_path))
        }
        if (!Files.exists(Paths.get("$cache_path/$request_id"))) {
            Files.createDirectory(Paths.get("$cache_path/$request_id"))
            if (!Files.exists(Paths.get("$cache_path/$request_id/lockfile"))) {
                FileOutputStream("$cache_path/$request_id/lockfile").write("LOCK".toByteArray())
            }
        }
        return "$cache_path/$request_id"
    }

}