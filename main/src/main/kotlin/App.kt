@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

import utils.startsWith
import controllers.*
import db.DBManager
import db.OrientDatabase
import i18n.t
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.basicAuthentication
import io.ktor.features.Compression
import io.ktor.features.DefaultHeaders
import io.ktor.features.gzip
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import logger.LogLevels
import logger.Logger
import system.ConfigManager
import system.cleanCacheDirs
import java.util.*


/**
 * Main Application object. Initializes web server, registers controllers and starts main loop
 */
object Application {
    // HTTP port, on which server listens requests
    var port = 8085
    var host = "localhost"
    // List of available application controllers, which are able to process requests
    var controllers:HashMap<String,CRUDControllerInterface> = HashMap()
    /**
     * Application starter method
     */
    fun run() {
        ConfigManager.loadConfig()
        port = Integer(ConfigManager.webConfig["port"].toString()).toInt()
        host =ConfigManager.webConfig["host"].toString()
        val conf = ConfigManager.dbConfig
        DBManager.setDB(OrientDatabase("${conf["host"].toString()}:${conf["port"].toString()}",
                conf["name"].toString(),conf["login"].toString(),conf["password"].toString()))
        Timer().schedule(cleanCacheDirs(),0,3000)
        val server = embeddedServer(Netty, port = port) {
            install(Authentication) {
                this.basicAuthentication("wab3") {
                    val db = db.DBManager.getDB()
                    if (db != null) {
                        val uid = db.auth(it.name,it.password)
                        if (uid == null) {
                            null
                        } else {
                            io.ktor.auth.UserIdPrincipal(uid)
                        }
                    } else {
                        null
                    }
                }
                this.skipWhen {
                    val freeUrls = kotlin.collections.arrayListOf(
                            "/api/user/register",
                            "/api/user/activate",
                            "/api/user/reset_password",
                            "/api/user/request_reset_password")
                    if (it.request.httpMethod.value === "OPTIONS" || it.request.uri.startsWith("/static") ||
                            it.request.uri === "/") true
                    else if (it.request.uri.startsWith(freeUrls)) true
                    else if (it.request.queryParameters.contains("token")) {
                        val db = db.DBManager.getDB()
                        db!!.tokenAuth(it.request.queryParameters["token"]!!) !== null
                    } else {
                        false
                    }
                }
            }
            install(Compression) {
                gzip()
            }
            install(DefaultHeaders) {
                header("Access-Control-Allow-Origin","*")
                header("Access-Control-Allow-Methods","GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS")
                header("Access-Control-Allow-Headers","Authorization, Content-Type")
            }
            intercept(ApplicationCallPipeline.Call) {
                if (call.request.httpMethod == HttpMethod.Options ) {
                    call.respond(HttpStatusCode.OK)
                }
            }
            routing {
                routes()
            }
        }
        server.start(true)
        Logger.log(LogLevels.INFO,"${t("Server started on port")} $port")
    }

    /**
     * Method adds link to specified controller to global controllers list
     * @param name: String name of controller
     * @param controller: Instance of controller
     */
    fun registerController(controller:CRUDControllerInterface) {
        if (!controllers.containsKey(controller.javaClass.name)) {
            controllers[controller.javaClass.name] = controller
        }
    }
}

/**
 * Method Defines all HTTP endpoints of all controllers, available for API and other HTTP calls
 */
fun Routing.routes() {
    root()
    users()
    companies()
    accounts()
    income()
    spending()
    report()
}

// Program entry point. Starts the application
fun main(argv:Array<String>) {
    Application.run()
}