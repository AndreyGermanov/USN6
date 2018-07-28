package controllers

import io.ktor.routing.*
import models.Income

/**
 * Controller used to process all requests to Account model
 */
class IncomeController: Controller() {

    /****************************************************************
     * Implementation of methods, defined in CRUDControllerInterface
     ***************************************************************/

    override fun getModelInstance(): Income {
        return Income()
    }
}

/**
 * Function defines HTTP endpoints for actions
 */
fun Routing.income() {
    crud("income",IncomeController())
}