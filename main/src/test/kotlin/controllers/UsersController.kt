package controllers

import utils.HashUtils
import com.google.gson.Gson
import models.User
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
        var response = TestEnvironment.parseResponse(khttp.post(url, data=""))
        assertTrue(response!!.containsKey("errors"),"Should return errors for empty body")
        val request = hashMapOf("name" to "test2")
        response = TestEnvironment.parseResponse(khttp.post(url,data=gson.toJson(request)))!!
        assertNotNull(TestEnvironment.getError(response,"email"),
                "Should return error if email not specified")
        assertNotNull(TestEnvironment.getError(response,"password"),
                "Should return error if password not specified")
        request["email"] = "andrey@it-port.ru"
        response = TestEnvironment.parseResponse(khttp.post(url,data=gson.toJson(request)))!!
        assertNotNull(TestEnvironment.getError(response,"password"),
                "Should return error if password not specified")
        request["password"] = "111111"
        response = TestEnvironment.parseResponse(khttp.post(url,data=gson.toJson(request)))!!
        assertNotNull(TestEnvironment.getError(response,"password"),
                "Should return error if passwords not match")
        request["confirm_password"] = "111111"
        response = TestEnvironment.parseResponse(khttp.post(url,data=gson.toJson(request)))!!
        assertEquals("ok",response["status"].toString(),"Should return success response")
    }

    @Test
    fun activateTest() {
        TestEnvironment.createTestUser()
        val url = "http://localhost:8086/api/user/activate"
        var response = TestEnvironment.parseResponse(khttp.get("$url/fsdfsdf"))
        assertNotNull(TestEnvironment.getError(response!!,"general"),
                "Should return error if incorrect activation token provided")
        response = TestEnvironment.parseResponse(khttp.get("$url/${HashUtils.sha512("test2andrey@it-port.ru")}"))
        assertFalse(response!!.containsKey("errors"))
        val user = (User()).getItemByCondition("name='test2'")!!
        assertEquals(1,user["active"],"Should activate user")
        response = TestEnvironment.parseResponse(khttp.get("$url/${HashUtils.sha512("test2andrey@it-port.ru")}"))
        assertNotNull(TestEnvironment.getError(response!!,"general"),
                "Should return error if user already activated")
    }

    @Test
    fun requestRestPasswordTest() {
        TestEnvironment.createTestUser()
        val url = "http://localhost:8086/api/user/request_reset_password"
        var response = TestEnvironment.parseResponse(khttp.get("$url/ema@il.ru"))
        assertNotNull(TestEnvironment.getError(response!!,"general"),
                "Should return error if incorrect email provided")
        response = TestEnvironment.parseResponse(khttp.get("$url/andrey@it-port.ru"))
        assertEquals("ok",response!!["status"],"Should return ok status after success")
    }

    @Test
    fun resetPasswordTest() {
        TestEnvironment.createTestUser()
        val gson = Gson()
        val url = "http://localhost:8086/api/user/reset_password"
        val token = HashUtils.sha512("test2andrey@it-port.ru")
        var response = TestEnvironment.parseResponse(khttp.post("$url/fdsf",data=""))
        assertNotNull(TestEnvironment.getError(response!!,"general"),
                "Should return error if incorrect token provided")
        response = TestEnvironment.parseResponse(khttp.post("$url/$token"))
        assertNotNull(TestEnvironment.getError(response!!,"general"),
                "Should return error if no new password provided")
        val request = hashMapOf("password" to "123456")
        response = TestEnvironment.parseResponse(khttp.post("$url/$token",data=gson.toJson(request)))
        assertNotNull(TestEnvironment.getError(response!!,"general"),
                "Should return error if no confirm password proviced")
        request["confirm_password"] = "111111"
        response = TestEnvironment.parseResponse(khttp.post("$url/$token",data=gson.toJson(request)))
        assertNotNull(TestEnvironment.getError(response!!,"general"),
                "Should return error if passwords not match")
        request["confirm_password"] = "123456"
        response = TestEnvironment.parseResponse(khttp.post("$url/$token",data=gson.toJson(request)))
        assertNotNull(TestEnvironment.getError(response!!,"general"),
                "Should return error if user not activated")
        var model = (User()).getItemByCondition("name='test2'")!!
        val user = User()
        user.setRecord(model.getRecord())
        user["active"] = 1
        user.putItem()
        response = TestEnvironment.parseResponse(khttp.post("$url/$token",data=gson.toJson(request)))
        assertEquals("ok",response!!["status"],"Should return success response")
        model = (User()).getItemByCondition("name='test2'")!!
        assertEquals(model["password"],HashUtils.sha512("123456"),
                "Should change user password")
    }



    @After
    fun destroyEnvironment() {
        TestEnvironment.destroyEnvironment()
    }
}
