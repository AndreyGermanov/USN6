package db

import models.Model

/**
 * Interface, which should implement any database provider
 */
interface Database {

    /**
     * Method used to get count of database models in collection
     * @param model: Model to get count of items from
     * @param user_id: ID of user to which affected records should belong
     * @param options: Options used to select, filter and order items in database query*
     */
    fun getCount(model:Model,options:HashMap<String,Any>,user_id:String?=null): Int

    /**
     * Method used to get list of database models from database
     * @param model: Model to get list of items from
     * @param user_id: ID of user to which affected records should belong
     * @param options: Options used to select, filter and order items in database query
     */
    fun getList(model:Model,options:HashMap<String,Any>,user_id:String?=null): ArrayList<Model>

    /**
     * Method used to add new record to database from specified model
     * @param model: Model to add to database
     * @param user_id: ID of user to which affected record should belong
     * @returns New database model with assigned ID or null if error
     */
    fun postItem(model:Model,user_id:String?=null): Model?

    /**
     * Method used to update record in database from specified model
     * @param model: Data model, which properties need to update
     * @param user_id: ID of user to which affected records should belong
     * @return Returns link to updated model or null if error
     */
    fun putItem(model:Model,user_id:String?=null): Model?

    /**
     * Method used to delete record of specified model from database
     * @param model: Model to which removed items belong
     * @param ids: List of comma separated list of models to delete
     * @param user_id: ID of user to which affected records should belong
     * @return: Returns null if success or HashMap of error descriptions if error
     */
    fun deleteItems(model:Model,ids:String,user_id:String?=null): HashMap<String,Any>?

   /**
    * Method tries to find record of specified model by UID in database and populate
    * record data to this model
    * @param model: Model to populate
    * @param user_id: ID of user to which affected record should belong
    * @return: Returns model of found record or null if error
    */
    fun getItem(model: Model,user_id:String?=null): Model?

    /**
     * Method used to authenticate in this database by user and password
     * @param name: Username
     * @param password: Password
     * @return Authenticated user ID or null if not authenticated
     */
    fun auth(name:String,password:String): String?

    /**
     * Method used to authenticate in this database by auth token
     * @param token: Auth token
     * @return Authenticated user ID or null if not authenticated
     */
    fun tokenAuth(token:String): String?

}

/**
 * Manager class, which used to load and configure database adapter instance of specified type. Database adapter
 * instance must implement "Database" interface
 */
object DBManager {
    // Link to database adapter instance
    private var db:Database? = null

    /**
     * Method returns link to database adapter instance
     */
    fun getDB(): Database? {
        return db
    }

    fun setDB(db:Database) {
        this.db = db
    }
}