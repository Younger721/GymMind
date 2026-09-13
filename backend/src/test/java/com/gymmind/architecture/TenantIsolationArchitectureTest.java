package com.gymmind.architecture;

import com.gymmind.shared.persistence.TenantScopedEntity;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

@AnalyzeClasses(packages = "com.gymmind", importOptions = ImportOption.DoNotIncludeTests.class)
class TenantIsolationArchitectureTest {

    private static final Set<String> UNSCOPED_METHODS = Set.of("findById", "deleteById", "existsById");

    @ArchTest
    static final ArchRule apiMustNotDependOnRepositories = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    @Test
    void tenantRequestDtosMustNotExposeTenantId() {
        JavaClasses requestDtos = new ClassFileImporter()
                .importPackages("com.gymmind..api.request..");

        requestDtos.stream()
                .filter(request -> !request.isInterface())
                .forEach(request -> assertThat(request.getFields())
                        .as("request DTO %s", request.getName())
                        .noneMatch(field -> field.getName().equals("tenantId")));
    }

    @Test
    void tenantScopedRepositoryPortsMustUseScopedQueries() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.gymmind..domain.repository..");

        classes.stream()
                .filter(JavaClass::isInterface)
                .filter(TenantIsolationArchitectureTest::handlesTenantScopedEntity)
                .forEach(repository -> assertThat(repository.getMethods())
                        .as("tenant repository %s", repository.getName())
                        .noneMatch(method -> UNSCOPED_METHODS.contains(method.getName())));
    }

    private static boolean handlesTenantScopedEntity(JavaClass repository) {
        return repository.getMethods().stream().anyMatch(method ->
                method.getRawReturnType().isAssignableTo(TenantScopedEntity.class)
                        || method.getRawParameterTypes().stream()
                        .anyMatch(type -> type.isAssignableTo(TenantScopedEntity.class)));
    }
}
