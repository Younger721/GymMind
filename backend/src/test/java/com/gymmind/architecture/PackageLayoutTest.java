package com.gymmind.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.gymmind", importOptions = ImportOption.DoNotIncludeTests.class)
class PackageLayoutTest {

    @ArchTest
    static final ArchRule noLegacyTopLevelPackages = noClasses()
            .should().resideInAnyPackage(
                    "com.gymmind.controller..", "com.gymmind.service..",
                    "com.gymmind.entity..", "com.gymmind.repository..",
                    "com.gymmind.security..", "com.gymmind.common..",
                    "com.gymmind.dto..", "com.gymmind.config..");
}
