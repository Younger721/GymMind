package com.gymmind.member.application;

import com.gymmind.shared.security.CurrentActor;

public interface MemberService {
    MemberView create(CurrentActor actor, CreateMemberCommand command);
    MemberView find(CurrentActor actor, Long memberId);
    MemberView findSelf(CurrentActor actor);
    void update(CurrentActor actor, Long memberId, String fullName, String phone);
    void suspend(CurrentActor actor, Long memberId);
}
