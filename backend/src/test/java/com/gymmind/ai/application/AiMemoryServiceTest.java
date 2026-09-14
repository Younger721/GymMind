package com.gymmind.ai.application;
import com.gymmind.shared.security.CurrentActor; import com.gymmind.iam.domain.model.RoleCode; import org.junit.jupiter.api.Test; import java.util.*; import static org.assertj.core.api.Assertions.*;
class AiMemoryServiceTest {
 @Test void memoryIsIsolatedByTenantAndUser(){var s=new DefaultAiMemoryService(); var a=actor(7L,1L); s.write(a,"goal","lose weight"); assertThat(s.list(a)).hasSize(1); assertThat(s.list(actor(8L,1L))).isEmpty(); assertThat(s.list(actor(7L,2L))).isEmpty(); s.delete(a,"goal"); assertThat(s.list(a)).isEmpty();}
 private CurrentActor actor(long t,long u){return new CurrentActor(u,t,Set.of(RoleCode.MEMBER),Set.of("ai:memory"),0,"t" );}
}
