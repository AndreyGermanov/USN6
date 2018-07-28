package controllers

import io.ktor.routing.*
import models.User

/**
 * Controller used to process all requests to User model
 */
class UsersController: Controller() {

    /****************************************************************
     * Implementation of methods, defined in CRUDControllerInterface
     ***************************************************************/

    override fun getModelInstance(): User {
        return User()
    }

    override fun cleanListItem(item: Any): HashMap<String, Any> {
        val result = super.cleanListItem(item)
        if (result.containsKey("password")) {
            result.remove("password")
        }
        return result
    }
}

/**
 * Function defines HTTP endpoints for actions
 */
fun Routing.users() {
    crud("user",UsersController())
}