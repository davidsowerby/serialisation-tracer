# Serialisation Tracer

[ ![Download](https://api.bintray.com/packages/dsowerby/maven/serialisation-tracer/images/download.svg) ](https://bintray.com/dsowerby/maven/serialisation-tracer/_latestVersion)

When you try to serialise a fairly complex object graph, it is common to get unexpected `NotSerializableException`s - and these can be really hard to track down.

This utility provides an output which identifies the exact source(s) of a Serialization failure, for example:

> DefaultSubPagePanel.option.optionCache.cache.localCache.segments -> SerializationResult(outcome=FAIL, info=java.io.NotSerializableException: com.google.common.cache.LocalCache$AccessQueue)
  DefaultSubPagePanel.option.optionCache.cache.localCache.removalNotificationQueue -> SerializationResult(outcome=FAIL, info=com.google.common.cache.LocalCache$2 cannot be cast to java.io.Serializable)

In this case example, it looks like the solution involves the cache itself, but whatever the solution, you have some clear information to work with. 


Strangely, there do not seem to be any tools around to help - so either I just failed to find one, and this library has re-invented a wheel - or this could be a really useful utility!

There are code examples around which use a static approach (testing to see whether fields implement `Serializable`), but this can be misleading if you use interfaces - the interface could extend `Serializable` but the implementation may be using non-serialisable fields. 

This utility uses dynamic analysis (by testing field values) when they are present, and falls back to using static analysis where fields are null or are empty collections.

There are a number of methods available to query the output, including methods which generate an `AssertionError` to use with test frameworks.

## Download

### Gradle

```
compile 'uk.q3c.util:serialisation-tracer:x.x.x.x'
```

### Maven
```
<dependency>
  <groupId>uk.q3c.util</groupId>
  <artifactId>serialisation-tracer</artifactId>
  <version>x.x.x.x</version>
  <type>pom</type>
</dependency>

```


## What it does

`SerializationTracer` walks through the graph, testing the value of each field in each object.  If a field is null, or an empty collection, then static analysis is carried out instead.  Static analysis simply checks whether the field (including generic parameters) implement Serializable.

There are a number of possible outcomes

| Outcome | Notes |
|---------|-------|
|TRANSIENT| Field is marked as transient and therefore ignored|
|STATIC| Field is marked as static and therefore ignored|
|PASS| The field content was serialised successfully|
|FAIL| Serialisation failed - this usually means that the target or one of its fields does not implement `Serializable`|
|NULL_PASSED_STATIC_ANALYSIS| Field is null, but static analysis passed.  See static analysis below|
|NULL_FAILED_STATIC_ANALYSIS| Field is null, and static analysis failed. See static analysis below|
|EMPTY_PASSED_STATIC_ANALYSIS| Field is an empty Collection, and static analysis passed. See static analysis below|
|EMPTY_FAILED_STATIC_ANALYSIS | Field is an empty Collection, and static analysis failed. See static analysis below|

## Static Analysis

When a field is null, or is an empty collection, the field type, and the types of any generic parameters it uses, are all checked to see if they implement.  `Serializable`.  If any one of them does not, a result of **NULL_FAILED_STATIC_ANALYSIS** or **EMPTY_FAILED_STATIC_ANALYSIS** 


## Invoking the Trace

Construct the object to be tested using your normal methods (for example, if your code uses dependency injection, make sure you construct the test instance the same way).  If a field can refer to different types (typically multiple sub-classes), it would be wise to test with all of those types.

To invoke:

```java
SerializationTracer tracer = new SerializationTracer();
tracer.trace(myObject);
```

## Output

The example below shows an sample of full output, but you can also use the `results()` method to filter for just the results you want to see - static fields, for example, are not usually of interest.  

Note that the first part is an object 'path' to describe which element has failed - this would be really useful in Java Serialization !

>StandardTestObject.emptyArrayListOfSerializableObject.MAX_ARRAY_SIZE -> SerializationResult(outcome=STATIC_FIELD, info=)
 StandardTestObject.emptyArrayListOfSerializableObject.modCount -> SerializationResult(outcome=TRANSIENT, info=)
 StandardTestObject.nullArrayListOfSerializableObject -> SerializationResult(outcome=NULL_PASSED_STATIC_ANALYSIS, info=ArrayList is Serializable. SerializableObject is Serializable.)
 StandardTestObject.arrayListOfNonSerializableObject -> SerializationResult(outcome=FAIL, info=java.io.NotSerializableException: uk.q3c.util.serial.tracer.NonSerializableObject)
 StandardTestObject.arrayListOfNonSerializableObject.serialVersionUID -> SerializationResult(outcome=STATIC_FIELD, info=)
 StandardTestObject.arrayListOfNonSerializableObject.DEFAULT_CAPACITY -> SerializationResult(outcome=STATIC_FIELD, info=)
 StandardTestObject.arrayListOfNonSerializableObject.EMPTY_ELEMENTDATA -> SerializationResult(outcome=STATIC_FIELD, info=)
 StandardTestObject.arrayListOfNonSerializableObject.DEFAULTCAPACITY_EMPTY_ELEMENTDATA -> SerializationResult(outcome=STATIC_FIELD, info=)
 StandardTestObject.arrayListOfNonSerializableObject.elementData -> SerializationResult(outcome=TRANSIENT, info=)
 StandardTestObject.arrayListOfNonSerializableObject.MAX_ARRAY_SIZE -> SerializationResult(outcome=STATIC_FIELD, info=)
 StandardTestObject.arrayListOfNonSerializableObject.modCount -> SerializationResult(outcome=TRANSIENT, info=)
  
## Support for Testing

It is expected that this utility would be used primarily in testing, and therefore supports the following:

### JUnit et al

For JUnit and other frameworks which expect an `AssertionError` to be thrown if a trace does not meet the required conditions use one of the methods:

```java
// when
SerializationTracer tracer = new SerializationTracer();
tracer.trace(myObject);

// then
tracer.shouldNotHaveAnyFailures()
```

will throw an `AssertionError` if any object in the graph produces a result of **FAIL, NULL_FAILED_STATIC_ANALYSIS, EMPTY_FAILED_STATIC_ANALYSIS**

```java
// when
SerializationTracer tracer = new SerializationTracer();
tracer.trace(myObject);

// then
tracer.shouldNotHaveAny( ... )
``` 

Allows you to specify exactly which outcomes should cause an `AssertionError` to be thrown

### Spock

Spock expects a 'false' to be returned for a test failure.  Use the `hasNo` methods instead


## Tips

- Try to run your tests with all fields populated - null fields can only be checked with static analysis
- If you have a generalised field, try to test with an instance of every type it may hold 
- Collection interfaces - like List for example - do not implement `Serializable`, but most implementations do
- An empty ArrayList\<Anything> is equal to an empty ArrayList\<AnythingElse> because of type erasure 
 
 
# Status

Test coverage is not as comprehensive as it should be, but this utility has been used as intended.

# Contributions

Would be very welcome 