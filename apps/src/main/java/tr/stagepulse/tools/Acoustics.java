package tr.stagepulse.tools;

public final class Acoustics {
    private Acoustics() {}

    public static double distanceLossDb(double distanceMeters) {
        return 20.0 * Math.log10(Math.max(1.0, distanceMeters));
    }

    public static double powerGainDb(double watts) {
        return 10.0 * Math.log10(Math.max(1.0, watts));
    }

    public static double directivityCorrectionDb(double angleDeg, double coverageDeg) {
        if (coverageDeg <= 0) return -18.0;
        double half = coverageDeg / 2.0;
        if (angleDeg <= half) return 0.0;
        return -Math.min(18.0, (angleDeg - half) * 0.6);
    }

    public static double spl(double sensitivityDb, double watts, double distanceMeters,
                             double angleDeg, double coverageDeg, double maxSplDb) {
        double value = sensitivityDb + powerGainDb(watts)
                - distanceLossDb(distanceMeters)
                + directivityCorrectionDb(angleDeg, coverageDeg);
        return Math.min(value, maxSplDb);
    }

    public static double arrayGainDb(int elementCount) {
        if (elementCount <= 1) return 0.0;
        return 10.0 * Math.log10(elementCount);
    }
}
