package com.collegefest;

import java.util.List;

import com.collegefest.db.ItemDAO;
import com.collegefest.models.Item;

/**
 * One-off runner: inserts 5 menu items for VEN001 into the "items"
 * collection. SAFE TO RE-RUN - it exits without touching anything if the
 * vendor already has items. (User seeding via SeedData must NOT be re-run;
 * this one is different because it checks first.)
 * Run once before the first demo: right-click -> Run As -> Java Application.
 */
public class SeedItems {

    public static void main(String[] args) {
        ItemDAO itemDAO = new ItemDAO();
        List<Item> existing = itemDAO.findByVendor("VEN001");
        if (!existing.isEmpty()) {
            System.out.println("VEN001 already has " + existing.size()
                    + " items - nothing to do.");
            return;
        }
        itemDAO.insertItem(new Item("VEN001", "Veg Biryani", 80, true, 8));
        itemDAO.insertItem(new Item("VEN001", "Masala Dosa", 60, true, 6));
        itemDAO.insertItem(new Item("VEN001", "Paneer Roll", 70, true, 5));
        itemDAO.insertItem(new Item("VEN001", "Cold Coffee", 40, true, 3));
        itemDAO.insertItem(new Item("VEN001", "Samosa",      20, true, 2));
        System.out.println("Seeded 5 menu items for VEN001. Done.");
    }
}
