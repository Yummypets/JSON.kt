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
    getValue<T>(json, key)?.let { property.set(it) }
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
    if (parser != null) {
        mapJSON(property, json, key, parser)
    } else {
        mapJSON(property, json, key)
    }
}

// Null getters with json("id")
inline operator fun <reified T> JSON.invoke(key: String): T? {
    return getValue(this, key)
}


// Null getters with json("id")
inline fun <reified T> getValue(json: JSON, key: String): T? {
    if (isKeyPath(key)) {
        val keys = key.split(".")
        val lastKey = keys.last()
        val allKeysButLast = keys.dropLast(1)
        var nestedJSON = json
        allKeysButLast.forEach { k ->
            if (!nestedJSON.jsonObject.has(k)) {
                return null
            }
            nestedJSON = JSON(nestedJSON.jsonObject.getJSONObject(k))
        }
        return getSingleValue(nestedJSON, lastKey)
    }
    return getSingleValue(json, key)
}


inline fun <reified T> getSingleValue(json: JSON, key: String): T? {
    if(key.isBlank() || json.jsonObject.length() < 1 || !json.jsonObject.has(key)) {
        return null
    }
    return when (T::class) {
        Boolean::class -> json.bool(key) as? T
        String::class -> json.string(key) as? T
        Int::class -> json.int(key) as? T
        Long::class -> json.long(key) as? T
        Double::class -> json.double(key) as? T
        JSONObject::class -> json.jsonObject.getJSONObject(key) as? T
        JSON::class -> json.jsonObject.getJSONObject(key)?.let { JSON(it) } as? T
        JSONArray::class -> json.jsonObject.getJSONArray(key) as? T
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


fun isKeyPath(key: String) = key.contains(".")


/* Example
class Car {
    var id = 0
    var name = ""
    var statistics = CarStatistics()
}
class CarJSONParser : JSONParser<Car> {
    @Throws(JSONException::class)
    override fun parse(json: JSON): Car {
         val car = Car()
        // Classic parsing, if key does not exist, do nothing, aka keep model's default value.
        car::id < json["id"]
        car::name < json["name"]
        car::statistics < json["stats", CarStatsJSONMapper()]
        // Parsing whith default value. if key does not exist set a default.
        car.id = json("id") ?: 0
        car.name = json("name") ?: "unknown car"
        car.statistics = json("stats", CarStatsJSONMapper()) ?: CarStatistics()
        return car
    }
}
fun usage() {
    val json = JSON("[aJSONString]")
    val car = CarJSONParser().parse(json)
}
*/
