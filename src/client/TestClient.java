package client;

import java.net.Socket;
import java.io.*;
import java.util.Scanner;
import java.time.LocalDateTime;

public class TestClient {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Scanner scanner;
    private String username;
    private boolean isAuthenticated;
    private boolean shouldShowMenu;  // Add flag to control menu display

    public TestClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.scanner = new Scanner(System.in);
        this.isAuthenticated = false;
        this.shouldShowMenu = true;  // Initialize flag
    }

    public void start() {
        try {
            // Start message listener thread
            Thread messageListener = new Thread(this::listenForMessages);
            messageListener.start();

            // Main menu loop
            while (true) {
                if (!isAuthenticated) {
                    showAuthMenu();
                } else {
                    showMainMenu();
                }
                Thread.sleep(100); // Add small delay to prevent CPU spinning
            }
        } catch (Exception e) {
            System.err.println("Error in client: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void showAuthMenu() {
        while (true) {
            System.out.println("\n=== Auction System ===");
            System.out.println("1. Register");
            if (!isAuthenticated) {
                System.out.println("2. Login");
            }
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        handleRegister();
                        return;
                    case 2:
                        if (!isAuthenticated) {
                            handleLogin();
                            return;
                        } else {
                            System.out.println("Invalid option");
                        }
                        break;
                    case 3:
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid option. Please choose 1, 2, or 3.");
                }
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number (1, 2, or 3).");
                scanner.nextLine(); // Clear the invalid input
            }
        }
    }

    private void showMainMenu() {
        while (true) {
            System.out.println("\n=== Auction System ===");
            System.out.println("1. Post an Auction");
            System.out.println("2. Join an Auction");
            System.out.println("3. Place a Bid");
            System.out.println("4. View Active Auctions");
            System.out.println("5. Check Highest Bid");
            System.out.println("6. Withdraw from Auction");
            System.out.println("7. Disconnect");
            System.out.print("Choose an option: ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        handlePostAuction();
                        return;
                    case 2:
                        handleJoinAuction();
                        return;
                    case 3:
                        handlePlaceBid();
                        return;
                    case 4:
                        handleViewAuctions();
                        return;
                    case 5:
                        handleCheckHighestBid();
                        return;
                    case 6:
                        handleWithdrawFromAuction();
                        return;
                    case 7:
                        handleDisconnect();
                        return;
                    default:
                        System.out.println("Invalid option. Please choose a number between 1 and 7.");
                }
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number between 1 and 7.");
                scanner.nextLine(); // Clear the invalid input
            }
        }
    }

    private void handleRegister() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        System.out.print("Enter first name: ");
        String firstName = scanner.nextLine();
        System.out.print("Enter last name: ");
        String lastName = scanner.nextLine();
        System.out.print("Enter email: ");
        String email = scanner.nextLine();

        String message = String.format("REGISTER|%s|%s|%s|%s|%s|%s",
            username, password, firstName, lastName, email, socket.getLocalAddress().getHostAddress());
        out.println(message);
    }

    private void handleLogin() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        String message = String.format("LOGIN|%s|%s", username, password);
        out.println(message);
    }

    private void handlePostAuction() {
        System.out.print("Enter item name: ");
        String itemName = scanner.nextLine();
        System.out.print("Enter description: ");
        String description = scanner.nextLine();
        System.out.print("Enter starting price: ");
        double startingPrice = scanner.nextDouble();
        System.out.print("Enter duration in minutes: ");
        int durationMinutes = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        System.out.println("Choose closing mode:");
        System.out.println("1. Timer-based (closes after duration)");
        System.out.println("2. Inactivity-based (closes after 15 seconds of no bids)");
        int closingMode = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        String closingModeStr = closingMode == 1 ? "TIMER_BASED" : "INACTIVITY_BASED";
        String message = String.format("AUCTION_POST|%s|%s|%.2f|%d|%s",
            itemName, description, startingPrice, durationMinutes, closingModeStr);
        out.println(message);
    }

    private void handleJoinAuction() {
        System.out.print("Enter auction ID: ");
        String auctionId = scanner.nextLine();

        String message = String.format("AUCTION_JOIN|%s", auctionId);
        out.println(message);
    }

    private void handlePlaceBid() {
        System.out.print("Enter auction ID: ");
        String auctionId = scanner.nextLine();
        System.out.print("Enter bid amount: ");
        double bidAmount = scanner.nextDouble();
        scanner.nextLine(); // Consume newline

        String message = String.format("BID|%s|%.2f", auctionId, bidAmount);
        out.println(message);
    }

    private void handleViewAuctions() {
        out.println("LIST_AUCTIONS");
    }

    private void handleCheckHighestBid() {
        System.out.print("Enter auction ID: ");
        String auctionId = scanner.nextLine();

        String message = String.format("CHECK_HIGHEST_BID|%s", auctionId);
        out.println(message);
    }

    private void handleWithdrawFromAuction() {
        System.out.print("Enter auction ID: ");
        String auctionId = scanner.nextLine();

        String message = String.format("AUCTION_WITHDRAW|%s", auctionId);
        out.println(message);
    }

    private void handleDisconnect() {
        out.println("DISCONNECT");
        cleanup();
        System.exit(0);
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                handleServerMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Error reading from server: " + e.getMessage());
        }
    }

    private void handleServerMessage(String message) {
        String[] parts = message.split("\\|");
        String command = parts[0];

        switch (command) {
            case "WELCOME":
                System.out.println("\n" + parts[1]);
                break;
            case "GOODBYE":
                System.out.println("\n" + parts[1]);
                cleanup();
                System.exit(0);
                break;
            case "AUCTION_UPDATE":
                handleAuctionUpdate(parts);
                break;
            case "SUCCESS":
                handleSuccess(parts);
                break;
            case "ERROR":
                handleError(parts);
                break;
            case "AUCTIONS":
                handleAuctionsList(parts);
                break;
            case "AUCTION_NEW":
                handleNewAuction(parts);
                break;
            case "BID_UPDATE":
                handleBidUpdate(parts);
                break;
            case "HIGHEST_BID":
                handleHighestBid(parts);
                break;
            case "AUCTION_GOING_ONCE":
                handleGoingOnce(parts);
                break;
            case "AUCTION_GOING_TWICE":
                handleGoingTwice(parts);
                break;
            case "AUCTION_END":
                handleAuctionEnd(parts);
                break;
            default:
                System.out.println("Unknown message: " + message);
        }
    }

    private void handleAuctionUpdate(String[] parts) {
        if (parts.length != 4) {
            System.out.println("Auction update message format error");
            return;
        }

        String auctionId = parts[1];
        String itemName = parts[2];
        String message = parts[3];

        System.out.printf("\nAuction Update for %s (ID: %s):\n", itemName, auctionId);
        System.out.println(message);
    }

    private void handleSuccess(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Success message format error");
            return;
        }

        String message = parts[1];
        
        // Check if this is a login/register success
        if (message.contains("successful") && parts.length > 2) {
            if (!this.isAuthenticated) {  // Only set authentication if not already authenticated
                this.username = parts[2];
                this.isAuthenticated = true;
                System.out.println("Success: " + message);
            } else {
                System.out.println("You are already logged in as: " + this.username);
            }
        } else if (message.contains("Auction created successfully")) {
            System.out.println("Success: " + message);
            // No need to explicitly show menu as it will be shown in the next loop iteration
        } else {
            System.out.println("Success: " + message);
        }
    }

    private void handleError(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Error: Invalid error message format");
            return;
        }

        String message = parts[1];
        
        // Check if this is a login error
        if (message.contains("Invalid username or password")) {
            System.out.println("Login failed: Invalid username or password");
            this.isAuthenticated = false;
        } else if (message.contains("User not found")) {
            System.out.println("Login failed: User not found");
            this.isAuthenticated = false;
        } else {
            System.out.println("Error: " + message);
        }
    }

    private void handleAuctionsList(String[] parts) {
        if (parts.length == 1 || parts[1].equals("NONE")) {
            System.out.println("No active auctions");
            return;
        }

        System.out.println("\nActive Auctions:");
        for (int i = 1; i < parts.length; i += 8) {
            if (i + 7 >= parts.length) break; // Prevent array out of bounds
            
            String auctionId = parts[i];
            String itemName = parts[i + 1];
            String description = parts[i + 2];
            double startingPrice = Double.parseDouble(parts[i + 3]);
            double currentPrice = Double.parseDouble(parts[i + 4]);
            String seller = parts[i + 5];
            String sellerIp = parts[i + 6];
            LocalDateTime endTime = LocalDateTime.parse(parts[i + 7]);

            // Format the end time in a user-friendly way
            String formattedEndTime = endTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            System.out.printf("ID: %s\n", auctionId);
            System.out.printf("Item: %s\n", itemName);
            System.out.printf("Description: %s\n", description);
            System.out.printf("Starting Price: $%.2f\n", startingPrice);
            System.out.printf("Current Price: $%.2f\n", currentPrice);
            System.out.printf("Seller: %s (IP: %s)\n", seller, sellerIp);
            System.out.printf("Auction ends at: %s\n", formattedEndTime);
            System.out.println("-------------------");
        }
    }

    private void handleNewAuction(String[] parts) {
        if (parts.length != 6) {
            System.out.println("New auction message format error");
            return;
        }

        String auctionId = parts[1];
        String itemName = parts[2];
        String seller = parts[3];
        double startingPrice = Double.parseDouble(parts[4]);
        LocalDateTime endTime = LocalDateTime.parse(parts[5]);

        // Format the end time in a user-friendly way
        String formattedEndTime = endTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.out.printf("\nNew Auction Posted!\n");
        System.out.printf("ID: %s\n", auctionId);
        System.out.printf("Item: %s\n", itemName);
        System.out.printf("Seller: %s\n", seller);
        System.out.printf("Starting Price: $%.2f\n", startingPrice);
        System.out.printf("Auction will end at: %s\n", formattedEndTime);
    }

    private void handleBidUpdate(String[] parts) {
        if (parts.length != 5) {
            System.out.println("Bid update message format error");
            return;
        }

        String auctionId = parts[1];
        String itemName = parts[2];
        String bidder = parts[3];
        double bidAmount = Double.parseDouble(parts[4]);

        System.out.printf("\nNew Bid on %s!\n", itemName);
        System.out.printf("Auction ID: %s\n", auctionId);
        System.out.printf("Bidder: %s\n", bidder);
        System.out.printf("Amount: $%.2f\n", bidAmount);
    }

    private void handleHighestBid(String[] parts) {
        if (parts.length != 4) {
            System.out.println("Highest bid message format error");
            return;
        }

        String auctionId = parts[1];
        String bidder = parts[2];
        double amount = Double.parseDouble(parts[3]);

        System.out.printf("\nHighest Bid for Auction %s:\n", auctionId);
        if (bidder.equals("NONE")) {
            System.out.println("No bids yet");
        } else {
            System.out.printf("Bidder: %s\n", bidder);
            System.out.printf("Amount: $%.2f\n", amount);
        }
    }

    private void handleGoingOnce(String[] parts) {
        if (parts.length != 5) {
            System.out.println("Going once message format error");
            return;
        }

        String auctionId = parts[1];
        String itemName = parts[2];
        double currentPrice = Double.parseDouble(parts[3]);
        String message = parts[4];

        System.out.println("\n" + "=".repeat(50));
        System.out.println("⚠️  GOING ONCE!  ⚠️");
        System.out.println(message);
        System.out.printf("Auction ID: %s\n", auctionId);
        System.out.printf("Current Price: $%.2f\n", currentPrice);
        System.out.println("=".repeat(50) + "\n");
    }

    private void handleGoingTwice(String[] parts) {
        if (parts.length != 5) {
            System.out.println("Going twice message format error");
            return;
        }

        String auctionId = parts[1];
        String itemName = parts[2];
        double currentPrice = Double.parseDouble(parts[3]);
        String message = parts[4];

        System.out.println("\n" + "=".repeat(50));
        System.out.println("⚠️  GOING TWICE!  ⚠️");
        System.out.println(message);
        System.out.printf("Auction ID: %s\n", auctionId);
        System.out.printf("Current Price: $%.2f\n", currentPrice);
        System.out.println("=".repeat(50) + "\n");
    }

    private void handleAuctionEnd(String[] parts) {
        if (parts.length != 4) {
            System.out.println("Auction end message format error");
            return;
        }

        String auctionId = parts[1];
        String itemName = parts[2];
        String message = parts[3];

        System.out.println("\n" + "=".repeat(50));
        System.out.println("🎉  AUCTION ENDED!  🎉");
        System.out.println(message);
        System.out.printf("Auction ID: %s\n", auctionId);
        System.out.printf("Item: %s\n", itemName);
        System.out.println("=".repeat(50) + "\n");
    }

    private void cleanup() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            if (scanner != null) scanner.close();
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            TestClient client = new TestClient("localhost", 9000);
            client.start();
        } catch (IOException e) {
            System.err.println("Error starting client: " + e.getMessage());
        }
    }
} 