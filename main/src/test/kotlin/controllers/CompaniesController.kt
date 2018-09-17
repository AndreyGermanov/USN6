package controllers

import com.google.gson.Gson
import org.json.simple.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import utils.ResponseParser
import utils.TestEnvironment
import kotlin.test.*

class CompaniesControllerTests {

    private val baseUrl = "http://localhost:8086/api/company"
    private val gson = Gson()
    private val headers = TestEnvironment.getBasicAuthHeader("test2","111111")

    @Before
    fun setupEnvironment() {
        TestEnvironment.createEnvironment()
        TestEnvironment.createTestUser()
    }

    @Test
    fun getItem() {
        var url = "$baseUrl/sdsd"
        var response = khttp.get(url)
        assertEquals(401,response.statusCode,"Should not accept unauthenticated requests")
        response = khttp.get(url, headers=headers)
        assertFalse(response.jsonObject.has("uid"),"Should return empty JSON if incorrect item specified")
        val request = mapOf("name" to "company1", "inn" to "4324233", "type" to "2", "kpp" to "2342324", "address" to "Addr")
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        val result = responseJson["result"] as JSONObject
        val uid = result["uid"].toString().replace("#","").replace(":","_")
        url = "$baseUrl/$uid"
        responseJson = ResponseParser(khttp.get(url,headers=headers)).asObject()
        assertEquals("company1",responseJson["name"],"Should return correct object")
    }

    @Test
    fun getCount() {
        val url = "$baseUrl/count"
        var response = khttp.get(url)
        assertEquals(401,response.statusCode,"Should not accept unauthenticated requests")
        response = khttp.get(url, headers=headers)
        assertEquals("0",response.text,"Should return 0 if no items in database")
        val request = mapOf("name" to "company1", "inn" to "4324233", "type" to "2", "kpp" to "2342324", "address" to "Addr")
        khttp.post(baseUrl, data = gson.toJson(request), headers = headers)
        response = khttp.get(url, headers=headers)
        assertEquals("1",response.text,"Should return 1 if 1 item in database")
    }

    @Test
    fun postItem() {
        var response = khttp.post(baseUrl, data=gson.toJson(mapOf("name" to "Test company","inn" to 2353434545)))
        assertEquals(401,response.statusCode,"Should not accept unauthenticated requests")
        response = khttp.post(baseUrl, data=gson.toJson(mapOf<String,String>()), headers=headers)
        assertEquals(200,response.statusCode,"Should return correct status code")
        assertEquals("error",response.jsonObject["status"].toString(),"Should return error status")
        assertTrue(response.jsonObject.has("errors"),"Should contain errors object with error descriptions")
        var request = mapOf("name" to "company1")
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"inn"),
                "Should return error if INN not specified")
        assertNotNull(TestEnvironment.getError(responseJson,"type"),
                "Should return error if type not specified")
        assertNotNull(TestEnvironment.getError(responseJson,"address"),
                "Should return error if address not specified")
        request = mapOf("name" to "company1", "inn" to "gdfgdfg")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"inn"),
                "Should return error if incorrect INN specified")
        request = mapOf("name" to "company1", "inn" to "4324233")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl,data=gson.toJson(request),headers=headers))!!
        assertNull(TestEnvironment.getError(responseJson,"inn"),
                "Should not return error if orrect INN specified")
        assertNotNull(TestEnvironment.getError(responseJson,"address"),
                "Should return error if address not specified")
        assertNotNull(TestEnvironment.getError(responseJson,"type"),
                "Should return error if type not specified")
        request = mapOf("name" to "company1", "inn" to "4324233", "type" to "5")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"type"),
                "Should return error if incorrect type specified")
        request = mapOf("name" to "company1", "inn" to "4324233", "type" to "2")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"kpp"),
                "Should return error if KPP not specified for item with type=2")
        request = mapOf("name" to "company1", "inn" to "4324233", "type" to "2","kpp" to "sdfsdf")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"kpp"),
                "Should return error if incorrect KPP specified")
        request = mapOf("name" to "company1", "inn" to "4324233", "type" to "2","kpp" to "2342324","address" to "Addr")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl,data=gson.toJson(request),headers=headers))!!
        assertFalse(responseJson.containsKey("errors"), "Should not return any errors")
        assertTrue(responseJson.containsKey("result"),"Should contain result field in response")
        val result = responseJson["result"] as JSONObject
        assertTrue(result.containsKey("uid"))
        request = mapOf("name" to "company1", "inn" to "4324233", "type" to "2","kpp" to "2342324","address" to "Addr")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"inn"),
                "Should return error if try to insert item with the same INN")
    }

    @Test
    fun putItem() {
        var request = mapOf("name" to "company1", "inn" to "4324233", "type" to "2", "kpp" to "2342324", "address" to "Addr")
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        var result = responseJson["result"] as JSONObject
        val uid = result["uid"].toString().replace("#","").replace(":","_")
        var url = "$baseUrl/223"
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"general"),
                "Should return error if incorrect RID specified to update")
        url = "$baseUrl/$uid"
        val response = khttp.post(url, data=gson.toJson(request))
        assertEquals(401,response.statusCode,"Should not accept unauthenticated requests")

        request = mapOf("name" to "company1","inn" to "","kpp" to "","address" to "", "type" to "")
        responseJson = TestEnvironment.parseResponse(khttp.put(url,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"inn"),
                "Should return error if INN not specified")
        assertNotNull(TestEnvironment.getError(responseJson,"type"),
                "Should return error if type not specified")
        assertNotNull(TestEnvironment.getError(responseJson,"address"),
                "Should return error if address not specified")
        request = mapOf("name" to "company1", "inn" to "gdfgdfg")
        responseJson = TestEnvironment.parseResponse(khttp.put(url,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"inn"),
                "Should return error if incorrect INN specified")
        request = mapOf("name" to "company1", "inn" to "4324233","address" to "","type" to "")
        responseJson = TestEnvironment.parseResponse(khttp.put(url,data=gson.toJson(request),headers=headers))!!
        assertNull(TestEnvironment.getError(responseJson,"inn"),
                "Should not return error if orrect INN specified")
        assertNotNull(TestEnvironment.getError(responseJson,"address"),
                "Should return error if address not specified")
        assertNotNull(TestEnvironment.getError(responseJson,"type"),
                "Should return error if type not specified")
        request = mapOf("name" to "company1", "inn" to "4324233", "type" to "5")
        responseJson = TestEnvironment.parseResponse(khttp.put(url,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"type"),
                "Should return error if incorrect type specified")
        request = mapOf("name" to "company1", "inn" to "4324233", "type" to "2","kpp" to "")
        responseJson = TestEnvironment.parseResponse(khttp.put(url,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"kpp"),
                "Should return error if KPP not specified for item with type=2")
        request = mapOf("name" to "company1", "inn" to "4324233", "type" to "2","kpp" to "sdfsdf")
        responseJson = TestEnvironment.parseResponse(khttp.put(url,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"kpp"),
                "Should return error if incorrect KPP specified")
        request = mapOf("name" to "company1", "inn" to "4324233", "type" to "2","kpp" to "2342324","address" to "Addr")
        responseJson = TestEnvironment.parseResponse(khttp.put(url,data=gson.toJson(request),headers=headers))!!
        assertFalse(responseJson.containsKey("errors"), "Should not return any errors")
        result = responseJson["result"] as JSONObject
        assertEquals(result["uid"].toString(),"#${uid.replace("_",":")}")
        request = mapOf("name" to "company2", "inn" to "123456", "type" to "2", "kpp" to "2342324", "address" to "Addr")
        TestEnvironment.parseResponse(khttp.post(baseUrl,data=gson.toJson(request),headers=headers))!!
        request = mapOf("name" to "company1", "inn" to "123456", "type" to "2","kpp" to "1243224")
        responseJson = TestEnvironment.parseResponse(khttp.put(url,data=gson.toJson(request),headers=headers))!!
        assertNotNull(TestEnvironment.getError(responseJson,"inn"),
                "Should return error if update INN to INN of other item")
        request = mapOf("name" to "company5", "inn" to "123458", "type" to "2","kpp" to "1243224")
        responseJson = TestEnvironment.parseResponse(khttp.put(url,data=gson.toJson(request),headers=headers))!!
        assertFalse(responseJson.containsKey("errors"))
        result = responseJson["result"] as JSONObject
        assertEquals(result["inn"].toString(),"123458")
        assertEquals(result["name"].toString(),"company5")
    }

    @Test
    fun deleteItems() {
        var request = mapOf("name" to "company1", "inn" to "4324233", "type" to "2", "kpp" to "2342324", "address" to "Addr")
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        var result = responseJson["result"] as JSONObject
        val uid1 = result["uid"].toString().replace("#","").replace(":","_")
        request = mapOf("name" to "company2", "inn" to "145323534", "type" to "1", "kpp" to "2342324", "address" to "Addr2")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        result = responseJson["result"] as JSONObject
        val uid2 = result["uid"].toString().replace("#","").replace(":","_")
        request = mapOf("name" to "company3", "inn" to "999999", "type" to "2", "kpp" to "1234353", "address" to "Addr3")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        result = responseJson["result"] as JSONObject
        val uid3 = result["uid"].toString().replace("#","").replace(":","_")
        var url = "$baseUrl/1,2,3,4,5"
        val response = khttp.delete(url)
        assertEquals(401,response.statusCode,"Should not accept unauthenticated requests")
        responseJson = TestEnvironment.parseResponse(khttp.delete(url, headers = headers))!!
        assertTrue(responseJson.containsKey("errors"),"Should return error if delete incorrect items")
        url = "$baseUrl/$uid3,$uid1"
        responseJson = TestEnvironment.parseResponse(khttp.delete(url, headers = headers))!!
        assertFalse(responseJson.containsKey("errors"),"Should not return error if delete correct items")
        assertEquals("ok",responseJson["status"].toString(),"Should return status=ok")
        var responseArray = ResponseParser(khttp.get(baseUrl,headers=headers)).asArray()
        assertEquals(1,responseArray.size,"Should remove two items from database")
        url = "$baseUrl/$uid2"
        responseJson = TestEnvironment.parseResponse(khttp.delete(url, headers = headers))!!
        assertFalse(responseJson.containsKey("errors"),"Should not return error if delete correct items")
        assertEquals("ok",responseJson["status"].toString(),"Should return status=ok")
        responseArray = ResponseParser(khttp.get(baseUrl,headers=headers)).asArray()
        assertEquals(0,responseArray.size,"Should remove one item from database")
    }

    @After
    fun destroyEnvironment() {
        TestEnvironment.destroyEnvironment()
    }
}