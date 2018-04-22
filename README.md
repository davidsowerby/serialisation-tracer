# Serialisation Tracer

When you try to serialise a fairly complex object graph, it is not unusual to get `SerializationException`s which can be really hard to track down.

There do not seem to be any tools around to help - so either this library has re-invented a wheel - or it could be useful!

There are certainly code examples around which uses a static approach (testing to see whether fields implement `Serializable`), but this can be misleading if you use interfaces - the interface could extend `Serializable` but the implementation may be using non-serialisable fields. 

This library tests instances rather than classes.



## What it does

`SerializationTracer` walks through the graph, testing each field in each object.  The outcome of that is one of the following

| Outcome | Notes |
|---------|-------|
|TRANSIENT| Field is marked as transient and therefore ignored|
|STATIC| Field is marked as static and therefore ignored|
|NULL| Field content is null
|PASS| The field content was serialised successfully
|FAIL| Serialisation failed


## Invoking the Trace

Construct the object to be tested using your normal methods (for example, if your code uses dependency injection, make sure you constrcut the test instance the same way)

 

