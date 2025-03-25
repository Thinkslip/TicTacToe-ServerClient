import java.io.IOException;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

// Handles a single Tic-Tac-Toe game session between two players.
// Manages game logic, player moves, win conditions, and reconnection logic.
public class GameSession implements Runnable {
    private PlayerHandler playerX, playerO;
    private Board board;
    private boolean running = true;
    private BlockingQueue<PlayerHandler> waitingPlayers;

    // Initializes a new game session with two players.
    public GameSession(PlayerHandler p1, PlayerHandler p2, BlockingQueue<PlayerHandler> waitingPlayers) {
        this.playerX = p1;
        this.playerO = p2;
        this.board = new Board();
        this.waitingPlayers = waitingPlayers;

        // Assign player symbols
        playerX.setSymbol('X');
        playerO.setSymbol('O');
        playerX.setTurn(true);
        playerO.setTurn(false);

        // Notify players of their symbols
        if (!playerX.isDisconnected()) playerX.sendMessage("x");
        if (!playerO.isDisconnected()) playerO.sendMessage("o");

        updateClients();
    }

    // Runs the game loop, handling turns until the game ends.
    @Override
    public void run() {
        while (running) {
            try {
                // Determine the current player and opponent
                PlayerHandler currentPlayer = playerX.isTurn() ? playerX : playerO;
                PlayerHandler opponent = playerX.isTurn() ? playerO : playerX;

                // Read player move
                String move = currentPlayer.readMessage();
                if (move == null || move.equals("Q")) {
                    handleDisconnection(currentPlayer, opponent);
                    return;
                }

                // Validate move input
                int position;
                try {
                    position = Integer.parseInt(move);
                } catch (NumberFormatException e) {
                    currentPlayer.sendMessage("I"); 
                    continue;
                }

                // Make move on the board
                if (!board.makeMove(position, currentPlayer.getSymbol())) {
                    currentPlayer.sendMessage("I"); 
                    continue;
                }

                // Check for win, draw, or continue game
                if (board.checkWin(currentPlayer.getSymbol())) {
                    handleWin(currentPlayer, opponent);
                    return;
                } else if (board.isFull()) {
                    handleDraw();
                    return;
                } else {
                    // Swap turns
                    currentPlayer.setTurn(false);
                    opponent.setTurn(true);
                }

                updateClients();
            } catch (IOException e) {
                System.out.println("Error in game session: " + e.getMessage());
                break;
            }
        }
        System.out.println("Game session ended.");
    }

    // Handles a player disconnection mid-game.
    private void handleDisconnection(PlayerHandler disconnected, PlayerHandler opponent) throws IOException {
        System.out.println("Player " + disconnected.getSymbol() + " disconnected.");
        opponent.sendMessage("Opponent has left the game. You win by default.");
        opponent.incrementWinStreak();
        opponent.sendMessage("W" + (char) opponent.getWinStreak());
    
        waitingPlayers.offer(opponent);  // Requeue opponent
        disconnected.close();
        running = false;
    }

    // Handles the game ending with a winner.
    private void handleWin(PlayerHandler winner, PlayerHandler loser) throws IOException {
        System.out.println(winner.getSymbol() + " has won the game!");
        winner.incrementWinStreak();
        loser.resetWinStreak();

        // Notify players of results
        winner.sendMessage("W" + (char) winner.getWinStreak());
        loser.sendMessage("L");

        // Ask winner if they want to play again
        winner.sendMessage("Do you want to play again? (Y/N)");
        String response = winner.readMessage();

        if (response != null && response.equalsIgnoreCase("Y")) {
            // WINNER GOES TO THE FRONT OF THE QUEUE
            synchronized (waitingPlayers) {
                List<PlayerHandler> tempQueue = new ArrayList<>(waitingPlayers);
                tempQueue.add(0, winner); // Add winner to the front
                waitingPlayers.clear();
                waitingPlayers.addAll(tempQueue);
            }
            sendQueueUpdate(winner);
        }

        if (!loser.isDisconnected()) {
            waitingPlayers.offer(loser);
            sendQueueUpdate(loser);
        }

        running = false;
    }
    
    // Handles a draw game scenario.
    // Randomly decides which player is requeued first.
    private void handleDraw() {
        System.out.println("Game ended in a draw.");
        playerX.sendMessage("T");
        playerO.sendMessage("T");
    
        // Randomly decide order for requeueing
        if (Math.random() < 0.5) {
            waitingPlayers.offer(playerX);
            waitingPlayers.offer(playerO);
        } else {
            waitingPlayers.offer(playerO);
            waitingPlayers.offer(playerX);
        }
    
        // Send immediate queue updates to both players
        sendQueueUpdate(playerX);
        sendQueueUpdate(playerO);
    
        running = false;
    }

    // Helper method to send a queue position update
    private void sendQueueUpdate(PlayerHandler player) {
        int position = Math.min(waitingPlayers.size(), 255);
        player.sendMessage("Q" + (char) position);
    }

    // Updates both players with the current board state and whose turn it is.
    private void updateClients() {
        StringBuilder state = new StringBuilder();
        char[] boardArray = board.toString().toCharArray();
    
        for (int i = 0; i < 9; i++) {
            state.append("square ").append(i + 1).append(": ").append(boardArray[i]).append(" ");
        }
    
        String formattedBoard = state.toString().trim();
        
        playerX.sendMessage(formattedBoard + (playerX.isTurn() ? " 1" : " 0"));
        playerO.sendMessage(formattedBoard + (playerO.isTurn() ? " 1" : " 0"));
    }
}