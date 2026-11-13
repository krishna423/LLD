package GamingBaseSkeleton;

public class Main {

    public static void main(String[] args) {

        TicTocToeGamingBoard outer = new TicTocToeGamingBoard();

        TicTocToeGamingBoard.TicTacToeGame game =
                outer.new TicTacToeGame();

        // Initialize all required components
        game.start();

        // Player 1 plays at (0, 0)
        // Player 2 plays at (1, 1)
        // Further moves...

    }
}
