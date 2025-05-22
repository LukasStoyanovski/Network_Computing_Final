package server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private final Map<String, User> users;
    private final Map<String, Auction> auctions;
    private final Map<String, Set<ClientHandler>> auctionParticipants;
    private final Set<ClientHandler> clients;

    public AuctionManager() {
        this.users = new ConcurrentHashMap<>();
        this.auctions = new ConcurrentHashMap<>();
        this.auctionParticipants = new ConcurrentHashMap<>();
        this.clients = Collections.synchronizedSet(new HashSet<>());
    }

    // User management
    public void addUser(User user) {
        users.put(user.getUsername(), user);
    }

    public User getUser(String username) {
        return users.get(username);
    }

    // Auction management
    public void addAuction(Auction auction) {
        auctions.put(auction.getAuctionId(), auction);
        auctionParticipants.put(auction.getAuctionId(), new HashSet<>());
    }

    public Auction getAuctionById(String auctionId) {
        return auctions.get(auctionId);
    }

    public List<Auction> getActiveAuctions() {
        return auctions.values().stream()
            .filter(Auction::isActive)
            .toList();
    }

    public boolean isHighestBidderInAnyAuction(String username) {
        return auctions.values().stream()
            .filter(Auction::isActive)
            .anyMatch(auction -> username.equals(auction.getHighestBidder()));
    }

    // Client management
    public void addClient(ClientHandler client) {
        clients.add(client);
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        // Remove from all auction participant lists
        for (Set<ClientHandler> participants : auctionParticipants.values()) {
            participants.remove(client);
        }
    }

    // Broadcast message to all clients
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void broadcastToAuction(Auction auction, String message) {
        Set<ClientHandler> participants = auctionParticipants.get(auction.getAuctionId());
        if (participants != null) {
            for (ClientHandler client : participants) {
                client.sendMessage(message);
            }
        }
    }

    public void addParticipantToAuction(String auctionId, ClientHandler client) {
        Set<ClientHandler> participants = auctionParticipants.get(auctionId);
        if (participants != null) {
            participants.add(client);
        }
    }

    public void removeParticipantFromAuction(String auctionId, ClientHandler client) {
        Set<ClientHandler> participants = auctionParticipants.get(auctionId);
        if (participants != null) {
            participants.remove(client);
        }
    }
}
