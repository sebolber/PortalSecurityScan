package com.ahs.cvm.application.reachability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PurlSymbolDeriverTest {

    @Test
    @DisplayName("Maven: groupId + artifactId werden zu einem Java-Paket-Prefix")
    void maven_commonsText() {
        Optional<PurlSymbolDeriver.Suggestion> s = PurlSymbolDeriver.derive(
                "pkg:maven/org.apache.commons/commons-text@1.9");

        assertThat(s).isPresent();
        assertThat(s.get().symbol()).isEqualTo("org.apache.commons.text");
        assertThat(s.get().language()).isEqualTo("java");
    }

    @Test
    @DisplayName("Maven: zusaetzliche Artifact-Komponenten werden angehaengt")
    void maven_mehrTeiligerArtifact() {
        Optional<PurlSymbolDeriver.Suggestion> s = PurlSymbolDeriver.derive(
                "pkg:maven/io.netty/netty-codec-http@4.1.100");

        assertThat(s).isPresent();
        assertThat(s.get().symbol()).isEqualTo("io.netty.codec.http");
    }

    @Test
    @DisplayName("Maven: groupId ohne Ueberlappung wird komplett uebernommen")
    void maven_ohneUeberlappung() {
        Optional<PurlSymbolDeriver.Suggestion> s = PurlSymbolDeriver.derive(
                "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.17.2");

        assertThat(s).isPresent();
        // Best-effort Ableitung; der Analyst kann auf com.fasterxml.jackson.databind verfeinern.
        assertThat(s.get().symbol()).startsWith("com.fasterxml.jackson.core");
        assertThat(s.get().language()).isEqualTo("java");
    }

    @Test
    @DisplayName("npm: Paketname bleibt wie er ist; Sprache javascript")
    void npm_lodash() {
        Optional<PurlSymbolDeriver.Suggestion> s = PurlSymbolDeriver.derive(
                "pkg:npm/lodash@4.17.21");

        assertThat(s).isPresent();
        assertThat(s.get().symbol()).isEqualTo("lodash");
        assertThat(s.get().language()).isEqualTo("javascript");
    }

    @Test
    @DisplayName("npm: scoped Paket behaelt den @scope")
    void npm_scoped() {
        Optional<PurlSymbolDeriver.Suggestion> s = PurlSymbolDeriver.derive(
                "pkg:npm/%40angular/core@18.2.0");

        assertThat(s).isPresent();
        assertThat(s.get().symbol()).isEqualTo("@angular/core");
    }

    @Test
    @DisplayName("PyPI: Paketname wird auf Modulform (lowercase, underscore) normalisiert")
    void pypi() {
        Optional<PurlSymbolDeriver.Suggestion> s = PurlSymbolDeriver.derive(
                "pkg:pypi/Requests-OAuthlib@2.0.0");

        assertThat(s).isPresent();
        assertThat(s.get().symbol()).isEqualTo("requests_oauthlib");
        assertThat(s.get().language()).isEqualTo("python");
    }

    @Test
    @DisplayName("Golang: URL-dekodierter Import-Pfad wird uebernommen")
    void golang() {
        Optional<PurlSymbolDeriver.Suggestion> s = PurlSymbolDeriver.derive(
                "pkg:golang/golang.org%2Fx%2Fcrypto@v0.17.0");

        assertThat(s).isPresent();
        assertThat(s.get().symbol()).isEqualTo("golang.org/x/crypto");
        assertThat(s.get().language()).isEqualTo("go");
    }

    @Test
    @DisplayName("Cargo: Crate-Name mit Sprache rust")
    void cargo() {
        Optional<PurlSymbolDeriver.Suggestion> s = PurlSymbolDeriver.derive(
                "pkg:cargo/openssl@0.10.55");

        assertThat(s).isPresent();
        assertThat(s.get().symbol()).isEqualTo("openssl");
        assertThat(s.get().language()).isEqualTo("rust");
    }

    @Test
    @DisplayName("Query-String und Subpath werden ignoriert")
    void qualifierWerdenGestrippt() {
        Optional<PurlSymbolDeriver.Suggestion> s = PurlSymbolDeriver.derive(
                "pkg:maven/org.apache.commons/commons-text@1.9?classifier=sources#path/to");

        assertThat(s).isPresent();
        assertThat(s.get().symbol()).isEqualTo("org.apache.commons.text");
    }

    @Test
    @DisplayName("Unbekannter Typ liefert empty - Analyst muss manuell eingeben")
    void unbekannterTyp() {
        assertThat(PurlSymbolDeriver.derive("pkg:generic/foo/bar@1")).isEmpty();
    }

    @Test
    @DisplayName("Kaputte PURL liefert empty")
    void kaputt() {
        assertThat(PurlSymbolDeriver.derive(null)).isEmpty();
        assertThat(PurlSymbolDeriver.derive("")).isEmpty();
        assertThat(PurlSymbolDeriver.derive("not-a-purl")).isEmpty();
        assertThat(PurlSymbolDeriver.derive("pkg:maven/")).isEmpty();
        assertThat(PurlSymbolDeriver.derive("pkg:maven/group/")).isEmpty();
    }
}
