package com.gymmind.member.application;

import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.api.PageResponse;
import org.springframework.data.domain.Pageable;

public interface MemberService {
    MemberView create(CurrentActor actor, CreateMemberCommand command);
    MemberView find(CurrentActor actor, Long memberId);
    MemberView findSelf(CurrentActor actor);
    void update(CurrentActor actor, Long memberId, String fullName, String phone);
    void suspend(CurrentActor actor, Long memberId);
    PageResponse<MemberView> list(CurrentActor actor, String query, Pageable pageable);
}
