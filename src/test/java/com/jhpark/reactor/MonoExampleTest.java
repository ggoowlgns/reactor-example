package com.jhpark.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
class MonoExampleTest {
  MonoExample monoExample;

  @BeforeEach
  void setUp() {
    monoExample = new MonoExample();
  }

  /**
   * request(unbounded) -> 제한이 없이 다 달라
   */
  @Test
  void SubscribingMonoTest() {
    Mono mono = monoExample.makePublisher();
    mono.subscribe();

    StepVerifier.create(mono)
        .expectNext("jhpark")
        .verifyComplete();
  }

  @Test
  void SubscribingMonoErrorTest() {
    Mono mono = monoExample.makePublisher()
        .map(o -> {throw new RuntimeException();});
    mono.subscribe(
        s-> log.info("subscriber 1-a : {}", s),
        t-> log.error("subscriber 1-a ", t)
    );
    mono.subscribe(s-> log.info("subscriber 1-b : {}", s),
        t-> log.error("subscriber 1-b ", t)
        );

    StepVerifier.create(mono)
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void SubscribingMonoCompleteTest() {
    Mono mono = monoExample.makePublisher()
        .map(String::toUpperCase);

    mono.subscribe(
        s-> log.info("[subscriber 1-a] : {}", s),
        t-> log.error("[subscriber 1-a] ", t),
        () -> log.info("[subscriber 1-a] : FINISHED")
    );
    mono.subscribe(s-> log.info("subscriber 1-b : {}", s),
        t-> log.error("subscriber 1-b ", t)
    );

    StepVerifier.create(mono)
        .expectNext("jhpark".toUpperCase())
        .verifyComplete();
  }

  @Test
  void SubscribingMonoSubscriptionTest() {
    Mono<String> mono = monoExample.makePublisher()
        .map(String::toUpperCase);

    mono.subscribe(
        s-> log.info("[subscriber 1-a] : {}", s),
        t-> log.error("[subscriber 1-a] ", t),
        () -> log.info("[subscriber 1-a] : FINISHED"),
        subscription -> subscription.request(5)
    );
    mono.subscribe(s-> log.info("subscriber 1-b : {}", s),
        Throwable::printStackTrace
    );

    StepVerifier.create(mono)
        .expectNext("jhpark".toUpperCase())
        .verifyComplete();
  }

  @Test
  void monoDoOn_Test() {
    Mono<String> mono = monoExample.makePublisher()
        .map(String::toUpperCase)
        .doOnSubscribe(subscription -> log.info("doOnSubscribe executed"))
        .doOnRequest(longNumber -> log.info("Request Received, starting ~"))
        .doOnNext(s -> log.info("value is in here (1) value : {}", s))
        .doOnNext(s -> log.info("value is in here (2) value : {}", s))
        .doOnSuccess(s -> log.info("doOnSuccess executed"));
    mono.subscribe(
        s-> log.info("[subscriber 1-a] : {}", s),
        t-> log.error("[subscriber 1-a] ", t),
        () -> log.info("[subscriber 1-a] : FINISHED"),
        subscription -> subscription.request(5)
    );
      mono.subscribe(
              s-> log.info("[subscriber 2-a] : {}", s),
              t-> log.error("[subscriber 2-a] ", t),
              () -> log.info("[subscriber 2-a] : FINISHED"),
              subscription -> subscription.request(5)
      );

    log.info("------------------------------------------------------");
/*
    Mono<Object> mono2 = monoExample.makePublisher()
        .map(String::toUpperCase)
        .doOnSubscribe(subscription -> log.info("doOnSubscribe executed 2"))
        .doOnRequest(longNumber -> log.info("Request Received, starting 2 ~"))
        .doOnNext(s -> log.info("2 value is in here (1) value : {}", s))
        .flatMap(s -> Mono.empty())
        .doOnNext(s -> log.info("doOnNext2 value is in here (2) value : {}", s))
        .doOnSuccess(s -> log.info("doOnSuccess2 executed"));
    mono2.subscribe(
        s-> log.info("[subscriber 2] : {}", s),
        t-> log.error("[subscriber 2] ", t),
        () -> log.info("[subscriber 2] : FINISHED"),
        subscription -> subscription.request(5)
    );*/
  }

  @Test
  void monoErrorResumeAndReturnTest() {
    Mono<Object> monoError = monoExample.makePublisher()
        .map(String::toUpperCase)
        .map(s -> {throw new IllegalArgumentException();})
        .onErrorResume(throwable -> {
          log.warn("onErrorResume :", throwable);
          return Mono.just("jhpark");
        })
        .onErrorReturn("onErrorReturn"); // onErrorReturn 이 onErrorResume 위로 올라가면 element 를 "onErrorReturn"로 방출하여 test 실

    monoError.subscribe(
        s-> log.info("[subscriber 1-a] : {}", s),
        t-> log.error("[subscriber 1-a] ", t),
        () -> log.info("[subscriber 1-a] : FINISHED"),
        subscription -> subscription.request(5)
    );
    monoError.subscribe(s-> log.info("subscriber 1-b : {}", s),
        Throwable::printStackTrace
    );

    StepVerifier.create(monoError)
        .expectNext("jhpark")
        .verifyComplete();
  }
}