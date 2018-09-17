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

class AccountsControllerTests {

    private val baseUrl = "http://localhost:8086/api/account"
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
        val request = mapOf("number" to "432354333", "bik" to "4324233", "ks" to "242342343",
                "bank_name" to "Bank of NY", "company" to companyRid)
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
        val request = mapOf("number" to "432354333", "bik" to "4324233", "ks" to "242342343",
                "bank_name" to "Bank of NY", "company" to companyRid)
        khttp.post(baseUrl, data = gson.toJson(request), headers = headers)
        response = khttp.get(url, headers = headers)
        assertEquals("1", response.text, "Should return 1 if 1 item in database")
    }

    @Test
    fun postItem() {
        var response = khttp.post(baseUrl, data = gson.toJson(mapOf("number" to "123456789", "bik" to 567545)))
        assertEquals(401, response.statusCode, "Should not accept unauthenticated requests")
        response = khttp.post(baseUrl, data = gson.toJson(mapOf<String, String>()), headers = headers)
        assertEquals(200, response.statusCode, "Should return correct status code")
        assertEquals("error", response.jsonObject["status"].toString(), "Should return error status")
        assertTrue(response.jsonObject.has("errors"), "Should contain errors object with error descriptions")
        var request = mapOf("number" to "123456789")
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "bank_name"),
                "Should return error if bank_name not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "ks"),
                "Should return error if KS not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "bik"),
                "Should return error if bik not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if company not specified")
        request = mapOf("bank_name" to "Bank of NY", "number" to "123456789")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNull(TestEnvironment.getError(responseJson, "number"),
                "Should not return error if correct number specified")
        assertNotNull(TestEnvironment.getError(responseJson, "ks"),
                "Should return error if KS not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "bik"),
                "Should return error if BIK not specified")
        request = mapOf("bank_name" to "Bank of NY", "number" to "123456789", "ks" to "gdfgdfg", "bik" to "jf4egdf", "company" to "#33343:12")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "bik"),
                "Should return error if incorrect bik specified")
        request = mapOf("bank_name" to "Bank of NY", "number" to "123456789","ks" to "34242343","bik" to "4324234","company" to "#342343:14")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if incorrect company specified")
        request = mapOf("bank_name" to "Bank of NY", "number" to "123456789", "ks" to "87943234343", "bik" to "12435453", "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertFalse(responseJson.containsKey("errors"), "Should not return any errors")
        assertTrue(responseJson.containsKey("result"), "Should contain result field in response")
        val result = responseJson["result"] as JSONObject
        assertTrue(result.containsKey("uid"))
        request = mapOf("bank_name" to "Bank of NY", "number" to "123456789", "ks" to "87943234343", "bik" to "12435453", "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "number"),
                "Should return error if try to insert item with the same number")
    }

    @Test
    fun putItem() {
        var request = mapOf("bank_name" to "Bank of NY", "number" to "123456789", "ks" to "7654332123", "bik" to "234234334", "company" to companyRid)
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


        request = mapOf("number" to "123456789","bank_name" to "", "ks" to "", "bik" to "", "company" to "")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "bank_name"),
                "Should return error if bank_name not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "ks"),
                "Should return error if KS not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "bik"),
                "Should return error if bik not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if company not specified")
        request = mapOf("bank_name" to "Bank of NY", "number" to "123456789", "ks" to "", "bik" to "", "company" to "")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNull(TestEnvironment.getError(responseJson, "number"),
                "Should not return error if correct INN specified")
        assertNotNull(TestEnvironment.getError(responseJson, "ks"),
                "Should return error if KS not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "bik"),
                "Should return error if BIK not specified")
        request = mapOf("bank_name" to "Bank of NY", "number" to "123456789", "ks" to "gdfgdfg", "bik" to "jf4egdf", "company" to "#33343:12")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "bik"),
                "Should return error if incorrect bik specified")
        request = mapOf("bank_name" to "Bank of NY", "number" to "123456789","ks" to "34242343","bik" to "4324234","company" to "#342343:14")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if incorrect company specified")
        request = mapOf("bank_name" to "Bank of NY", "number" to "123456789", "ks" to "87943234343", "bik" to "12435453", "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertFalse(responseJson.containsKey("errors"), "Should not return any errors")
        assertTrue(responseJson.containsKey("result"), "Should contain result field in response")
        result = responseJson["result"] as JSONObject
        assertTrue(result.containsKey("uid"))
        request = mapOf("bank_name" to "Bank of NY", "number" to "987654321", "ks" to "23434345", "bik" to "2342324", "company" to companyRid)
        TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        request = mapOf("bank_name" to "Bank of NY", "number" to "987654321", "ks" to "87943234343", "bik" to "12435453", "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "number"),
                "Should return error if try to set number of other account")
        request = mapOf("bank_name" to "Bank of America", "number" to "1234567810", "ks" to "87943234343", "bik" to "12435453", "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertFalse(responseJson.containsKey("errors"))
        result = responseJson["result"] as JSONObject
        assertEquals(result["number"].toString(), "1234567810","Should update number successfully")
        assertEquals(result["bank_name"].toString(), "Bank of America", "Should update bank name successfully")
    }

    @Test
    fun deleteItems() {
        var request = mapOf("number" to "432354333", "bik" to "4324233", "ks" to "242342343",
                "bank_name" to "Bank of NY", "company" to companyRid)
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        var result = responseJson["result"] as JSONObject
        val uid1 = result["uid"].toString().replace("#", "").replace(":", "_")
        request = mapOf("number" to "123456789", "bik" to "4324233", "ks" to "242342343",
                "bank_name" to "Bank of NY", "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        result = responseJson["result"] as JSONObject
        val uid2 = result["uid"].toString().replace("#", "").replace(":", "_")
        request = mapOf("number" to "987654321", "bik" to "4324233", "ks" to "242342343",
                "bank_name" to "Bank of NY", "company" to companyRid)
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