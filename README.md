# Serialisation Tracer

[ ![Download](https://api.bintray.com/packages/dsowerby/maven/serialization-tracer/images/download.svg) ](https://bintray.com/dsowerby/maven/serialization-tracer/_latestVersion)

**This project has moved to [GitLab](https://gitlab.com/dsowerby/serialization-tracer/blob/develop/README.md)**


When you try to serialise a fairly complex object graph, it is common to get unexpected `NotSerializableException`s - and these can be really hard to track down.

This utility provides an output which identifies the exact source(s) of a Serialization failure, for example:

> DefaultSubPagePanel.option.optionCache.cache.localCache.segments -> SerializationResult(outcome=FAIL, info=java.io.NotSerializableException: com.google.common.cache.LocalCache$AccessQueue)
  DefaultSubPagePanel.option.optionCache.cache.localCache.removalNotificationQueue -> SerializationResult(outcome=FAIL, info=com.google.common.cache.LocalCache$2 cannot be cast to java.io.Serializable)

