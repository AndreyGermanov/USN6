package controllers

import db.DBManager
import db.OrientDatabase
import models.Model
import models.User
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MockController: Controller() {
    override fun getModelInstance(): User {
        return User()
    }

}

class ControllerTests {

    var db = OrientDatabase("localhost:2480","test","admin","admin")
    @Before
    fun start() {
        DBManager.setDB(db)
    }

    fun populateDB() {
        db.execQuery("INSERT INTO users SET name='Andrey',surname='Germanov',uid='1'",hashMapOf())
        db.execQuery("INSERT INTO users SET name='Bob',surname='Johnson',uid='2'",hashMapOf())
        db.execQuery("INSERT INTO users SET name='Robin',surname='Hood',uid='3'",hashMapOf())
        for (i in 1..100) {
            db.execQuery("INSERT INTO users SET name='user$i',surname='Sur$i',uid='${i+3}'",hashMapOf())
        }
    }

    @After
    fun stop() {
       db.execQuery("DELETE FROM users", hashMapOf())
    }

    @Test
    fun getList() {
        val model = MockController()
        var result = model.getList()
        assertEquals("Should return empty list if no items", result.size,0)
        populateDB()
        result = model.getList()
        assertEquals("Should return correct number of items in list", result.size,103)
    }
}