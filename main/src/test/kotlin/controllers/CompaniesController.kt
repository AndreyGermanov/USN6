package controllers

import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import utils.TestEnvironment
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompaniesControllerTests {

    @Before
    fun setupEnvironment() {
        TestEnvironment.createEnvironment()
    }

    @Test
    fun postItem() {
        var response = khttp.post("http://localhost:8086/api/company",
                data=mapOf("name" to "Test company","inn" to 2353434545))
        assertEquals(401,response.statusCode,"Should not accept unauthenticated requests")
        val token = String(Base64.getEncoder().encode("test:test".toByteArray()))
        val headers = mapOf("Authorization" to "Basic $token")
        response = khttp.post("http://localhost:8086/api/company",
                data=mapOf<String,String>(),
                headers=headers)
        assertEquals(200,response.statusCode,"Should return correct status code")
        assertEquals("error",response.jsonObject["status"].toString(),"Should return error status")
        assertTrue(response.jsonObject.has("errors"),"Should contain errors object with error descriptions")
        val errors = response.jsonObject["errors"] as JSONObject
        assertTrue(errors.has("inn") && errors.has("name") && errors.has("address")
        && errors.has("type"),"Should contain errors related to all required fields")
    }

    @After
    fun destroyEnvironment() {
        TestEnvironment.destroyEnvironment()
    }
}