package controllers

import utils.HashUtils
import utils.jsonObjectToHashMap
import utils.SendMail
import i18n.t
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.*
import models.User
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import system.ConfigManager

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

    /**
     * Method handles "Register User" API call
     * @param requestBody: user register parameters as JSON string. Should contain "name", "email", "password" and
     * "confirm_password" fields. If success, sends email with activation link
     * @returns: Null if success or HashMap with errors.
     */
    fun register(requestBody:String):HashMap<String,Any>? {
        val fields = this.parseRequestBody(requestBody) ?:
            return this.getErrorResponse("Некорректный формат запроса")
        val user = this.getModelInstance()
        val errors = user.validate(fields,true)
        if (errors!==null) return errors
        val activationToken = this.generateToken(fields)
        user.setRecord(hashMapOf(
            "name" to fields["name"]!!,
            "password" to fields["password"]!!,
            "email" to fields["email"]!!,
            "activation_token" to activationToken
        ))
        val result = user.postItem(null)
        if (result.containsKey("general")) return result
        if (!sendActivationEmail(user["email"].toString(),activationToken))
            return this.getErrorResponse("Не удалось отправить ссылку для активации по указанному email")
        return null
    }

    /**
     * Method handles "Activate user account" call. If success, change "active" flag for user account to 1 value.
     * @param token: Activation token
     * @returns: Null if success or Hashmap with errors
     */
    fun activate(token:String):HashMap<String,Any>? {
        val user = User().getItemByCondition("activation_token='$token'") ?:
            return this.getErrorResponse("Пользователь не найден")
        if (user["active"]!=0) return this.getErrorResponse("Учетная запись данного пользователя уже активирована")
        user["active"] = 1
        val result = user.putItem()
        if (result.containsKey("general")) return result
        return null
    }

    /**
     * Method handles "Reset password request" API Call. If success, sends "Reset password" email to user
     * @param email: Email of user account to reset password
     * @returns: Null if success or Hashmap with errors
     */
    fun requestResetPassword(email:String):HashMap<String,Any>? {
        val user = User().getItemByCondition("email='$email'") ?:
            return this.getErrorResponse("Пользователь не найден")
        user["activation_token"] = this.generateToken(user.getRecord())
        val result = user.putItem()
        if (result.containsKey("general")) return result
        if (!sendResetPasswordEmail(user["email"].toString(),user["activation_token"].toString()))
            return this.getErrorResponse("Не удалось отправить ссылку сброса пароля по указанному email")
        return null
    }

    /**
     * Method handles "Reset password" API Call. If success, changes password in database
     * @param resetToken: Token from "Reset password" email
     * @param requestBody: JSON string with "password" and "confirm_password" fields
     * @returns: Null if success or Hashmap with errors
     */
    fun resetPassword(resetToken:String,requestBody:String):HashMap<String,Any>? {
        val user = User().getItemByCondition("activation_token='$resetToken'") ?:
            return this.getErrorResponse("Пользователь не найден")
        val fields = this.parseRequestBody(requestBody) ?:
            return this.getErrorResponse("Некорректный формат запроса")
        val error = user.validatePassword(fields)
        if (error!==null) return this.getErrorResponse(error)
        if (user["active"] == 0) return this.getErrorResponse("Учетная запись не активирована")
        user["password"] = fields["password"].toString()
        val result = user.putItem(null)
        if (result.containsKey("general")) return result
        return null
    }

    /**
     * Utility method used to send email message with activation link to user
     * @param email: Destination email address
     * @param activationToken: Activation token
     * @returns: Boolean true if sent successfully and false otherwise
     */
    private fun sendActivationEmail(email:String,activationToken:String):Boolean {
        val web_config = ConfigManager.webConfig
        return sendEmail(email,t("Активация учетной записи"),
                t("Пожалуйста, перейдите по следующей ссылке для активации своей учетной записи")+": "+
                        "http:${web_config["host"]}:${web_config["port"]}/api/user/activate/$activationToken")
    }

    /**
     * Utility method used to send reset password email to user
     * @param email: Destination email address
     * @param resetToken: Reset password token
     * @returns: Boolean true if sent successfully and false otherwise
     */
    private fun sendResetPasswordEmail(email:String,resetToken:String):Boolean {
        val web_config = ConfigManager.webConfig
        return sendEmail(email,t("Сброс пароля"),
                t("Пожалуйста, перейдите по следующей ссылке для изменения своего пароля")+": "+
                        "http:${web_config["host"]}:${web_config["port"]}#/reset_password/$resetToken")
    }

    /**
     * Utility method used to send email message
     * @param email: Destination email address
     * @param subject: Message subject
     * @param text: Message body
     * @returns: Boolean true if sent successfully and false otherwise
     */
    private fun sendEmail(email:String,subject:String,text:String): Boolean {
        val config = ConfigManager.mailConfig
        val email_config = hashMapOf(
                "host" to config["host"].toString(),
                "port" to config["port"].toString(),
                "login" to config["login"].toString(),
                "password" to config["password"].toString(),
                "from" to config["from"].toString(),
                "address" to email,
                "subject" to subject
        )
        SendMail.init(email_config)
        return SendMail.sendMessage(text)
    }

    /**
     * Utility method used to transform JSON string of request body to JSON Object
     * @param requestBody: RAW request body
     * @returns JSONObject with parsed result or null if incorrect JSON format
     */
    private fun parseRequestBody(requestBody:String):JSONObject? {
        val parser = JSONParser()
        return try {
            parser.parse(requestBody) as JSONObject
        } catch (e:Exception) {
            null
        }
    }

    /**
     * Utility method used to return uniform general error response
     * @param text: Text of error
     * @returns: HashMap with error response
     */
    private fun getErrorResponse(text:String):HashMap<String,Any> {
        return hashMapOf("general" to t(text))
    }

    /**
     * Method used to generate token for provided record (activation or reset password token)
     * @param fields: User record either as JSONObject or as HashMap
     * @returns string Generated token
     */
    private fun generateToken(fields:Any):String {
        val map = when (fields) {
            is HashMap<*, *> -> fields as HashMap<String,Any>
            is JSONObject -> jsonObjectToHashMap(fields)
            else -> return ""
        }
        return HashUtils.sha512("${map["name"]}${map["email"]}")
    }
}

/**
 * Function defines HTTP endpoints for actions
 */
fun Routing.users() {
    val ctrl = UsersController()
    crud("user",ctrl)
    post("/api/user/register") {
        call.respond(HttpStatusCode.OK,ctrl.getResponse(ctrl.register(call.receiveText())))
    }
    get("/api/user/activate/{token}") {
        val result = ctrl.activate(call.parameters["token"]!!)
        var response = t("Ваша учетная запись активирована, можете входить в систему")
        if (result !== null) {
            response = result["general"].toString()
        }
        call.respond(HttpStatusCode.OK,response)
    }
    get("/api/user/request_reset_password/{email}") {
        call.respond(HttpStatusCode.OK,ctrl.getResponse(ctrl.requestResetPassword(call.parameters["email"]!!)))
    }
    post("/api/user/reset_password/{resetToken}") {
        call.respond(HttpStatusCode.OK,
                ctrl.getResponse(ctrl.resetPassword(call.parameters["resetToken"]!!, call.receiveText())))
    }
}