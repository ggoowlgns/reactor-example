/*
package com.jhpark.example;

import lombok.RequiredArgsConstructor;
import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.Map;
import java.util.stream.Collectors;

*/
/**
 * https://techblog.woowahan.com/2667/
 * ref by : https://www.novatec-gmbh.de/en/blog/how-can-the-mdc-context-be-used-in-the-reactive-spring-applications/
 *//*

public class MdcContextLifterConfiguration {

    public static final String MDC_CONTEXT_REACTOR_KEY = MdcContextLifterConfiguration.class.getName();

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void contextOperatorHook() {
        Hooks.onEachOperator(MDC_CONTEXT_REACTOR_KEY,
                Operators.lift((scannable, subscriber) -> new MdcContextLifter(subscriber)));
    }

    @Bean
    public MdcLoggingFilter mdcLoggingFilter() {
        return new MdcLoggingFilter();
    }

    @PreDestroy
    public void cleanupHook() {
        Hooks.resetOnEachOperator(MDC_CONTEXT_REACTOR_KEY);
    }

    */
/**
     * Helper that copies the state of Reactor [Context] to MDC on the #onNext function.
     *//*

    @RequiredArgsConstructor
    public static class MdcContextLifter<T> implements CoreSubscriber<T> {

        private final CoreSubscriber<T> coreSubscriber;

        @Override
        public void onSubscribe(Subscription subscription) {
            coreSubscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(T t) {
            copyToMdc(coreSubscriber.currentContext());
            coreSubscriber.onNext(t);
        }

        @Override
        public void onError(Throwable throwable) {
            coreSubscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            coreSubscriber.onComplete();
        }

        @Override
        public Context currentContext() {
            return coreSubscriber.currentContext();
        }

        */
/**
         * Extension function for the Reactor [Context]. Copies the current context to the MDC, if context is empty clears the MDC.
         * State of the MDC after calling this method should be same as Reactor [Context] state.
         * One thread-local access only.
         *//*

        void copyToMdc(Context context) {
            if (context != null && !context.isEmpty()) {
                Map<String, String> map = context.stream()
                        .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));

                MDC.setContextMap(map);
            } else {
                MDC.clear();
            }
        }
    }
}*/
