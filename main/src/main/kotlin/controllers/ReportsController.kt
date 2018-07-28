@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package controllers

import Utils.SendMail
import com.google.gson.Gson
import i18n.t
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.*
import io.ktor.routing.Routing
import io.ktor.routing.get
import models.Report
import models.ReportTypes
import reports.ReportKudir
import system.ConfigManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


/**
 * Controller used to process all requests to Report model
 */
class ReportsController: Controller() {

    /****************************************************************
     * Implementation of methods, defined in CRUDControllerInterface
     ***************************************************************/

    override fun getModelInstance(): Report {
        return Report()
    }

    /**
     * Method used to generate report
     * @param companyRid: UID of company for which to generate report
     * @param type: Type of report to generate
     * @param period: Period of report (some date)
     * @param format: Output format (html or pdf)
     * @param token: Authentication token
     * @return Generated report in appropriate format or null if could not generate report
     */
    fun generate(companyRid:String,type:String,period:Long,format:String,token:String):String {
        return when (type) {
            "kudir" -> {
                when (format) {
                    "html" -> ReportKudir().generate(companyRid,period)
                    "pdf"  -> ReportKudir().generatePdf(companyRid,period,token)
                    else   -> ""
                }
            }
            else -> ""
        }
    }

    /**
     * Method used to generate and send report by email in PDF format
     * @param companyRid: UID of company for which to generate report
     * @param type: Type of report to generate
     * @param period: Period of report (some date)
     * @param token: Authentication token
     * @param email: Destination email address
     * @return String with error message if fail or empty string if success
     */
    fun sendByEmail(companyRid:String,type:String,period:Long,token:String,email:String?): String {
        if (email == null || email.toString().isEmpty()) {
            return t("Не указан адрес email для отправки")
        }
        val filename = this.generate(companyRid, type, period, "pdf",token) as? String
                ?: t("Ошибка при создании файла отчета для отправки ")
        if (filename.isEmpty()) {
            return t("Ошибка при создании файла отчета для отправки ")
        }
        val config = ConfigManager.mailConfig
        val email_config = hashMapOf(
                "host" to config["host"].toString(),
                "port" to config["port"].toString(),
                "login" to config["login"].toString(),
                "password" to config["password"].toString(),
                "from" to config["from"].toString(),
                "address" to email,
                "subject" to t("Отчет")
        )
        SendMail.init(email_config)
        val result: String
        result = if (SendMail.sendMessage("",arrayListOf(filename))) {
            ""
        } else {
            t("Не удалось отправить отчет по указанному адресу")
        }
        if (Files.exists(Paths.get(filename))) {
            Files.delete(Paths.get(filename))
        }
        return result
    }
}

/**
 * Function defines HTTP endpoints for actions
 */
fun Routing.report() {
    val ctrl = ReportsController()
    crud("report",ctrl)
    get("/api/report/types") {
        val gson = Gson()
        call.respond(HttpStatusCode.OK, gson.toJson(ReportTypes))
    }
    get("/report/generate/{companyRid}/{type}/{period}/{format}") {
        val format = call.parameters["format"].toString()
        val token = call.parameters["token"].toString()
        val type = call.parameters["type"].toString()
        var period: Long = 0
        try {
            period = call.parameters["period"]!!.toLong()
        } catch (e:Exception) {
            call.respond(HttpStatusCode.InternalServerError,e.message ?: t("Error getting report period"))
        }
        val companyRid = call.parameters["companyRid"].toString()
        when (format) {
            "html" -> call.respondText(ctrl.generate(companyRid, type, period, "html",token),ContentType.Text.Html)
            "pdf" -> {
                val filename = ctrl.generate(companyRid, type,period,"pdf",token)
                if (filename.isEmpty()) {
                    call.respond(HttpStatusCode.InternalServerError, "Could not generate PDF file")
                } else {
                    if (Files.exists(Paths.get(filename))) {
                        call.respondFile(File(filename))
                        Files.delete(Paths.get(filename))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Could not generate PDF file")
                    }
                }
            }
            "email" -> {
                val result = ctrl.sendByEmail(companyRid,type,period,token, call.request.queryParameters["email"])
                call.respond(HttpStatusCode.OK,result)
            }
        }
    }
}