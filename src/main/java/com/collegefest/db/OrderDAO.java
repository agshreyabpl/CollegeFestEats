package com.collegefest.db;

import com.collegefest.models.Order;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    private MongoCollection<Document> collection;

    public OrderDAO() {
        collection = Database.getInstance().getDb().getCollection("orders");
    }

    public void insertOrder(Order order) {
        Document doc = new Document("studentId", order.getStudentId())
                .append("vendorId", order.getVendorId())
                .append("itemNames", order.getItemNames())
                .append("status", order.getStatus())
                .append("placedAt", order.getPlacedAt())
                .append("etaMinutes", order.getEtaMinutes());
        collection.insertOne(doc);
    }

    public List<Order> findByStudent(String studentId) {
        List<Order> results = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("studentId", studentId))) {
            results.add(toOrder(doc));
        }
        return results;
    }

    public List<Order> findByVendor(String vendorId) {
        List<Order> results = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("vendorId", vendorId))) {
            results.add(toOrder(doc));
        }
        return results;
    }

    public long countActiveOrders(String vendorId) {
        return collection.countDocuments(Filters.and(
                Filters.eq("vendorId", vendorId),
                Filters.in("status", "pending", "preparing")
        ));
    }

    public void updateStatus(String orderId, String newStatus) {
        collection.updateOne(
                Filters.eq("_id", new ObjectId(orderId)),
                Updates.set("status", newStatus)
        );
    }

    private Order toOrder(Document doc) {
        Order order = new Order();
        order.setId(doc.getObjectId("_id").toString());
        order.setStudentId(doc.getString("studentId"));
        order.setVendorId(doc.getString("vendorId"));
        order.setItemNames((List<String>) doc.get("itemNames"));
        order.setStatus(doc.getString("status"));
        order.setPlacedAt(doc.getDate("placedAt"));
        Integer eta = doc.getInteger("etaMinutes");
        order.setEtaMinutes(eta == null ? 0 : eta);
        return order;
    }
}