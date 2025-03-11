// Represents the Tic-Tac-Toe game board.
// Manages the board state, move validation, and win condition checks.
public class Board {
    private char[] grid = new char[9]; // 1D array representing a 3x3 Tic-Tac-Toe board

    // Initializes an empty board with spaces.
    public Board() {
        for (int i = 0; i < 9; i++) {
            grid[i] = ' ';
        }
    }

    // Places a move on the board if the position is valid and empty.
    public boolean makeMove(int position, char symbol) {
        int index = position - 1; // Convert 1-based input to 0-based array index
        if (index < 0 || index > 8 || grid[index] != ' ') {
            return false; // Invalid move if out of range or position is already occupied
        }
        grid[index] = symbol;
        return true;
    }

    // Checks if the given player has won the game.
    public boolean checkWin(char symbol) {
        // All possible winning combinations (rows, columns, diagonals)
        int[][] winPatterns = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},  // Rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},  // Columns
            {0, 4, 8}, {2, 4, 6}              // Diagonals
        };

        // Check if any winning pattern is completed by the given symbol
        for (int[] pattern : winPatterns) {
            if (grid[pattern[0]] == symbol && grid[pattern[1]] == symbol && grid[pattern[2]] == symbol) {
                return true; // Winning condition met
            }
        }
        return false;
    }

    // Checks if the board is full, indicating a tie.
    public boolean isFull() {
        for (char cell : grid) {
            if (cell == ' ') {
                return false; // Empty space found, board is not full
            }
        }
        return true; // No empty spaces, game is a tie
    }

    // Returns a string representation of the board.
    public String toString() {
        return new String(grid);
    }
}    