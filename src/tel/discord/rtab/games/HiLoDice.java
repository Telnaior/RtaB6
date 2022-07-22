package tel.discord.rtab.games;

import java.util.LinkedList;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.games.objs.Dice;

/**
 *
 * @author Jeffrey Hope
 */
public class HiLoDice extends MiniGameWrapper
{
    static final String NAME = "Hi/Lo Dice";
    static final String SHORT_NAME = "Hi/Lo";
    static final boolean BONUS = false;
    static final int DICE_MULTIPLIER = 10_000;
    static final int BASE_CORRECT_WIN = 50_000;
    boolean[] closedSpaces;
    Dice dice;
    boolean isAlive; 
    int score, lastRoll;
    
    @Override
    void startGame()
    {
        LinkedList<String> output = new LinkedList<>();
        
        dice = new Dice();
        closedSpaces = new boolean[dice.getDice().length * dice.getNumFaces()
                - (dice.getDice().length - 1)];
        isAlive = true;
        score = 0;
        
        output.add("In Hi/Lo Dice, the object is to predict how long you can " 
                + "roll two six-sided dice without repeating a number and "
                + "whether each roll is higher or lower than the roll before "
                + "it.");
        output.add(String.format("You will start off with $%,d times the first roll.",applyBaseMultiplier(DICE_MULTIPLIER)));
        output.add("For each successful higher/lower prediction, you'll get "
                + String.format("$%,d plus $%,d times the amount thrown.",
                		applyBaseMultiplier(BASE_CORRECT_WIN),applyBaseMultiplier(DICE_MULTIPLIER)));
        output.add("But if you predict incorrectly or you roll a number that "
                + "you've already rolled, you lose everything.");
        output.add("You are free to walk away with your winnings at any point, "
                + "however.");
        output.add("Good luck!");
        sendSkippableMessages(output);
        output.clear();
        
        dice.rollDice();
        score += dice.getDiceTotal() * applyBaseMultiplier(DICE_MULTIPLIER);
        closedSpaces[dice.getDiceTotal() - 2] = true;
        lastRoll = dice.getDiceTotal();
        output.add("You rolled: " + dice.toString());
        output.add(promptChoice());
        sendMessages(output);
        
        getInput();
    }
    
    @Override
    void playNextTurn(String pick)
    {
        LinkedList<String> output = new LinkedList<>();
        switch (pick.toUpperCase()) {
            case "STOP":
                isAlive = false;
                int rolls = 0;
                for (boolean roll : closedSpaces)
                    if (roll)
                        rolls++;
                if (rolls >= 6)
                    Achievement.HILO_JACKPOT.check(getCurrentPlayer());
                break;
            //outputResult is where all the actual logic happens
            case "HIGHER":
                output.addAll(outputResult(true));
                break;
            case "LOWER":
                output.addAll(outputResult(false));
                break;
        }
        sendMessages(output);
        if(!isAlive)
        	awardMoneyWon(score);
        else
        	getInput();
    }

    String promptChoice() {
        // Make it grammatically correct
        StringBuilder display = new StringBuilder();
        if (allSpacesClosed()) {
            display.append("You rolled all the numbers!");
            isAlive = false;
        }
        else {
            display.append(generateBoard());
        
            display.append("Type STOP to stop with your total, or you can try to "
                    + "guess whether the next roll will be HIGHER or LOWER than a");
            if (dice.getDiceTotal() == 8 || dice.getDiceTotal() == 11)
                display.append("n");
            display.append(" ").append(dice.getDiceTotal()).append(".");
        }
        return display.toString();
    }

    LinkedList<String> outputResult(boolean guessHigher) {
        LinkedList<String> output = new LinkedList<>();
        
        dice.rollDice();
        output.add("You rolled: " + dice.toString());
        if(dice.getDiceTotal() == lastRoll)
        {
        	output.add(dice.getDiceTotal() + " is not " + (guessHigher ? "higher" : "lower") + " than itself. Sorry.");
        	score = 0;
        	isAlive = false;
        }
        else if (guessHigher && dice.getDiceTotal() < lastRoll) {
            output.add(dice.getDiceTotal() + " is not higher than "
                    + lastRoll + ". Sorry.");
            score = 0;
            isAlive = false;
        }
        else if (!guessHigher && dice.getDiceTotal() > lastRoll) {
            output.add(dice.getDiceTotal() + " is not lower than "
                    + lastRoll + ". Sorry.");
            score = 0;
            isAlive = false;
        }
        else if (closedSpaces[dice.getDiceTotal() - 2]) {
            // Make the sentence grammatically correct
            String s = "You already rolled a";
            if (dice.getDiceTotal() == 8 || dice.getDiceTotal() == 11)
                s += "n";
            s += " " + dice.getDiceTotal() + ". Sorry.";
            output.add(s);
            score = 0;
            isAlive = false;
        }
        else {
            output.add("Correct prediction!");
            score += applyBaseMultiplier(BASE_CORRECT_WIN) + applyBaseMultiplier(DICE_MULTIPLIER) * dice.getDiceTotal();
            closedSpaces[dice.getDiceTotal() - 2] = true;
            lastRoll = dice.getDiceTotal();
            output.add(promptChoice());
        }
        
        return output;
    }

    String generateBoard() {
        StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("   H I / L O   D I C E\n");
		display.append("    Total: $").append(String.format("%,9d", score));
                display.append("\n\n");
                for (int i = 0; i < closedSpaces.length; i++) {
                    if (!closedSpaces[i]) {
                        display.append(i + dice.getDice().length).append(" ");
                    }
                    else {
                        display.append("  ");
                        if (i+dice.getDice().length >= 10)
                            display.append(" ");
                    }
                }
		display.append("\n```");
		return display.toString();
    }

    boolean allSpacesClosed() {
        for (boolean closedSpace : closedSpaces)
            if (!closedSpace)
                return false;
        return true;
    }

    @Override
    String getBotPick()
    {
        boolean tryHigherFirst;
        
        if (lastRoll < 7)
            tryHigherFirst = true;
        else if (lastRoll > 7)
            tryHigherFirst = false;
        else tryHigherFirst = Math.random() > 0.5;
        
        dice.rollDice();
        
        if (tryHigherFirst) {
            if (dice.getDiceTotal() > lastRoll &&
                    !closedSpaces[dice.getDiceTotal() - dice.getDice().length])
                return "HIGHER";
            else if (lastRoll != 2) {
                dice.rollDice();
                if (dice.getDiceTotal() < lastRoll &&
                        !closedSpaces[dice.getDiceTotal() -
                        dice.getDice().length])
                return "LOWER";
            }
        }
        else { // Go in the reverse order
            if (dice.getDiceTotal() < lastRoll &&
                    !closedSpaces[dice.getDiceTotal() - dice.getDice().length])
                return "LOWER";
            else if (lastRoll != 12) {
                dice.rollDice();
                if (dice.getDiceTotal() > lastRoll &&
                        !closedSpaces[dice.getDiceTotal() -
                        dice.getDice().length])
                return "HIGHER";
            }            
        }
        
        return "STOP";
    }

	@Override
	void abortGame()
	{
		//Push your luck game = auto-stop
		awardMoneyWon(score);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
	@Override
	public String getShortName()
	{
		return SHORT_NAME;
	}
	@Override
	public boolean isBonus()
	{
		return BONUS;
	}
}
