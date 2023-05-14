package com.jxx.querydslstart.repository;

import com.jxx.querydslstart.dto.MemberSearchCondition;
import com.jxx.querydslstart.dto.MemberTeamDto;

import java.util.List;

public interface MemberDynamicQuery {
    List<MemberTeamDto> search(MemberSearchCondition condition);
}
