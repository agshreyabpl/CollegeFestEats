package com.collegefest.db;

import com.collegefest.models.User;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class UserDAO {
    private MongoCollection<Document> collection;

    public UserDAO() {
        collection = Database.getInstance().getDb().getCollection("users");
    }

    public User findByUserId(String userId) {
        Document doc = collection.find(new Document("userId", userId)).first();
        if (doc == null) return null;

        User user = new User();
        user.setId(doc.getObjectId("_id").toString());
        user.setUserId(doc.getString("userId"));
        user.setPassword(doc.getString("password"));
        user.setName(doc.getString("name"));
        user.setRole(doc.getString("role"));
        return user;
    }

    public void insertUser(User user) {
        Document doc = new Document("userId", user.getUserId())
                .append("password", user.getPassword())
                .append("name", user.getName())
                .append("role", user.getRole());
        collection.insertOne(doc);
    }
}