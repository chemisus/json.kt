package json

import java.math.BigDecimal

class JSON {
    class JsonException(message: String) : Exception(message)

    interface JsonBuilder {
        fun addString(value: String)
        fun add(value: Any?)
        fun build(): Any?
    }

    class ValueBuilder : JsonBuilder {
        var value: Any? = null

        override fun addString(value: String) {
            this.value = value
        }

        override fun add(value: Any?) {
            this.value = value
        }

        override fun build(): Any? {
            return value
        }
    }

    class ArrayBuilder(val values: MutableList<Any?> = mutableListOf<Any?>()) : JsonBuilder {
        override fun addString(value: String) {
            values.add(value)
        }

        override fun add(value: Any?) {
            values.add(value)
        }

        override fun build(): Any? {
            return values
        }
    }

    class ObjectBuilder(val values: MutableMap<String, Any?> = mutableMapOf<String, Any?>()) : JsonBuilder {
        enum class State {
            NEED_KEY,
            NEED_VALUE
        }

        var state: State = State.NEED_KEY
        var key: String = ""

        override fun addString(value: String) {
            when (state) {
                State.NEED_KEY -> {
                    key = value
                    state = State.NEED_VALUE
                }
                State.NEED_VALUE -> add(value)
            }
        }

        override fun add(value: Any?) {
            when (state) {
                State.NEED_VALUE -> {
                    values[key] = value
                    state = State.NEED_KEY
                }
                else -> {
                    throw JsonException("expected key to be a string")
                }
            }
        }

        override fun build(): Any? {
            return values
        }
    }

    class Reader(val json: CharSequence, var i: Int = 0) {
        val n = json.length
        val stacks = mutableListOf<JsonBuilder>()
        var stack: JsonBuilder = ValueBuilder()
        val current get() = json[i]
        val valid get() = i < n

        fun readNull() {
            stack.add(null)
            i += 4
        }

        fun readTrue() {
            stack.add(true)
            i += 4
        }

        fun readFalse() {
            stack.add(false)
            i += 5
        }

        fun readNumber() {
            val start = i
            i++
            while (valid && current in '0'..'9') {
                i++
            }
            if (valid && current == '.') {
                i++
                while (valid && current in '0'..'9') {
                    i++
                }
            }
            if (valid && current.toLowerCase() == 'e') {
                i++

                if (valid && (current == '+' || current == '-')) {
                    i++
                }

                while (valid && current in '0'..'9') {
                    i++
                }
            }
            val decimal = BigDecimal(json.subSequence(start, i).toString())
            stack.add(decimal)
            i++
        }

        fun readString() {
            val builder = StringBuilder()
            i++
            while (valid && current != '"') {
                val start = i
                while (valid && current != '"' && current != '\\') {
                    i++
                }
                builder.append(json.subSequence(start, i))
                if (current == '\\') {
                    i++
                    builder.append(when (current) {
                        '"' -> '"'
                        '\\' -> '\\'
                        '/' -> '/'
                        'b' -> '\b'
                        'f' -> '\u000C'
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                    /**
                     * @todo fix \u handlers
                     */
                        'u' -> {
                            i += 4
                        }
                        else -> current
                    })
                    i++
                }
            }
            stack.addString(builder.toString())
            i++
        }

        fun readOpenContainer(builder: JsonBuilder) {
            stack.add(builder.build())
            stacks.add(stack)
            stack = builder
            i++
        }

        fun readCloseContainer() {
            stack = stacks.removeAt(stacks.lastIndex)
            i++
        }

        fun read(): Any? {
            while (i < n) {
                when (current) {
                    'n' -> readNull()
                    't' -> readTrue()
                    'f' -> readFalse()
                    '-', in '0'..'9' -> readNumber()
                    '"' -> readString()
                    '[' -> readOpenContainer(ArrayBuilder())
                    '{' -> readOpenContainer(ObjectBuilder())
                    ']', '}' -> readCloseContainer()
                    else -> {
                        i++
                    }
                }
            }

            return stack.build()
        }
    }

    companion object {
        fun parse(json: CharSequence): Any? {
            return Reader(json).read()
        }

        fun stringify(value: Any?): String {
            return when (value) {
                null -> "null"
                is Boolean, is Number -> value.toString()
                is CharSequence -> '"' + value.toString()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\b", "\\b")
                        .replace("\u000C", "\\f")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t") + '"'
                is Collection<*> -> {
                    val items = value.map { JSON.stringify(it) }.joinToString(",")
                    "[$items]"
                }
                is Map<*, *> -> {
                    val entries = value.map {
                        val key = JSON.stringify(it.key.toString())
                        val value = JSON.stringify(it.value)
                        "$key:$value"
                    }.joinToString(",")
                    "{$entries}"
                }
                else -> {
                    val name = value.javaClass.name
                    throw JsonException("could not stringify type of $name")
                }
            }
        }
    }
}

fun CharSequence.fromJson(): Any? = JSON.parse(this)
fun Any?.toJson(): String = JSON.stringify(this)

