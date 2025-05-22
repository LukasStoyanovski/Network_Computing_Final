package server;

import java.net.Socket;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.time.LocalDateTime;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final MainServer server;
    private String username;
    private final AtomicBoolean isRunning;
    private final Timer auctionTimer;

    public ClientHandler(Socket socket, MainServer server) throws IOException {
        this.clientSocket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.server = server;
        this.isRunning = new AtomicBoolean(true);
        this.auctionTimer = new Timer();
    }

    @Override
    public void run() {
        try {
            // Send welcome message
            out.println("WELCOME|Welcome to the Auction System! You are connected from IP: " + clientSocket.getInetAddress().getHostAddress());
            
            String message;
            while ((message = in.readLine()) != null) {
                processMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split("\\|");
        String command = parts[0];

        try {
            switch (command) {
                case "REGISTER":
                    handleRegister(parts);
                    break;
                case "LOGIN":
                    handleLogin(parts);
                    break;
                case "AUCTION_POST":
                    handleAuctionPost(parts);
                    break;
                case "AUCTION_JOIN":
                    handleAuctionJoin(parts);
                    break;
                case "BID":
                    handleBid(parts);
                    break;
                case "AUCTION_WITHDRAW":
                    handleAuctionWithdraw(parts);
                    break;
                case "LIST_AUCTIONS":
                    handleListAuctions();
                    break;
                case "CHECK_HIGHEST_BID":
                    handleCheckHighestBid(parts);
                    break;
                case "DISCONNECT":
                    handleDisconnect();
                    break;
                default:
                    sendMessage("ERROR|Unknown command: " + command);
            }
        } catch (Exception e) {
            sendMessage("ERROR|" + e.getMessage());
        }
    }

    private void handleRegister(String[] parts) throws IOException {
        if (parts.length != 7) {
            throw new IllegalArgumentException("Invalid register format");
        }
        String username = parts[1];
        String password = parts[2];
        String firstName = parts[3];
        String lastName = parts[4];
        String email = parts[5];
        String ipAddress = parts[6];

        // Check if username already exists
        if (server.getAuctionManager().getUser(username) != null) {
            sendMessage("ERROR|Username already exists");
            return;
        }

        // Create and save new user
        User newUser = new User(username, password, firstName, lastName, email, ipAddress);
        FileUtils.saveUser(newUser);
        server.getAuctionManager().addUser(newUser);
        this.username = username;
        server.getAuctionManager().addClient(this);
        sendMessage("SUCCESS|Registration successful|" + username);
    }

    private void handleLogin(String[] parts) throws IOException {
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid login format");
        }
        String username = parts[1];
        String password = parts[2];
        String ipAddress = clientSocket.getInetAddress().getHostAddress();

        User user = server.getAuctionManager().getUser(username);
        if (user == null || !user.getPassword().equals(password)) {
            sendMessage("ERROR|Invalid username or password");
            return;
        }

        if (!user.getIpAddress().equals(ipAddress)) {
            sendMessage("ERROR|Login not allowed from this IP address");
            return;
        }

        this.username = username;
        server.getAuctionManager().addClient(this);
        sendMessage("SUCCESS|Login successful|" + username);
    }

    private void handleAuctionPost(String[] parts) throws IOException {
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid auction post format");
        }
        String itemName = parts[1];
        String description = parts[2];
        double startingPrice = Double.parseDouble(parts[3]);
        int durationMinutes = Integer.parseInt(parts[4]);
        Auction.AuctionClosingMode closingMode = Auction.AuctionClosingMode.valueOf(parts[5]);

        LocalDateTime endTime = java.time.LocalDateTime.now().plusMinutes(durationMinutes);
        Auction auction = new Auction(
            itemName,
            description,
            username,
            startingPrice,
            endTime,
            closingMode
        );

        // Update seller's IP address
        auction.addParticipant(username, clientSocket.getInetAddress().getHostAddress());

        FileUtils.saveAuction(auction);
        server.getAuctionManager().addAuction(auction);
        server.getAuctionManager().broadcast(String.format("AUCTION_NEW|%s|%s|%s|%.2f|%s",
            auction.getAuctionId(), itemName, username, startingPrice, endTime));
        sendMessage("SUCCESS|Auction created successfully|" + auction.getAuctionId());

        // Start auction timer if needed
        if (closingMode == Auction.AuctionClosingMode.TIMER_BASED) {
            scheduleAuctionClosing(auction);
        }
    }

    private void handleAuctionJoin(String[] parts) throws IOException {
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid auction join format");
        }
        String auctionId = parts[1];

        Auction auction = server.getAuctionManager().getAuctionById(auctionId);
        if (auction == null) {
            sendMessage("ERROR|Auction not found");
            return;
        }

        if (!auction.isActive()) {
            sendMessage("ERROR|Auction is not active");
            return;
        }

        if (auction.isParticipant(username)) {
            sendMessage("ERROR|Already participating in this auction");
            return;
        }

        auction.addParticipant(username, clientSocket.getInetAddress().getHostAddress());
        FileUtils.saveAuction(auction);
        sendMessage("SUCCESS|Joined auction successfully");
    }

    private void handleBid(String[] parts) throws IOException {
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid bid format");
        }
        String auctionId = parts[1];
        double bidAmount = Double.parseDouble(parts[2]);

        Auction auction = server.getAuctionManager().getAuctionById(auctionId);
        if (auction == null || !auction.isActive()) {
            sendMessage("ERROR|Auction not found or not active");
            return;
        }

        if (!auction.isParticipant(username)) {
            sendMessage("ERROR|Must join auction before placing bid");
            return;
        }

        if (bidAmount <= auction.getCurrentPrice()) {
            sendMessage("ERROR|Bid must be higher than current price");
            return;
        }

        if (auction.addBid(username, bidAmount)) {
            FileUtils.saveAuction(auction);

            // Notify all participants
            String bidMessage = String.format("BID_UPDATE|%s|%s|%s|%.2f",
                auctionId, auction.getItemName(), username, bidAmount);
            server.getAuctionManager().broadcastToAuction(auction, bidMessage);
            sendMessage("SUCCESS|Bid placed successfully");

            // Reset inactivity timer if needed
            if (auction.getClosingMode() == Auction.AuctionClosingMode.INACTIVITY_BASED) {
                resetInactivityTimer(auction);
            }
        } else {
            sendMessage("ERROR|Failed to place bid");
        }
    }

    private void handleAuctionWithdraw(String[] parts) throws IOException {
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid auction withdraw format");
        }
        String auctionId = parts[1];

        Auction auction = server.getAuctionManager().getAuctionById(auctionId);
        if (auction == null) {
            sendMessage("ERROR|Auction not found");
            return;
        }

        if (!auction.isParticipant(username)) {
            sendMessage("ERROR|Not participating in this auction");
            return;
        }

        if (username.equals(auction.getHighestBidder())) {
            sendMessage("ERROR|Cannot withdraw while being highest bidder");
            return;
        }

        auction.removeParticipant(username);
        FileUtils.saveAuction(auction);
        sendMessage("SUCCESS|Withdrawn from auction successfully");
    }

    private void handleCheckHighestBid(String[] parts) throws IOException {
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid check highest bid format");
        }
        String auctionId = parts[1];

        Auction auction = server.getAuctionManager().getAuctionById(auctionId);
        if (auction == null) {
            sendMessage("ERROR|Auction not found");
            return;
        }

        String highestBidder = auction.getHighestBidder();
        if (highestBidder == null) {
            sendMessage("HIGHEST_BID|" + auctionId + "|NONE|0.00");
        } else {
            sendMessage(String.format("HIGHEST_BID|%s|%s|%.2f",
                auctionId, highestBidder, auction.getCurrentPrice()));
        }
    }

    private void handleListAuctions() {
        List<Auction> activeAuctions = server.getAuctionManager().getActiveAuctions();
        if (activeAuctions.isEmpty()) {
            sendMessage("AUCTIONS|NONE");
            return;
        }

        StringBuilder response = new StringBuilder("AUCTIONS");
        for (Auction auction : activeAuctions) {
            response.append(String.format("|%s|%s|%s|%.2f|%.2f|%s|%s|%s",
                auction.getAuctionId(),
                auction.getItemName(),
                auction.getDescription(),
                auction.getStartingPrice(),
                auction.getCurrentPrice(),
                auction.getSellerUsername(),
                auction.getParticipantIp(auction.getSellerUsername()),
                auction.getEndTime()));
        }
        sendMessage(response.toString());
    }

    private void handleDisconnect() {
        try {
            // Check if user is highest bidder in any auction
            if (username != null) {
                List<Auction> activeAuctions = FileUtils.findActiveAuctions();
                for (Auction auction : activeAuctions) {
                    if (auction.getHighestBidder() != null && 
                        auction.getHighestBidder().equals(username)) {
                        out.println("ERROR|DISCONNECT|Cannot disconnect while being the highest bidder in auction: " + auction.getAuctionId());
                        return;
                    }
                }
            }

            // Send goodbye message
            out.println("GOODBYE|Thank you for using the Auction System. Goodbye!");
            
            // Remove user from all auctions
            if (username != null) {
                List<Auction> activeAuctions = FileUtils.findActiveAuctions();
                for (Auction auction : activeAuctions) {
                    auction.removeParticipant(username);
                }
            }
            
            cleanup();
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
    }

    private void scheduleAuctionClosing(Auction auction) {
        try {
            // Parse the end time
            LocalDateTime endTime = auction.getEndTime();
            LocalDateTime currentTime = LocalDateTime.now();
            
            // Calculate delay in milliseconds
            long delay = java.time.Duration.between(currentTime, endTime).toMillis();
            
            System.out.println("Current time: " + currentTime);
            System.out.println("End time: " + endTime);
            System.out.println("Delay in milliseconds: " + delay);

            if (delay <= 0) {
                // If auction should end immediately
                closeAuction(auction);
                return;
            }

            // Schedule auction closing at the end time
            auctionTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (auction.isActive()) {
                        System.out.println("Auction end time reached: " + LocalDateTime.now());
                        closeAuction(auction);
                    }
                }
            }, delay);

        } catch (Exception e) {
            System.err.println("Error scheduling auction closing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetInactivityTimer(Auction auction) {
        // Cancel any existing timer for this auction
        if (auction.getTimer() != null) {
            auction.getTimer().cancel();
        }
        
        // Create a new timer for this auction
        Timer auctionTimer = new Timer();
        auction.setTimer(auctionTimer);
        
        // Schedule going once message (5 seconds)
        auctionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (auction.isActive()) {
                    String message = String.format("AUCTION_GOING_ONCE|%s|%s|%.2f|Last bid for %s was $%.2f: going once",
                        auction.getAuctionId(),
                        auction.getItemName(),
                        auction.getCurrentPrice(),
                        auction.getItemName(),
                        auction.getCurrentPrice());
                    server.getAuctionManager().broadcast(message);
                    System.out.println("Broadcast going once message: " + message);
                }
            }
        }, 5000);

        // Schedule going twice message (10 seconds)
        auctionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (auction.isActive()) {
                    String message = String.format("AUCTION_GOING_TWICE|%s|%s|%.2f|Last bid for %s was $%.2f: going twice",
                        auction.getAuctionId(),
                        auction.getItemName(),
                        auction.getCurrentPrice(),
                        auction.getItemName(),
                        auction.getCurrentPrice());
                    server.getAuctionManager().broadcast(message);
                    System.out.println("Broadcast going twice message: " + message);
                }
            }
        }, 10000);

        // Schedule auction closing (15 seconds)
        auctionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (auction.isActive()) {
                    closeAuction(auction);
                }
            }
        }, 15000);
    }

    private void closeAuction(Auction auction) {
        if (!auction.isActive()) return;

        auction.close();
        try {
            FileUtils.saveAuction(auction);
            
            // Create a simple end message
            String winner = auction.getHighestBidder() != null ? auction.getHighestBidder() : "No winner";
            String endMessage;
            if (winner.equals("No winner")) {
                endMessage = String.format("AUCTION_END|%s|%s|The auction for %s has ended with no bids",
                    auction.getAuctionId(),
                    auction.getItemName(),
                    auction.getItemName());
            } else {
                endMessage = String.format("AUCTION_END|%s|%s|The auction for %s has ended. Winner: %s with bid: $%.2f",
                    auction.getAuctionId(),
                    auction.getItemName(),
                    auction.getItemName(),
                    winner,
                    auction.getCurrentPrice());
            }
            
            // Broadcast to all clients
            server.getAuctionManager().broadcast(endMessage);
            System.out.println("Broadcast auction end message: " + endMessage);
            
        } catch (IOException e) {
            System.err.println("Error saving closed auction: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void cleanup() {
        isRunning.set(false);
        auctionTimer.cancel();
        if (username != null) {
            server.getAuctionManager().removeClient(this);
        }
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }
}