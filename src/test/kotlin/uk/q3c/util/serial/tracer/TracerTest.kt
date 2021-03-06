package uk.q3c.util.serial.tracer

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import uk.q3c.util.serial.tracer.SerializationOutcome.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable


/**
 * Created by David Sowerby on 21 Apr 2018
 */
object TracerTest : Spek({

    given("a standard test object with most single level cases covered") {
        lateinit var standardTestObject: StandardTestObject
        lateinit var tracer: SerializationTracer

        beforeEachTest {
            standardTestObject = StandardTestObject()
            tracer = SerializationTracer()
        }


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

                tracer.results["StandardTestObject.emptyArrayListOfSerializableObject"]?.outcome.shouldBe(EMPTY_PASSED_STATIC_ANALYSIS)
                tracer.results["StandardTestObject.emptyArrayListOfSerializableObject"]?.info.shouldEqual("ArrayList is Serializable. SerializableObject is Serializable.")

                tracer.results["StandardTestObject.nullArrayListOfSerializableObject"]?.outcome.shouldBe(NULL_PASSED_STATIC_ANALYSIS)
                tracer.results["StandardTestObject.nullArrayListOfSerializableObject"]?.info.shouldEqual("ArrayList is Serializable. SerializableObject is Serializable.")

                // field is interface

                tracer.results["StandardTestObject.listOfNonSerializableObject"]?.outcome.shouldBe(FAIL)
                tracer.results["StandardTestObject.listOfNonSerializableObject"]?.info.shouldEqual("java.io.NotSerializableException: uk.q3c.util.serial.tracer.NonSerializableObject")

                tracer.results["StandardTestObject.emptyListOfNonSerializableObject"]?.outcome.shouldBe(EMPTY_FAILED_STATIC_ANALYSIS)
                tracer.results["StandardTestObject.emptyListOfNonSerializableObject"]?.info.shouldEqual("EmptyList is Serializable. NonSerializableObject is NOT Serializable.")

                tracer.results["StandardTestObject.nullListOfNonSerializableObject"]?.outcome.shouldBe(NULL_FAILED_STATIC_ANALYSIS)
                tracer.results["StandardTestObject.nullListOfNonSerializableObject"]?.info.shouldEqual("List is NOT Serializable. NonSerializableObject is NOT Serializable.")

                tracer.results["StandardTestObject.listOfSerializableObject"]?.outcome.shouldBe(PASS)
                tracer.results["StandardTestObject.listOfSerializableObject"]?.info.shouldEqual("")

                tracer.results["StandardTestObject.emptyListOfSerializableObject"]?.outcome.shouldBe(EMPTY_PASSED_STATIC_ANALYSIS)
                tracer.results["StandardTestObject.emptyListOfSerializableObject"]?.info.shouldEqual("EmptyList is Serializable. SerializableObject is Serializable.")

                tracer.results["StandardTestObject.nullListOfSerializableObject"]?.outcome.shouldBe(NULL_FAILED_STATIC_ANALYSIS)
                tracer.results["StandardTestObject.nullListOfSerializableObject"]?.info.shouldEqual("List is NOT Serializable. SerializableObject is Serializable.")


                tracer.results["StandardTestObject.listOfNonSerializableObject"]?.outcome.shouldBe(FAIL)
                tracer.results["StandardTestObject.listOfNonSerializableObject"]?.info.shouldEqual("java.io.NotSerializableException: uk.q3c.util.serial.tracer.NonSerializableObject")

                tracer.results["StandardTestObject.emptyListOfNonSerializableObject"]?.outcome.shouldBe(EMPTY_FAILED_STATIC_ANALYSIS)
                tracer.results["StandardTestObject.emptyListOfNonSerializableObject"]?.info.shouldEqual("EmptyList is Serializable. NonSerializableObject is NOT Serializable.")

                tracer.results["StandardTestObject.nullListOfNonSerializableObject"]?.outcome.shouldBe(NULL_FAILED_STATIC_ANALYSIS)
                tracer.results["StandardTestObject.nullListOfNonSerializableObject"]?.info.shouldEqual("List is NOT Serializable. NonSerializableObject is NOT Serializable.")

                tracer.results["StandardTestObject.classWithSerializedLambda"]?.outcome.shouldBe(PASS)
                tracer.results["StandardTestObject.classWithSerializedLambda"]?.info.shouldEqual("Lambda")


            }
        }

        on("using shouldNotContain on object with a failure") {
            val testObject1 = TestObject1()
            tracer.trace(testObject1)
            val result = { tracer.shouldNotHaveAny(anyFailure) }

            it("should throw assertion error") {
                result.shouldThrow(AssertionError::class)
            }
        }

        on("using shouldNotFail on object with a failure") {
            val testObject1 = TestObject1()
            tracer.trace(testObject1)
            val result = { tracer.shouldNotHaveAnyFailures() }

            it("should throw assertion error") {
                result.shouldThrow(AssertionError::class)
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


        val arrayListOfNonSerializableObject: ArrayList<NonSerializableObject> = arrayListOf(NonSerializableObject(6)),
        val emptyArrayListOfNonSerializableObject: ArrayList<NonSerializableObject> = arrayListOf(),
        val nullArrayListOfNonSerializableObject: ArrayList<NonSerializableObject>? = null,

        val listOfSerializableObject: List<SerializableObject> = listOf(SerializableObject(7)),
        val emptyListOfSerializableObject: List<SerializableObject> = listOf(),
        val nullListOfSerializableObject: List<SerializableObject>? = null,


        val listOfNonSerializableObject: List<NonSerializableObject> = listOf(NonSerializableObject(8)),
        val emptyListOfNonSerializableObject: List<NonSerializableObject> = listOf(),
        val nullListOfNonSerializableObject: List<NonSerializableObject>? = null,

        val classWithSerializedLambda: ClassWithSerializedLambda = ClassWithSerializedLambda()


) : Serializable

private class TestObject1(val serializableObject: SerializableObject = SerializableObject(),
                          val nonSerializableObject: NonSerializableObject = NonSerializableObject())

class SerializableObject(val value: Int = 2) : Serializable
class NonSerializableObject(val value: Int = 3)

class ClassWithSerializedLambda : Serializable {
    val int = 3
    @Transient
    val sz: String = "?"


    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(inputStream: ObjectInputStream) {
        inputStream.defaultReadObject()
        throw ClassCastException("cannot assign instance of java.lang.invoke.SerializedLambda to field com.vaadin.ui.AbstractOrderedLayout.rpc")
    }

}


private fun serialise(obj: Any?) {
    val outputStream = ByteArrayOutputStream(512)
    // stream closed in the finally
    val out = ObjectOutputStream(outputStream)
    out.writeObject(obj)
}