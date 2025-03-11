import java.io.*;
import java.net.*;
import java.util.Scanner;

// Client program for the Tic-Tac-Toe game.
// Handles communication with the server, processes game messages, and manages user input.
public class Client {
    private static final String SERVER_ADDRESS = "localhost"; // Change if running on another machine
    private static final int SERVER_PORT = 9876;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Scanner scanner;
    private char playerSymbol;
    private boolean waitingForReplay = false;

    // Initializes the client, connects to the server, and starts listening for messages.
    public Client() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            scanner = new Scanner(System.in);

            // Start listening to the server in a separate thread
            new Thread(this::listenToServer).start();

            // Handle user input
            handleUserInput();
        } catch (IOException e) {
            System.out.println("Unable to connect to server.");
        }
    }

    // Listens for messages from the server and processes them.
    private void listenToServer() {
        try {
            while (true) {
                String message = input.readLine();
                if (message == null) {
                    System.out.println("Server closed connection.");
                    break;
                }
    
                processMessage(message); // Let processMessage handle everything
            }
        } catch (IOException e) {
            System.out.println("Connection closed unexpectedly.");
        }
    }
    

    // Processes messages received from the server.
    private void processMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
    
        // Handle board updates
        if (message.startsWith("square 1")) {  
            String[] parts = message.split("square [1-9]: "); 
            if (parts.length < 10) { 
                System.out.println("ERROR: Malformed board message received. Data: " + message);
                return;
            }
    
            StringBuilder boardState = new StringBuilder();
            for (int i = 1; i <= 8; i++) {  
                String squareData = parts[i].trim();
                boardState.append(squareData.isEmpty() ? " " : squareData.charAt(0));
            }
    
            String lastPart = parts[9].trim();
            if (lastPart.length() < 1) {  
                System.out.println("ERROR: Missing turn indicator. Data: " + message);
                return;
            }
    
            char turnIndicator = lastPart.charAt(lastPart.length() - 1);
            String square9Value = lastPart.length() > 1 ? lastPart.substring(0, lastPart.length() - 1).trim() : " ";
    
            boardState.append(square9Value.isEmpty() ? " " : square9Value.charAt(0));
    
            displayBoard(boardState.toString());
    
            if (turnIndicator == '1') {
                System.out.println("Your turn! Enter a move (1-9): ");
            } else {
                System.out.println("Opponent's turn. Please wait...");
            }
        } 
        else if (message.equals("x") || message.equals("o")) { // Only process once
            if (playerSymbol == '\0') {
                playerSymbol = message.charAt(0);
                System.out.println("Game starting! You are " + playerSymbol);
            }
        } 
        else if (message.equals("w")) {
            System.out.println("Waiting for another player...");
        } 
        else if (message.startsWith("W")) {  
            int streak = message.length() > 1 ? message.charAt(1) & 0xFF : 0;
            System.out.println("You won! Current streak: " + streak);
    
            if (!waitingForReplay) {
                waitingForReplay = true;
                System.out.println("Do you want to play again? (Y/N)");
            }
        } 
        else if (message.equals("L")) {
            System.out.println("You lost.");
        } 
        else if (message.equals("T")) {
            System.out.println("Game tied!");
        } 
        else if (message.equals("I")) {
            System.out.println("Invalid move. Try again.");
        } 
        else if (message.startsWith("Q")) {  
            int queueSize = message.charAt(1) & 0xFF;
            System.out.println("You are in the queue. Position: " + queueSize);
        } 
        else if (message.equals("Opponent has left the game. You win by default.")) {
            System.out.println(message);
        } 
        else if (message.equals("Do you want to play again? (Y/N)")) {
            if (!waitingForReplay) {  
                waitingForReplay = true;
                System.out.println(message);
                handleReplayInput();
            }
        }
    }

    // Handles user input for replaying a game.
    private void handleReplayInput() {
        System.out.print("Enter Y or N: ");
        try {
            String response = scanner.nextLine().trim().toUpperCase();
            if (response.equals("Y") || response.equals("N")) {
                output.println(response);
                output.flush();
                waitingForReplay = false;
            } else {
                System.out.println("Invalid input. Enter 'Y' or 'N'.");
                handleReplayInput(); // Retry input if invalid
            }
        } catch (Exception e) {
            System.err.println("Error reading replay input: " + e.getMessage());
        }
    }

    // Handles user input for game actions.
    private void handleUserInput() {
        while (true) {
            String userInput = scanner.nextLine();

            // Handle quitting
            if (userInput.equalsIgnoreCase("Q")) {
                output.println("Q");
                break;
            }

            // If waiting for replay, only allow "Y" or "N"
            if (waitingForReplay) {
                if (userInput.equalsIgnoreCase("Y") || userInput.equalsIgnoreCase("N")) {
                    output.println(userInput);
                    waitingForReplay = false; // Reset after responding
                    if (userInput.equalsIgnoreCase("N")) {
                        break; // Exit client if user declines to play again
                    }
                } else {
                    System.out.println("Invalid input. Enter 'Y' to play again or 'N' to exit.");
                }
                continue;
            }

            // Normal move input (only when the game is active)
            try {
                int move = Integer.parseInt(userInput);
                if (move < 1 || move > 9) {
                    System.out.println("Invalid input. Enter a number between 1 and 9.");
                    continue;
                }
                output.println(userInput);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a number between 1 and 9.");
            }
        }
        closeConnection();
    }

    // Displays the current game board.
    private void displayBoard(String boardState) {
        if (boardState.length() != 9) {
            System.out.println("ERROR: Invalid board format received. Data: " + boardState);
            return;
        }
    
        System.out.println("\nCurrent Board:");
        System.out.println(" " + boardState.charAt(0) + " | " + boardState.charAt(1) + " | " + boardState.charAt(2));
        System.out.println("---+---+---");
        System.out.println(" " + boardState.charAt(3) + " | " + boardState.charAt(4) + " | " + boardState.charAt(5));
        System.out.println("---+---+---");
        System.out.println(" " + boardState.charAt(6) + " | " + boardState.charAt(7) + " | " + boardState.charAt(8));
    }

    // Closes the client connection and cleans up resources.
    private void closeConnection() {
        try {
            socket.close();
            input.close();
            output.close();
            scanner.close();
        } catch (IOException e) {
            System.out.println("Error closing connection.");
        }
    }

    // Starts the client.
    public static void main(String[] args) {
        new Client();
    }
}