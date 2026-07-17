package org.kie.api.definition.process;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Objects;
import java.util.Optional;

public record KogitoProcessId(String id, String version, Optional<Version> versionObj) implements Comparable<KogitoProcessId> {

    public KogitoProcessId(String id, String version) {
        this(Objects.requireNonNull(id), version, version == null ? Optional.empty() : parseVersion(version));
    }

    public KogitoProcessId(String id) {
        this(id, null);
    }

    public static KogitoProcessId from(org.kie.api.definition.process.Process process) {
        return process.getId();
    }

    private static Optional<Version> parseVersion(String version) {
        try {
            return Optional.of(Version.parse(version));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public int compareTo(KogitoProcessId o) {
        int compare = id.compareTo(o.id);
        if (compare == 0 && versionObj.isPresent() && o.versionObj.isPresent()) {
            compare = versionObj.orElseThrow().compareTo(o.versionObj.orElseThrow());
        }
        if (compare == 0 && version != null && o.version != null) {
            compare = version.compareTo(o.version);
        }
        return compare;
    }
}
