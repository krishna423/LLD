package GamingBaseSkeleton;

import java.util.List;


public class GamingBoardBaseSkeleton {
    enum Symbol {
        X, O, EMPTY
    }

    enum GameStatus {
        IN_PROGRESS, X_WON, O_WON, DRAW
    }

    class Player {
        private String id;
        private String name;
        private Symbol symbol;
    }

    class Move {
        private Player player;
        private int row;
        private int col;
    }

    interface Board {
        boolean isCellEmpty(int row, int col);

        void move(Move move);

        Symbol getCell(int row, int col);

        boolean isFull();

        void display();
    }

    interface MoveValidator {
        boolean isValidMove(Board board, Move move);
    }

    interface WinningStrategy {
        GameStatus checkWinner(Board board, Symbol symbol);
    }

    //---------------------------Driver class/ orchestrator -----------------------------------------------

    static class TicTacToeGame {

        private Board board;
        private List<Player> players;
        private int currentPlayerIndex;
        private MoveValidator moveValidator;
        private WinningStrategy winningStrategy;
        private GameStatus status;


        public void start() {

            // 1. Initialize board
            // 2. Initialize players
            // 3. Initialize move validator
            // 4. Initialize winning strategy
            // 5. Initialize game state
            // 6. Initialize currentPlayerIndex = 0;


            System.out.println("Tic-Tac-Toe game started!");
        }

        public void makeMove(Move move) {

            // 1. Check game is still active
            // 2. Validate player turn
            // 3. Validate player move
            // 4. Execute move
            // 5. Check winning status (winner)
            // 6. Switch turn if game continues
        }
    }
}




