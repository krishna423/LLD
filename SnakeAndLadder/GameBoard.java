package SnakeAndLadder;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Queue;

@Data
@AllArgsConstructor
public class GameBoard {

    private Dice dice;
    private Queue<Player> players;
    private List<Jumper> snakes;
    private List<Jumper> ladders;
    private Map<String, Integer> playerPosition;
    private int boardSize;


    public void startGame() {
        while (this.players.size() > 1) {
            System.out.println(players.size() + "   ");
            Player p = players.poll();
            int d = dice.rollDice();
            int cp = playerPosition.get(p.getPlayerName());
            int np = cp + d;
            if (np > boardSize) {

                playerPosition.replace(p.getPlayerName(), cp);
                players.add(p);
                continue;
            }
            if (np == boardSize) {
                System.out.println(p.getPlayerName() + " won the match");
                continue;
            }
            boolean snake = false;
            boolean ladder = true;

            for (Jumper j : ladders) {
                if (j.getStartPosition() == np) {
                    System.out.println(p.getPlayerName() + " beaten by the snakes");
                    np = j.getEndPosition();
                    ladder = true;
                }
            }
            for (Jumper j : snakes) {
                if (j.getStartPosition() == np) {
                    System.out.println(p.getPlayerName() + " got the ladder");
                    np = j.getEndPosition();
                    snake = true;
                }
            }
            if (np == boardSize) {
                System.out.println(p.getPlayerName() + " won the match");
            } else {
                System.out.println(p.getPlayerName() + " is at position" + np);
                playerPosition.replace(p.getPlayerName(), np);
                players.add(p);
            }


        }
    }
}
