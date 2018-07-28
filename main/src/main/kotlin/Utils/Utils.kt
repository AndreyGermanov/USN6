package Utils

import io.ktor.http.Parameters

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