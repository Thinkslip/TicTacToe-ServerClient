import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// Tic-Tac-Toe Server
// Manages player connections, matchmaking, and game sessions.
public class Server {
    private static final int PORT = 9876;
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);
    private static final BlockingQueue<PlayerHandler> waitingPlayers = new LinkedBlockingQueue<>();
    private static final Map<PlayerHandler, Integer> lastKnownPositions = new ConcurrentHashMap<>();

    // Starts the server, listens for incoming player connections, and manages game sessions.
    public static void main(String[] args) {
        System.out.println("Tic-Tac-Toe Server started...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Background thread to start new games and notify queued players
            pool.execute(() -> {
                while (true) {
                    try {
                        startNewGameIfPossible();
                        notifyQueuedPlayers();
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            // Accept incoming player connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New player connected.");
                PlayerHandler player = new PlayerHandler(clientSocket);
                addPlayerToQueue(player);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Adds a player to the matchmaking queue.
    // If an opponent is available, starts a new game session.
    private static void addPlayerToQueue(PlayerHandler player) {
        if (!waitingPlayers.isEmpty()) { 
            PlayerHandler opponent = waitingPlayers.poll();
            if (opponent != null && !opponent.isDisconnected()) {
                System.out.println("Starting game between new player and waiting opponent.");
                GameSession game = new GameSession(player, opponent, waitingPlayers);
                pool.execute(game);
            } else {
                requeuePlayer(player);
            }
        } else {
            requeuePlayer(player);
            player.sendMessage("w");  // Send 'w' to indicate waiting for another player
        }
    }

    // Starts a new game if there are at least two players in the queue.
    // Ensures an extra waiting player is properly updated on their queue position.
    private static void startNewGameIfPossible() {
        while (waitingPlayers.size() >= 2) {
            PlayerHandler p1 = waitingPlayers.poll();
            PlayerHandler p2 = waitingPlayers.poll();
    
            if (p1 != null && p2 != null && !p1.isDisconnected() && !p2.isDisconnected()) {
                System.out.println("Starting a new game.");
                GameSession newGame = new GameSession(p1, p2, waitingPlayers);
                pool.execute(newGame);
            } else {
                if (p1 != null) requeuePlayer(p1);
                if (p2 != null) requeuePlayer(p2);
            }
        }
    }
    
    // Sends updated queue positions to all players waiting for a game.
    private static void notifyQueuedPlayers() {
        List<PlayerHandler> queueSnapshot = new ArrayList<>(waitingPlayers);
        for (int i = 0; i < queueSnapshot.size(); i++) {
            PlayerHandler player = queueSnapshot.get(i);
            if (!player.isDisconnected()) {
                int newPosition = i + 1;
                if (lastKnownPositions.get(player) == null || lastKnownPositions.get(player) != newPosition) {
                    sendQueuePosition(player, newPosition);
                    lastKnownPositions.put(player, newPosition);
                }
            }
        }
    }

    // Re-adds a player to the matchmaking queue and updates their position.
    private static void requeuePlayer(PlayerHandler player) {
        if (!player.isDisconnected()) {
            waitingPlayers.offer(player);
        }
    }

    // Sends a queue position update to a player.
    private static void sendQueuePosition(PlayerHandler player, int position) {
        int queueSize = Math.min(position, 255);  // Cap at 255
        player.sendMessage("Q" + (char) queueSize);
    }
}