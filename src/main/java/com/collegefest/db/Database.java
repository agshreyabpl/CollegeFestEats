package com.collegefest.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class Database {
    private static MongoClient client;
    private static MongoDatabase db;

    public static void connect() {
        String uri = "mongodb+srv://collegefest_admin:Fest%402024@collegefest.tufvxb5.mongodb.net/?appName=CollegeFest";
        client = MongoClients.create(uri);
        db = client.getDatabase("collegefest");
        System.out.println("✅ Connected to MongoDB: " + db.getName());
    }

    public static MongoDatabase get() {
        return db;
    }
}