package SnakeAndLadder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Dice {
    private int noOfDice;

    public int rollDice() {
        int max = 6 * noOfDice;
        int min = noOfDice;
        return (int) Math.floor((Math.random() * (max - min + 1) + min));
    }
}
