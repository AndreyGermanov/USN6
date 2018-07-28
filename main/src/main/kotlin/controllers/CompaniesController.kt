package controllers

import io.ktor.routing.*
import models.Company

/**
 * Controller used to process all requests to Company model
 */
class CompaniesController: Controller() {

    /****************************************************************
     * Implementation of methods, defined in CRUDControllerInterface
     ***************************************************************/

    override fun getModelInstance(): Company {
        return Company()
    }
}

/**
 * Function defines HTTP endpoints for actions
 */
fun Routing.companies() {
    crud("company",CompaniesController())
}