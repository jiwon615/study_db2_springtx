package com.study.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class MemberServiceTest {
    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    LogRepository logRepository;


    /**
     * 트랜잭션 각각 따로 사용
     * MemberService    @Transactional:OFF
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON
     */
    @Test
    void outerTxOff_success() {
        //given
        String username = "outerTxOff_success";

        //when
        memberService.joinV1(username);

        //then: 회원, 로그 리포지토리 둘 다 커밋에 성공
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * 트랜잭션 각각 따로 사용
     * MemberService    @Transactional:OFF
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON Exception
     */
    @Test
    void outerTxOff_fail() {
        //given
        String username = "로그예외_outerTxOff_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);
        //then: 완전히 롤백되지 않고, member 데이터가 남아서 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * 트랜잭션 하나만 사용하기 (둘을 묶는 간단한 방법은 둘을 호출하는 회원 서비스에만 트랜잭션을 사용하는 것)
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:OFF
     * LogRepository    @Transactional:OFF
     */
    @Test
    void singleTx() {
        // given
        String username = "singleTx";

        // when
        memberService.joinV1(username);

        // then: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * 서비스에서 물리 트랜잭션 시작하고, 레포지토리에서는 기존 트랜잭션이 잇으므로 참여한다 (논리 트랜잭션들이 됨)
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON
     */
    @Test
    void outerTxOn_success() {
        // given
        String username = "outerTxOn_success";

        // when
        memberService.joinV1(username);
        //then: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }


    /**
     * 로그 레포지토리에서 예외를 잡아서 정상 흐름으로 변환하는 로직
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON Exception
     */
    @Test
    void outerTxOn_fail() {
        // given
        String username = "로그예외_outerTxOn_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        // then: 모든 데이터가 롤백된다.
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON Exception
     */
    @Test
    void recoverException_fail() {
        // given
        String username = "로그예외_recoverException_fail";

        // when
        assertThatThrownBy(() -> memberService.joinV2(username))
                .isInstanceOf(UnexpectedRollbackException.class);

        // then: 모든 데이터가 롤백된다.
         assertTrue(memberRepository.find(username).isEmpty());
         assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * 회원가입을 시도한 로그남기는 것에 실패하더라도 회원가입은 유지되게 하는 겅우
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional(REQUIRES_NEW) Exception
     */
    @Test
    void recoverException_success() {
        // given
        String username = "로그예외_recoverException_success"; //when

        memberService.joinV2(username);

        //then: member 저장, log 롤백
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }
}