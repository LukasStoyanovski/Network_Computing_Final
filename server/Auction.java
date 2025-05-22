package server;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction implements Serializable {
    private String itemName;
    private String description;
    private String ownerUsername;
    private double startingPrice;
    private double currentPrice;
    private List<Bid> bidHistory;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isActive;
    private String winnerUsername;
    private int inactivityCountdown; // For "going once, going twice" logic

    // Constructor
    public Auction(String itemName, String description, String ownerUsername, 
                  double startingPrice, LocalDateTime endTime) {
        this.itemName = itemName;
        this.description = description;
        this.ownerUsername = ownerUsername;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.bidHistory = new ArrayList<>();
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.isActive = true;
        this.winnerUsername = null;
        this.inactivityCountdown = 0;
    }

    // Getters and Setters
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public List<Bid> getBidHistory() { return bidHistory; }
    public void setBidHistory(List<Bid> bidHistory) { this.bidHistory = bidHistory; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String winnerUsername) { this.winnerUsername = winnerUsername; }

    public int getInactivityCountdown() { return inactivityCountdown; }
    public void setInactivityCountdown(int inactivityCountdown) { this.inactivityCountdown = inactivityCountdown; }

    // Add a new bid to the auction
    public void addBid(Bid bid) {
        if (bid.getAmount() > currentPrice) {
            bidHistory.add(bid);
            currentPrice = bid.getAmount();
            winnerUsername = bid.getBidderUsername();
            inactivityCountdown = 0; // Reset inactivity countdown on new bid
        }
    }

    // Convert Auction object to string for file storage
    @Override
    public String toString() {
        return String.format("%s|%s|%s|%.2f|%.2f|%s|%s|%s|%b|%s|%d",
            itemName, description, ownerUsername, startingPrice, currentPrice,
            startTime.toString(), endTime.toString(), 
            winnerUsername != null ? winnerUsername : "NONE",
            isActive, bidHistory.toString(), inactivityCountdown);
    }

    // Create Auction object from string (for file reading)
    public static Auction fromString(String auctionString) {
        String[] parts = auctionString.split("\\|");
        if (parts.length != 11) {
            throw new IllegalArgumentException("Invalid auction string format");
        }

        Auction auction = new Auction(
            parts[0], // itemName
            parts[1], // description
            parts[2], // ownerUsername
            Double.parseDouble(parts[3]), // startingPrice
            LocalDateTime.parse(parts[6]) // endTime
        );

        auction.setCurrentPrice(Double.parseDouble(parts[4]));
        auction.setStartTime(LocalDateTime.parse(parts[5]));
        auction.setWinnerUsername(parts[7].equals("NONE") ? null : parts[7]);
        auction.setActive(Boolean.parseBoolean(parts[8]));
        // Parse bid history from string representation
        String bidHistoryStr = parts[9];
        if (!bidHistoryStr.equals("[]")) {
            // Parse bid history string and add bids
            // This is a simplified version - you might want to implement more robust parsing
            String[] bids = bidHistoryStr.substring(1, bidHistoryStr.length() - 1).split(",");
            for (String bidStr : bids) {
                if (!bidStr.trim().isEmpty()) {
                    auction.getBidHistory().add(Bid.fromString(bidStr.trim()));
                }
            }
        }
        auction.setInactivityCountdown(Integer.parseInt(parts[10]));

        return auction;
    }

    // Inner class to represent a bid
    public static class Bid implements Serializable {
        private String bidderUsername;
        private double amount;
        private LocalDateTime timestamp;

        public Bid(String bidderUsername, double amount) {
            this.bidderUsername = bidderUsername;
            this.amount = amount;
            this.timestamp = LocalDateTime.now();
        }

        public String getBidderUsername() { return bidderUsername; }
        public double getAmount() { return amount; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("%s|%.2f|%s", bidderUsername, amount, timestamp.toString());
        }

        public static Bid fromString(String bidString) {
            String[] parts = bidString.split("\\|");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid bid string format");
            }
            Bid bid = new Bid(parts[0], Double.parseDouble(parts[1]));
            bid.timestamp = LocalDateTime.parse(parts[2]);
            return bid;
        }
    }
}
