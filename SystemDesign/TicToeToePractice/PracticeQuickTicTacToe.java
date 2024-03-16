package SystemDesign.TicToeToePractice;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.util.*;

public class PracticeQuickTicTacToe {

    public static void main(String[] args) {
        int boardSize = 3;
        int noOfplayer =2;
        GameBoard gameBoard = new GameBoard(boardSize,getPLayer());
        gameBoard.play();

    }


    private static List<Player> getPLayer(){
        Player player1 = new Player("krishna",null,PieceType.X_PieceType);
        Player player2 = new Player("Pankaj",null,PieceType.O_PieceType);
        return new ArrayList<Player>(Arrays.asList(player1,player2));
    }

}

@AllArgsConstructor
class Player{
    private String name;
    private String status;
    private PieceType pieceType;


}

enum PieceType{
    X_PieceType,O_PieceType
}

class Cell{
    private PieceType pieceType;
    private int cellNo ;

}


class GameBoard{
    private int boardSize;
    private int noOfPlayer;
    private Cell [][]  cells;
    private Deque<Player> pLayerDeque;
    Scanner scanner = new Scanner(System.in);


    public GameBoard(int boardSize,List<Player> playerList) {
        this.boardSize = boardSize;
        cells = new Cell[boardSize][boardSize];
        playerList = playerList;
        //pLayerDeque =
    }


    public void play(){

        while(true){
            Player player = pLayerDeque.pollFirst();


        }

    }

    boolean validatePosition(int position){
        if(position<0 || position>9){
            return false;
        }
        else{
            return true;
        }
    }

    int getValidInputPosition(){
        while (true){
            int position = scanner.nextInt();
            if(!validatePosition(position)){
                return getValidInputPosition();
            }
            return position;
        }
    }
}




