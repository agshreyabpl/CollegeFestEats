package com.collegefest;

import com.collegefest.db.UserDAO;
import com.collegefest.models.User;
import org.mindrot.jbcrypt.BCrypt;

public class SeedData {
    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();

        // Create a student
        User student = new User(
            "STU001",
            BCrypt.hashpw("student123", BCrypt.gensalt()),
            "Shreya Agrawal",
            "student"
        );
        userDAO.insertUser(student);
        System.out.println("Student inserted: STU001 / student123");

        // Create a vendor
        User vendor = new User(
            "VEN001",
            BCrypt.hashpw("vendor123", BCrypt.gensalt()),
            "Rajesh Food Stall",
            "vendor"
        );
        userDAO.insertUser(vendor);
        System.out.println("Vendor inserted: VEN001 / vendor123");

        System.out.println("Done! Check MongoDB Atlas.");
    }
}