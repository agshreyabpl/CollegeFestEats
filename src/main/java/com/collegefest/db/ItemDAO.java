package com.collegefest.db;

import com.collegefest.models.Item;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the "items" collection in MongoDB.
 * Provides: insertItem(), findByVendor(), updateAvailability()
 */
public class ItemDAO {

    private final MongoCollection<Document> collection;

    public ItemDAO() {
        this.collection = Database.getInstance()
                                  .getDb()
                                  .getCollection("items");
    }

    /**
     * Inserts a new menu item into MongoDB.
     * Sets the generated _id back onto the Item object.
     */
    public void insertItem(Item item) {
        Document doc = new Document()
                .append("vendorId",        item.getVendorId())
                .append("name",            item.getName())
                .append("price",           item.getPrice())
                .append("prepTimeMinutes", item.getPrepTimeMinutes())
                .append("available",       item.isAvailable());
        collection.insertOne(doc);
        item.setId(doc.getObjectId("_id").toHexString());
    }

    /**
     * Returns all menu items for a given vendor.
     * Called by StudentDashboard to populate the item checkbox list.
     */
    public List<Item> findByVendor(String vendorId) {
        List<Item> items = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("vendorId", vendorId))) {
            Item item = new Item();
            item.setId(doc.getObjectId("_id").toHexString());
            item.setVendorId(doc.getString("vendorId"));
            item.setName(doc.getString("name"));
            item.setPrice(doc.getDouble("price"));
            item.setPrepTimeMinutes(doc.getInteger("prepTimeMinutes", 5));
            item.setAvailable(doc.getBoolean("available", true));
            items.add(item);
        }
        return items;
    }

    /**
     * Toggles the availability of a menu item.
     * Used by vendor to mark items as sold out or back in stock.
     */
    public void updateAvailability(String itemId, boolean available) {
        collection.updateOne(
                Filters.eq("_id", new ObjectId(itemId)),
                Updates.set("available", available)
        );
    }
}
