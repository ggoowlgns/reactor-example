package com.jhpark.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class FluxExampleTest {
  FluxExample fluxExample;

  @BeforeEach
  void setUp() {
    fluxExample = new FluxExample();
  }

  @Test
  void fluxTest() {
    Flux flux = fluxExample.makePublisher();
    StepVerifier.create(flux)
        .expectNext("jhpark0", "jhpark1","jhpark2","jhpark3","jhpark4","jhpark5","jhpark6","jhpark7","jhpark8","jhpark9")
        .verifyComplete();
  }
  @Test
  public void fluxSubscriberNumbersError() {
    Flux<Integer> flux = Flux.range(1,5)
        .log()
        .map(i -> {
          if(i == 4) {
            throw new IndexOutOfBoundsException("index error");
          }
          return i;
        });

    flux.subscribe(i -> log.info("Number {}", i), Throwable::printStackTrace,
        () -> log.info("DONE!"), subscription -> subscription.request(3));

    log.info("-----------------------------------");

    StepVerifier.create(flux)
        .expectNext(1, 2, 3)
        .expectError(IndexOutOfBoundsException.class)
        .verify();
  }

  @Test
  void fluxBackPressureTest() {
    Flux flux = fluxExample.makePublisher()
        .doOnNext(s -> log.info("doOnNext : {}", s))
        .doOnComplete(() -> log.info("doOnComplete"));

    flux.subscribe(new Subscriber<String>() {
      private int count = 0;
      private int requestCount = 2;
      private Subscription subscription;
      @Override
      public void onSubscribe(Subscription subscription) {
        subscription.request(requestCount);
        this.subscription = subscription;
      }

      @Override
      public void onNext(String o) {
        log.info("onNext : {}", o);
        count++;
        if (count >= requestCount) {
          count = 0;
          subscription.request(requestCount);
        }
      }

      @Override
      public void onError(Throwable throwable) {

      }

      @Override
      public void onComplete() {

      }
    });

    log.info("------------------------------------------------");
    StepVerifier.create(flux)
        .expectNext("jhpark0", "jhpark1","jhpark2","jhpark3","jhpark4","jhpark5","jhpark6","jhpark7","jhpark8","jhpark9")
        .verifyComplete();
  }

    /**
     * bug : log 는 항상 limitRate 아래 찍자 그렇지 않으면 반영이 안된다.
     * @throws InterruptedException
     */
  @Test
  void fluxBackPressureWithLimitRate() throws InterruptedException {
      AtomicInteger count = new AtomicInteger(0);
    Flux<Integer> flux = Flux.range(1, 300)
        .doOnRequest(value -> {
            log.info("value : {}, count : {}", value, count.get());
            count.getAndIncrement();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnComplete(() -> log.info("doOnComplete"))
        .limitRate(3);

    flux.subscribe(
            (s) -> log.info("nextConsumer value : {}", s),
            throwable -> log.error("errorConsumer", throwable),
            () -> log.info("completeRunnable count : {}", count.get())
    );

    Thread.sleep(500);


//    StepVerifier.create(flux)
//        .expectNext(1,2,3,4,5,6,7,8,9,10)
//        .verifyComplete();
  }

  @Test
  void fluxBackPressureWithBaseSubscriberTest() {
    // Subscriber -> BaseSubscriber 사용
  }

  @Test
  void fluxSubscriberInterval() throws InterruptedException {
    Flux<Long> interval = createInterval();

//    Thread.sleep(3000); // Sleep 이 없으면 flux 가 모두 방출하기 전에 main thread 가 죽어서 수신을 하지 못한다.

    //Test with virtual time
    StepVerifier.withVirtualTime(this::createInterval)
        .expectSubscription()
        .expectNoEvent(Duration.ofDays(1))
        .thenAwait(Duration.ofDays(1))
        .expectNext(0L)
        .thenAwait(Duration.ofDays(1))
        .expectNext(1L)
        .thenCancel()
        .verify();
  }

  private Flux<Long> createInterval() {
    return Flux.interval(Duration.ofDays(1))
        .take(10)
        .log();
  }

  @Test
  void connectableFluxTest_HotPublisher() throws InterruptedException {
    ConnectableFlux<Integer> connectableFlux = Flux.range(1,10)
//                                                    .log()
                                                    .delayElements(Duration.ofMillis(100))
                                                    .publish();
    connectableFlux.connect();

    log.info("Sleep for 300ms");
    Thread.sleep(300); //subscriber 1는 앞에 데이터 1,2는 놓치게 된다.. : subscribe 를 하는 순간부터 consume 하는게 아니다. : publish 순간부터 데이터 소모, emit 한다.
    connectableFlux.subscribe(i -> log.info("Sub1 number : {}", i));

    log.info("Sleep for 200ms");
    Thread.sleep(200); //subscriber 2는 앞에 데이터 1,2,3,4 를 놓치게 된다.
    connectableFlux.subscribe(i -> log.info("Sub2 number : {}", i));

  }

  @Test
  void connectableFluxWithAutoConnectTest() throws InterruptedException {
    Flux<Integer> connectableFluxWithAutoConnect = Flux.range(1,5)
        .delayElements(Duration.ofMillis(100))
        .publish()
        .autoConnect(2)
        .log();

    StepVerifier.create(connectableFluxWithAutoConnect)
        .then(connectableFluxWithAutoConnect::subscribe)
        .expectNext(1,2,3,4,5)
        .verifyComplete();
  }
}