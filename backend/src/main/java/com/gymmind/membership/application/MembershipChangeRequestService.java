package com.gymmind.membership.application;

import com.gymmind.membership.domain.model.MembershipChangeRequestStatus;
import com.gymmind.shared.security.CurrentActor;

import java.util.List;

public interface MembershipChangeRequestService {

    MembershipChangeRequestView create(CurrentActor actor, CreateMembershipChangeCommand command);

    List<MembershipChangeRequestView> list(CurrentActor actor, MembershipChangeRequestStatus status);

    MembershipChangeRequestView find(CurrentActor actor, Long id);

    MembershipChangeRequestView cancel(CurrentActor actor, Long id);

    MembershipChangeRequestView approve(CurrentActor actor, Long id, String comment);

    MembershipChangeRequestView reject(CurrentActor actor, Long id, String comment);
}
