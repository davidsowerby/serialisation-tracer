package uk.q3c.util.serial.tracer

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
    var processedObjects: MutableList<Any> = mutableListOf()


    var results: MutableMap<String, SerializationResult> = mutableMapOf()
        private set

    fun trace(target: Any) {
        results = mutableMapOf()
        processedObjects = mutableListOf()

        processFieldValue(target.javaClass.simpleName, target)

    }

    fun assert(target: Any) {
        trace(target)
        results.forEach({ (k, v) ->
            if (v.outcome == FAIL) {
                println(results(FAIL))
                throw AssertionError("One or more serialisations failed")
            }
        })
    }

    fun results(vararg outcomes: SerializationOutcome): String {
        val buf = StringBuilder()
        results.forEach({ (k, v) ->
            if (outcomes.contains(v.outcome)) {
                buf.append("$k -> $v\n")
            }
        })
        return buf.toString()
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
        return results.filter({ (k, v) -> outcomes.contains(v.outcome) })
    }

    fun processFieldValue(fieldPath: String, fieldValue: Any) {
        if (isProcessed(fieldValue)) {
            // duplicate - what should be done here?
        } else {
            addToProcessedList(fieldValue)// add early, object could refer to itself

            trySerialisation(fieldPath, fieldValue)
            val targetClass = fieldValue.javaClass
            val fields = FieldUtils.getAllFields(targetClass)

            for (field in fields) {
                if (!excluded(fieldPath, field, fieldValue)) {
                    processFieldValue("$fieldPath.${field.name}", fieldValue(field, fieldValue))
                }
            }

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

    private fun trySerialisation(fieldPath: String, fieldValue: Any): SerializationResult {
        var result: SerializationResult
        try {
            val output = SerializationUtils.serialize(fieldValue as Serializable)
            SerializationUtils.deserialize<Any>(output)
            result = SerializationResult(PASS)
        } catch (e: Exception) {
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

    private fun excluded(fieldPath: String, field: Field, fieldValue: Any): Boolean {
        return fieldIsTransient(fieldPath, field) || fieldIsStatic(fieldPath, field) || fieldIsNull(fieldPath, field, fieldValue)
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
            val staticAnalysisResult = staticAnalysis(field)
            results["$fieldPath.${field.name}"] = staticAnalysisResult
            return true
        }
        return false
    }

    /**
     * Checks [clazz] to ensure it implements [Serializable].  If [clazz] is genericised,  it also checks generic parameters to ensure they are also [Serializable]
     *
     * @return SerializationOutcome of [PASS] if field and its generics are [Serializable], otherwise [NULL_FAILED_STATIC_ANALYSIS]
     */
    private fun staticAnalysis(field: Field): SerializationResult {
        val buf = StringBuilder()
        var outcome = SerializationOutcome.NULL_PASSED_STATIC_ANALYSIS
        val clazz = field.type
        if (Serializable::class.java.isAssignableFrom(clazz)) {
            buf.append("${clazz.simpleName} is Serializable.")
            val genericType = field.genericType
            if (genericType != null && genericType is ParameterizedType) {
                genericType.actualTypeArguments.forEach { t ->
                    if (Serializable::class.java.isAssignableFrom(t as Class<*>)) {
                        buf.append(" ${t.simpleName} is Serializable.")
                    } else {
                        buf.append(" ${t.simpleName} is NOT Serializable.")
                        outcome = NULL_FAILED_STATIC_ANALYSIS
                    }
                }
            }
        }
        return SerializationResult(outcome, buf.toString())
    }
}

enum class SerializationOutcome {
    PASS, FAIL, TRANSIENT, NULL_PASSED_STATIC_ANALYSIS, STATIC_FIELD, NULL_FAILED_STATIC_ANALYSIS
}

data class SerializationResult(val outcome: SerializationOutcome, val info: String = "")
data class TraceOutcome(val path: String, val outcome: SerializationOutcome)
