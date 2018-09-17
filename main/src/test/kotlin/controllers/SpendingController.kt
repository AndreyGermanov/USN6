package controllers

import com.google.gson.Gson
import khttp.get
import org.json.simple.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import utils.ResponseParser
import utils.TestEnvironment
import kotlin.test.*


class SpendingControllerTests {

    private val baseUrl = "http://localhost:8086/api/spending"
    private val gson = Gson()
    private val headers = TestEnvironment.getBasicAuthHeader("test2", "111111")
    private var companyRid = ""
    private var userRid = ""

    @Before
    fun setupEnvironment() {
        TestEnvironment.createEnvironment()
        TestEnvironment.createTestUser()
        userRid = TestEnvironment.getUserRid("name='test2'")
        TestEnvironment.addTestCompany(userRid)
        companyRid = "#${TestEnvironment.getTestCompanyUid().replace("_",":")}"
    }

    @Test
    fun getItem() {
        var url = "$baseUrl/sdsd"
        var response = khttp.get(url)
        assertEquals(401, response.statusCode, "Should not accept unauthenticated requests")
        response = khttp.get(url, headers = headers)
        assertFalse(response.jsonObject.has("uid"), "Should return empty JSON if incorrect item specified")
        val request = mapOf("number" to "432354333", "date" to "1234567890", "description" to "New operation",
                "amount" to "145.50", "period" to "2017", "type" to "5", "company" to companyRid)
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        val result = responseJson["result"] as JSONObject
        val uid = result["uid"].toString().replace("#", "").replace(":", "_")
        url = "$baseUrl/$uid"
        responseJson = ResponseParser(get(url, headers = headers)).asObject()
        assertEquals("432354333", responseJson["number"], "Should return correct object")
    }

    @Test
    fun getCount() {
        val url = "$baseUrl/count"
        var response = khttp.get(url)
        assertEquals(401, response.statusCode, "Should not accept unauthenticated requests")
        response = khttp.get(url, headers = headers)
        assertEquals("0", response.text, "Should return 0 if no items in database")
        val request = mapOf("number" to "432354333", "date" to "1234567890", "description" to "New operation",
                "amount" to "145.50", "period" to "2017", "type" to "5", "company" to companyRid)
        khttp.post(baseUrl, data = gson.toJson(request), headers = headers)
        response = khttp.get(url, headers = headers)
        assertEquals("1", response.text, "Should return 1 if 1 item in database")
    }

    @Test
    fun postItem() {
        var response = khttp.post(baseUrl, data = gson.toJson(mapOf("number" to "123456789", "date" to 1234567890)))
        assertEquals(401, response.statusCode, "Should not accept unauthenticated requests")
        response = khttp.post(baseUrl, data = gson.toJson(mapOf<String, String>()), headers = headers)
        assertEquals(200, response.statusCode, "Should return correct status code")
        assertEquals("error", response.jsonObject["status"].toString(), "Should return error status")
        assertTrue(response.jsonObject.has("errors"), "Should contain errors object with error descriptions")
        var request = mapOf("number" to "sdfsdf")
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "number"),
                "Should return error if incorrect number specified")
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if date not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "period"),
                "Should return error if period not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if type not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "amount"),
                "Should return error if amount not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "description"),
                "Should return error if description not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if company not specified")
        request = mapOf("number" to "123456789","description" to "Payment")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNull(TestEnvironment.getError(responseJson, "number"),
                "Should not return error if correct number specified")
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if date not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "amount"),
                "Should return error if amount not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "period"),
                "Should return error if period not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if type not specified")
        request = mapOf("description" to "Payment", "number" to "123456789", "date" to "gdfgdfg", "amount" to "jf4egdf",
                "period" to "test", "type" to "154", "company" to "#33343:12")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if incorrect date specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if incorrect type specified")
        assertNotNull(TestEnvironment.getError(responseJson, "amount"),
                "Should return error if incorrect amount specified")
        request = mapOf("description" to "Payment", "number" to "123456789","date" to "1234567890","amount" to "134.40",
                "period" to "test","type" to "5","company" to "#342343:14")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if incorrect company specified")
        request = mapOf("description" to "Payment", "number" to "123456789", "date" to "1234567890",
                "amount" to "134.40", "period" to "test","type" to "5", "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertFalse(responseJson.containsKey("errors"), "Should not return any errors")
        assertTrue(responseJson.containsKey("result"), "Should contain result field in response")
        val result = responseJson["result"] as JSONObject
        assertTrue(result.containsKey("uid"))
    }

    @Test
    fun putItem() {
        var request = mapOf("description" to "Payment", "number" to "123456789", "date" to "1234567890",
                "amount" to "134.40", "period" to "test","type" to "5","company" to companyRid)
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        var result = responseJson["result"] as JSONObject
        val uid = result["uid"].toString().replace("#", "").replace(":", "_")
        var url = "$baseUrl/223"
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "general"),
                "Should return error if incorrect RID specified to update")
        url = "$baseUrl/$uid"
        val response = khttp.post(url, data = gson.toJson(request))
        assertEquals(401, response.statusCode, "Should not accept unauthenticated requests")

        request = mapOf("number" to "123456789","description" to "", "amount" to "", "date" to "",
                "period" to "", "type" to "","company" to "")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "description"),
                "Should return error if description not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if date not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "amount"),
                "Should return error if amount not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "period"),
                "Should return error if period not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if type not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if company not specified")
        request = mapOf("description" to "Payment", "number" to "123456789", "amount" to "", "date" to "",
                "company" to "","period" to "","type" to "")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNull(TestEnvironment.getError(responseJson, "number"),
                "Should not return error if correct number specified")
        assertNotNull(TestEnvironment.getError(responseJson, "amount"),
                "Should return error if amount not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if date not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "period"),
                "Should return error if period not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if type not specified")
        request = mapOf("description" to "Payment", "number" to "123456789", "amount" to "sdfsd", "date" to "jf4egdf",
                "type" to "155", "period" to "test", "company" to "#33343:12")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "amount"),
                "Should return error if incorrect amount specified")
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if incorrect date specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if incorrect type specified")
        request = mapOf("description" to "Payment", "number" to "123456789","date" to "1234567890",
                "amount" to "134.40","period" to "test","type" to "5","company" to "#342343:14")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if incorrect company specified")
        request = mapOf("description" to "Payment", "number" to "123456789","date" to "1234567890",
                "amount" to "134.40","period" to "test","type" to "5", "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertFalse(responseJson.containsKey("errors"), "Should not return any errors")
        assertTrue(responseJson.containsKey("result"), "Should contain result field in response")
        result = responseJson["result"] as JSONObject
        assertTrue(result.containsKey("uid"))
        assertEquals(result["number"].toString(), "123456789","Should update number successfully")
        assertEquals(result["description"].toString(), "Payment", "Should update description successfully")
        assertEquals(result["amount"].toString(), "134.40", "Should update amount successfully")
        assertEquals(result["period"].toString(), "test", "Should update period successfully")
        assertEquals(result["type"].toString(), "5", "Should update type successfully")
    }

    @Test
    fun deleteItems() {
        var request = mapOf("number" to "432354333", "date" to "1234567890", "description" to "New operation",
                "amount" to "145.50", "period" to "2017", "type" to "5", "company" to companyRid)
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        var result = responseJson["result"] as JSONObject
        val uid1 = result["uid"].toString().replace("#", "").replace(":", "_")
        request = mapOf("number" to "432354333", "date" to "1234567890", "description" to "New operation",
                "amount" to "145.50", "period" to "2017", "type" to "5", "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        result = responseJson["result"] as JSONObject
        val uid2 = result["uid"].toString().replace("#", "").replace(":", "_")
        request = mapOf("number" to "432354333", "date" to "1234567890", "description" to "New operation",
                "amount" to "145.50", "period" to "2017", "type" to "5", "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        result = responseJson["result"] as JSONObject
        val uid3 = result["uid"].toString().replace("#", "").replace(":", "_")
        var url = "$baseUrl/1,2,3,4,5"
        val response = khttp.delete(url)
        assertEquals(401, response.statusCode, "Should not accept unauthenticated requests")
        responseJson = TestEnvironment.parseResponse(khttp.delete(url, headers = headers))!!
        assertTrue(responseJson.containsKey("errors"), "Should return error if delete incorrect items")
        url = "$baseUrl/$uid3,$uid1"
        responseJson = TestEnvironment.parseResponse(khttp.delete(url, headers = headers))!!
        assertFalse(responseJson.containsKey("errors"), "Should not return error if delete correct items")
        assertEquals("ok", responseJson["status"].toString(), "Should return status=ok")
        var responseArray = ResponseParser(get(baseUrl, headers = headers)).asArray()
        assertEquals(1, responseArray.size, "Should remove two items from database")
        url = "$baseUrl/$uid2"
        responseJson = TestEnvironment.parseResponse(khttp.delete(url, headers = headers))!!
        assertFalse(responseJson.containsKey("errors"), "Should not return error if delete correct items")
        assertEquals("ok", responseJson["status"].toString(), "Should return status=ok")
        responseArray = ResponseParser(get(baseUrl, headers = headers)).asArray()
        assertEquals(0, responseArray.size, "Should remove one item from database")
    }

    @After
    fun destroyEnvironment() {
        TestEnvironment.destroyEnvironment()
    }
}