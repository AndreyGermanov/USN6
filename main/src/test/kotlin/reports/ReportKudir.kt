package reports

import db.DBManager
import db.orientdb.OrientDatabase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import utils.TestEnvironment
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReportKudirTests {

    @Before
    fun setupEnvironment() {
        TestEnvironment.createEnvironment()
        TestEnvironment.addTestCompany()
    }

    fun createTestingData() {
        val rid = "#${TestEnvironment.getTestCompanyUid().replace("_",":")}"
        val db = DBManager.getDB() as OrientDatabase
        db.execQuery("INSERT INTO accounts SET number='40802810800000090239',ks='30101810145250000974'," +
                "bik='044525974',bank_name='АО \"ТИНЬКОФФ БАНК\"',company='$rid'")
        db.execQuery("INSERT INTO accounts SET number='40802810702550007719',ks='30101810900000000585'," +
                "bik='040349585',bank_name='ФИЛИАЛ N 2351 ВТБ 24 (ПАО)',company='$rid'")
        val date = Calendar.Builder().setDate(2018,1-1,5).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='270663',date=$date,description='1',amount=eval('143876.62'),company='$rid'")
        val date2 = Calendar.Builder().setDate(2018,1-1,17).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='405444',date=$date2,description='2',amount=eval('150514.07'),company='$rid'")
        val date3 = Calendar.Builder().setDate(2018,1-1,31).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='3857369',date=$date3,description='3',amount=eval('0.43'),company='$rid'")
        val date4 = Calendar.Builder().setDate(2018,2-1,2).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='592912',date=$date4,description='4',amount=eval('117921.39'),company='$rid'")
        val date5 = Calendar.Builder().setDate(2018,2-1,16).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='106554',date=$date5,description='5',amount=eval('1716.72'),company='$rid'")
        val date6 = Calendar.Builder().setDate(2018,2-1,28).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='6471655',date=$date6,description='6',amount=eval('2.17'),company='$rid'")
        val date7 = Calendar.Builder().setDate(2018,3-1,12).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='256692',date=$date7,description='7',amount=eval('840.83'),company='$rid'")
        val date8 = Calendar.Builder().setDate(2018,4-1,4).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='283612',date=$date8,description='8',amount=eval('110359.51'),company='$rid'")
        val date9 = Calendar.Builder().setDate(2018,4-1,12).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='372146',date=$date9,description='9',amount=eval('98138.57'),company='$rid'")
        val date10 = Calendar.Builder().setDate(2018,4-1,30).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='7679008',date=$date10,description='10',amount=eval('2.22'),company='$rid'")
        val date11 = Calendar.Builder().setDate(2018,5-1,3).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='611684',date=$date11,description='11',amount=eval('26992.34'),company='$rid'")
        val date12 = Calendar.Builder().setDate(2018,5-1,25).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='879360',date=$date12,description='12',amount=eval('50406.88'),company='$rid'")
        val date13 = Calendar.Builder().setDate(2018,5-1,30).build().time.time/1000
        db.execQuery("INSERT INTO income SET number='6006225',date=$date13,description='13',amount=eval('2.31'),company='$rid'")
        val date14 = Calendar.Builder().setDate(2018,3-1,13).build().time.time/1000
        db.execQuery("INSERT INTO spendings SET number='1807209449020',date=$date14,description='14',period='2017 г',type=3,amount=eval('1000'),company='$rid'")
        val date15 = Calendar.Builder().setDate(2018,3-1,21).build().time.time/1000
        db.execQuery("INSERT INTO spendings SET number='1808010054275',date=$date15,description='15',period='2017 г',type=3,amount=eval('33669'),company='$rid'")

    }

    @Test
    fun getData_TestStructure() {
        val ctrl = ReportKudir()
        val rid = TestEnvironment.getTestCompanyUid()
        var result = ctrl.getData(hashMapOf())
        assertEquals(0,result.keySet().size,"Should return empty object, if no companyRid provided")
        result = ctrl.getData(hashMapOf("companyRid" to rid))
        assertEquals(0,result.keySet().size,"Should return empty object, if no period provided")
        result = ctrl.getData(hashMapOf("companyRid" to rid,"period" to "123456"))
        assertNotEquals(0,result.keySet().size,"Should contain not empty object, if basic parameters specified")
        assertNotNull(result["header"] as? JSONObject,"Should contain 'header' section")
        assertNotNull(result["incomes"] as? JSONObject,"Should contain 'incomes' section")
        assertNotNull(result["spendings"] as? JSONObject,"Should contain 'spendings' section")
        val incomes = result["incomes"] as JSONObject
        assertTrue(incomes["1"]!=null && incomes["2"] != null && incomes["3"]!=null && incomes["4"] != null,
                "Should contain incomes sections for all 4 quarters")
        val spendings = result["spendings"] as JSONObject
        assertTrue(spendings["1"]!=null && spendings["2"] != null && spendings["3"]!=null && spendings["4"] != null,
                "Should contain spendings sections for all 4 quarters")
    }

    @Test
    fun getData_TestCalculations() {
        val ctrl = ReportKudir()
        val rid = TestEnvironment.getTestCompanyUid()
        createTestingData()
        val result = ctrl.getData(hashMapOf("companyRid" to rid,"period" to "1520888400"))
        val header = result["header"] as JSONObject
        val incomes = result["incomes"] as JSONObject
        val spendings = result["spendings"] as JSONObject
        val incomes1 = incomes["1"] as JSONObject
        val incomes2 = incomes["2"] as JSONObject
        val incomes3 = incomes["3"] as JSONObject
        val incomes4 = incomes["4"] as JSONObject
        val spendings1 = spendings["1"] as JSONObject
        val spendings2 = spendings["2"] as JSONObject
        val spendings3 = spendings["3"] as JSONObject
        val spendings4 = spendings["4"] as JSONObject
        assertEquals(414872.23,incomes1["quarter_amount"].toString().toDouble(),"Should return correct income amount for 1 quarter")
        assertEquals(414872.23,incomes1["total_amount"].toString().toDouble(),"Should return correct income total amount for 1 quarter")
        assertEquals(7,(incomes1["rows"] as JSONArray).length(),"Should contain correct number of income rows for 1 quarter")
        assertEquals(285901.83,incomes2["quarter_amount"].toString().toDouble(),"Should return correct income amount for 2 quarter")
        assertEquals(700774.06,incomes2["total_amount"].toString().toDouble(),"Should return correct income total amount for 2 quarter")
        assertEquals(6,(incomes2["rows"] as JSONArray).length(),"Should contain correct number of income rows for 2 quarter")
        assertEquals(0.0,incomes3["quarter_amount"].toString().toDouble(),"Should return correct income amount for 3 quarter")
        assertEquals(700774.06,incomes3["total_amount"].toString().toDouble(),"Should return correct income total amount for 3 quarter")
        assertEquals(0,(incomes3["rows"] as JSONArray).length(),"Should contain correct number of income rows for 3 quarter")
        assertEquals(0.0,incomes4["quarter_amount"].toString().toDouble(),"Should return correct income amount for 4 quarter")
        assertEquals(700774.06,incomes4["total_amount"].toString().toDouble(),"Should return correct income total amount for 4 quarter")
        assertEquals(0,(incomes4["rows"] as JSONArray).length(),"Should contain correct number of income rows for 4 quarter")
        assertEquals(34669.0,spendings1["quarter_amount"].toString().toDouble(),"Should return correct amount of spendings for 1 quarter")
        assertEquals(34669.0,spendings1["total_amount"].toString().toDouble(),"Should return correct total amount of spendings for 1 quarter")
        assertEquals(2,(spendings1["rows"] as JSONArray).length(),"Should contain correct number of spendings rows for 1 quarter")
        assertEquals(0.0,spendings2["quarter_amount"].toString().toDouble(),"Should return correct amount of spendings for 2 quarter")
        assertEquals(34669.0,spendings2["total_amount"].toString().toDouble(),"Should return correct total amount of spendings for 2 quarter")
        assertEquals(0,(spendings2["rows"] as JSONArray).length(),"Should contain correct number of spendings rows for 2 quarter")
        assertEquals(0.0,spendings3["quarter_amount"].toString().toDouble(),"Should return correct amount of spendings for 3 quarter")
        assertEquals(34669.0,spendings3["total_amount"].toString().toDouble(),"Should return correct total amount of spendings for 3 quarter")
        assertEquals(0,(spendings3["rows"] as JSONArray).length(),"Should contain correct number of spendings rows for 3 quarter")
        assertEquals(0.0,spendings4["quarter_amount"].toString().toDouble(),"Should return correct amount of spendings for 4 quarter")
        assertEquals(34669.0,spendings4["total_amount"].toString().toDouble(),"Should return correct total amount of spendings for 4 quarter")
        assertEquals(0,(spendings4["rows"] as JSONArray).length(),"Should contain correct number of spendings rows for 4 quarter")
        assertEquals("№ 40802810702550007719 в ФИЛИАЛ N 2351 ВТБ 24 (ПАО),№ 40802810800000090239 в АО \"ТИНЬКОФФ БАНК\"",header["bank_accounts"].toString(),
                "Should contain correct string for bank accounts in header")
    }

    @After
    fun destroyEnvironment() {
       // TestEnvironment.destroyEnvironment()
    }
}