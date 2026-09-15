package com.defistrategyarena.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.defistrategyarena.shared.infra.http.HttpServerBootstrap;
import com.defistrategyarena.shared.kernel.ArchitectureCatalog;
import com.defistrategyarena.shared.messaging.DomainEventListener;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@AnalyzeClasses(
        packages = ArchitectureCatalog.BASE_PACKAGE,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    private static final String BASE = ArchitectureCatalog.BASE_PACKAGE;
    private static final String CREATE_FACTORY = "create";

    private static final String SHARED_INFRA = BASE + ".shared.infra..";

    private static final String[] CONTEXT_PACKAGE_PATTERNS = {
        BASE + ".identity..",
        BASE + ".strategy..",
        BASE + ".marketdata..",
        BASE + ".arena..",
        BASE + ".leaderboard.."
    };

    private static final String[] HTTP_SERVER_LIBRARIES = {
        "org.eclipse.jetty..",
        "io.helidon.."
    };

    private static final DescribedPredicate<JavaClass> DOMAIN_MODELS =
            DescribedPredicate.describe(
                    "top-level concrete domain models",
                    javaClass ->
                            isDomainPackage(javaClass.getPackageName())
                                    && javaClass.isTopLevelClass()
                                    && !javaClass.isInterface()
                                    && !javaClass.isEnum()
                                    && !javaClass.isAnnotation()
                                    && !javaClass.getSimpleName().equals("package-info")
                                    && !javaClass.getModifiers().contains(JavaModifier.ABSTRACT));

    private static boolean isDomainPackage(String packageName) {
        return packageName.endsWith(".domain") || packageName.contains(".domain.");
    }
    @ArchTest
    static final ArchRule domain_must_not_depend_on_application_or_adapters =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..adapter..")
                    .allowEmptyShould(true)
                    .because("domain is the innermost hexagonal layer");

    @ArchTest
    static final ArchRule application_must_not_depend_on_adapters =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..adapter..")
                    .allowEmptyShould(true)
                    .because("application depends inward on domain only, adapters implement ports");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_frameworks =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.servlet..",
                            "javax.servlet..",
                            "org.eclipse.jetty..",
                            "io.helidon..")
                    .allowEmptyShould(true)
                    .because("domain stays framework-free; HTTP runtime lives in shared.infra only");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_persistence_apis =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "jakarta.persistence..",
                            "javax.persistence..",
                            "org.hibernate..",
                            "java.sql..",
                            "javax.sql..")
                    .allowEmptyShould(true)
                    .because("persistence stays in adapters");

    @ArchTest
    static final ArchRule domain_models_must_expose_static_create_factory =
            classes()
                    .that(DOMAIN_MODELS)
                    .should(haveIdempotentCreateFactory())
                    .allowEmptyShould(true)
                    .because(
                            "domain models must be created via static create(...) that derives an idempotent UUID id from fields");

    @ArchTest
    static final ArchRule non_record_domain_models_must_hide_constructors =
            classes()
                    .that(DOMAIN_MODELS)
                    .and(DescribedPredicate.describe("non-record", javaClass -> !javaClass.isRecord()))
                    .should()
                    .haveOnlyPrivateConstructors()
                    .allowEmptyShould(true)
                    .because("non-record domain models must be instantiated only through create(...)");

    @ArchTest
    static final ArchRule web_adapters_must_reside_in_adapter_web =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .should()
                    .resideInAPackage("..adapter.web..")
                    .allowEmptyShould(true)
                    .because("HTTP entrypoints belong in adapter.web");

    @ArchTest
    static final ArchRule bounded_contexts_must_not_depend_on_each_other =
            slices()
                    .assignedFrom(boundedContextSlices())
                    .should()
                    .notDependOnEachOther()
                    .allowEmptyShould(true)
                    .because(
                            "contexts communicate only asynchronously via shared DomainEventPublisher / DomainEventListener");

    @ArchTest
    static final ArchRule shared_kernel_must_not_depend_on_contexts =
            noClasses()
                    .that()
                    .resideInAPackage(BASE + ".shared..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(CONTEXT_PACKAGE_PATTERNS)
                    .because("shared is a thin kernel and must not depend on contexts");

    @ArchTest
    static final ArchRule context_packages_should_be_free_of_cycles =
            slices()
                    .matching(BASE + ".(*)..")
                    .should()
                    .beFreeOfCycles()
                    .because("context packages must not form cycles");

    @ArchTest
    static final ArchRule lombok_is_forbidden =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("lombok..")
                    .because("Lombok is forbidden; prefer records or explicit code");

    @ArchTest
    static final ArchRule domain_event_publisher_implementations_live_in_shared =
            classes()
                    .that()
                    .implement(DomainEventPublisher.class)
                    .should()
                    .resideInAPackage(BASE + ".shared..")
                    .allowEmptyShould(true)
                    .because("async publishing adapter is owned by shared until a runtime is chosen");

    @ArchTest
    static final ArchRule domain_event_listeners_live_in_application_or_adapters =
            classes()
                    .that()
                    .implement(DomainEventListener.class)
                    .should()
                    .resideInAnyPackage("..application..", "..adapter..")
                    .allowEmptyShould(true)
                    .because("listeners belong at the context boundary, not in domain");

    @ArchTest
    static final ArchRule http_server_libraries_only_in_shared_infra =
            noClasses()
                    .that()
                    .resideOutsideOfPackages(SHARED_INFRA)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(HTTP_SERVER_LIBRARIES)
                    .because(
                            "Jetty/Helidon APIs are confined to shared.infra; switch runtime via bootstrap in main");

    @ArchTest
    static final ArchRule http_server_bootstrap_implementations_in_shared_infra =
            classes()
                    .that()
                    .implement(HttpServerBootstrap.class)
                    .should()
                    .resideInAPackage(SHARED_INFRA)
                    .allowEmptyShould(true)
                    .because("HttpServerBootstrap implementations belong in shared.infra");

    @ArchTest
    static final ArchRule hexagonal_layers_must_not_depend_on_shared_infra =
            noClasses()
                    .that()
                    .resideInAnyPackage("..domain..", "..application..", "..adapter..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(SHARED_INFRA)
                    .allowEmptyShould(true)
                    .because("HTTP infra is wired at the composition root, not inside hexagonal layers");

    @ArchTest
    static final ArchRule composition_root_must_not_depend_on_http_server_libraries =
            noClasses()
                    .that()
                    .resideInAnyPackage(BASE + "..")
                    .and()
                    .resideOutsideOfPackage(BASE + ".shared..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(HTTP_SERVER_LIBRARIES)
                    .allowEmptyShould(true)
                    .because(
                            "main/bootstrap select an HttpServerBootstrap impl; they must not import Jetty/Helidon directly");

    @ArchTest
    static final ArchRule shared_kernel_and_messaging_must_not_depend_on_infra =
            noClasses()
                    .that()
                    .resideInAnyPackage(BASE + ".shared.kernel..", BASE + ".shared.messaging..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(SHARED_INFRA)
                    .because("kernel and messaging stay runtime-free; infra is optional wiring");

    @ArchTest
    static final ArchRule production_types_reside_in_null_marked_packages =
            classes()
                    .that()
                    .resideInAPackage(BASE + "..")
                    .and()
                    .areTopLevelClasses()
                    .and()
                    .doNotHaveSimpleName("package-info")
                    .should(resideInNullMarkedPackage())
                    .because("JSpecify @NullMarked is required so APIs default to non-null");

    @ArchTest
    static final ArchRule methods_must_not_declare_nullable_inputs_or_outputs =
            methods()
                    .that()
                    .areDeclaredInClassesThat()
                    .resideInAPackage(BASE + "..")
                    .and()
                    .arePublic()
                    .should(haveNoNullableParametersOrReturnType())
                    .because("public method parameters and return types must never be @Nullable");

    @ArchTest
    static final ArchRule constructors_must_not_declare_nullable_parameters =
            constructors()
                    .that()
                    .areDeclaredInClassesThat()
                    .resideInAPackage(BASE + "..")
                    .and()
                    .arePublic()
                    .should(haveNoNullableParameters())
                    .because("public constructor parameters must never be @Nullable");

    private static ArchCondition<JavaClass> resideInNullMarkedPackage() {
        return new ArchCondition<>("reside in a package annotated with @NullMarked") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean marked = item.getPackage().isAnnotatedWith(NullMarked.class);
                events.add(
                        new SimpleConditionEvent(
                                item,
                                marked,
                                item.getPackageName() + " must declare @NullMarked in package-info"));
            }
        };
    }

    private static ArchCondition<JavaMethod> haveNoNullableParametersOrReturnType() {
        return new ArchCondition<>("not use @Nullable on parameters or return type") {
            @Override
            public void check(JavaMethod item, ConditionEvents events) {
                Method reflected = item.reflect();
                if (reflected.isSynthetic() || reflected.isBridge()) {
                    return;
                }
                checkAnnotatedType(
                        item, events, reflected.getAnnotatedReturnType(), "return type");
                Parameter[] parameters = reflected.getParameters();
                for (int index = 0; index < parameters.length; index++) {
                    checkAnnotatedType(
                            item,
                            events,
                            parameters[index].getAnnotatedType(),
                            "parameter " + index);
                }
            }
        };
    }

    private static ArchCondition<JavaConstructor> haveNoNullableParameters() {
        return new ArchCondition<>("not use @Nullable on parameters") {
            @Override
            public void check(JavaConstructor item, ConditionEvents events) {
                Constructor<?> reflected = item.reflect();
                if (reflected.isSynthetic()) {
                    return;
                }
                Parameter[] parameters = reflected.getParameters();
                for (int index = 0; index < parameters.length; index++) {
                    checkAnnotatedType(
                            item,
                            events,
                            parameters[index].getAnnotatedType(),
                            "parameter " + index);
                }
            }
        };
    }

    private static void checkAnnotatedType(
            JavaCodeUnit owner, ConditionEvents events, AnnotatedType annotatedType, String where) {
        boolean nullable = hasNullable(annotatedType.getAnnotations());
        events.add(
                new SimpleConditionEvent(
                        owner,
                        !nullable,
                        owner.getFullName() + " " + where + " must not be @Nullable"));
    }

    private static boolean hasNullable(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(Nullable.class)) {
                return true;
            }
        }
        return false;
    }

    private static ArchCondition<JavaClass> haveIdempotentCreateFactory() {
        return new ArchCondition<>(
                "have public static create(...) returning the domain type with at least one parameter") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean matched = item.getMethods().stream().anyMatch(method -> isCreateFactory(method, item));
                String message =
                        item.getName()
                                + " must declare public static create(<fields...>) returning "
                                + item.getSimpleName()
                                + " (id derived via IdempotentUuid from those fields)";
                events.add(new SimpleConditionEvent(item, matched, message));
            }
        };
    }

    private static boolean isCreateFactory(JavaMethod method, JavaClass owner) {
        if (!CREATE_FACTORY.equals(method.getName())) {
            return false;
        }
        if (!method.getModifiers().contains(JavaModifier.STATIC)) {
            return false;
        }
        if (!method.getModifiers().contains(JavaModifier.PUBLIC)) {
            return false;
        }
        if (!method.getRawReturnType().equals(owner)) {
            return false;
        }
        return !method.getParameters().isEmpty();
    }

    private static SliceAssignment boundedContextSlices() {
        return new SliceAssignment() {
            @Override
            public String getDescription() {
                return "bounded contexts";
            }

            @Override
            public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
                String context = contextOf(javaClass.getPackageName());
                if (context == null) {
                    return SliceIdentifier.ignore();
                }
                return SliceIdentifier.of(context);
            }
        };
    }

    private static String contextOf(String packageName) {
        for (String context : ArchitectureCatalog.CONTEXT_PACKAGES) {
            String prefix = BASE + '.' + context;
            if (packageName.equals(prefix) || packageName.startsWith(prefix + '.')) {
                return context;
            }
        }
        return null;
    }
}
