package GamingBaseSkeleton;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class TicTocToeGamingBoard {
    enum Symbol {
        X, O, EMPTY
    }

    enum GameStatus {
        IN_PROGRESS, X_WON, O_WON, DRAW
    }

    @Getter
    @AllArgsConstructor
    class Player {
        private String id;
        private String name;
        private Symbol symbol;


    }

    @Getter
    @AllArgsConstructor
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



    //---------------------------------Classess-------------------------------------------
    class TicTacToeBoard implements Board{
        // Initialize all cells to EMPTY
        private final Symbol[][] grid = new Symbol[3][3];

        @Override
        public boolean isCellEmpty(int row, int col) {
            return false;
        }

        @Override
        public void move(Move move) {

        }


        @Override
        public Symbol getCell(int row, int col) {
            return null;
        }

        @Override
        public boolean isFull() {
            return false;
        }

        @Override
        public void display() {

        }
        // Implement board interface functions
    }

    class TicTacToeMoveValidator implements MoveValidator {

        public boolean isValidMove(Board board, Move move) {
            // Check row and column boundaries
            // Check whether cell is empty
            return true;
        }
    }

    class TicTacToeWinningStrategy implements WinningStrategy {

        public GameStatus checkWinner(Board board, Symbol symbol) {
            // Check 3 rows
            // Check 3 columns
            // Check 2 diagonals
            return GameStatus.IN_PROGRESS;
        }
    }



    //---------------------------Driver class/ orchestrator -----------------------------------------------

     class TicTacToeGame {

        private Board board;
        private List<Player> players;
        private int currentPlayerIndex;
        private MoveValidator moveValidator;
        private WinningStrategy winningStrategy;
        private GameStatus status;


        public void start() {

            // 1. Initialize board
            this.board = new TicTacToeBoard();

            // 2. Initialize players
            Player player1 = new Player("1", "Player 1", Symbol.X);
            Player player2 = new Player("2", "Player 2", Symbol.O);

            this.players = new ArrayList<>();
            players.add(player1);
            players.add(player2);

            // 3. Initialize move validator
            this.moveValidator = new TicTacToeMoveValidator();

            // 4. Initialize winning strategy
            this.winningStrategy = new TicTacToeWinningStrategy();

            // 5. Initialize game state
            this.currentPlayerIndex = 0;
            this.status = GameStatus.IN_PROGRESS;

            System.out.println("Tic-Tac-Toe game started!");


        }

        public void makeMove(Move move) {

            if (status != GameStatus.IN_PROGRESS) {
                throw new IllegalStateException("Game is not active");
            }

            Player currentPlayer = players.get(currentPlayerIndex);

            // 1. Validate player turn
            if (!currentPlayer.getId().equals(move.getPlayer().getId())) {
                throw new IllegalArgumentException("Not your turn");
            }

            // 2. Validate move
            if (!moveValidator.isValidMove(board, move)) {
                throw new IllegalArgumentException("Invalid move");
            }

            // 3. Execute move
            board.move(move);

            // 4. Check winner
            status = winningStrategy.checkWinner(board, currentPlayer.getSymbol());

            // 5. Check draw
            if (status == GameStatus.IN_PROGRESS && board.isFull()) {
                status = GameStatus.DRAW;
            }

            // 6. Switch turn if game continues
            if (status == GameStatus.IN_PROGRESS) {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            }
        }
    }
}





