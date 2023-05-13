package com.jxx.querydslstart.entity;

import com.jxx.querydslstart.dto.MemberDto;
import com.jxx.querydslstart.dto.QMemberDto;
import com.jxx.querydslstart.dto.UserDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.jxx.querydslstart.entity.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslMiddleTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach
    void beforeEach() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
        //초기화
        em.flush();
        em.clear();
    }

    /**
     * 순수 JPA에서는 DTO를 조회할 때 new 명령어를 사용해야함
     * DTO package 이름을 다 적어줘야헤서 지저분함
     * 생성자 방식만 지원함
     */

    @Test
    void select_to_dto() {
        List<MemberDto> result = em.createQuery("select new com.jxx.querydslstart.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Querydsl은 DTO를 반환할 때 3가지 방법을 지원
     * 1. 프로퍼티 접근
     * 2. 필드 직접 접근
     * 3. 생성자 사용
     */

    /**
     * 프로퍼티 접근법 - setter 가 존재해야 한다.
     */
    @Test
    void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 필드 접근 법 - DTO에 setter 없어도 됨
     */
    @Test
    void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 생성자 접근 법
     */
    @Test
    void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 필드, 프로퍼티 접근법은 각각 필드 명, 세터명이 일치해야 가져올 수 있다.
     */

    @Test
    void findUserDto() {
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), // 여기서 별칭
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findUserDtoV2() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), // 여기서 별칭
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age") // 별칭
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
    }

    /**
     * booleanBuilder 를 이용한 동적 쿼리
     */

    @Test
    void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder(); // builder 에 조건을 하나씩 추가하는 형식
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    void dynamicQuery_whereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private Predicate usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private Predicate ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    /**
     * 조합하는 형태로도 가능 - Predicate 인터페이스 대신 BooleanExpression 을 반환해야 한다.
     */

    @Test
    void dynamicQuery_whereParamV2() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember3(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember3(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEqV2(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEqV2(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEqV2(usernameCond).and(ageEqV2(ageCond));
    }

    /**
     * JPA를 사용한다면 벌크 연산은 조심해야 하는 것이 있다.
     * 벌크연산은 JPA를 무시하고 바로 DB와 connection 한다.
     * 따라 JPA 영속성 컨텍스트와 DB가 동기화 되어 있지 않을 수 있다.
     */

    @Test
    void bulkUpdate() {
        queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();
        // member1 = 10 -> Persist Context member1 | DB 비회원
        // member2 = 20 -> Persist Context member2 | DB 비회원
        // member3 = 30 -> Persist Context member3 | DB member3
        // member4 = 40 -> Persist Context member4 | DB member4

        List<Member> result = queryFactory.select(member)
                .from(member).fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }

        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    @Test
    void bulkAdd() {
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        em.flush();
        em.clear();

        List<Member> results = queryFactory
                .select(member)
                .from(member)
                .fetch();

        for (Member result : results) {
            System.out.println("result = " + result);
        }
    }

    @Test
    void sqlFunction() {
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username, "member", "M")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 대부분의 DB에서 사용할 수 있는 내장 함수는 Querydsl에도 기본적으로 내장되어 있다.
     */
    @Test
    void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)
                .where(member.username.eq(member.username.lower())
                ).fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
