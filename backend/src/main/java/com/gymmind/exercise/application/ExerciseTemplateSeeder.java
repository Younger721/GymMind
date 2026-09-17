package com.gymmind.exercise.application;

import com.gymmind.exercise.domain.model.Exercise;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.tenancy.application.port.TenantProvisioningContributor;
import com.gymmind.tenancy.domain.model.Tenant;
import org.springframework.stereotype.Component;

@Component
public class ExerciseTemplateSeeder implements TenantProvisioningContributor {

    private final ExerciseRepository exercises;

    public ExerciseTemplateSeeder(ExerciseRepository exercises) {
        this.exercises = exercises;
    }

    @Override
    public void contribute(Tenant tenant) {
        if (tenant == null || tenant.getId() == null) {
            return;
        }
        seedIfMissing(tenant.getId(), "杠铃深蹲", "力量", "股四头肌", "中级", "杠铃",
                "经典下肢复合动作，提升整体力量。",
                "1. 杠铃置于斜方肌上\n2. 屈髋屈膝下蹲至平行\n3. 脚跟发力站起",
                "膝盖内扣、弓背、重心前移",
                "充分热身，控制重量，必要时使用护具",
                "compound,legs,strength");
        seedIfMissing(tenant.getId(), "平板卧推", "力量", "胸大肌", "中级", "杠铃",
                "上肢推力训练基础动作。",
                "1. 肩胛收紧贴凳\n2. 杠铃下放至胸骨附近\n3. 推起至肘部微屈",
                "耸肩、臀部离凳、弹震",
                "建议使用保护杠或保护者",
                "compound,chest,strength");
        seedIfMissing(tenant.getId(), "硬拉", "力量", "后链", "高级", "杠铃",
                "全身后链力量动作。",
                "1. 脚与髋同宽\n2. 保持脊柱中立\n3. 髋膝协同伸展",
                "圆背、杠铃远离身体",
                "从轻重量学习动作模式",
                "compound,posterior,strength");
    }

    private void seedIfMissing(Long tenantId, String name, String category, String targetMuscle, String difficulty,
                               String equipment, String description, String steps, String mistakes, String safety,
                               String tags) {
        if (exercises.existsByTenantIdAndName(tenantId, name)) {
            return;
        }
        exercises.save(Exercise.create(tenantId, name, category, targetMuscle, difficulty, equipment,
                description, steps, mistakes, safety, tags));
    }
}
