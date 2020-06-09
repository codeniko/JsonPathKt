package com.nfeld.jsonpathlite

import com.nfeld.jsonpathlite.cache.CacheProvider
import org.json.JSONArray
import org.junit.jupiter.api.BeforeAll

open class BaseTest {

    protected val jsonArray: JSONArray

    init {
        jsonArray = JSONArray(LARGE_JSON)
    }

    companion object {
        // we need to reset this singleton across test suites
        fun resetCacheProvider() {
            // use reflection to reset CacheProvider singleton to its initial state
            CacheProvider.javaClass.getDeclaredField("cache").apply {
                isAccessible = true
                set(null, null)
            }
            CacheProvider.javaClass.getDeclaredField("useDefault").apply {
                isAccessible = true
                setBoolean(null, true)
            }
        }

        fun resetJaywayCacheProvider() {
            com.jayway.jsonpath.spi.cache.CacheProvider::class.java.getDeclaredField("cache").apply {
                isAccessible = true
                set(null, null)
            }
            com.jayway.jsonpath.spi.cache.CacheProvider::class.java.getDeclaredField("cachingEnabled").apply {
                isAccessible = true
                setBoolean(null, false)
            }
        }

        const val SMALL_JSON = "{\"key\": 5}"
        const val SMALL_JSON_ARRAY = "[1,2,3,4, $SMALL_JSON]"
        const val LARGE_JSON = "[\n" +
                "  {\n" +
                "    \"_id\": \"5c77a899cd278f94d64b996e\",\n" +
                "    \"index\": 0,\n" +
                "    \"guid\": \"fbb80dca-349c-4b49-8dc1-b01c6684e9b7\",\n" +
                "    \"isActive\": true,\n" +
                "    \"balance\": \"\$1,396.91\",\n" +
                "    \"picture\": \"http://placehold.it/32x32\",\n" +
                "    \"age\": 30,\n" +
                "    \"eyeColor\": \"brown\",\n" +
                "    \"name\": \"Salazar Casey\",\n" +
                "    \"gender\": \"male\",\n" +
                "    \"company\": \"ACUSAGE\",\n" +
                "    \"email\": \"salazarcasey@acusage.com\",\n" +
                "    \"phone\": \"+1 (825) 518-2194\",\n" +
                "    \"address\": \"535 Bassett Avenue, Clara, Virgin Islands, 3427\",\n" +
                "    \"about\": \"Laboris ea anim exercitation occaecat incididunt magna ipsum. Nostrud aliqua culpa esse quis. Irure pariatur consectetur dolore id mollit dolor non nisi. Adipisicing incididunt ut laborum quis magna aute adipisicing proident excepteur dolore nisi incididunt officia. Velit culpa occaecat consequat ipsum consequat aliquip adipisicing.\\r\\n\",\n" +
                "    \"registered\": \"2018-04-06T11:49:49 +07:00\",\n" +
                "    \"latitude\": -85.888651,\n" +
                "    \"longitude\": 38.287152,\n" +
                "    \"tags\": [\n" +
                "      \"occaecat\",\n" +
                "      \"mollit\",\n" +
                "      \"ullamco\",\n" +
                "      \"labore\",\n" +
                "      \"cillum\",\n" +
                "      \"laboris\",\n" +
                "      \"qui\"\n" +
                "    ],\n" +
                "    \"friends\": [\n" +
                "      {\n" +
                "        \"id\": 0,\n" +
                "        \"name\": \"Kathrine Osborn\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 1,\n" +
                "        \"name\": \"Vonda Howe\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 2,\n" +
                "        \"name\": \"Harrell Pratt\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"greeting\": \"Hello, Salazar Casey! You have 4 unread messages.\",\n" +
                "    \"favoriteFruit\": \"banana\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"_id\": \"5c77a89993e1f14b74b25242\",\n" +
                "    \"index\": 1,\n" +
                "    \"guid\": \"a3499511-2785-4256-95ab-2a670cdd8970\",\n" +
                "    \"isActive\": true,\n" +
                "    \"balance\": \"\$1,647.14\",\n" +
                "    \"picture\": \"http://placehold.it/32x32\",\n" +
                "    \"age\": 20,\n" +
                "    \"eyeColor\": \"brown\",\n" +
                "    \"name\": \"Porter Cummings\",\n" +
                "    \"gender\": \"male\",\n" +
                "    \"company\": \"ZBOO\",\n" +
                "    \"email\": \"portercummings@zboo.com\",\n" +
                "    \"phone\": \"+1 (934) 443-3056\",\n" +
                "    \"address\": \"382 Cropsey Avenue, Brownsville, Minnesota, 8135\",\n" +
                "    \"about\": \"Non laborum adipisicing laborum consequat. Labore ex aliquip adipisicing labore nisi occaecat magna sunt cillum. Occaecat tempor minim quis dolor duis tempor duis nisi culpa adipisicing est eu laborum sit. Laborum fugiat sit minim proident incididunt cillum Lorem consequat consequat cupidatat elit velit. Aliquip ad excepteur sit proident ut aute sint sit adipisicing. Nisi ex velit sit consectetur ullamco laborum esse.\\r\\n\",\n" +
                "    \"registered\": \"2018-03-28T10:28:58 +07:00\",\n" +
                "    \"latitude\": 71.831798,\n" +
                "    \"longitude\": -6.47102,\n" +
                "    \"tags\": [\n" +
                "      \"aliquip\",\n" +
                "      \"cillum\",\n" +
                "      \"qui\",\n" +
                "      \"ut\",\n" +
                "      \"ea\",\n" +
                "      \"eu\",\n" +
                "      \"reprehenderit\"\n" +
                "    ],\n" +
                "    \"friends\": [\n" +
                "      {\n" +
                "        \"id\": 0,\n" +
                "        \"name\": \"Mason Leach\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 1,\n" +
                "        \"name\": \"Spencer Valenzuela\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 2,\n" +
                "        \"name\": \"Hope Medina\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"greeting\": \"Hello, Porter Cummings! You have 2 unread messages.\",\n" +
                "    \"favoriteFruit\": \"banana\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"_id\": \"5c77a8990b5cb33ad72de49c\",\n" +
                "    \"index\": 2,\n" +
                "    \"guid\": \"cfd43d1f-b2ef-4253-a178-ef2005def58b\",\n" +
                "    \"isActive\": true,\n" +
                "    \"balance\": \"\$1,931.71\",\n" +
                "    \"picture\": \"http://placehold.it/32x32\",\n" +
                "    \"age\": 25,\n" +
                "    \"eyeColor\": \"brown\",\n" +
                "    \"name\": \"Marie Hampton\",\n" +
                "    \"gender\": \"female\",\n" +
                "    \"company\": \"ZENCO\",\n" +
                "    \"email\": \"mariehampton@zenco.com\",\n" +
                "    \"phone\": \"+1 (991) 513-3236\",\n" +
                "    \"address\": \"447 Mayfair Drive, Waterview, South Carolina, 6589\",\n" +
                "    \"about\": \"Eu non excepteur aute ipsum occaecat et deserunt veniam minim. Ea est exercitation incididunt ut id. Nulla sit labore Lorem aliqua quis aute et excepteur reprehenderit.\\r\\n\",\n" +
                "    \"registered\": \"2016-06-17T03:15:38 +07:00\",\n" +
                "    \"latitude\": 78.266157,\n" +
                "    \"longitude\": 123.788551,\n" +
                "    \"tags\": [\n" +
                "      \"nulla\",\n" +
                "      \"elit\",\n" +
                "      \"ipsum\",\n" +
                "      \"pariatur\",\n" +
                "      \"ullamco\",\n" +
                "      \"ut\",\n" +
                "      \"sint\"\n" +
                "    ],\n" +
                "    \"friends\": [\n" +
                "      {\n" +
                "        \"id\": 0,\n" +
                "        \"name\": \"Felecia Bright\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": \"Maryanne Wiggins\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 2,\n" +
                "        \"name\": \"Marylou Caldwell\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"greeting\": \"Hello, Marie Hampton! You have 2 unread messages.\",\n" +
                "    \"favoriteFruit\": \"strawberry\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"_id\": \"5c77a899e9049ac59c961b66\",\n" +
                "    \"index\": 3,\n" +
                "    \"guid\": \"b19105ce-5f9b-402b-aa79-43e784204409\",\n" +
                "    \"isActive\": true,\n" +
                "    \"balance\": \"\$1,988.61\",\n" +
                "    \"picture\": \"http://placehold.it/32x32\",\n" +
                "    \"age\": 27,\n" +
                "    \"eyeColor\": \"blue\",\n" +
                "    \"name\": \"Mari Pugh\",\n" +
                "    \"gender\": \"female\",\n" +
                "    \"company\": \"NITRACYR\",\n" +
                "    \"email\": \"maripugh@nitracyr.com\",\n" +
                "    \"phone\": \"+1 (919) 440-2447\",\n" +
                "    \"address\": \"785 Hamilton Walk, Osage, Arkansas, 6691\",\n" +
                "    \"about\": \"Commodo nostrud et est excepteur tempor deserunt incididunt aliquip irure eu enim pariatur dolore. Tempor officia in ullamco cupidatat tempor sunt aliqua. Ipsum est ipsum aute nisi Lorem ut velit sint.\\r\\n\",\n" +
                "    \"registered\": \"2015-04-18T12:18:09 +07:00\",\n" +
                "    \"latitude\": -10.214391,\n" +
                "    \"longitude\": -161.704708,\n" +
                "    \"tags\": [\n" +
                "      \"fugiat\",\n" +
                "      \"sit\",\n" +
                "      \"ad\",\n" +
                "      \"voluptate\",\n" +
                "      \"officia\",\n" +
                "      \"aute\",\n" +
                "      \"duis\"\n" +
                "    ],\n" +
                "    \"friends\": [\n" +
                "      {\n" +
                "        \"id\": 0,\n" +
                "        \"name\": \"Rios Norton\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 1,\n" +
                "        \"name\": \"Judy Good\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 2,\n" +
                "        \"name\": \"Rosetta Stanley\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"greeting\": \"Hello, Mari Pugh! You have 7 unread messages.\",\n" +
                "    \"favoriteFruit\": \"banana\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"_id\": \"5c77a8994c3eff3e50e09963\",\n" +
                "    \"index\": 4,\n" +
                "    \"guid\": \"f879acba-9cf3-4b73-ab2a-277c47fc0cbf\",\n" +
                "    \"isActive\": true,\n" +
                "    \"balance\": \"\$3,199.50\",\n" +
                "    \"picture\": \"http://placehold.it/32x32\",\n" +
                "    \"age\": 28,\n" +
                "    \"eyeColor\": \"green\",\n" +
                "    \"name\": \"Margret Quinn\",\n" +
                "    \"gender\": \"female\",\n" +
                "    \"company\": \"IZZBY\",\n" +
                "    \"email\": \"margretquinn@izzby.com\",\n" +
                "    \"phone\": \"+1 (827) 486-2105\",\n" +
                "    \"address\": \"809 Crescent Street, Clarktown, Michigan, 3904\",\n" +
                "    \"about\": \"Ut do Lorem fugiat esse exercitation cillum. Id cupidatat dolore fugiat pariatur qui voluptate id anim officia sit irure aliquip. Consectetur consectetur proident enim pariatur pariatur ad do. Et elit cillum duis laboris.\\r\\n\",\n" +
                "    \"registered\": \"2018-05-28T07:53:36 +07:00\",\n" +
                "    \"latitude\": 32.293366,\n" +
                "    \"longitude\": -138.054955,\n" +
                "    \"tags\": [\n" +
                "      \"est\",\n" +
                "      \"dolor\",\n" +
                "      \"dolore\",\n" +
                "      \"exercitation\",\n" +
                "      \"minim\",\n" +
                "      \"dolor\",\n" +
                "      \"pariatur\"\n" +
                "    ],\n" +
                "    \"friends\": [\n" +
                "      {\n" +
                "        \"id\": 0,\n" +
                "        \"name\": \"Lora Cotton\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 1,\n" +
                "        \"name\": \"Gaines Henry\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 2,\n" +
                "        \"name\": \"Dorothea Irwin\",\n" +
                "        \"other\": {\n" +
                "          \"a\": {\n" +
                "            \"b\": {\n" +
                "              \"c\": \"yo\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"greeting\": \"Hello, Margret Quinn! You have 2 unread messages.\",\n" +
                "    \"favoriteFruit\": \"strawberry\"\n" +
                "  },{\n" +
                "       nums: [1,2,3,4,5]\n" +
                "  }\n" +
                "]"

        const val BOOKS_JSON = "{\"store\": {\"book\": [{\"category\": \"reference\", \"price\": 8.95, \"title\": \"Sayings of the Century\", \"author\": \"Nigel Rees\"}, {\"category\": \"fiction\", \"price\": 12.99, \"title\": \"Sword of Honour\", \"author\": \"Evelyn Waugh\"}, {\"category\": \"fiction\", \"price\": 8.99, \"title\": \"Moby Dick\", \"isbn\": \"0-553-21311-3\", \"author\": \"Herman Melville\"}, {\"category\": \"fiction\", \"price\": 22.99, \"title\": \"The Lord of the Rings\", \"isbn\": \"0-395-19395-8\", \"author\": \"J. R. R. Tolkien\"}], \"bicycle\": {\"color\": \"red\", \"price\": 19.95}}}"
    }

}