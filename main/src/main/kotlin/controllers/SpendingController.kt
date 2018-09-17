@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package controllers

import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import models.Spending
import models.SpendingTypes

/**
 * Controller used to process all requests to Account model
 */
class SpendingController: Controller() {

    /****************************************************************
     * Implementation of methods, defined in CRUDControllerInterface
     ***************************************************************/

    override fun getModelInstance(): Spending {
        return Spending()
    }
}

/**
 * Function defines HTTP endpoints for actions
 */
fun Routing.spending() {
    crud("spending",SpendingController())
    get("/api/spending/types") {
        val gson = Gson()
        call.respond(HttpStatusCode.OK, gson.toJson(SpendingTypes))
    }
}