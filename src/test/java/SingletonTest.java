import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class SingletonTest {

    @Test
    public void systemTest() {
        System system1 = System.getInstance(new BigDecimal(1_000_000_000));
        assertNotNull(system1);
        System system2 = System.getInstance(new BigDecimal(1_000_000_000));
        assertNotNull(system2);
        assertSame(system1, system2);
    }

    @Test
    public void timetableTest() {
        Timetable timetable1 = Timetable.getInstance();
        assertNotNull(timetable1);
        Timetable timetable2 = Timetable.getInstance();
        assertNotNull(timetable2);
        assertSame(timetable1, timetable2);
    }
}
