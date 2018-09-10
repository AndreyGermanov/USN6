package Utils

import com.google.gson.Gson
import io.ktor.http.Parameters
import java.net.URL
import java.net.URLDecoder
import java.util.regex.Pattern

/**
 * Method used to get fields of model from POST parameters and return as HashMap
 * @param params: POST parameters hashmap
 * @return HashMap with extracted fields
 */
fun paramsToHashMap(params: Parameters): HashMap<String,Any> {
    var result = HashMap<String,Any>()
    for (param in params.entries()) {
        result[param.key] = param.value[0]
    }
    return result
}

/**
 * Utility function used to return either value of list item or "" if no item in specified index
 * @param index: Index of item in list
 * @return value of item specified by index or "" if no value
 */
fun List<String>.getOrEmpty(index: Int) : String {
    return getOrElse(index) {""}
}

/**
 * Utility Extension Method used to decode URL query using UTF-8
 * @return decoded string
 */
fun String.decodeToUTF8(): String {
    return URLDecoder.decode(this, "UTF-8")
}

/**
 * Function used to transform URL query string to Hashmap of keys and values
 */
fun splitQuery(url: URL): Map<String,String> {

    val queryPairs = LinkedHashMap<String, String>()

    url.query.split("&".toRegex())
            .dropLastWhile { it.isEmpty() }
            .map { it.split('=') }
            .map { it.getOrEmpty(0).decodeToUTF8() to it.getOrEmpty(1).decodeToUTF8() }
            .forEach { (key, value) ->
                queryPairs[key] = value
            }

    return queryPairs
}

/**
 * Function checks if passed string is HTTP query string and converts it to JSON string. If it already JSON
 * string, then just returns it
 * @param queryString: input string
 * @return JSON string
 */
fun queryStringToJSON(queryString:String):String {
    var result = queryString
    if (!queryString.startsWith("{") && !queryString.startsWith("[")) {
        val gson = Gson()
        result = gson.toJson(splitQuery(URL("http://localhost?$queryString")))
    }
    return result
}

/**
 * Function validates email address
 *
 * @param email Email address to check
 * @return True if provided email address is correct and False otherwise
 */
fun isValidEmail(email: CharSequence): Boolean {
    var result = false
    try {
        val pattern = Pattern.compile("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")
        val matches = pattern.matcher(email)
        result = (matches!=null && matches.matches())
    } catch (e:Exception) {
        result = false
    }
    return result
}

/**
 * Method used to determine if source string starts with any of sequence
 * from provided [array] argument
 * @param array: ArrayList - list of string prefixes to compare with
 * @returns Boolean: True if any match found and False otherwise
 */
fun String.startsWith(array:ArrayList<String>):Boolean {
    val source = this;
    val result = array.filter({source.startsWith(it)})
    return result.isNotEmpty()
}