package com.jhpark.reactor;

import org.reactivestreams.Publisher;

public interface PublisherExample<T> {
  Publisher<T> makePublisher();
}
