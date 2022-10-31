package com.jhpark.reactor;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FluxExample implements PublisherExample{
  @Override
  public Flux<String> makePublisher() {
    return Flux.just("jhpark0", "jhpark1","jhpark2","jhpark3","jhpark4","jhpark5","jhpark6","jhpark7","jhpark8","jhpark9")
        .log();
  }
}
