package controllers

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.junit.After
import org.junit.Before
import org.junit.Test
import system.ConfigManager
import utils.TestEnvironment
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportsControllerTests {

    @Before
    fun setupEnvironment() {
        TestEnvironment.createEnvironment()
        TestEnvironment.addTestCompany()

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
      //  TestEnvironment.destroyEnvironment()
    }
}