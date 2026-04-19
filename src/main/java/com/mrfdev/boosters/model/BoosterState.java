package com.mrfdev.boosters.model;

public record BoosterState(
        BoosterType type,
        boolean active,
        double rate,
        long startedAtEpochMillis,
        long durationMillis,
        long endsAtEpochMillis,
        boolean announceOnStart,
        String jobsTarget,
        String jobsScope
) {

    public BoosterState {
        jobsTarget = jobsTarget == null ? "" : jobsTarget;
        jobsScope = jobsScope == null ? "" : jobsScope;
    }

    public static BoosterState inactive(BoosterType type) {
        return new BoosterState(type, false, 1.0D, 0L, 0L, 0L, false, "", "");
    }

    public boolean hasTrackedDuration() {
        return active && durationMillis > 0L && endsAtEpochMillis > 0L;
    }

    public long remainingMillis(long now) {
        if (!hasTrackedDuration()) {
            return 0L;
        }
        return Math.max(0L, endsAtEpochMillis - now);
    }

    public boolean isExpired(long now) {
        return hasTrackedDuration() && endsAtEpochMillis <= now;
    }

    public boolean isJobsGlobal() {
        return type == BoosterType.JOBS
                && jobsTarget.equalsIgnoreCase("all")
                && jobsScope.equalsIgnoreCase("all");
    }

    public String jobsDescriptor() {
        if (jobsTarget.isBlank() || jobsScope.isBlank()) {
            return "global";
        }
        return jobsTarget + "/" + jobsScope;
    }
}
