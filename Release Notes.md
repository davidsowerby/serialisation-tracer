# Release notes for 0.7.0.0

- The stacktrace for a non-Serialization exception is logged
- ``SerializationTracer.shouldNotHaveAnyDynamicFailures()`` added (just ignores failures from static analysis)
-  ``SerializationTracer.trace()`` returns this for fluency.  In Kotlin this enables:

```kotlin
SerializationTracer().trace(myObject).shouldNotHaveAnyDynamicFailures()
```

```java
new SerializationTracer().trace(myObject).shouldNotHaveAnyDynamicFailures();
```