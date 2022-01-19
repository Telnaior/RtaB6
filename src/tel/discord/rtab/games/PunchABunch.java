package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

public class PunchABunch extends MiniGameWrapper {
    static final String NAME = "Punch a Bunch";
    static final String SHORT_NAME = "Punch";
    static final boolean BONUS = false;
    static final int BOARD_SIZE = 50;
    boolean isAlive;
    int score;
    ArrayList<Integer> board = new ArrayList<Integer>(BOARD_SIZE);
    boolean[] pickedSpaces = new boolean[BOARD_SIZE];

    @Override
    void startGame() {
        LinkedList<String> output = new LinkedList<>();
        
        // initialize game variables
        board.clear();
        board.addAll(Arrays.asList(
            0, 0, 0, 0, 0, // zeroes represent bombs
            10_000, 10_000, 10_000, 10_000, 10_000,
            25_000, 25_000, 25_000, 25_000, 25_000,
            50_000, 50_000, 50_000, 50_000, 50_000,
            50_000, 50_000, 50_000, 50_000, 50_000,
            100_000, 100_000, 100_000, 100_000, 100_000,
            100_000, 100_000, 100_000, 100_000, 100_000,
            250_000, 250_000, 250_000, 250_000, 250_000,
            250_000, 250_000, 250_000, 500_000, 500_000,
            500_000, 500_000, 1_000_000, 1_000_000, 2_500_000
        ));
        if(enhanced) {
            for (int i = 5; i < 10; i++) {
                board.set(i, 25_000);
            }    
        }
        for (int i = 0; i < board.size(); i++) {
            board.set(i, applyBaseMultiplier(board.get(i)));
        }
        Collections.shuffle(board);

        //Display instructions
        output.add("In Punch a Bunch, you will be making up to four punches on "
                + "a 50-space board. Each space contains either a bomb or an "
                + "amount of cash.");
        output.add("Five spaces contain bombs, "
                + String.format("five spaces contain $%,d, ", applyBaseMultiplier(10_000))
                + String.format("five spaces contain $%,d, ", applyBaseMultiplier(25_000))
                + String.format("ten spaces contain $%,d, ", applyBaseMultiplier(50_000))
                + String.format("ten spaces contain $%,d, ", applyBaseMultiplier(100_000))
                + String.format("eight spaces contain $%,d, ", applyBaseMultiplier(250_000))
                + String.format("four spaces contain $%,d, ", applyBaseMultiplier(500_000))
                + String.format("two spaces contain $%,d, ", applyBaseMultiplier(1_000_000))
                + String.format("and one space contains $%,d!", applyBaseMultiplier(2_500_000)));
        if(enhanced) {
            output.add(String.format("ENHANCE BONUS: All the $%,d spaces have been ", applyBaseMultiplier(10_000))
                    + String.format("increased to $%,d spaces.", applyBaseMultiplier(25_000)));  
        }
        output.add("After you make each punch, we'll add its value to your total.");
        output.add("You may stop after any punch, but if you draw a bomb, the "
                + "game is over and you win nothing.");
        output.add("Good luck! Make your first punch when ready.");
        sendSkippableMessages(output);
        getInput();
    }

    @Override
    void playNextTurn(String input) {
        LinkedList<String> output = new LinkedList<>();
        if (isNumber(input)) {
            int pick = Integer.parseInt(input);
            if (!checkValidNumber(pick)) {
                output.add("Invalid pick.");
            } else if (pickedSpaces[pick-1]) {
                output.add("That space has already been picked.");
            }
        }
        sendMessages(output);
        getInput();
    }

    @Override
    String getBotPick() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    void abortGame() {
        //Auto-stop, as it is a push-your-luck style game.
        awardMoneyWon(score);
    }

	private boolean checkValidNumber(int number)
	{
		return (number > 0 && number <= BOARD_SIZE);
	}

	boolean isNumber(String message) {
		try {
			//If this doesn't throw an exception we're good
			Integer.parseInt(message);
			return true;
		} catch(NumberFormatException e1) {
			return false;
		}
	}

    @Override public String getName() { return NAME; }
    @Override public String getShortName() { return SHORT_NAME; }
    @Override public boolean isBonus() { return BONUS; }
    @Override public String getEnhanceText() {
        return "The lowest five cash spaces will be increased from $10,000 to "
                + "$25,000.";
    }
}
