package server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.*;
import java.util.stream.Collectors;

public class FileUtils {
    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = DATA_DIR + "/users.txt";
    private static final String AUCTIONS_FILE = DATA_DIR + "/auctions.txt";

    // Initialize data directory and files
    static {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            createFileIfNotExists(USERS_FILE);
            createFileIfNotExists(AUCTIONS_FILE);
        } catch (IOException e) {
            System.err.println("Error initializing data directory: " + e.getMessage());
        }
    }

    // User-related file operations
    public static void saveUser(User user) throws IOException {
        List<User> users = loadUsers();
        // Check if user already exists
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equals(user.getUsername())) {
                users.set(i, user); // Update existing user
                writeUsers(users);
                return;
            }
        }
        // Add new user
        users.add(user);
        writeUsers(users);
    }

    public static List<User> loadUsers() throws IOException {
        List<User> users = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        users.add(User.fromString(line));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Error parsing user: " + e.getMessage());
                    }
                }
            }
        }
        return users;
    }

    // Auction-related file operations
    public static void saveAuction(Auction auction) throws IOException {
        List<Auction> auctions = loadAuctions();
        boolean found = false;
        
        for (int i = 0; i < auctions.size(); i++) {
            if (auctions.get(i).getItemName().equals(auction.getItemName()) &&
                auctions.get(i).getSellerUsername().equals(auction.getSellerUsername())) {
                auctions.set(i, auction);
                found = true;
                break;
            }
        }
        
        if (!found) {
            auctions.add(auction);
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(AUCTIONS_FILE))) {
            for (Auction a : auctions) {
                writer.write(a.toString());
                writer.newLine();
            }
        }
    }

    public static List<Auction> loadAuctions() throws IOException {
        List<Auction> auctions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(AUCTIONS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        auctions.add(Auction.fromString(line));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Error parsing auction: " + e.getMessage());
                    }
                }
            }
        }
        return auctions;
    }

    // Helper methods
    private static void createFileIfNotExists(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    private static void writeUsers(List<User> users) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User user : users) {
                writer.write(user.toString());
                writer.newLine();
            }
        }
    }

    private static void writeAuctions(List<Auction> auctions) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(AUCTIONS_FILE))) {
            for (Auction auction : auctions) {
                writer.write(auction.toString());
                writer.newLine();
            }
        }
    }

    // Utility methods for finding specific data
    public static User findUserByUsername(String username) throws IOException {
        List<User> users = loadUsers();
        return users.stream()
                   .filter(user -> user.getUsername().equals(username))
                   .findFirst()
                   .orElse(null);
    }

    public static List<Auction> findAuctionsByOwner(String ownerUsername) throws IOException {
        return loadAuctions().stream()
                      .filter(auction -> auction.getSellerUsername().equals(ownerUsername))
                      .collect(Collectors.toList());
    }

    public static List<Auction> findActiveAuctions() throws IOException {
        List<Auction> auctions = loadAuctions();
        return auctions.stream()
                      .filter(Auction::isActive)
                      .toList();
    }
} 