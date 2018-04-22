package uk.q3c.util.serial.tracer

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import uk.q3c.util.serial.tracer.SerializationOutcome.FAIL
import uk.q3c.util.serial.tracer.SerializationOutcome.PASS
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Created by David Sowerby on 21 Apr 2018
 */
object TracerTest : Spek({

    given("a standard test object with most single level cases covered") {
        val standardTestObject = StandardTestObject()
        val tracer = SerializationTracer()

        on("round trip serialisation") {
            tracer.trace(standardTestObject)
            println(tracer.resultsAll())

            it("shows fail on all but one field") {
                tracer.results["StandardTestObject.serializableObject"]?.outcome.shouldBe(PASS)
                tracer.results["StandardTestObject.serializableObject"]?.info.shouldEqual("")

                tracer.results["StandardTestObject.nonSerializableObject"]?.outcome.shouldBe(FAIL)
                tracer.results["StandardTestObject.nonSerializableObject"]?.info.shouldEqual("uk.q3c.util.serial.tracer.NonSerializableObject cannot be cast to java.io.Serializable")

                tracer.results["StandardTestObject.arrayListOfSerializableObject"]?.outcome.shouldBe(PASS)
                tracer.results["StandardTestObject.arrayListOfSerializableObject"]?.info.shouldEqual("")

                tracer.results["StandardTestObject.emptyArrayListOfSerializableObject"]?.outcome.shouldBe(PASS)
                tracer.results["StandardTestObject.emptyArrayListOfSerializableObject"]?.info.shouldEqual("")

                tracer.results["StandardTestObject.nullArrayListOfSerializableObject"]?.outcome.shouldBe(SerializationOutcome.NULL_PASSED_STATIC_ANALYSIS)
                tracer.results["StandardTestObject.nullArrayListOfSerializableObject"]?.info.shouldEqual("ArrayList is Serializable. SerializableObject is Serializable.")


                tracer.results["StandardTestObject.arrayListOfNonSerializableObject"]?.outcome.shouldBe(FAIL)
                tracer.results["StandardTestObject.arrayListOfNonSerializableObject"]?.info.shouldEqual("java.io.NotSerializableException: uk.q3c.util.serial.tracer.NonSerializableObject")

                tracer.results["StandardTestObject.emptyArrayListOfNonSerializableObject"]?.outcome.shouldBe(FAIL)
                tracer.results["StandardTestObject.emptyArrayListOfNonSerializableObject"]?.info.shouldEqual("java.io.NotSerializableException: uk.q3c.util.serial.tracer.NonSerializableObject")

                tracer.results["StandardTestObject.nullArrayListOfNonSerializableObject"]?.outcome.shouldBe(SerializationOutcome.NULL_PASSED_STATIC_ANALYSIS)
                tracer.results["StandardTestObject.nullArrayListOfNonSerializableObject"]?.info.shouldEqual("")

            }
        }
    }

})


private class StandardTestObject(
        val serializableObject: SerializableObject = SerializableObject(),
        val nonSerializableObject: NonSerializableObject = NonSerializableObject(),

        val arrayListOfSerializableObject: ArrayList<SerializableObject> = arrayListOf(SerializableObject(5)),
        val emptyArrayListOfSerializableObject: ArrayList<SerializableObject> = arrayListOf(),
        val nullArrayListOfSerializableObject: ArrayList<SerializableObject>? = null,


        val arrayListOfNonSerializableObject: ArrayList<NonSerializableObject> = arrayListOf(NonSerializableObject(5)), // fails correctly
        val emptyArrayListOfNonSerializableObject: ArrayList<NonSerializableObject> = arrayListOf(), // pass, but shouldn't
        val nullArrayListOfNonSerializableObject: ArrayList<NonSerializableObject>? = null // pass, but shouldn't


) : Serializable

class SerializableObject(val value: Int = 2) : Serializable
class NonSerializableObject(val value: Int = 3)


private fun serialise(obj: Any?) {
    val outputStream = ByteArrayOutputStream(512)
    // stream closed in the finally
    val out = ObjectOutputStream(outputStream)
    out.writeObject(obj)
}