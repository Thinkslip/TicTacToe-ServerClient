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
    private static volatile boolean isGameRunning = false; // Track if a game is in progress

    public static void main(String[] args) {
        System.out.println("Tic-Tac-Toe Server started...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
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

    private static synchronized void startNewGameIfPossible() {
        if (isGameRunning || waitingPlayers.size() < 2) {
            return; // Exit if a game is already running or not enough players
        }
    
        PlayerHandler winner = waitingPlayers.poll(); // First player (could be the returning winner)
    
        if (winner != null && !winner.isDisconnected()) {
            PlayerHandler nextPlayer = waitingPlayers.poll(); // Next available player
    
            if (nextPlayer != null && !nextPlayer.isDisconnected()) {
                System.out.println("Starting a new game.");
                isGameRunning = true;
                GameSession newGame = new GameSession(winner, nextPlayer, waitingPlayers);
                pool.execute(() -> {
                    newGame.run();
                    isGameRunning = false;
                });
            } else {
                // If there's no available second player, put the winner back
                requeuePlayer(winner);
            }
        }
    }

    private static void addPlayerToQueue(PlayerHandler player) {
        waitingPlayers.offer(player);
        player.sendMessage("w");  // Inform player they are waiting
    }

    private static void notifyQueuedPlayers() {
        List<PlayerHandler> queueSnapshot = new ArrayList<>(waitingPlayers);
        for (int i = 0; i < queueSnapshot.size(); i++) {
            PlayerHandler player = queueSnapshot.get(i);
            if (!player.isDisconnected()) {
                int newPosition = i + 1;
                if (!Objects.equals(lastKnownPositions.get(player), newPosition)) {
                    sendQueuePosition(player, newPosition);
                    lastKnownPositions.put(player, newPosition);
                }
            }
        }
        lastKnownPositions.keySet().removeIf(PlayerHandler::isDisconnected);
    }

    private static void requeuePlayer(PlayerHandler player) {
        if (!player.isDisconnected()) {
            waitingPlayers.offer(player);
        }
    }

    private static void sendQueuePosition(PlayerHandler player, int position) {
        int queueSize = Math.min(position, 255);
        player.sendMessage("Q" + (char) queueSize);
    }
}