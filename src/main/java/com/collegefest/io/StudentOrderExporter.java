package com.collegefest.io;

import com.collegefest.models.Order;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class StudentOrderExporter {

    // try-with-resources ensures file is closed even if exception occurs
    public static void exportToCSV(List<Order> orders, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Header row
            writer.write("Order ID,Items,Vendor,Status,ETA (min),Placed At");
            writer.newLine();

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");

            for (Order order : orders) {
                String items = String.join(" | ", order.getItemNames());
                String placedAt = order.getPlacedAt() != null ?
                    sdf.format(order.getPlacedAt()) : "—";

                writer.write(String.format("%s,\"%s\",%s,%s,%d,%s",
                    order.getId(),
                    items,
                    order.getVendorId(),
                    order.getStatus(),
                    order.getEtaMinutes(),
                    placedAt
                ));
                writer.newLine();
            }
        }
        // File is auto-closed here by try-with-resources
    }
}