package com.collegefest.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import com.collegefest.db.OrderDAO;
import com.collegefest.exceptions.CollegeFestException;
import com.collegefest.models.Order;

/**
 * Exports every order placed at one vendor's stall to a CSV file.
 * Same pattern as StudentOrderExporter: BufferedWriter over FileWriter in
 * a TRY-WITH-RESOURCES block, so the file is closed automatically even if
 * an exception fires mid-write. Item names are joined with " | " (never
 * commas - a comma inside a cell would split it into extra CSV columns).
 */
public class VendorOrderExporter {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd-MM-yyyy HH:mm");

    public static void exportToCSV(String vendorId, String filePath) {
        List<Order> orders = new OrderDAO().findByVendor(vendorId);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("Order ID,Student,Items,Status,Placed At,ETA (min)");
            writer.newLine();
            for (Order order : orders) {
                String items = String.join(" | ", order.getItemNames());
                String line = String.join(",",
                        order.getId(),
                        order.getStudentId(),
                        items,
                        order.getStatus(),
                        (order.getPlacedAt() == null) ? "" : DATE_FORMAT.format(order.getPlacedAt()),
                        String.valueOf(order.getEtaMinutes()));
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new CollegeFestException(
                    "Could not write CSV file: " + e.getMessage(), e);
        }
    }
}
