import java.io.*;
import java.net.*;

// Handles an individual player in the Tic-Tac-Toe game.
// Manages player communication, turn status, and win streak tracking.
public class PlayerHandler {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private char symbol; // 'X' or 'O'
    private boolean isTurn;
    private int winStreak = 0;

    // Initializes a new player connection.
    public PlayerHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.output = new PrintWriter(socket.getOutputStream(), true);
    }

    // Checks if the player is disconnected.
    public boolean isDisconnected() {
        return socket.isClosed() || !socket.isConnected();
    }

    // Increments the player's win streak, capped at 255.
    public void incrementWinStreak() {
        if (winStreak < 255) {
            winStreak++;
        }
    }

    // Resets the player's win streak to 0.
    public void resetWinStreak() {
        winStreak = 0;
    }

    // Gets the player's current win streak.
    public int getWinStreak() {
        return winStreak;
    }

    // Sets the player's symbol ('X' or 'O').
    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }

    // Gets the player's symbol.
    public char getSymbol() {
        return symbol;
    }

    // Sets whether it's the player's turn.
    public void setTurn(boolean turn) {
        this.isTurn = turn;
    }

    // Checks if it's the player's turn.
    public boolean isTurn() {
        return isTurn;
    }

    // Reads a message from the player.
    public String readMessage() throws IOException {
        return input.readLine();
    }

    // Sends a message to the player.
    public void sendMessage(String message) {
        output.println(message);
        output.flush();
    }

    // Closes the player's connection.
    public void close() throws IOException {
        socket.close();
    }
}