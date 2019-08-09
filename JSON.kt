package com.octopepper.yummypets.goservices.jsonparsers.helpers

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.reflect.KMutableProperty0


// JSON Abstraction
class JSON {
    var jsonObject: JSONObject = JSONObject()

    constructor(jsonObject: JSONObject) {
        this.jsonObject = jsonObject
    }

    constructor(jsonString: String?) {
        try {
            this.jsonObject = JSONObject(jsonString)
        } catch (e: JSONException) {
            this.jsonObject = JSONObject()
        }
    }
}

// Container to store infos in DSL.
data class JSONMappingInfo<T>(val json: JSON, val key: String, var parser: JSONParser<T>? = null)

// "[]" overloading returning JSONMappingInfo<T> containing all the data needed for the parsing.
// Enables writing json["key"] or json["key", MyJSONParser()]
operator fun <T> JSON.get(key: String, parser: JSONParser<T>? = null): JSONMappingInfo<T> = JSONMappingInfo(this, key, parser)

// An interface to create your own JSONParser objects, that can then be used while parsing.
interface JSONParser<Model> {
    @Throws(JSONException::class)
    fun parse(json: JSON): Model
}

// Adds support for "<" operator for parsing.
inline operator fun <reified T> KMutableProperty0<T>.compareTo(mapping: JSONMappingInfo<T>): Int {
    mapJSON(this, mapping)
    return 0
}

inline fun <reified T>mapJSON(property:KMutableProperty0<T>, json: JSON, key: String) {

    var jsonObject: JSON = json
    var jsonKey: String = key

    if(isPathKey(key)){
        val temp = jsonObject.parsePathKey(key)
        jsonKey = temp.first
        jsonObject = temp.second
    }

    when (T::class) {
        Boolean::class -> (property as? KMutableProperty0<Boolean>)?.apply { set(jsonObject.bool(jsonKey)) }
        String::class -> (property as? KMutableProperty0<String>)?.apply { set(jsonObject.string(jsonKey)) }
        Int::class -> (property as? KMutableProperty0<Int>)?.apply { set(jsonObject.int(jsonKey)) }
        Long::class -> (property as? KMutableProperty0<Long>)?.apply { set(jsonObject.long(jsonKey)) }
        Double::class -> (property as? KMutableProperty0<Double>)?.apply { set(jsonObject.double(jsonKey)) }
        JSONArray::class -> (property as? KMutableProperty0<JSONArray>)?.apply { set(jsonObject.jsonObject.getJSONArray(jsonKey)) }
        JSONObject::class -> (property as? KMutableProperty0<JSONObject>)?.apply { set(jsonObject.jsonObject.getJSONObject(jsonKey)) }
    }
}

// Mapping with a custom parser.
inline fun <reified T>mapJSON(property:KMutableProperty0<T>, json: JSON, key: String, parser: JSONParser<T>) {
    json.jsonOrNull(key)?.let { subJSON ->
        val model = parser.parse(subJSON)
        property.set(model)
    }
}

inline fun <reified T>mapJSON(property:KMutableProperty0<T>, mapping: JSONMappingInfo<T>) {
    val json = mapping.json
    val key = mapping.key
    val parser = mapping.parser
    if (json.jsonObject.has(key)) {
        if (parser != null) {
            mapJSON(property, json, key, parser)
        } else {
            mapJSON(property, json, key)
        }
    }
}

// Null getters with json("id")
inline operator fun <reified T> JSON.invoke(key: String): T? {

    if(key.isBlank() || jsonObject.length() < 1 || !jsonObject.has(key)){
        return null
    }

    var json: JSON? = this
    var jsonKey: String = key

    if(isPathKey(key)){
        val temp = parsePathKey(key)
        jsonKey = temp.first
        json = temp.second
    }

    return when (T::class) {
        Boolean::class -> json?.bool(jsonKey) as? T
        String::class -> json?.string(jsonKey) as? T
        Int::class -> json?.int(jsonKey) as? T
        Long::class -> json?.long(jsonKey) as? T
        Double::class -> json?.double(jsonKey) as? T
        JSONObject::class -> json?.jsonObject?.getJSONObject(jsonKey) as? T
        JSON::class -> json?.jsonObject?.getJSONObject(jsonKey)?.let { JSON(it) } as? T
        JSONArray::class -> json?.jsonObject?.getJSONArray(jsonKey) as? T
        else -> null
    }
}

inline operator fun <reified T> JSON.invoke(key: String, parser: JSONParser<T>): T? {
    return jsonOrNull(key)?.let { return parser.parse(it) }
}

// Helpers
fun JSON.bool(key: String): Boolean = jsonObject.getBoolean(key)
fun JSON.string(key: String): String = jsonObject.getString(key)
fun JSON.int(key: String): Int = jsonObject.getInt(key)
fun JSON.long(key: String): Long = jsonObject.getLong(key)
fun JSON.double(key: String): Double = jsonObject.getDouble(key)

// Optional Helpers
fun JSON.boolOrNull(key: String): Boolean? = if (jsonObject.has(key)) jsonObject.getBoolean(key) else null
fun JSON.intOrNull(key: String): Int? = if (jsonObject.has(key)) jsonObject.getInt(key) else null
fun JSON.doubleOrNull(key: String): Double? = if (jsonObject.has(key)) jsonObject.getDouble(key) else null
fun JSON.longOrNull(key: String): Long? = if (jsonObject.has(key)) jsonObject.getLong(key) else null
fun JSON.stringOrNull(key: String): String? = if (jsonObject.has(key)) jsonObject.getString(key) else null
fun JSON.jsonOrNull(key: String): JSON? {
    if (jsonObject.has(key)) {
        return try {
            JSON(jsonObject.getJSONObject(key))
        } catch( e: JSONException) {e
            null
        }
    }
    return null
}

fun JSON.getKey(key: String): JSON {

    if(key.isBlank()) throw JSONException("The string must end with a proper key. Make sure the last character isn't a dot")

    try {
        return if(isPathKey(key)) {
            val properties = key.split(".")
            val firstKey = properties[0]

            val subJson: JSON = this(firstKey)!!

            //Use loop instead of recursivity
            subJson.getKey(key.replaceFirst(Regex("^[^\\.]*\\."), ""))
        }
        else JSON(jsonObject.getJSONObject(key))
    }
    catch (e: Throwable) {
        throw Throwable("Tried to access $key in ")
    }

}

fun JSON.parsePathKey(key: String): Pair<String, JSON>{

    val keys = key.split(".")

    var newKey = ""
    keys.dropLast(1).forEachIndexed { i, item ->
        if(i > 0) { newKey += "." }
        newKey += item
    }

    return Pair(keys.last(), getKey(newKey))
}

fun isPathKey(key: String): Boolean = key.contains(".")

