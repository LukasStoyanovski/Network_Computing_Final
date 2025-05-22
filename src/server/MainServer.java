package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainServer {
    private static final int PORT = 9000;
    private final ServerSocket serverSocket;
    private final AuctionManager auctionManager;
    private final ScheduledExecutorService scheduler;
    private boolean isRunning;

    public MainServer() throws IOException {
        this.serverSocket = new ServerSocket(PORT);
        this.auctionManager = new AuctionManager();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.isRunning = true;
        
        // Load existing users and auctions
        loadExistingData();
        
        // Start auction monitoring
        startAuctionMonitoring();
    }

    private void loadExistingData() {
        try {
            // Load users
            for (User user : FileUtils.loadUsers()) {
                auctionManager.addUser(user);
            }
            
            // Load auctions
            for (Auction auction : FileUtils.loadAuctions()) {
                auctionManager.addAuction(auction);
            }
        } catch (IOException e) {
            System.err.println("Error loading existing data: " + e.getMessage());
        }
    }

    private void startAuctionMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAuctions();
            } catch (Exception e) {
                System.err.println("Error in auction monitoring: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private void checkAuctions() {
        for (Auction auction : auctionManager.getActiveAuctions()) {
            if (auction.shouldClose()) {
                closeAuction(auction);
            }
        }
    }

    private void closeAuction(Auction auction) {
        auction.close();
        try {
            FileUtils.saveAuction(auction);
            String message = String.format("AUCTION_CLOSED|%s|%s|%.2f",
                auction.getItemName(),
                auction.getHighestBidder(),
                auction.getCurrentPrice());
            auctionManager.broadcast(message);
        } catch (IOException e) {
            System.err.println("Error saving closed auction: " + e.getMessage());
        }
    }

    public void start() {
        System.out.println("Server started on port " + PORT);
        try {
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    public void stop() {
        isRunning = false;
        try {
            serverSocket.close();
            scheduler.shutdown();
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public static void main(String[] args) {
        try {
            MainServer server = new MainServer();
            server.start();
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }
}
