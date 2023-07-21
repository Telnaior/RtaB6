package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.games.objs.Jackpots;

public class Supercash extends MiniGameWrapper
{
	static final String NAME = "Supercash";
	static final String SHORT_NAME = "Super";
	static final boolean BONUS = true;
	static final int BOARD_SIZE = 24;
	int maxValue;
	int[] values = {0,500_000,1_000_000,2_000_000,3_000_000,4_000_000,5_000_000,
			6_000_000,7_000_000,8_000_000,9_000_000,-1}; //Bad things happen if this isn't sorted
	int[] numberPicked = new int[values.length];
	int neededToWin = BOARD_SIZE/values.length;
	ArrayList<Integer> board = new ArrayList<>(BOARD_SIZE);
	int lastSpace;
	int lastPicked;
	boolean[] pickedSpaces = new boolean[BOARD_SIZE];
	
	@Override
	void startGame()
	{
		//Prepare the jackpot and apply base multiplier
		maxValue = Jackpots.SUPERCASH.getJackpot(channel);
		values[values.length-1] = maxValue;
		maxValue = applyBaseMultiplier(maxValue);
		for(int i = 0; i<values.length; i++)
			values[i] = applyBaseMultiplier(values[i]);
		//Initialise board
		board.clear();
        for (int value : values)
            for (int j = 0; j < neededToWin; j++)
                board.add(value);
		//Switch one of the lowest values for an extra copy of the highest value
		board.set(0,maxValue);
		Collections.shuffle(board);
		numberPicked = new int[values.length];
		pickedSpaces = new boolean[BOARD_SIZE];
		//Streak bonus achievement
		Achievement.FOUR.check(getPlayer());
		//Display instructions
		LinkedList<String> output = new LinkedList<>();
		output.add("For reaching a streak bonus of x4, you have earned the right to play the first bonus game!");
		output.add("In Supercash, you can win a jackpot of up to "+String.format("$%,d!",values[values.length-1]));
		output.add("Hidden on the board are three jackpot spaces, simply pick them all to win!");
		output.add("There are also other, lesser values, make a pair of those to win that amount instead.");
		output.add("Oh, and there's also a single bomb hidden somewhere on the board. If you pick that, you win nothing.");
		output.add("Best of luck! Make your first pick when you are ready.");
		sendSkippableMessages(output);
		sendMessage(generateBoard(false));
		getInput();
	}
	
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		if(!isNumber(pick))
		{
			//Random unrelated non-number doesn't need feedback
		}
		else if(!checkValidNumber(pick))
		{
			output.add("Invalid pick.");
		}
		else
		{
			lastSpace = Integer.parseInt(pick)-1;
			pickedSpaces[lastSpace] = true;
			lastPicked = board.get(lastSpace);
			numberPicked[Arrays.binarySearch(values,lastPicked)] ++;
			output.add(String.format("Space %d selected...",lastSpace+1));
			if(lastPicked == 0)
				output.add("**BOOM**");
			else
				output.add(String.format("$%,d!",lastPicked));
			output.add(generateBoard(isGameOver()));
		}
		sendMessages(output);
		if(isGameOver())
			awardMoneyWon(getMoneyWon());
		else
			getInput();
	}
	
	private boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < BOARD_SIZE && !pickedSpaces[location]);
	}

	private String generateBoard(boolean reveal)
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("    SUPERCASH    \n");
		for(int i=0; i<BOARD_SIZE; i++)
		{
			if(pickedSpaces[i])
			{
				display.append("  ");
			}
			else
			{
				//Display an icon if we're revealing the board, and this space is either the bomb or jackpot
				if(reveal && board.get(i) == 0)
					display.append("XX");
				else if(reveal && board.get(i) == maxValue)
					display.append("$$");
				else
					display.append(String.format("%02d",(i+1)));
			}
			if((i%(values.length/2)) == ((values.length/2)-1))
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("\n");
		//Next display how many of each we have
		for(int i=1; i<values.length; i++)
		{
			if(numberPicked[i] > 0)
				display.append(String.format("%1$dx $%2$,d\n",numberPicked[i],values[i]));
		}
		display.append("```");
		return display.toString();
	}

	private boolean isGameOver()
	{
		for(int i=0; i<values.length; i++)
		{
			//Lowest amount is easier to win
			if(i == 0)
			{
				if(numberPicked[i] >= (neededToWin-1))
					return true;
			}
			//Highest amount is harder to win
			else if(i == (values.length-1))
			{
				if(numberPicked[i] >= (neededToWin+1))
					return true;
			}
			//Other amounts are normal rarity
			else
			{
				if(numberPicked[i] >= neededToWin)
					return true;
			}
		}
		return false;
	}

	private int getMoneyWon()
	{
		//Return the last value selected - but before then, figure out whether we need to increment or reset the jackpot
		if(lastPicked == values[values.length-1])
		{
			Achievement.SUPERCASH_JACKPOT.check(getPlayer());
			Jackpots.SUPERCASH.resetJackpot(channel);
		}
		else
			Jackpots.SUPERCASH.addToJackpot(channel, 100_000);
		return lastPicked;
	}
	
	@Override
	String getBotPick()
	{
		ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
		for(int i=0; i<BOARD_SIZE; i++)
			if(!pickedSpaces[i])
				openSpaces.add(i+1);
		return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size())));
	}

	@Override
	void abortGame()
	{
		//Sorry, you get nothing
		awardMoneyWon(0);
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
	public boolean isBonus() {
		return BONUS;
	}
}
