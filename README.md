
# 🧧 Auction System - Network Computing Project

> A client-server auction system built using Java sockets for the **CCS3320 - Network Computing** module at CITY College, University of York Europe Campus.

---

## 📚 Overview

This project implements a **multi-client auction platform** using the Java Sockets API. It allows users to register, login, list items for auction, place bids, and interact with other participants in real-time through TCP-based client-server communication.

---

## 🏗️ Features

### 👥 Client Functionality
- User Registration & Login (with IP validation)
- Post items for auction with description, start price, and closing rules
- View active auctions
- Register to participate in auctions
- Place and monitor bids
- Withdraw from auctions (if not highest bidder)
- Clean disconnection from server

### 🖥️ Server Functionality
- Accept multiple client connections using Java Threads
- Maintain auction sessions, participants, and bid histories
- Handle auction closing based on:
  - Fixed duration timer
  - Inactivity countdown with "Going once, going twice..." logic
- Broadcast updates to all relevant clients
- Persistent storage of users, auctions, and bid data

---

## 💡 Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java (JDK 11+) |
| Communication | TCP sockets (`java.net.Socket`, `ServerSocket`) |
| Concurrency | Java Threads (`Runnable`, `ExecutorService`) |
| Data Storage | File I/O (`BufferedWriter`, `ObjectOutputStream`) |
| IDE | IntelliJ IDEA / Eclipse |
| Diagrams | draw.io / Lucidchart |
| OS | Windows / Linux / Mac (cross-platform) |

---

## 🚀 Getting Started

### Prerequisites
- Java JDK 11 or later
- IntelliJ IDEA or Eclipse
- Git (optional)

### Running the System

1. Clone this repository:
   ```bash
   git clone https://github.com/yourusername/auction-system.git
   cd auction-system
````

2. Compile the Java code:

   ```bash
   javac -encoding UTF-8 src/server/*.java src/client/*.java
   ```

3. Start the server:

   ```bash
   java -cp src server.MainServer
   ```

4. Start one or more clients (in separate terminals or systems):

   ```bash
   java -cp src client.TestClient
   ```

---

## 🧪 Sample Commands (Client Side)

| Action       | Command Format  |           |            |            |        |       |
| ------------ | --------------- | --------- | ---------- | ---------- | ------ | ----- |
| Register     | \`REGISTER      | username  | password   | name       | email  | ...\` |
| Login        | \`LOGIN         | username  | password\` |            |        |       |
| Post Auction | \`AUCTION\_POST | itemName  | desc       | startPrice | mode\` |       |
| Place Bid    | \`BID           | auctionID | amount\`   |            |        |       |
| Disconnect   | `DISCONNECT`    |           |            |            |        |       |

📌 All messages are parsed and validated by the server.

---

## 📁 Project Structure

```
auction-system/
├── server/
│   ├── MainServer.java
│   ├── ClientHandler.java
│   ├── AuctionManager.java
│   └── ...
├── client/
│   ├── MainClient.java
│   └── ...
├── data/
│   ├── users.txt
│   ├── auctions.txt
│   └── ...
└── README.md
```

---

## 🧾 Report & Documentation

The full documentation including:

* Protocol design
* System architecture
* Design decisions
* Testing results


---


---

Let me know if you want this customized with real filenames or if you're ready for me to generate the initial folder structure and Java stubs.
```

