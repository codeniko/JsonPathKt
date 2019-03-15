# JsonPathLite
[![Build Status](https://travis-ci.com/codeniko/JsonPathLite.svg?branch=master)](https://travis-ci.com/codeniko/JsonPathLite)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.nfeld.jsonpathlite/json-path-lite/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.nfeld.jsonpathlite/json-path-lite)

**A lighter and more efficient implementation of JsonPath in Kotlin.**
With functional programming aspects found in langauges like Kotlin, Scala, and streams/lambdas in Java8, this library simplifies other implementations like [Jayway's JsonPath](https://github.com/json-path/JsonPath) by removing *filter operations* and *in-path functions* to focus solely on what matters most: modern fast value extractions from JSON objects. Up to **6x more efficient** in some cases; see [Benchmarks](#benchmarks).

In order to make the library functional programming friendly, JsonPathLite returns *null* rather than throwing exceptions while evaluating a *path* against a JSON object. Throwing exceptions breaks flow control and should be reserved for exceptional errors only.

## Getting started
JsonPathLite is available at the Maven Central repository.

**POM**
```xml
<dependency>
  <groupId>com.nfeld.jsonpathlite</groupId>
  <artifactId>json-path-lite</artifactId>
  <version>1.1.0</version>
  <type>pom</type>
</dependency>
```

**Gradle**
```gradle
dependencies {
    implementation 'com.nfeld.jsonpathlite:json-path-lite:1.1.0'
}
```

## Accessor operators

| Operator                  | Description                                                        |
| :------------------------ | :----------------------------------------------------------------- |
| `$`                       | The root element to query. This begins all path expressions.       |
| `..`                      | Deep scan for values behind followed key value accessor            |
| `.<name>`                 | Dot-notated key value accessor for JSON objects                    |
| `['<name>' (, '<name>')]` | Bracket-notated key value accessor for JSON objects, comma-delimited|
| `[<number> (, <number>)]` | JSON array accessor for index or comma-delimited indices           |
| `[start:end]`             | JSON array range accessor from start (inclusive) to end (exclusive)|

## Path expression examples
JsonPathLite expressions can use any combination of dot–notation and bracket–notation operators to access JSON values. For examples, these all evaluate to the same result:
```text
$.family.children[0].name
$['family']['children'][0]['name']
$['family'].children[0].name
```

Given the json:
```json
{
    "family": {
        "children": [{
                "name": "Thomas",
                "age": 13
            },
            {
                "name": "Mila",
                "age": 18
            },
            {
                "name": "Konstantin",
                "age": 29,
                "nickname": "Kons"
            },
            {
                "name": "Tracy",
                "age": 4
            }
        ]
    }
}
```

| JsonPath | Result |
| :------- | :----- |
| $.family                  |  The family object  |
| $.family.children         |  The children array  |
| $.family['children']      |  The children array  |
| $.family.children[2]      |  The second child object  |
| $.family.children[-1]     |  The last child object  |
| $.family.children[-3]     |  The 3rd to last child object  |
| $.family.children[1:3]    |  The 2nd and 3rd children objects |
| $.family.children[:3]     |  The first three children |
| $.family.children[:-1]    |  The first three children |
| $.family.children[2:]     |  The last two children  |
| $.family.children[-2:]    |  The last two children  |
| $..name                   |  All names  |
| $.family..name            |  All names nested within family object  |
| $.family.children[:3]..age     |  The ages of first three children |
| $..['name','nickname']    |  Names & nicknames (if any) of all children |

## Code examples
When parsing a JSON, you have the flexibility to either return null or to throw org.json.JSONException on parsing failure.
```kotlin
val json = "{\"hello\": \"world\"}"
val jsonResult = JsonPath.parseOrNull(json)
jsonResult?.read<String>("$.hello") // returns "world"
jsonResult?.read<Double>("$.otherkey") // returns null

val json2 = "{\"list\": [1,2,3,4]}"
JsonPath.parseOrNull(json2)?.read<List<Int>>("$.list[1:3]") // returns listOf(2, 3)

val json3 = "[{\"outer\": {\"inner\": 1}}]"
JsonPath.parseOrNull(json3)?.read<org.json.JSONObject>("$[0].outer") // returns JSONObject
JsonPath.parseOrNull(json3)?.read<org.json.JSONArray>("$") // returns JSONArray
```

JsonPathLite uses [org.json](https://mvnrepository.com/artifact/org.json/json) to decode JSON strings. If you happen to use kotlin in your project, you can evaluate a path against `org.json.JSONObject` and `org.json.JSONArray` directly with the included extension functions:
```kotlin
JSONObject("{\"key\":\"value\"}").read<String>("$.key") // returns "value"
JSONArray("[0,1,2,3,4,5]").read<List<Int>("$[2:]") // returns listOf(2, 3, 4, 5)
```

## Benchmarks
These are benchmark tests of JsonPathLite against Jayway's JsonPath implementation. Results for each test is the average of 30 runs with 80,000 reads per run. You can run these test locally with `./runBenchmarks.sh`

**Evaluating/reading path against large JSON**

| Path Tested | JsonPathLite (ms) | JsonPath (ms) |
| :---------- | :------ | :----- |
|  $[0]['tags'][3:]  |  67 ms *(34 ms w/ cache)* |  134 ms *(94 ms w/ cache)*  |
|  $[0]['tags'][0,3,5]  |  64 ms *(15 ms w/ cache)* |  140 ms *(65 ms w/ cache)*  |
|  $..[:2]  |  94 ms *(88 ms w/ cache)* |  581 ms *(564 ms w/ cache)*  |
|  $..[2:]  |  134 ms *(147 ms w/ cache)* |  568 ms *(537 ms w/ cache)*  |
|  $..[1:-1]  |  134 ms *(125 ms w/ cache)* |  445 ms *(433 ms w/ cache)*  |
|  $[2]._id  |  28 ms *(6 ms w/ cache)* |  64 ms *(36 ms w/ cache)*  |
|  $[0]['tags'][3:5]  |  67 ms *(18 ms w/ cache)* |  116 ms *(61 ms w/ cache)*  |
|  $[0]['tags'][-3]  |  46 ms *(5 ms w/ cache)* |  101 ms *(45 ms w/ cache)*  |
|  $[0].friends[1].other.a.b['c']  |  70 ms *(10 ms w/ cache)* |  167 ms *(82 ms w/ cache)*  |
|  $..name  |  88 ms *(93 ms w/ cache)* |  566 ms *(624 ms w/ cache)*  |
|  $..['email','name']  |  128 ms *(138 ms w/ cache)* |  570 ms *(564 ms w/ cache)*  |
|  $..[1]  |  84 ms *(89 ms w/ cache)* |  493 ms *(481 ms w/ cache)*  |
|  $[0]['latitude','longitude','isActive']  |  68 ms *(13 ms w/ cache)* |  136 ms *(74 ms w/ cache)*  |
|  $[0]['tags'][:3]  |  66 ms *(19 ms w/ cache)* |  126 ms *(71 ms w/ cache)*  |

**Compiling JsonPath string to internal tokens**

| Path size | JsonPathLite | JsonPath |
| :-------- | :----------- | :------- |
|  7 chars, 1 tokens  |  10 ms *(2 ms w/ cache)* |  10 ms *(11 ms w/ cache)* |
|  16 chars, 3 tokens  |  25 ms *(2 ms w/ cache)* |  31 ms *(32 ms w/ cache)* |
|  30 chars, 7 tokens  |  50 ms *(1 ms w/ cache)* |  70 ms *(69 ms w/ cache)* |
|  65 chars, 16 tokens  |  118 ms *(1 ms w/ cache)* |  166 ms *(166 ms w/ cache)* |
|  88 chars, 19 tokens  |  154 ms *(1 ms w/ cache)* |  225 ms *(227 ms w/ cache)* |

# Cache
JsonPathLite uses an LRU cache by default to cache compiled JsonPath tokens. If you don't want to use the cache, you can disable it or set the CacheProvider to use your own implementation of the Cache interface.
```kotlin
// Disable cache
CacheProvider.setCache(null)

// Implement your own cache
CacheProvider.setCache(object : Cache {
    override fun get(path: String): JsonPath? { ... }
    override fun put(path: String, jsonPath: JsonPath) { ... }
)
```

[![Analytics](https://ga-beacon.appspot.com/UA-116910991-3/jsonpathlite/index)](https://github.com/igrigorik/ga-beacon)
