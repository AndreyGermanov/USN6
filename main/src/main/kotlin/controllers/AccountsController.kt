package controllers

import io.ktor.routing.*
import models.Account

/**
 * Controller used to process all requests to Account model
 */
class AccountsController: Controller() {

    /****************************************************************
     * Implementation of methods, defined in CRUDControllerInterface
     ***************************************************************/

    override fun getModelInstance(): Account {
        return Account()
    }
}

/**
 * Function defines HTTP endpoints for actions
 */
fun Routing.accounts() {
    crud("account",AccountsController())
}