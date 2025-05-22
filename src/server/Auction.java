package server;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Auction implements Serializable {
    private final String auctionId;
    private final String itemName;
    private final String description;
    private final String sellerUsername;
    private final double startingPrice;
    private final LocalDateTime endTime;
    private double currentPrice;
    private String highestBidder;
    private final List<Bid> bidHistory;
    private boolean active;
    private int inactivityCountdown;
    private final Map<String, String> participants; // username -> ipAddress
    private final AuctionClosingMode closingMode;
    private LocalDateTime lastBidTime;
    private Timer timer;

    public enum AuctionClosingMode {
        TIMER_BASED,
        INACTIVITY_BASED
    }

    // Constructor
    public Auction(String itemName, String description, String sellerUsername, 
                  double startingPrice, LocalDateTime endTime, AuctionClosingMode closingMode) {
        this.auctionId = UUID.randomUUID().toString();
        this.itemName = itemName;
        this.description = description;
        this.sellerUsername = sellerUsername;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.endTime = endTime;
        this.bidHistory = new ArrayList<>();
        this.active = true;
        this.inactivityCountdown = 0;
        this.participants = new ConcurrentHashMap<>();
        this.closingMode = closingMode;
        this.lastBidTime = LocalDateTime.now();
        this.timer = null;
        
        // Add seller as first participant
        participants.put(sellerUsername, "NONE"); // IP will be updated when seller connects
    }

    // Getters
    public String getAuctionId() { return auctionId; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public String getSellerUsername() { return sellerUsername; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public LocalDateTime getEndTime() { return endTime; }
    public List<Bid> getBidHistory() { return bidHistory; }
    public boolean isActive() { return active; }
    public String getHighestBidder() { return highestBidder; }
    public Map<String, String> getParticipants() { return participants; }
    public AuctionClosingMode getClosingMode() { return closingMode; }
    public LocalDateTime getLastBidTime() { return lastBidTime; }
    public Timer getTimer() {
        return timer;
    }

    // Setters
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setActive(boolean active) { this.active = active; }
    public void setHighestBidder(String highestBidder) { this.highestBidder = highestBidder; }
    public void setInactivityCountdown(int count) { this.inactivityCountdown = count; }
    public void setLastBidTime(LocalDateTime time) { this.lastBidTime = time; }
    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    // Participant management
    public void addParticipant(String username, String ipAddress) {
        participants.put(username, ipAddress);
    }

    public void removeParticipant(String username) {
        participants.remove(username);
    }

    public boolean isParticipant(String username) {
        return participants.containsKey(username);
    }

    public String getParticipantIp(String username) {
        return participants.get(username);
    }

    // Add a new bid to the auction
    public boolean addBid(String username, double amount) {
        if (!active || amount <= currentPrice) {
            return false;
        }

        currentPrice = amount;
        highestBidder = username;
        lastBidTime = LocalDateTime.now();
        inactivityCountdown = 0;
        
        // Add bid to history with timestamp
        bidHistory.add(new Bid(username, amount));
        return true;
    }

    public boolean shouldClose() {
        if (!active) return false;
        
        if (closingMode == AuctionClosingMode.TIMER_BASED) {
            return LocalDateTime.now().isAfter(endTime);
        } else {
            // Check inactivity
            return inactivityCountdown >= 3;
        }
    }

    public void close() {
        active = false;
    }

    public void incrementInactivity() {
        if (active) {
            inactivityCountdown++;
        }
    }

    // Convert Auction object to string for file storage
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(auctionId).append("|")
          .append(itemName).append("|")
          .append(description).append("|")
          .append(sellerUsername).append("|")
          .append(String.format("%.2f", startingPrice)).append("|")
          .append(String.format("%.2f", currentPrice)).append("|")
          .append(endTime).append("|")
          .append(highestBidder != null ? highestBidder : "NONE").append("|")
          .append(active).append("|")
          .append(bidHistory).append("|")
          .append(inactivityCountdown).append("|")
          .append(closingMode.name()).append("|")
          .append(lastBidTime);
        return sb.toString();
    }

    // Create Auction object from string (for file reading)
    public static Auction fromString(String auctionString) {
        String[] parts = auctionString.split("\\|");
        if (parts.length < 14) {
            throw new IllegalArgumentException("Invalid auction string format");
        }

        try {
            Auction auction = new Auction(
                parts[1], // itemName
                parts[2], // description
                parts[3], // sellerUsername
                Double.parseDouble(parts[4]), // startingPrice
                LocalDateTime.parse(parts[6]), // endTime
                AuctionClosingMode.valueOf(parts[11]) // closingMode
            );

            auction.setCurrentPrice(Double.parseDouble(parts[5]));
            auction.setHighestBidder(parts[7].equals("NONE") ? null : parts[7]);
            auction.setActive(Boolean.parseBoolean(parts[8]));
            auction.setInactivityCountdown(Integer.parseInt(parts[10]));
            auction.setLastBidTime(LocalDateTime.parse(parts[12]));

            // Parse bid history from string representation
            String bidHistoryStr = parts[9];
            if (!bidHistoryStr.equals("[]")) {
                String[] bids = bidHistoryStr.substring(1, bidHistoryStr.length() - 1).split(",");
                for (String bidStr : bids) {
                    if (!bidStr.trim().isEmpty()) {
                        auction.getBidHistory().add(Bid.fromString(bidStr.trim()));
                    }
                }
            }

            return auction;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing auction: " + e.getMessage());
        }
    }

    // Inner class to represent a bid
    public static class Bid implements Serializable {
        private final String username;
        private final double amount;
        private final LocalDateTime timestamp;

        public Bid(String username, double amount) {
            this.username = username;
            this.amount = amount;
            this.timestamp = LocalDateTime.now();
        }

        public Bid(String username, double amount, LocalDateTime timestamp) {
            this.username = username;
            this.amount = amount;
            this.timestamp = timestamp;
        }

        public String getUsername() { return username; }
        public double getAmount() { return amount; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("%s|%.2f|%s", username, amount, timestamp);
        }

        public static Bid fromString(String bidString) {
            String[] parts = bidString.split("\\|");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid bid string format");
            }
            String username = parts[0];
            double amount = Double.parseDouble(parts[1]);
            LocalDateTime timestamp = LocalDateTime.parse(parts[2]);
            Bid bid = new Bid(username, amount);
            return bid;
        }
    }
}
