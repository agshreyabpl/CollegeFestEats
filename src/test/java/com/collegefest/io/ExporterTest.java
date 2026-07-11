package com.collegefest.io;

import com.collegefest.models.Order;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** CSV export: header row, " | " item join (commas would split cells). */
public class ExporterTest {

    @Test
    public void studentExportWritesHeaderAndPipeJoinedItems() throws Exception {
        Order order = new Order();
        order.setId("665f00000000000000000abc");
        order.setStudentId("STU001");
        order.setVendorId("VEN001");
        order.setItemNames(List.of("Samosa", "Chai"));
        order.setStatus(Order.STATUS_READY);
        order.setPlacedAt(new Date());
        order.setEtaMinutes(9);

        Path out = Files.createTempFile("orders", ".csv");
        try {
            StudentOrderExporter.exportToCSV(List.of(order), out.toString());
            List<String> lines = Files.readAllLines(out);
            assertEquals("Order ID,Items,Vendor,Status,ETA (min),Placed At", lines.get(0));
            assertEquals(2, lines.size());
            assertTrue(lines.get(1).contains("Samosa | Chai"),
                    "items must be joined with ' | ', never commas");
            assertTrue(lines.get(1).startsWith("665f00000000000000000abc,"));
            assertTrue(lines.get(1).contains("READY"));
        } finally {
            Files.deleteIfExists(out);
        }
    }
}
