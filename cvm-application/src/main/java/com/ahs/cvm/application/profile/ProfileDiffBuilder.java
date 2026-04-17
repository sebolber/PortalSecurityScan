package com.ahs.cvm.application.profile;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Baut einen deterministischen Diff zwischen zwei Profil-Baeumen. Pfade
 * verwenden Punkt-Notation fuer Objekte und {@code [index]} fuer Arrays.
 * Die Ausgabe ist alphabetisch nach Pfad sortiert.
 */
@Component
public class ProfileDiffBuilder {

    public List<ProfileFieldDiff> diff(JsonNode alt, JsonNode neu) {
        List<ProfileFieldDiff> out = new ArrayList<>();
        walk("", alt, neu, out);
        out.sort(Comparator.comparing(ProfileFieldDiff::path));
        return out;
    }

    private void walk(String path, JsonNode alt, JsonNode neu, List<ProfileFieldDiff> out) {
        boolean altAbwesend = alt == null || alt.isMissingNode();
        boolean neuAbwesend = neu == null || neu.isMissingNode();

        if (altAbwesend && neuAbwesend) {
            return;
        }
        if (altAbwesend) {
            out.add(new ProfileFieldDiff(path, null, neu, ProfileFieldDiff.ChangeType.CREATED));
            return;
        }
        if (neuAbwesend) {
            out.add(new ProfileFieldDiff(path, alt, null, ProfileFieldDiff.ChangeType.REMOVED));
            return;
        }
        if (Objects.equals(alt, neu)) {
            return;
        }
        if (alt.isObject() && neu.isObject()) {
            TreeSet<String> keys = new TreeSet<>();
            alt.fieldNames().forEachRemaining(keys::add);
            neu.fieldNames().forEachRemaining(keys::add);
            for (String k : keys) {
                String childPath = path.isEmpty() ? k : path + "." + k;
                walk(childPath, alt.get(k), neu.get(k), out);
            }
            return;
        }
        if (alt.isArray() && neu.isArray()) {
            int max = Math.max(alt.size(), neu.size());
            for (int i = 0; i < max; i++) {
                JsonNode l = i < alt.size() ? alt.get(i) : null;
                JsonNode r = i < neu.size() ? neu.get(i) : null;
                walk(path + "[" + i + "]", l, r, out);
            }
            return;
        }
        out.add(new ProfileFieldDiff(path, alt, neu, ProfileFieldDiff.ChangeType.CHANGED));
    }
}
