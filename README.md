# JSON.kt
[![Language: Kotlin](https://img.shields.io/badge/language-kotlin-7963FE.svg?style=flat)](https://kotlinlang.org)
![Platform: Android 8+](https://img.shields.io/badge/platform-Android-68b846.svg?style=flat)
[![codebeat badge](https://codebeat.co/badges/4199536d-2158-42e5-a89a-33259d32b384)](https://codebeat.co/projects/github-com-yummypets-json-kt-master)
[![License: MIT](http://img.shields.io/badge/license-MIT-lightgrey.svg?style=flat)](https://github.com/Yummypets/JSON.kt/blob/master/LICENSE)

<!-- TODO ![Release version](https://img.shields.io/github/release/Yummypets/JSON.kt.svg) -->
Kotlin JSON Parsing that infers type 🚀  
`JSON.kt` is a kotlin wrapper of java's `org.json.JSONObject` that exposes a nicer syntax for kotlin code.


```kotlin
car::id < json["id"]
car::name < json["name"]
car::statistics < json["stats", CarStatsJSONMapper()]
```

## Why?
Because parsing JSON is often full of **unecessary if statements**, **obvious casts** and **nil-checks**
We deserve better !

## How!
By using a simple `<` operator that takes care of the boilerplate code for us thanks to beautiful kotlin generics.
JSON mapping code becomes **concise** and **maintainable** ❤️


## Why use JSON.kt
- [x] Infers types
- [x] Leaves your models clean
- [x] Handles custom & nested models
<!-- TODO support - [x] Dot and array syntax -->
- [x] Pure Kotlin, Simple & Lightweight

## Example

Given a kotlin Model `Car`
```kotlin
class Car {
    var id = 0
    var name = ""
    var statistics = CarStatistics()
}
```
And the following `JSON` file

```json
{
    "id": 265,
    "name": "Ferrari",
    "stats": {
        "maxSpeed": 256,
        "numberOfWheels": 4
    }
}
```

### Before with JSONObject 😱
```kotlin
if (jsonObject.has("id")) {
    car.id = jsonObject.getInt("id")
}
if (jsonObject.has("name")) {
    car.name = jsonObject.getString("name")
}
if (jsonObject.has("stats")) {
    val statsParser = CarStatsJSONMapper()
    car.statistics = statsParser.parse(jsonObject.getJSONObject("stats"))
}
```

### With JSON.kt 😎
```kotlin
car::id < json["id"]
car::name < json["name"]
car::statistics < json["stats", CarStatsJSONMapper()]
```
The `<` operator maps a model property with a json key.  
Notice that this does **exactly the same as the old parsing above**, meaning that if key does not exist, nothing happens and the model keeps its previous value.

On the third line, we can provide our own custom mapper which enables us to reuse the same mapping at different places. 🤓

### Usage 🚘
```kotlin
val json = JSON("[aJSONString]") // or JSON(jsonObject)"
val car = CarJSONParser().parse(json)
```

### Providing default values
In the previous examples we used `json["key"]`, here we are going to use `json("key")` that returns an optional value.
Perfect for providing default values while parsing!
```kotlin
car.id = json("id") ?: 0
car.name = json("name") ?: "unknown car"
car.statistics = json("stats", CarStatsJSONMapper()) ?: CarStatistics()
```

### A typical parser
```kotlin
class CarJSONParser : JSONParser<Car> {

    @Throws(JSONException::class)
    override fun parse(json: JSON): Car {
        val car = Car()
        car::id < json["id"]
        car::name < json["name"]
        car::statistics < json["stats", CarStatsJSONMapper()]
        return car
    }
}
```

## Installation
#### Manual
Just copy-paste `JSON.kt` file \o/
