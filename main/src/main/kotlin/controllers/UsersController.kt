package controllers

import Utils.HashUtils
import Utils.SendMail
import com.google.gson.Gson
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

    fun register(requestBody:String):HashMap<String,Any>? {
        val parser = JSONParser()
        var fields:JSONObject
        try {
            fields = parser.parse(requestBody) as JSONObject
        } catch (e:Exception) {
            return hashMapOf("general" to t("Некорректный формат запроса"))
        }
        val user = this.getModelInstance()
        val errors = user.validate(fields,true)
        if (errors!==null) return errors
        val activationToken = HashUtils.sha512("${fields["name"]}${fields["email"]}")
        user["name"] = fields["name"]!!
        user["password"] = fields["password"]!!
        user["email"] = fields["email"]!!
        user["active"] = 0
        user["activation_token"] = activationToken
        val result = user.postItem(null)
        if (!result.containsKey("uid"))
            return hashMapOf("general" to t("Внутренняя ошибка при сохранении пользователя в БД"))
        if (!sendActivationEmail(user["email"].toString(),activationToken))
            return hashMapOf("general" to t("Не удалось отправить ссылку для активации по указанному email"))
        return null
    }

    fun activate(token:String):HashMap<String,Any>? {

        val users = (this.getModelInstance()).getList(hashMapOf("condition" to "activation_token='$token'"))
        if (users.isEmpty()) return hashMapOf("general" to t("Пользователь не найден"))
        val user = User()
        user.setRecord(users[0].getRecord())
        if (user["active"]!=0) return hashMapOf("general" to t("Учетная запись данного пользователя уже активирована"))
        user["active"] = 1
        val result = user.putItem(null)
        if (result.containsKey("general")) return result
        return null
    }

    fun requestResetPassword(email:String):HashMap<String,Any>? {
        val users = (this.getModelInstance()).getList(hashMapOf("condition" to "email='$email'"));
        if (users.isEmpty()) return hashMapOf("general" to t("Пользователь не найден"))
        val user = User()
        user.setRecord(users[0].getRecord())
        user["activation_token"] = HashUtils.sha512("${user["name"]}${user["email"]}")
        val result = user.putItem(null)
        if (!result.containsKey("uid"))
            return hashMapOf("general" to t("Внутренняя ошибка при сохранении пользователя в БД"))
        if (!sendResetPasswordEmail(user["email"].toString(),user["activation_token"].toString()))
            return hashMapOf("general" to t("Не удалось отправить ссылку сброса пароля по указанному email"))
        return null
    }

    private fun sendActivationEmail(email:String,activationToken:String):Boolean {
        val web_config = ConfigManager.webConfig
        return sendEmail(email,t("Активация учетной записи"),
                t("Пожалуйста, перейдите по следующей ссылке для активации своей учетной записи")+": "+
                        "http:${web_config["host"]}:${web_config["port"]}/api/user/activate/$activationToken");
    }

    private fun sendResetPasswordEmail(email:String,activationToken:String):Boolean {
        val web_config = ConfigManager.webConfig
        return sendEmail(email,t("Сброс пароля"),
                t("Пожалуйста, перейдите по следующей ссылке для изменения своего пароля")+": "+
                        "http:${web_config["host"]}:${web_config["port"]}/user/reset/$activationToken")
    }

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

    fun resetPassword(resetToken:String,requestBody:String):HashMap<String,Any>? {
        val users = (this.getModelInstance()).getList(hashMapOf("condition" to "activation_token='$resetToken'"))
        if (users.isEmpty()) return hashMapOf("general" to t("Пользователь не найден"))
        val user = User()
        user.setRecord(users[0].getRecord())
        val parser = JSONParser()
        var fields:JSONObject
        try {
            fields = parser.parse(requestBody) as JSONObject
        } catch (e:Exception) {
            return hashMapOf("general" to t("Некорректный формат запроса"))
        }
        val error = user.validatePassword(fields)
        if (error!==null) return hashMapOf("general" to error)
        user["password"] = fields["password"].toString()
        val result = user.putItem(null)
        if (!result.containsKey("uid"))
            return hashMapOf("general" to t("Внутренняя ошибка при сохранении пользователя в БД"))
        return null
    }

}

/**
 * Function defines HTTP endpoints for actions
 */
fun Routing.users() {
    val gson = Gson()
    val ctrl = UsersController()
    crud("user",ctrl)
    post("/api/user/register") {
        val result = ctrl.register(call.receiveText())
        if (result === null)
            call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "ok")))
        else
            call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "error", "errors" to result)))
    }
    get("/api/user/activate/{token}") {
        val result = ctrl.activate(call.parameters["token"]!!)
        if (result === null)
            call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "ok")))
        else
            call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "error", "errors" to result)))
    }
    get("/api/user/request_reset_password/{email}") {
        val result = ctrl.requestResetPassword(call.parameters["email"]!!)
        if (result === null)
            call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "ok")))
        else
            call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "error", "errors" to result)))
    }
    post("/api/user/reset_password/{resetToken}") {
        val result = ctrl.resetPassword(call.parameters["resetToken"]!!,call.receiveText())
        if (result === null)
            call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "ok")))
        else
            call.respond(HttpStatusCode.OK, gson.toJson(hashMapOf("status" to "error", "errors" to result)))
    }
}