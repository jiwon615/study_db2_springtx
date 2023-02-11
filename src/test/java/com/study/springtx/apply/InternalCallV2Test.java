package com.study.springtx.apply;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 프록시 내부 호출 문제 해결 (InternalCallV1Test 에서의 문제 해결)
 * - 트랜잭션 적용될 internal()을 별도의 클래스로 분리
 */
@Slf4j
@SpringBootTest
public class InternalCallV2Test {

    @Autowired
    CallService callService;

    @Test
    void printProxy() {
        log.info("callService class={}", callService.getClass());
    }

    @Test
    void externalCallV2() {
        callService.external();
    }

    @TestConfiguration
    static class InternalCallV1TestConfig {

        @Bean
        CallService callService() {
            return new CallService(internalService());
        }

        @Bean
        InternalService internalService() {
            return new InternalService();
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    static class CallService {

        private final InternalService internalService;

        public void external() {
            log.info("call external");
            printTxInfo();
            internalService.internal();
        }

        private void printTxInfo() {
            boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isTxActive = {}", isTxActive);

            boolean isTxReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("isTxReadOnly = {}", isTxReadOnly);
        }
    }

    @Slf4j
    static class InternalService {
        @Transactional
        public void internal() {
            log.info("call internal()");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isTxActive = {}", isTxActive);

            boolean isTxReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("isTxReadOnly = {}", isTxReadOnly);
        }
    }
}
