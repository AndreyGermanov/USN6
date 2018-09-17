package controllers

import com.google.gson.Gson
import khttp.get
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.json.simple.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import system.ConfigManager
import utils.ResponseParser
import utils.TestEnvironment
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.test.*

class ReportsControllerTests {

    private val baseUrl = "http://localhost:8086/api/report"
    private val gson = Gson()
    private val headers = TestEnvironment.getBasicAuthHeader("test", "test")
    private var companyRid = ""
    private var userRid = ""

    @Before
    fun setupEnvironment() {
        TestEnvironment.createEnvironment()
        userRid = TestEnvironment.getUserRid("name='test'")
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
        val request = mapOf("date" to "1234567890", "email" to "test@test.com",
                "period" to "1234567890", "type" to "kudir", "company" to companyRid)
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        val result = responseJson["result"] as JSONObject
        val uid = result["uid"].toString().replace("#", "").replace(":", "_")
        url = "$baseUrl/$uid"
        responseJson = ResponseParser(get(url, headers = headers)).asObject()
        assertEquals("1234567890", responseJson["date"], "Should return correct object")
    }

    @Test
    fun getCount() {
        val url = "$baseUrl/count"
        var response = khttp.get(url)
        assertEquals(401, response.statusCode, "Should not accept unauthenticated requests")
        response = khttp.get(url, headers = headers)
        assertEquals("0", response.text, "Should return 0 if no items in database")
        val request = mapOf("date" to "1234567890", "email" to "test@test.com",
                "period" to "1234567890", "type" to "kudir", "company" to companyRid)
        khttp.post(baseUrl, data = gson.toJson(request), headers = headers)
        response = khttp.get(url, headers = headers)
        assertEquals("1", response.text, "Should return 1 if 1 item in database")
    }

    @Test
    fun postItem() {
        var response = khttp.post(baseUrl, data = gson.toJson(mapOf("period" to "123456789", "date" to 1234567890)))
        assertEquals(401, response.statusCode, "Should not accept unauthenticated requests")
        response = khttp.post(baseUrl, data = gson.toJson(mapOf<String, String>()), headers = headers)
        assertEquals(200, response.statusCode, "Should return correct status code")
        assertEquals("error", response.jsonObject["status"].toString(), "Should return error status")
        assertTrue(response.jsonObject.has("errors"), "Should contain errors object with error descriptions")
        var request = mapOf("period" to "sdfsdf")
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "period"),
                "Should return error if incorrect period specified")
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if date not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if type not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if company not specified")
        request = mapOf("period" to "123456789","description" to "Payment")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNull(TestEnvironment.getError(responseJson, "period"),
                "Should not return error if correct number specified")
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if date not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if type not specified")
        request = mapOf("period" to "1234567890", "date" to "gdfgdfg", "email" to "fsdgdfgdf", "type" to "154",
                "company" to "#33343:12")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if incorrect date specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if incorrect type specified")
        assertNotNull(TestEnvironment.getError(responseJson, "email"),
                "Should return error if incorrect email specified")
        request = mapOf("period" to "123456789","date" to "1234567890",
                "email" to "test@test.com","type" to "kudir","company" to "#342343:14")
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if incorrect company specified")
        request = mapOf("period" to "123456789", "date" to "1234567890", "type" to "kudir", "email" to "test@test.com",
                "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        assertFalse(responseJson.containsKey("errors"), "Should not return any errors")
        assertTrue(responseJson.containsKey("result"), "Should contain result field in response")
        val result = responseJson["result"] as JSONObject
        assertTrue(result.containsKey("uid"))
    }

    @Test
    fun putItem() {
        var request = mapOf("period" to "123456789", "date" to "1234567890", "email" to "test@test.com",
                "type" to "kudir","company" to companyRid)
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

        request = mapOf("period" to "123fsdfdsf456789","email" to "", "date" to "", "type" to "","company" to "")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "period"),
                "Should return error if incorrect period specified")
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if date not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if type not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if company not specified")
        request = mapOf("period" to "123456789", "date" to "", "company" to "","email" to "","type" to "")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNull(TestEnvironment.getError(responseJson, "period"),
                "Should not return error if correct period specified")
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if date not specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if type not specified")
        request = mapOf("period" to "123456789", "date" to "jf4egdf",
                "type" to "155", "email" to "test", "company" to "#33343:12")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "email"),
                "Should return error if incorrect email specified")
        assertNotNull(TestEnvironment.getError(responseJson, "date"),
                "Should return error if incorrect date specified")
        assertNotNull(TestEnvironment.getError(responseJson, "type"),
                "Should return error if incorrect type specified")
        request = mapOf("period" to "123456789","date" to "1234567890", "email" to "test@test.com","type" to "kudir",
                "company" to "#342343:14")
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertNotNull(TestEnvironment.getError(responseJson, "company"),
                "Should return error if incorrect company specified")
        request = mapOf("period" to "123456789","date" to "1234567890", "email" to "test@test.com","type" to "kudir",
                "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.put(url, data = gson.toJson(request), headers = headers))!!
        assertFalse(responseJson.containsKey("errors"), "Should not return any errors")
        assertTrue(responseJson.containsKey("result"), "Should contain result field in response")
        result = responseJson["result"] as JSONObject
        assertTrue(result.containsKey("uid"))
        assertEquals(result["date"].toString(), "1234567890","Should update number successfully")
        assertEquals(result["period"].toString(), "123456789", "Should update period successfully")
        assertEquals(result["type"].toString(), "kudir", "Should update type successfully")
    }

    @Test
    fun deleteItems() {
        var request = mapOf("date" to "1234567890", "email" to "test@test.com",
                "period" to "1234567890", "type" to "kudir", "company" to companyRid)
        var responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        var result = responseJson["result"] as JSONObject
        val uid1 = result["uid"].toString().replace("#", "").replace(":", "_")
        request = mapOf("date" to "1234567890", "email" to "test@test.com",
                "period" to "1234567890", "type" to "kudir", "company" to companyRid)
        responseJson = TestEnvironment.parseResponse(khttp.post(baseUrl, data = gson.toJson(request), headers = headers))!!
        result = responseJson["result"] as JSONObject
        val uid2 = result["uid"].toString().replace("#", "").replace(":", "_")
        request = mapOf("date" to "1234567890", "email" to "test@test.com",
                "period" to "1234567890", "type" to "kudir", "company" to companyRid)
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

    @Test
    fun generatePdfTests() {
        val rid = TestEnvironment.getTestCompanyUid()
        val token = String(Base64.getEncoder().encode("test:test".toByteArray()))
        var result = khttp.get("http://localhost:8086/report/generate/$rid/kudir/1532507386/pdf",timeout=120.0)
        assertEquals(401,result.statusCode,"Should not allow unauthorized access")
        result = khttp.get("http://localhost:8086/report/generate/bogus/kudir/1532507386/pdf?token=$token",timeout=120.0)
        assertEquals(500,result.statusCode,"Should return failure if company RID is incorrect")
        result = khttp.get("http://localhost:8086/report/generate/$rid/kudir/1532507386/pdf?token=$token",timeout=120.0)
        assertEquals(200,result.statusCode,"Should return correct status code")
        assertTrue(result.content.isNotEmpty(),"Should return PDF report blank without any data")
        Thread.sleep(6000)
        assertEquals(0, File(ConfigManager.webConfig["cache_path"].toString()).listFiles().size,
                "Should remove all temporary files from cache directory after report generated")
    }

    @Test
    fun generatePDFMultithreadInternalTests() {
        var finish_counter=0
        val rid = TestEnvironment.getTestCompanyUid()
        val token = String(Base64.getEncoder().encode("test:test".toByteArray()))
        for (i in 0..19) {
            Thread {
                val ctrl = ReportsController()
                ctrl.generate(rid,"kudir",1532507386,"pdf",token)
                finish_counter += 1
            }.start()
        }
        while (finish_counter<19) {
            if (File(ConfigManager.webConfig["cache_path"].toString()).listFiles { dir,name ->
                        dir.isFile
                    }.size == 20)
                break
        }
        Thread.sleep(5000)
        val filesList = File(ConfigManager.webConfig["cache_path"].toString()).listFiles()
        val filesCount = filesList.size
        for (file in filesList) {
            file.delete()
        }
        assertEquals(20,filesCount,"All threads should generate report files properly")
    }

    @Test
    fun generateHTMLMultithreadExternalTests() {
        var finish_counter=0
        val rid = TestEnvironment.getTestCompanyUid()
        val token = String(Base64.getEncoder().encode("test:test".toByteArray()))
        for (i in 0..19) {
            Thread {
                var result = khttp.get("http://localhost:8086/report/generate/$rid/kudir/$i/pdf?token=$token",timeout=120.0)
                var strm = FileOutputStream(ConfigManager.webConfig["cache_path"].toString()+"/result-$i.pdf")
                strm.write(result.content);strm.flush()
                finish_counter += 1
            }.start()
        }
        while (finish_counter<20) {
            if (File(ConfigManager.webConfig["cache_path"].toString()).listFiles { dir,name ->
                dir.isFile && name.startsWith("result")
            }.size == 20)
                break
        }
        Thread.sleep(5000)
        val filesList = File(ConfigManager.webConfig["cache_path"].toString()).listFiles()
        val filesCount = filesList.size
        for (file in filesList) {
            file.delete()
        }
        assertEquals(20,filesCount,"All threads should generate report files properly")
    }

    @After
    fun destroyEnvironment() {
        TestEnvironment.destroyEnvironment()
    }
}