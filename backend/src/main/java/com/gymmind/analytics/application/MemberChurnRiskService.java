package com.gymmind.analytics.application;
import com.gymmind.shared.security.CurrentActor;
import java.util.List;
public interface MemberChurnRiskService { List<MemberChurnRiskView> list(CurrentActor actor); }
