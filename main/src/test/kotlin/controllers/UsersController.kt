package controllers

import utils.HashUtils
import com.google.gson.Gson
import khttp.responses.Response
import models.User
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.junit.After
import org.junit.Before
import org.junit.Test
import utils.TestEnvironment
import kotlin.test.*

class UsersControllerTests {

    @Before
    fun setupEnvironment() {
        TestEnvironment.createEnvironment()
    }

    @Test
    fun registerTest() {
        val gson = Gson()
        val url = "http://localhost:8086/api/user/register"
        var response = this.parseResponse(khttp.post(url, data=""))
        assertTrue(response!!.containsKey("errors"),"Should return errors for empty body")
        val request = hashMapOf("name" to "test2")
        var requestJson = gson.toJson(request)
        response = this.parseResponse(khttp.post(url,data=requestJson))!!
        assertNotNull(this.getError(response,"email"),
                "Should return error if email not specified")
        assertNotNull(this.getError(response,"password"),
                "Should return error if password not specified")
        request["email"] = "andrey@it-port.ru"
        requestJson = gson.toJson(request)
        response = this.parseResponse(khttp.post(url,data=requestJson))!!
        assertNotNull(this.getError(response,"password"),
                "Should return error if password not specified")
        request["password"] = "111111"
        requestJson = gson.toJson(request)
        response = this.parseResponse(khttp.post(url,data=requestJson))!!
        assertNotNull(this.getError(response,"password"),
                "Should return error if passwords not match")
        request["confirm_password"] = "111111"
        requestJson = gson.toJson(request)
        response = this.parseResponse(khttp.post(url,data=requestJson))!!
        assertEquals("ok",response["status"].toString(),"Should return success response")
    }

    @Test
    fun activateTest() {
        this.createTestUser()
        val url = "http://localhost:8086/api/user/activate"
        var response = this.parseResponse(khttp.get("$url/fsdfsdf"))
        assertNotNull(this.getError(response!!,"general"),
                "Should return error if incorrect activation token provided")
        response = this.parseResponse(khttp.get("$url/${HashUtils.sha512("test2andrey@it-port.ru")}"))
        assertFalse(response!!.containsKey("errors"))
        val user = (User()).getItemByCondition("name='test2'")!!
        assertEquals(1,user["active"],"Should activate user")
        response = this.parseResponse(khttp.get("$url/${HashUtils.sha512("test2andrey@it-port.ru")}"))
        assertNotNull(this.getError(response!!,"general"),
                "Should return error if user already activated")
    }

    @Test
    fun requestRestPasswordTest() {
        this.createTestUser()
        val url = "http://localhost:8086/api/user/request_reset_password"
        var response = this.parseResponse(khttp.get("$url/ema@il.ru"))
        assertNotNull(this.getError(response!!,"general"),
                "Should return error if incorrect email provided")
        response = this.parseResponse(khttp.get("$url/andrey@it-port.ru"))
        assertEquals("ok",response!!["status"],"Should return ok status after success")
    }

    @Test
    fun resetPasswordTest() {
        this.createTestUser()
        val gson = Gson()
        val url = "http://localhost:8086/api/user/reset_password"
        val token = HashUtils.sha512("test2andrey@it-port.ru")
        var response = this.parseResponse(khttp.post("$url/fdsf",data=""))
        assertNotNull(this.getError(response!!,"general"),
                "Should return error if incorrect token provided")
        response = this.parseResponse(khttp.post("$url/$token"))
        assertNotNull(this.getError(response!!,"general"),
                "Should return error if no new password provided")
        val request = hashMapOf("password" to "123456")
        var requestJson = gson.toJson(request)
        response = this.parseResponse(khttp.post("$url/$token",data=requestJson))
        assertNotNull(this.getError(response!!,"general"),
                "Should return error if no confirm password proviced")
        request["confirm_password"] = "111111"
        requestJson = gson.toJson(request)
        response = this.parseResponse(khttp.post("$url/$token",data=requestJson))
        assertNotNull(this.getError(response!!,"general"),
                "Should return error if passwords not match")
        request["confirm_password"] = "123456"
        requestJson = gson.toJson(request)
        response = this.parseResponse(khttp.post("$url/$token",data=requestJson))
        assertNotNull(this.getError(response!!,"general"),
                "Should return error if user not activated")
        var model = (User()).getItemByCondition("name='test2'")!!
        val user = User()
        user.setRecord(model.getRecord())
        user["active"] = 1
        user.putItem()
        response = this.parseResponse(khttp.post("$url/$token",data=requestJson))
        assertEquals("ok",response!!["status"],"Should return success response")
        model = (User()).getItemByCondition("name='test2'")!!
        assertEquals(model["password"],HashUtils.sha512("123456"),
                "Should change user password")
    }

    private fun createTestUser():User {
        val user = User()
        user["name"] = "test2"
        user["email"] = "andrey@it-port.ru"
        user["active"] = 0
        user["password"] = "111111"
        user["activation_token"] = HashUtils.sha512("test2andrey@it-port.ru")
        user.postItem()
        return user
    }

    private fun parseResponse(response:Response):JSONObject? {
        val parser = JSONParser()
        return try {
            parser.parse(response.text) as JSONObject
        } catch (e:Exception) {
            null
        }
    }

    private fun getError(response:JSONObject,field_name:String):String? {
        val errors = response["errors"] as JSONObject
        return errors[field_name] as? String
    }

    @After
    fun destroyEnvironment() {
        TestEnvironment.destroyEnvironment()
    }
}
