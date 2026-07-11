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

    // ---------------- extra edge cases ----------------

    @Test
    public void emptyOrderListStillProducesAValidHeaderOnlyFile() throws Exception {
        Path out = Files.createTempFile("orders_empty", ".csv");
        try {
            StudentOrderExporter.exportToCSV(List.of(), out.toString());
            List<String> lines = Files.readAllLines(out);
            assertEquals(1, lines.size(), "no orders means just the header row");
            assertEquals("Order ID,Items,Vendor,Status,ETA (min),Placed At", lines.get(0));
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    public void missingPlacedAtIsExportedAsAnEmDashNotACrash() throws Exception {
        Order order = new Order();
        order.setId("665f00000000000000000xyz");
        order.setStudentId("STU002");
        order.setVendorId("VEN002");
        order.setItemNames(List.of("Cold Coffee"));
        order.setStatus(Order.STATUS_PLACED);
        order.setPlacedAt(null);   // e.g. a malformed/legacy document
        order.setEtaMinutes(5);

        Path out = Files.createTempFile("orders_nodate", ".csv");
        try {
            StudentOrderExporter.exportToCSV(List.of(order), out.toString());
            List<String> lines = Files.readAllLines(out);
            assertTrue(lines.get(1).endsWith("\u2014"), "null placedAt must render as an em dash, not throw");
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    public void singleItemOrderIsExportedWithoutAStraySeparator() throws Exception {
        Order order = new Order();
        order.setId("665f00000000000000000one");
        order.setStudentId("STU003");
        order.setVendorId("VEN003");
        order.setItemNames(List.of("Samosa"));   // exactly one item - no " | " to join
        order.setStatus(Order.STATUS_SERVED);
        order.setPlacedAt(new Date());
        order.setEtaMinutes(0);

        Path out = Files.createTempFile("orders_single", ".csv");
        try {
            StudentOrderExporter.exportToCSV(List.of(order), out.toString());
            List<String> lines = Files.readAllLines(out);
            assertTrue(lines.get(1).contains("\"Samosa\""));
            assertFalse(lines.get(1).contains("|"), "a single item must not gain a join separator");
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    public void multipleOrdersAreWrittenInTheGivenOrderOneLineEach() throws Exception {
        Order first = new Order();
        first.setId("665f00000000000000000001");
        first.setStudentId("STU010"); first.setVendorId("VEN010");
        first.setItemNames(List.of("Dosa")); first.setStatus(Order.STATUS_PLACED);
        first.setPlacedAt(new Date()); first.setEtaMinutes(8);

        Order second = new Order();
        second.setId("665f00000000000000000002");
        second.setStudentId("STU011"); second.setVendorId("VEN011");
        second.setItemNames(List.of("Idli")); second.setStatus(Order.STATUS_SERVED);
        second.setPlacedAt(new Date()); second.setEtaMinutes(0);

        Path out = Files.createTempFile("orders_multi", ".csv");
        try {
            StudentOrderExporter.exportToCSV(List.of(first, second), out.toString());
            List<String> lines = Files.readAllLines(out);
            assertEquals(3, lines.size(), "header + one line per order");
            assertTrue(lines.get(1).startsWith("665f00000000000000000001,"));
            assertTrue(lines.get(2).startsWith("665f00000000000000000002,"));
        } finally {
            Files.deleteIfExists(out);
        }
    }
}
