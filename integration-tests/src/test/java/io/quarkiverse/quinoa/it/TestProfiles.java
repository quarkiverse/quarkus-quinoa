package io.quarkiverse.quinoa.it;

import io.quarkiverse.quinoa.testing.QuinoaTestProfiles;

public class TestProfiles {

    public static class YarnTests extends QuinoaTestProfiles.EnableAndRunTests {
        @Override
        public String getConfigProfile() {
            return "yarn";
        }
    }

    public static class ReactTests extends QuinoaTestProfiles.Enable {
        @Override
        public String getConfigProfile() {
            return "react";
        }
    }

    public static class AngularTests extends QuinoaTestProfiles.Enable {
        @Override
        public String getConfigProfile() {
            return "angular";
        }
    }

    public static class LitTests extends QuinoaTestProfiles.Enable {
        @Override
        public String getConfigProfile() {
            return "lit";
        }
    }

    public static class VueTests extends QuinoaTestProfiles.Enable {
        @Override
        public String getConfigProfile() {
            return "vue";
        }
    }
}
