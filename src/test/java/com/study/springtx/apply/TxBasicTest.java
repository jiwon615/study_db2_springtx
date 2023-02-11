package com.study.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootTest
public class TxBasicTest {

    @Slf4j
    static class BasicService {
        @Transactional
        public void tx() {

        }

        public void nonTx() {

        }
    }
}
