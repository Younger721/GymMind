package com.gymmind.ai.chat.application;
import com.gymmind.iam.domain.model.RoleCode; import com.gymmind.shared.security.CurrentActor; import org.springframework.stereotype.Component; import java.util.*;
@Component public class AiRoleProfileResolver {
 public AiRoleProfile resolve(CurrentActor actor){
  RoleCode role=actor.isPlatformAdmin()?RoleCode.PLATFORM_ADMIN: actor.roles().contains(RoleCode.GYM_ADMIN)?RoleCode.GYM_ADMIN:actor.roles().contains(RoleCode.COACH)?RoleCode.COACH:RoleCode.MEMBER;
  return switch(role){
   case PLATFORM_ADMIN->new AiRoleProfile("PLATFORM_ADMIN","平台 AI 管理助手","你是平台 AI 管理助手，可以结合平台知识库回答平台租户、配额、调用统计和平台配置问题。不得泄露未经授权的跨租户明细，所有数据必须经过平台权限校验。",List.of("查看 AI 使用情况","检查租户 AI 状态"),List.of("basic_chat","knowledge_search"),10);
   case GYM_ADMIN->new AiRoleProfile("GYM_ADMIN","门店运营助手","你是健身房门店运营助手，只回答当前门店的会员、课程、预约、运营和 AI 配置问题。不得访问其他门店。",List.of("查看本店 AI 使用情况","分析近期预约情况"),List.of("basic_chat","knowledge_search"),10);
   case COACH->new AiRoleProfile("COACH","教练工作助手","你是教练工作助手，可协助当前门店授权范围内的会员训练指导、动作建议和训练计划草案。不得访问未授权会员。",List.of("给会员制定训练建议","查看训练动作要点"),List.of("basic_chat","knowledge_search"),10);
   case MEMBER->new AiRoleProfile("MEMBER","我的训练助手","你是会员私人健身助手，只能使用当前会员自己的训练、课程和偏好数据，提供安全、清晰的健身建议。不要查看或推测其他人的数据。",List.of("如何开始力量训练","帮我安排本周训练"),List.of("basic_chat","knowledge_search"),10);
  };
 }
}
