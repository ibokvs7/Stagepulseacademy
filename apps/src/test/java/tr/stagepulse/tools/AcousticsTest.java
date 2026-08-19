package tr.stagepulse.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AcousticsTest {
    @Test public void inverseSquareLossIs20Log10() {
        assertEquals(0.0, Acoustics.distanceLossDb(1), 0.0001);
        assertEquals(6.0206, Acoustics.distanceLossDb(2), 0.001);
        assertEquals(20.0, Acoustics.distanceLossDb(10), 0.001);
    }

    @Test public void powerGainIs10Log10() {
        assertEquals(0.0, Acoustics.powerGainDb(1), 0.0001);
        assertEquals(10.0, Acoustics.powerGainDb(10), 0.001);
        assertEquals(30.0, Acoustics.powerGainDb(1000), 0.001);
    }

    @Test public void coverageOutsideBeamAttenuates() {
        assertEquals(0.0, Acoustics.directivityCorrectionDb(20, 90), 0.001);
        assertTrue(Acoustics.directivityCorrectionDb(80, 90) < 0.0);
    }

    @Test public void splNeverExceedsMaximum() {
        double value = Acoustics.spl(100, 1600, 1, 0, 90, 120);
        assertEquals(120.0, value, 0.001);
    }

    @Test public void arrayGainIncreasesWithElements() {
        assertEquals(0.0, Acoustics.arrayGainDb(1), 0.001);
        assertEquals(6.0206, Acoustics.arrayGainDb(4), 0.001);
    }
}
