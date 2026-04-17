package com.ahs.cvm.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verhindert die Klasse von Bugs, die uns Iteration 06 auf den Kopf
 * gefallen ist: Spring 6 verlangt fuer Beans mit mehreren oeffentlichen
 * Konstruktoren entweder einen mit {@code @Autowired} markierten Eintrag
 * oder einen no-arg-Konstruktor. Sonst scheitert der Bean-Bau zur
 * Laufzeit ({@code BeanInstantiationException: No default constructor}).
 *
 * <p>Der Test laeuft ohne Spring-Container und ohne Datenbank, weshalb er
 * lokal sehr schnell ist und auch ohne Docker greift.
 */
class SpringBeanKonstruktorTest {

    private static final List<String> SPRING_BEAN_ANNOTATIONS = List.of(
            "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Component",
            "org.springframework.stereotype.Repository",
            "org.springframework.stereotype.Controller",
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.context.annotation.Configuration");

    @Test
    @DisplayName("Spring-Beans mit mehreren oeffentlichen Konstruktoren markieren genau einen mit @Autowired")
    void springBeansHabenEindeutigInjizierbarenKonstruktor() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.ahs.cvm");

        List<String> verstoesse = importedClasses.stream()
                .filter(SpringBeanKonstruktorTest::istSpringBean)
                .filter(SpringBeanKonstruktorTest::istKonkreteKlasse)
                .filter(c -> oeffentlicheKonstruktoren(c).size() > 1)
                .filter(c -> mitAutowired(oeffentlicheKonstruktoren(c)).size() != 1)
                .map(JavaClass::getFullName)
                .sorted()
                .toList();

        assertThat(verstoesse)
                .as("Spring-Beans mit mehreren Konstruktoren brauchen genau einen "
                        + "@Autowired-Konstruktor (Spring 6 verlangt eindeutige Wahl).")
                .isEmpty();
    }

    private static boolean istSpringBean(JavaClass clazz) {
        return clazz.getAnnotations().stream()
                .map(a -> a.getRawType().getName())
                .anyMatch(SPRING_BEAN_ANNOTATIONS::contains);
    }

    private static boolean istKonkreteKlasse(JavaClass clazz) {
        return !clazz.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT)
                && !clazz.isInterface()
                && !clazz.isEnum()
                && !clazz.isAnnotation();
    }

    private static List<JavaConstructor> oeffentlicheKonstruktoren(JavaClass clazz) {
        return clazz.getConstructors().stream()
                .filter(c -> c.getModifiers()
                        .contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC))
                .collect(Collectors.toList());
    }

    private static List<JavaConstructor> mitAutowired(List<JavaConstructor> konstruktoren) {
        return konstruktoren.stream()
                .filter(c -> c.isAnnotatedWith("org.springframework.beans.factory.annotation.Autowired"))
                .collect(Collectors.toList());
    }
}
