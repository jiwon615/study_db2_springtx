package com.study.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;

/*
*  스프링에서 초기화 코드 (@PostConstruct)와 @Transactional 함께 사용 시 트랜잭션 적용되지 않음
* - 왜냐하면 초기화 코드가 먼저 호출되고, 그다음에 트랜잭션 AOP가 적용되기 떄문
* - 이에 대한 대안은 ApplicationReadyEvent 사용하는 것
*/
@SpringBootTest
public class InitTxTest {

    @Autowired
    HelloService helloService;

    @Test
    void go() {
        // 초기화 코드는 스프링이 초기화 시점에 호출한다.
    }

    @TestConfiguration
    static class InitTxTestConfig {
        @Bean
        HelloService hello() {
            return new HelloService();
        }
    }

    @Slf4j
    static class HelloService {

        @PostConstruct
        @Transactional
        public void initV1() {
            boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isTxActive for @PostConstruct ={}", isTxActive);
        }

        @EventListener(ApplicationReadyEvent.class)
        @Transactional
        public void initV2() {
            boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isTxActive for ApplicationReadyEvent ={}", isTxActive);
        }
    }
}
