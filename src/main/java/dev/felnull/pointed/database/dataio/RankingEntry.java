package dev.felnull.pointed.database.dataio;

import java.util.UUID;

public record RankingEntry(
        long subjectId,
        UUID playerUuid,
        String name,
        int held,
        int total
) {}
