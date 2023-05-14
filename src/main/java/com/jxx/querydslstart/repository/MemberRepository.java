package com.jxx.querydslstart.repository;

import com.jxx.querydslstart.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberDynamicQuery {
    List<Member> findByUsername(String username);
}
