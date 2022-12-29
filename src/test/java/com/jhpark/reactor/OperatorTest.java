package com.jhpark.reactor;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class OperatorTest {
    /**
   * subscribe with in other thread
   * https://projectreactor.io/docs/core/release/reference/index.html#schedulers
   *  - Schedulers.single()
   *
   *  정리 : 순서가 생각보다 중요하다.
   *   - subscribeOn : 복수개가 있어도 최상위 한개만 반영, 중간에 한개만 있어도 전체 chain 에 반영
   *   - publishOn : 정의한 아래부터 반영
   *   - 두개가 섞여있는 경우
   *      - publishOn > subscribeOn : publishOn 이 힘이 더 세다
   */

  @Test
  void subscribeOnSimple() {
    Flux flux = Flux.range(1,400)
        .map(i -> {
          log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
          return i;
        })
        .subscribeOn(Schedulers.boundedElastic()) // 중간에 들어가 있어도 위아래 모두 다 영향을 미친다. : affects entire chain
        .map(i -> {
          log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
          return i;
        });
    flux.subscribe();
    flux.subscribe();
    flux.subscribe();
  }
  @Test
  void subscribeMultiple() {
    Flux flux = Flux.range(1,400)
        .subscribeOn(Schedulers.single()) //위에 것만 반영
        .map(i -> {
          log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
          return i;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .map(i -> {
          log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
          return i;
        });
    flux.subscribe();
    flux.subscribe();
    flux.subscribe();
  }

  @Test
  void publishOnSimple() throws InterruptedException {
    Flux flux = Flux.range(1,400)
            .doOnRequest(value -> log.info("doOnRequest value : {}", value))
        .map(i -> {
          log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
          return i;
        })
        .publishOn(Schedulers.single()) //는 아래부터 반영
        .map(i -> {
          log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
          return i;
        });
    flux.subscribe(o -> log.info("nextConsumer : {}", o),
            throwable -> log.error("errorConsumer : {}", throwable),
            () -> log.info("completeConsumer"));

    Thread.sleep(5000);
  }


  /**
   * Blocking in background
   */
  @Test
  void subscribeOnIOInBackgroundTest() throws InterruptedException {
    Mono<List<String>> listMono = Mono.fromCallable(() -> Files.readAllLines(Path.of("example-text")))
        .log()
        .subscribeOn(Schedulers.boundedElastic())
        ;
    listMono.subscribe(strings -> log.info("jhpark line : ", strings));
//    Thread.sleep(100);
  }

  /**
   * switchIfEmpty : 없으면 바꿔줌
   * &
   * Defer : delay execution inside operator
   */
  @Test
  void switchIfEmptyTest() {
    Flux emptyFlux = Flux.empty()
        .switchIfEmpty(Flux.just("not empty anymore"))
        .log();
    StepVerifier.create(emptyFlux)
        .expectSubscription()
        .expectNext("not empty anymore")
        .verifyComplete();
  }

  @Test
  void deferOperator() throws InterruptedException {
    // For 비교 : just() operator 는 instantiation time 시점에 data 를 방출한다. -> 아래 결과는 모두 같다.
    Mono just = Mono.just(System.currentTimeMillis());
    just.subscribe(l -> log.info("just time : {}", l));
    Thread.sleep(100);
    just.subscribe(l -> log.info("just time : {}", l));
    Thread.sleep(100);
    just.subscribe(l -> log.info("just time : {}", l));
    Thread.sleep(100);
    just.subscribe(l -> log.info("just time : {}", l));
    Thread.sleep(100);
    just.subscribe(l -> log.info("just time : {}", l));

    Mono defer = Mono.defer(() -> Mono.just(System.currentTimeMillis()));
    defer.subscribe(l -> log.info("defer time : {}", l));
    Thread.sleep(100);
    defer.subscribe(l -> log.info("defer time : {}", l));
    Thread.sleep(100);
    defer.subscribe(l -> log.info("defer time : {}", l));
    Thread.sleep(100);
    defer.subscribe(l -> log.info("defer time : {}", l));
    Thread.sleep(100);
    defer.subscribe(l -> log.info("defer time : {}", l));
    Mono<Consumer> dataIsFunctionalInterfaceMono = Mono.just(new Consumer<Long>() {
      @Override
      public void accept(Long o) {
          log.info("consumer time : {}", System.currentTimeMillis());
      }
    });
    dataIsFunctionalInterfaceMono.subscribe(f -> f.accept(null));
    Thread.sleep(100);
    dataIsFunctionalInterfaceMono.subscribe(f -> f.accept(null));
    Thread.sleep(100);
    dataIsFunctionalInterfaceMono.subscribe(f -> f.accept(null));
    Thread.sleep(100);
    dataIsFunctionalInterfaceMono.subscribe(f -> f.accept(null));
    Thread.sleep(100);
    dataIsFunctionalInterfaceMono.subscribe(f -> f.accept(null));
  }

  /**
   * concat, concatWith, combineLatest : 19 -> Lazy Operator
   * merge -> Eager Operator : 방출한 element 는 기다리지 않고 바로 소모된다.
   *
   * Error : concatDelayError, mergeDelayError
   */
  @Test
  void concatAndCombineMergeFluxTest() {
    Flux<String> flux1 = Flux.just("a", "b");
    Flux<String> flux2 = Flux.just("c", "d");
    Flux concatFlux = flux1.concatWith(flux2).log();
    StepVerifier.create(concatFlux)
        .expectSubscription()
        .expectNext("a","b","c","d")
        .verifyComplete();

    flux1 = Flux.just("a", "b");
    flux2 = Flux.just("c", "d");
    Flux<String> combineLatest = Flux.combineLatest(flux1, flux2,
        (s1, s2) -> s1.toUpperCase() + s2.toUpperCase()).log();

    StepVerifier.create(combineLatest)
        .expectSubscription()
        .expectNext("BC","BD") //틀릴수도 있다. : 거의 동시에 발생하므로
        .verifyComplete();

    flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(100));
    flux2 = Flux.just("c", "d");
    Flux<String> mergeFlux = Flux.merge(flux1, flux2).log();

    StepVerifier.create(mergeFlux)
        .expectSubscription()
        .expectNext("c","d","a","b")
        .verifyComplete();
  }


  /**
   * flatMap, flatMapSequential
   *
   * flatMap 의 로직은 merge 와 매우 유사하다 : eager logic
   */
  @Test
  void flatMapTest() throws InterruptedException {
    Flux<String> flux = Flux.just("a", "b");

    Flux<String> flatFlux = flux.map(String::toUpperCase)
        .flatMap(this::findByName)
        .log();

    StepVerifier
        .create(flatFlux)
            .expectSubscription()
                .expectNext("nameB1", "nameB2", "nameA1", "nameA2")
                    .verifyComplete();

    Thread.sleep(500);

    Flux<String> flatSequenceFlux = flux.map(String::toUpperCase)
        .flatMapSequential(this::findByName)
        .log();
    StepVerifier
        .create(flatSequenceFlux)
        .expectSubscription()
        .expectNext("nameA1", "nameA2", "nameB1", "nameB2")
        .verifyComplete();
  }

  private Flux<String> findByName(String name) {
    return name.equals("A") ? Flux.just("nameA1", "nameA2").delayElements(Duration.ofMillis(100))
        : Flux.just("nameB1", "nameB2");
  }


  /**
   * Flux.zip, flux.zipWith
   */
  @Test
  void zipTest() {
    Flux<String> titleFlux = Flux.just("Grand Blue", "Baki");
    Flux<String> studioFlux = Flux.just("Zero-G", "TMS Entertainment");
    Flux<Integer> episodesFlux = Flux.just(12, 24);

    Flux<Anime> animeFlux = Flux.zip(titleFlux, studioFlux, episodesFlux)
        .flatMap(tuple -> Flux.just(new Anime(tuple.getT1(), tuple.getT2(), tuple.getT3())));


    StepVerifier
        .create(animeFlux)
        .expectSubscription()
        .expectNext(
            new Anime("Grand Blue", "Zero-G", 12),
            new Anime("Baki","TMS Entertainment", 24))
        .verifyComplete();
  }

  @AllArgsConstructor
  @ToString
  @EqualsAndHashCode
  class Anime {
    private String titile;
    private String studio;
    private int episode;
  }


  /**
   * Avoid Blocking calls with BlockHound : https://github.com/reactor/BlockHound
   * reactive programming 을 하면 block 이 되고 있는지 파악하기가 쉽지 않다. -> BlockHound 의 도움을 받자 : dependency 필요
   */
}
