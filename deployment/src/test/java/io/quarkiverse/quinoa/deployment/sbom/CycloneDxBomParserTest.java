package io.quarkiverse.quinoa.deployment.sbom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import io.quarkus.sbom.ComponentDependencies;
import io.quarkus.sbom.ComponentDescriptor;
import io.quarkus.sbom.SbomContribution;

class CycloneDxBomParserTest {

    private static final Path RESOURCES = Path.of("src/test/resources/sbom");
    private static final Path PACKAGE_JSON = RESOURCES.resolve("package.json");

    @Test
    void parseBasicComponents() throws IOException {
        SbomContribution contribution = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-npm-basic.json"), PACKAGE_JSON);
        Collection<ComponentDescriptor> components = contribution.components();

        assertEquals(3, components.size());

        ComponentDescriptor lodash = findByName(components, "lodash");
        assertEquals("npm", lodash.getType());
        assertNull(lodash.getNamespace());
        assertEquals("lodash", lodash.getName());
        assertEquals("4.17.21", lodash.getVersion());
        assertEquals(ComponentDescriptor.SCOPE_RUNTIME, lodash.getScope());
        assertNotNull(lodash.getIntegrity());
    }

    @Test
    void devDependencyDetectedFromProperties() throws IOException {
        SbomContribution contribution = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-npm-basic.json"), PACKAGE_JSON);

        ComponentDescriptor vite = findByName(contribution.components(), "vite");
        assertEquals(ComponentDescriptor.SCOPE_DEVELOPMENT, vite.getScope());

        ComponentDescriptor esbuild = findByName(contribution.components(), "esbuild");
        assertEquals(ComponentDescriptor.SCOPE_DEVELOPMENT, esbuild.getScope());
    }

    @Test
    void topLevelDetectedFromPackageJson() throws IOException {
        SbomContribution contribution = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-npm-basic.json"), PACKAGE_JSON);

        ComponentDescriptor lodash = findByName(contribution.components(), "lodash");
        assertTrue(lodash.isTopLevel(), "lodash is a direct dependency");

        ComponentDescriptor vite = findByName(contribution.components(), "vite");
        assertTrue(vite.isTopLevel(), "vite is a direct devDependency");

        ComponentDescriptor esbuild = findByName(contribution.components(), "esbuild");
        assertFalse(esbuild.isTopLevel(), "esbuild is transitive");
    }

    @Test
    void scopedPackagesParsedCorrectly() throws IOException {
        SbomContribution contribution = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-scoped.json"), PACKAGE_JSON);
        Collection<ComponentDescriptor> components = contribution.components();

        assertEquals(2, components.size());

        ComponentDescriptor litReactive = findByName(components, "reactive-element");
        assertEquals("@lit", litReactive.getNamespace());
        assertEquals("reactive-element", litReactive.getName());
        assertEquals("2.0.4", litReactive.getVersion());
        assertEquals(ComponentDescriptor.SCOPE_RUNTIME, litReactive.getScope());
        assertTrue(litReactive.isTopLevel(), "@lit/reactive-element is a direct dependency");

        ComponentDescriptor ssrShim = findByName(components, "ssr-dom-shim");
        assertEquals("@lit-labs", ssrShim.getNamespace());
        assertEquals("ssr-dom-shim", ssrShim.getName());
        assertFalse(ssrShim.isTopLevel(), "@lit-labs/ssr-dom-shim is transitive");
    }

    @Test
    void dependencyRelationshipsResolved() throws IOException {
        SbomContribution contribution = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-npm-basic.json"), PACKAGE_JSON);

        ComponentDescriptor vite = findByName(contribution.components(), "vite");
        ComponentDescriptor esbuild = findByName(contribution.components(), "esbuild");

        ComponentDependencies viteDeps = contribution.dependencies().stream()
                .filter(d -> d.getBomRef().equals(vite.getBomRef()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("vite should have dependencies"));

        assertTrue(viteDeps.getDependsOn().contains(esbuild.getBomRef()),
                "vite should depend on esbuild");
    }

    @Test
    void scopedDependencyRelationshipsResolved() throws IOException {
        SbomContribution contribution = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-scoped.json"), PACKAGE_JSON);

        ComponentDescriptor litReactive = findByName(contribution.components(), "reactive-element");
        ComponentDescriptor ssrShim = findByName(contribution.components(), "ssr-dom-shim");

        ComponentDependencies litDeps = contribution.dependencies().stream()
                .filter(d -> d.getBomRef().equals(litReactive.getBomRef()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("@lit/reactive-element should have dependencies"));

        assertEquals(1, litDeps.getDependsOn().size());
        assertTrue(litDeps.getDependsOn().contains(ssrShim.getBomRef()));
    }

    @Test
    void integrityExtractedFromHashes() throws IOException {
        SbomContribution contribution = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-npm-basic.json"), PACKAGE_JSON);

        ComponentDescriptor lodash = findByName(contribution.components(), "lodash");
        assertNotNull(lodash.getIntegrity());
        assertTrue(lodash.getIntegrity().startsWith("sha512-"),
                "SHA-512 hash should be formatted as sha512-<content>");

        ComponentDescriptor esbuild = findByName(contribution.components(), "esbuild");
        assertEquals("sha256-47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=",
                esbuild.getIntegrity(),
                "hex-encoded SHA-256 should be converted to base64 in SRI format");
    }

    @Test
    void hexHashConvertedToBase64() {
        String result = CycloneDxBomParser.toSriIntegrity("SHA-256", "abcdef0123456789");
        assertEquals("sha256-", result.substring(0, 7));
        assertFalse(result.substring(7).matches("[0-9a-fA-F]+"),
                "hex content should be converted to base64");
    }

    @Test
    void base64HashPassedThrough() {
        String result = CycloneDxBomParser.toSriIntegrity("SHA-512",
                "v2kDEe57lecTulaDIuNTPy3Ry4gLGJ6Z1O3vE1krgXZNrsQ");
        assertEquals("sha512-v2kDEe57lecTulaDIuNTPy3Ry4gLGJ6Z1O3vE1krgXZNrsQ", result);
    }

    @Test
    void parseWithoutPackageJsonAllNonTopLevel() throws IOException {
        SbomContribution contribution = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-npm-basic.json"), null);
        Collection<ComponentDescriptor> components = contribution.components();

        assertEquals(3, components.size());
        for (ComponentDescriptor c : components) {
            assertFalse(c.isTopLevel(), c.getName() + " should not be top-level without package.json");
        }
    }

    @Test
    void excludeDevDependencies_removesDevComponents() throws IOException {
        SbomContribution full = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-npm-basic.json"), PACKAGE_JSON);
        assertEquals(3, full.components().size());

        SbomContribution filtered = CycloneDxBomParser.excludeDevDependencies(full);
        assertEquals(1, filtered.components().size());

        ComponentDescriptor lodash = findByName(filtered.components(), "lodash");
        assertEquals(ComponentDescriptor.SCOPE_RUNTIME, lodash.getScope());
    }

    @Test
    void excludeDevDependencies_removesDependencyEntries() throws IOException {
        SbomContribution full = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-npm-basic.json"), PACKAGE_JSON);

        SbomContribution filtered = CycloneDxBomParser.excludeDevDependencies(full);

        assertTrue(filtered.dependencies().isEmpty(),
                "vite→esbuild dependency should be removed since both are dev-scoped");
    }

    @Test
    void excludeDevDependencies_skipsEmptyDependsOn() throws IOException {
        SbomContribution full = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-npm-basic.json"), PACKAGE_JSON);

        SbomContribution filtered = CycloneDxBomParser.excludeDevDependencies(full);

        for (ComponentDependencies dep : filtered.dependencies()) {
            assertFalse(dep.getDependsOn().isEmpty(),
                    "no dependency entry should have an empty dependsOn list: " + dep.getBomRef());
        }
    }

    @Test
    void excludeDevDependencies_noDevReturnsOriginal() throws IOException {
        SbomContribution full = CycloneDxBomParser.parse(
                RESOURCES.resolve("cyclonedx-scoped.json"), PACKAGE_JSON);

        SbomContribution filtered = CycloneDxBomParser.excludeDevDependencies(full);

        assertEquals(full.components().size(), filtered.components().size());
        assertEquals(full.dependencies().size(), filtered.dependencies().size());
    }

    private static ComponentDescriptor findByName(Collection<ComponentDescriptor> components, String name) {
        return components.stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(name + " not found"));
    }
}
