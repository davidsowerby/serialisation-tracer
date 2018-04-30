package uk.q3c.util.serial.tracer

import org.apache.commons.lang3.SerializationException
import org.apache.commons.lang3.SerializationUtils
import org.apache.commons.lang3.reflect.FieldUtils
import uk.q3c.util.serial.tracer.SerializationOutcome.*
import java.io.Serializable
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

/**
 * Created by David Sowerby on 19 Apr 2018
 */
class SerializationTracer {
    private var processedObjects: MutableList<Any> = mutableListOf()


    var results: MutableMap<String, SerializationResult> = mutableMapOf()
        private set

    fun trace(target: Any) {
        results = mutableMapOf()
        processedObjects = mutableListOf()
        processInitialValue(target)
    }

    /**
     * Throws [AssertionError] if any of the [outcomes] are present in [results]
     */
    fun shouldNotHaveAny(vararg outcomes: SerializationOutcome) {
        val oc = setOf(*outcomes)
        shouldNotHaveAny(oc)
    }

    /**
     * Throws [AssertionError] if any of the [outcomes] are present in [results]
     */
    fun shouldNotHaveAny(outcomes: Set<SerializationOutcome>) {
        results.forEach({ (_, v) ->
            if (outcomes.contains(v.outcome)) {
                val resultString = results(outcomes)
                throw AssertionError("One or more serialisations failed: \n$resultString")
            }
        })
    }

    /**
     * Throws [AssertionError] if any of the outcomes defined by [anyFailure] are present in [results]
     */
    fun shouldNotHaveAnyFailures() {
        shouldNotHaveAny(anyFailure)
    }

    fun results(vararg outcomes: SerializationOutcome): String {
        val oc: Set<SerializationOutcome> = setOf(*outcomes)
        return results(oc)
    }

    fun results(outcomes: Set<SerializationOutcome>): String {
        val buf = StringBuilder()
        results.forEach({ (k, v) ->
            if (outcomes.contains(v.outcome)) {
                buf.append("$k -> $v\n")
            }
        })
        return buf.toString()
    }

    fun hasNoFailures(): Boolean {
        return outcomes(anyFailure).isEmpty()
    }

    fun hasNo(vararg outcomes: SerializationOutcome): Boolean {
        return outcomes(*outcomes).isEmpty()
    }

    fun hasNo(outcomes: Set<SerializationOutcome>): Boolean {
        return outcomes(outcomes).isEmpty()
    }

    fun resultsAll(): String {
        val buf = StringBuilder()
        results.forEach({ (k, v) -> buf.append("$k -> $v\n") })
        return buf.toString()
    }

    /**
     * Returns [results] filtered for [outcomes]
     */
    fun outcomes(vararg outcomes: SerializationOutcome): Map<String, SerializationResult> {
        return results.filter({ (_, v) -> outcomes.contains(v.outcome) })
    }

    fun outcomes(outcomes: Set<SerializationOutcome>): Map<String, SerializationResult> {
        return results.filter({ (_, v) -> outcomes.contains(v.outcome) })
    }

    private fun processInitialValue(x: Any) {
        addToProcessedList(x)
        trySerialisation(x.javaClass.simpleName, x)
        drilldown(x.javaClass.simpleName, x)
    }

    private fun drilldown(fieldPath: String, target: Any) {
        val targetClass = target.javaClass
        val fields = FieldUtils.getAllFields(targetClass)

        for (field in fields) {
            if (!excluded(fieldPath, field, target)) {
                processFieldValue("$fieldPath.${field.name}", fieldValue(field, target), field)
            }
        }
    }

    fun processFieldValue(fieldPath: String, fieldValue: Any, field: Field) {
        if (isProcessed(fieldValue)) {
            // duplicate - what should be done here?
        } else {
            addToProcessedList(fieldValue)// add early, object could refer to itself

            trySerialisation(fieldPath, fieldValue, field)
            drilldown(fieldPath, fieldValue)
        }
    }

    private fun addToProcessedList(x: Any) {
        if (!isPrimitive(x)) {
            processedObjects.add(x)
        }
    }

    private fun isProcessed(x: Any): Boolean {
        if (isPrimitive(x)) {
            return true
        }

        // we cannot rely on comparisons for empty lists - as far as I can see, Kotlin uses an EmptyList internally,
        // which gets re-used - which means the comparison operator gives the wrong result
        // this can not make the trace cyclic as the list is empty
        if (x is Collection<*> && x.isEmpty()) {
            return false
        }
        for (a in processedObjects) {
            if (a === x) {
                return true
            }
        }
        return false
    }

    private fun isPrimitive(x: Any): Boolean {
        return x::class.javaPrimitiveType != null
    }

    private fun trySerialisation(fieldPath: String, fieldValue: Any, field: Field? = null): SerializationResult {
        var result: SerializationResult
        try {
            val output = SerializationUtils.serialize(fieldValue as Serializable)
            try {
                SerializationUtils.deserialize<Any>(output)
            } catch (cce: SerializationException) {
                if (cce.cause is ClassCastException) {
                    val cause = cce.cause as ClassCastException
                    if (cause.message != null) {
                        val msg = cause.message as String
                        if (msg.contains("cannot assign instance of java.lang.invoke.SerializedLambda")) {
                            result = SerializationResult(PASS, "Lambda")
                            return result
                        }
                    }

                }
            }
            result = if (fieldValue is Collection<*> && fieldValue.isEmpty()) {
                if (field != null) {
                    emptyCollectionStaticAnalysis(field, fieldValue)
                } else {
                    SerializationResult(PASS)
                }
            } else {
                SerializationResult(PASS)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            result = SerializationResult(FAIL, e.message ?: "")
        }
        results[fieldPath] = result
        return result
    }


    private fun fieldValue(field: Field, target: Any): Any {
        field.isAccessible = true
        val value = field.get(target)
        if (value == null) {
            throw NullPointerException("Nulls should not get this far")
        } else {
            return value
        }
    }

    private fun excluded(fieldPath: String, field: Field, target: Any): Boolean {
        return fieldIsTransient(fieldPath, field) || fieldIsStatic(fieldPath, field) || fieldIsNull(fieldPath, field, target) || fieldIsLambda(field, target)
    }

    private fun fieldIsTransient(fieldPath: String, field: Field): Boolean {
        if (Modifier.isTransient(field.modifiers)) {
            results["$fieldPath.${field.name}"] = SerializationResult(TRANSIENT)
            return true
        }
        return false
    }

    private fun fieldIsStatic(fieldPath: String, field: Field): Boolean {
        if (Modifier.isStatic(field.modifiers)) {
            results["$fieldPath.${field.name}"] = SerializationResult(STATIC_FIELD)
            return true
        }
        return false
    }

    private fun fieldIsNull(fieldPath: String, field: Field, target: Any): Boolean {
        field.isAccessible = true
        if (field.get(target) == null) {
            val staticAnalysisResult = nullStaticAnalysis(field)
            results["$fieldPath.${field.name}"] = staticAnalysisResult
            return true
        }
        return false
    }

    private fun fieldIsLambda(field: Field, target: Any): Boolean {
        field.isAccessible = true
        val value = field.get(target)
        return if (value == null) {
            false
        } else {
            valueIsLambda(value)
        }
    }

    private fun valueIsLambda(value: Any): Boolean {
        return value.javaClass.name.contains(ignoreCase = true, other = "\$lambda")
    }


    private fun staticPass(isEmptyCollection: Boolean): SerializationOutcome {
        return if (isEmptyCollection) {
            SerializationOutcome.EMPTY_PASSED_STATIC_ANALYSIS
        } else {
            SerializationOutcome.NULL_PASSED_STATIC_ANALYSIS
        }
    }

    private fun staticFail(isEmptyCollection: Boolean): SerializationOutcome {
        return if (isEmptyCollection) {
            SerializationOutcome.EMPTY_FAILED_STATIC_ANALYSIS
        } else {
            SerializationOutcome.NULL_FAILED_STATIC_ANALYSIS
        }
    }

    private fun nullStaticAnalysis(field: Field): SerializationResult {
        return staticAnalysis(field.type, field, false)
    }

    private fun emptyCollectionStaticAnalysis(field: Field, fieldValue: Any): SerializationResult {
        return staticAnalysis(fieldValue.javaClass, field, true)
    }


    /**
     * Checks [clazz] to ensure it implements [Serializable].  If [clazz] is genericised,  it also checks generic parameters to ensure they are also [Serializable]
     *
     * @return SerializationOutcome of [PASS] if field and its generics are [Serializable], otherwise [NULL_FAILED_STATIC_ANALYSIS]
     */
    private fun staticAnalysis(clazz: Class<*>, field: Field, emptyCollection: Boolean): SerializationResult {
        val buf = StringBuilder()
        var outcome = staticPass(emptyCollection)
        if (Serializable::class.java.isAssignableFrom(clazz)) {
            buf.append("${clazz.simpleName} is Serializable.")
        } else {
            buf.append("${clazz.simpleName} is NOT Serializable.")
            outcome = staticFail(emptyCollection)
        }

        val genericType = field.genericType
        if (genericType != null && genericType is ParameterizedType) {
            genericType.actualTypeArguments.forEach { t ->

                var isSerializable: Boolean

                try {
                    isSerializable = (Serializable::class.java.isAssignableFrom(t as Class<*>))
                } catch (cce: ClassCastException) {
                    isSerializable = false // not a class, so cannot be Serializable - typically it is a wildcard
                }

                val simpleTypeName = t.typeName.substring(t.typeName.lastIndexOf(".") + 1)

                if (isSerializable) {
                    buf.append(" $simpleTypeName is Serializable.")
                } else {
                    buf.append(" $simpleTypeName is NOT Serializable.")
                    outcome = staticFail(emptyCollection)
                }
            }
        }

        return SerializationResult(outcome, buf.toString())
    }
}

enum class SerializationOutcome {
    PASS, FAIL, TRANSIENT, NULL_PASSED_STATIC_ANALYSIS, STATIC_FIELD, NULL_FAILED_STATIC_ANALYSIS, EMPTY_PASSED_STATIC_ANALYSIS, EMPTY_FAILED_STATIC_ANALYSIS
}

val anyFailure: Set<SerializationOutcome> = setOf(FAIL, NULL_FAILED_STATIC_ANALYSIS, EMPTY_FAILED_STATIC_ANALYSIS)

data class SerializationResult(val outcome: SerializationOutcome, val info: String = "")
data class TraceOutcome(val path: String, val outcome: SerializationOutcome)
