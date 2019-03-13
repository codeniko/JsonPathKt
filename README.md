# JsonPathLite
[![Build Status](https://travis-ci.com/codeniko/JsonPathLite.svg?branch=master)](https://travis-ci.com/codeniko/JsonPathLite)

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
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Gradle**
```gradle
repositories {
    mavenCentral()
    maven { url "https://oss.sonatype.org/content/groups/public" } // for snapshot builds
}

dependencies {
    implementation 'com.nfeld.jsonpathlite:json-path-lite:1.0.0-SNAPSHOT'
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
|  $[0]['tags'][3:]  |  86 ms |  136 ms |
|  $[0]['tags'][0,3, 5]  |  65 ms |  148 ms |
|  $..[:2]  |  96 ms |  575 ms |
|  $..[2:]  |  156 ms |  569 ms |
|  $..[1:-1]  |  135 ms |  430 ms |
|  $[2]._id  |  27 ms |  63 ms |
|  $[0]['tags'][3:5]  |  66 ms |  115 ms |
|  $[0]['tags'][-3]  |  45 ms |  100 ms |
|  $[0].friends[1].other.a.b['c']  |  70 ms |  164 ms |
|  $..name  |  86 ms |  556 ms |
|  $..['email','name']  |  138 ms |  557 ms |
|  $..[1]  |  86 ms |  467 ms |
|  $[0]['latitude','longitude', 'isActive']  |  70 ms |  138 ms |
|  $[0]['tags'][:3]  |  64 ms |  120 ms |

**Compiling JsonPath string to internal tokens**

| Path size | JsonPathLite | JsonPath |
| :-------- | :----------- | :------- |
|  7 chars, 1 tokens  |  9 ms  |  10 ms  |
|  16 chars, 3 tokens  |  25 ms  |  32 ms  |
|  30 chars, 7 tokens  |  50 ms  |  72 ms  |
|  65 chars, 16 tokens  |  120 ms  |  172 ms  |
|  88 chars, 19 tokens  |  159 ms  |  232 ms  |

[![Analytics](https://ga-beacon.appspot.com/UA-116910991-3/jsonpathlite/index)](https://github.com/igrigorik/ga-beacon)
