package com.zerowasteeats.app.data.session

class InMemoryKeyValueStore(initial: Map<String, String> = emptyMap()) : KeyValueStore {

    var contents: Map<String, String> = initial
        private set

    override fun getString(key: String): String? = contents[key]

    override fun replaceAll(values: Map<String, String>) {
        contents = values.toMap()
    }
}
