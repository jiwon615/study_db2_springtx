package com.study.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 트랜잭션 각각 따로 (두번) 사용
 * - 트랜잭션 1이 완전히 끝나고 나서 트랜잭션 2를 수행함
 */
@Slf4j
@SpringBootTest
public class BasicTxTest {

    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config {
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    @DisplayName("트랜잭션 1개 커밋")
    void commit() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 커밋");
        txManager.commit(status);
        log.info("트랜잭션 커밋 완료");
    }

    @Test
    @DisplayName("트랜잭션 1개 롤백")
    void rollback() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 롤백");
        txManager.rollback(status);
        log.info("트랜잭션 커밋 완료");
    }

    @Test
    @DisplayName("트랜잭션1과2 각각 따로 커밋 & 커밋")
    void double_commit() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1Status = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1Status);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2Status = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 커밋");
        txManager.commit(tx2Status);
    }


    @Test
    @DisplayName("트랜잭션1과2 각각 따로 커밋 & 롤백")
    void double_commit_rollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1Status = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1Status);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2Status = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 롤백");
        txManager.rollback(tx2Status);
    }

    @Test
    @DisplayName("외부 트랜잭션 실행 중에 내부 트랜잭션 추가로 수행")
    void inner_commit() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerStatus = txManager.getTransaction(new DefaultTransactionAttribute());
        // 처음 실행하는 트랜잭션인지 여부
        log.info("outer.isNewTransaction()={}", outerStatus.isNewTransaction());

        innerTxStartAndCommit();

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outerStatus);
    }

    @Test
    @DisplayName("외부 롤백 상황 : 내부 트랜잭션은 커밋되는데, 외부 트랜잭션이 롤백되는 상황")
    void outer_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerStatus = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outerStatus.isNewTransaction());

        innerTxStartAndCommit();

        log.info("외부 트랜잭션 롤백");
        txManager.rollback(outerStatus);
    }

    @Test
    @DisplayName("내부 롤백 상황 : 외부 트랜잭션은 커밋되는데, 내부 트랜잭션이 롤백되는 상황")
    void inner_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerStatus = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outerStatus.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        TransactionStatus innerStatus = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("내부 트랜잭션 롤백");
        txManager.rollback(innerStatus);  // marking as rollback-only

        log.info("외부 트랜잭션 커밋");
        assertThatThrownBy(() -> txManager.commit(outerStatus))
                .isInstanceOf(UnexpectedRollbackException.class);
    }

    @Test
    @DisplayName("requires new 옵션을 추가하여 내부 트랜잭션은 롤백, 외부는 커밋하는 상황")
    void inner_rollback_requires_new() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerStatus = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outerStatus.isNewTransaction());  // true

        log.info("내부 트랜잭션 시작");
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus innerStatus = txManager.getTransaction(definition);
        log.info("innerStatus.isNewTransaction()={}", innerStatus.isNewTransaction()); // true

        log.info("내부 트랜잭션 롤백");
        txManager.rollback(innerStatus);
        log.info("외부 트랜잭션 커밋");
        txManager.commit(outerStatus);
    }


    private void innerTxStartAndCommit() {
        log.info("내부 트랜잭션 시작");
        TransactionStatus innerStatus = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("innerStatus.isNewTransaction()={}", innerStatus.isNewTransaction());

        log.info("내부 트랜잭션 커밋");
        txManager.commit(innerStatus);
    }
}
