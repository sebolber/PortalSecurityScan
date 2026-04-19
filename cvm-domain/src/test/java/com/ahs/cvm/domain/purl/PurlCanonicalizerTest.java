package com.ahs.cvm.domain.purl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PurlCanonicalizerTest {

    @Test
    @DisplayName("null und leerer String werden unveraendert durchgereicht")
    void nullUndLeer() {
        assertThat(PurlCanonicalizer.canonicalize(null)).isNull();
        assertThat(PurlCanonicalizer.canonicalize("")).isEmpty();
    }

    @Test
    @DisplayName("Ohne pkg:-Praefix bleibt der Originalstring erhalten")
    void ohnePraefix() {
        assertThat(PurlCanonicalizer.canonicalize("foo/bar@1"))
                .isEqualTo("foo/bar@1");
    }

    @Test
    @DisplayName("Type/Namespace/Name werden lowercased")
    void lowercase() {
        assertThat(PurlCanonicalizer.canonicalize("pkg:MAVEN/Org.Acme/MyLib@1.0"))
                .isEqualTo("pkg:maven/org.acme/mylib@1.0");
    }

    @Test
    @DisplayName("Version (Case-sensitive) und Subpath bleiben unveraendert")
    void versionCaseErhalten() {
        assertThat(PurlCanonicalizer.canonicalize("pkg:npm/lodash@4.17.21#src/Index.js"))
                .isEqualTo("pkg:npm/lodash@4.17.21#src/Index.js");
    }

    @Test
    @DisplayName("Qualifier werden alphabetisch sortiert und leere Werte entfernt")
    void qualifierSortiert() {
        String in = "pkg:maven/org/lib@1.0?zType=jar&classifier=&arch=amd64";
        assertThat(PurlCanonicalizer.canonicalize(in))
                .isEqualTo("pkg:maven/org/lib@1.0?arch=amd64&ztype=jar");
    }

    @Test
    @DisplayName("Ohne Qualifier kommt kein Fragezeichen in den Output")
    void keineQualifier() {
        assertThat(PurlCanonicalizer.canonicalize("pkg:pypi/flask@2.0"))
                .isEqualTo("pkg:pypi/flask@2.0");
    }

    @Test
    @DisplayName("Nur leere Qualifier werden komplett entfernt")
    void leereQualifier() {
        assertThat(PurlCanonicalizer.canonicalize("pkg:maven/org/lib@1.0?foo=&bar="))
                .isEqualTo("pkg:maven/org/lib@1.0");
    }

    @Test
    @DisplayName("sameAfterCanonicalization: Case-Varianten gelten als gleich")
    void gleichheit() {
        assertThat(PurlCanonicalizer.sameAfterCanonicalization(
                "pkg:MAVEN/Org.Acme/Foo@1.0",
                "pkg:maven/org.acme/foo@1.0"))
                .isTrue();
    }

    @Test
    @DisplayName("sameAfterCanonicalization: verschiedene Versionen gelten als ungleich")
    void ungleich() {
        assertThat(PurlCanonicalizer.sameAfterCanonicalization(
                "pkg:maven/org.acme/foo@1.0",
                "pkg:maven/org.acme/foo@1.1"))
                .isFalse();
    }

    @Test
    @DisplayName("Ohne version bleibt kein At-Zeichen uebrig")
    void ohneVersion() {
        assertThat(PurlCanonicalizer.canonicalize("pkg:generic/MyPkg"))
                .isEqualTo("pkg:generic/mypkg");
    }
}
