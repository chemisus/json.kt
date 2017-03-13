package json

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.math.BigDecimal

@RunWith(Parameterized::class)
class JSONTest(val value: Any?) {
    companion object {
        fun case(vararg params: Any?): Array<Any?> = params.toList().toTypedArray()
        fun arrayOf(vararg items: Any?) = items.toList()
        fun objectOf(vararg items: Pair<Any?, Any?>) = items.toMap()

        @JvmStatic
        @Parameterized.Parameters
        fun dataProvider(): Collection<Array<Any?>> {
            return listOf(
                    case(null),
                    case(true),
                    case(BigDecimal(0)),
                    case(BigDecimal(1)),
                    case(BigDecimal(-1)),
                    case(BigDecimal(1.5)),
                    case(BigDecimal(-1.5)),
                    case(BigDecimal("0.1E-50")),
                    case(""),
                    case("null"),
                    case("true"),
                    case("false"),
                    case("0"),
                    case("1"),
                    case("-1"),
                    case("[]"),
                    case("{}"),
                    case("a\u000Cb"),
                    case("a"),
                    case("a\""),
                    case("\"a\""),
                    case("\"a\"b\"\"\"\"\\\\\\\\\"\n\n\n\"\t\t\t\\\\\t\\\\\\\rabc\""),
                    case(arrayOf()),
                    case(arrayOf(
                            null,
                            true,
                            false,
                            BigDecimal(0),
                            BigDecimal(1),
                            BigDecimal(-1),
                            BigDecimal(1.5),
                            BigDecimal(-1.5),
                            BigDecimal("0.1E-50"),
                            "",
                            "a",
                            "b",
                            arrayOf(),
                            objectOf()
                    )),
                    case(objectOf()),
                    case(objectOf(
                            "null" to null,
                            "true" to true,
                            "false" to false,
                            "number" to BigDecimal(5),
                            "array" to arrayOf(
                                    null,
                                    true,
                                    false,
                                    BigDecimal(5),
                                    "",
                                    arrayOf(),
                                    objectOf()
                            ),
                            "object" to objectOf(
                                    "null" to null,
                                    "true" to true,
                                    "false" to false,
                                    "number" to BigDecimal(5),
                                    "string" to "",
                                    "array" to arrayOf(),
                                    "object" to objectOf()
                            )
                    ))
            )
        }
    }

    @Test
    fun test() {
        val encoded: String = value.toJson()
        assert(encoded.isNotBlank())

        val decoded = encoded.fromJson()
        assert(value == decoded)
        assert(value == decoded.toJson().fromJson())
        assert(decoded == decoded.toJson().fromJson())
    }
}

