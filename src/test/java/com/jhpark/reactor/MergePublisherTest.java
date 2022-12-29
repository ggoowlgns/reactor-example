package com.jhpark.reactor;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@Slf4j
public class MergePublisherTest {


    @Test
    void mergeMonosWithZip() {
        String[] arr1 = {"1", "2", "3", "4", "5"};
        String[] arr2 = {"a", "b", "c", "d", "e"};
        List<String> list1 = Arrays.asList(arr1);
        List<String> list2 = Arrays.asList(arr2);

        Mono<List<String>> mono1 = Mono.just(list1);
        Mono<List<String>> mono2 = Mono.just(list2).delayElement(Duration.ofMillis(30000));

        Mono<MergedList> mergedMono = Mono.zip(mono1, mono2,
                (strings, strings2) ->
                        MergedList.builder().list1(strings).list2(strings2).build())
                .log();

        mergedMono.subscribe(
                s-> log.info("[subscriber 1] : {}", s),
                t-> log.error("[subscriber 1] ", t),
                () -> log.info("[subscriber 1] : FINISHED"),
                subscription -> subscription.request(5)
        );
//        monoError.subscribe(s-> log.info("subscriber 1-b : {}", s),
//                Throwable::printStackTrace
//        );

        StepVerifier.create(mergedMono)
                .expectNext(MergedList.builder().build())
                .verifyComplete();
    }


    @Data
    @Builder
    static class MergedList {
        List<String> list1;
        List<String> list2;
    }
}
