package com.collegefest.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class Database {
    private static Database instance;
    private MongoClient client;
    private MongoDatabase db;

    private Database() {
        String uri = "mongodb+srv://collegefest_admin:Fest%402024@collegefest.tufvxb5.mongodb.net/?appName=CollegeFest";
        client = MongoClients.create(uri);
        db = client.getDatabase("collegefest");
        System.out.println("Connected to MongoDB: " + db.getName());
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public MongoDatabase getDb() {
        return db;
    }
}