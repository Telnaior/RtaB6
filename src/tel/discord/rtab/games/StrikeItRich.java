package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import tel.discord.rtab.Achievement;

public class StrikeItRich extends MiniGameWrapper
{
	static final String NAME = "Strike it Rich";
	static final String SHORT_NAME = "Strike";
	static final boolean BONUS = false;
	static final int BOARD_SIZE = 15;
	static final int[] VALUES = {0,5_000,50_000,500_000,5_000_000}; //Bad things happen if this isn't sorted
	static final int NEEDED_TO_WIN = (BOARD_SIZE/VALUES.length);
	int[] numberPicked = new int[VALUES.length];
	ArrayList<Integer> board = new ArrayList<>(BOARD_SIZE);
	int lastSpace;
	int lastPicked;
	int multiplier;
	boolean[] pickedSpaces = new boolean[BOARD_SIZE];
	boolean pinchMode = false;
	
	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		//Initialise board
		board.clear();
        for (int value : VALUES)
            for (int j = 0; j < NEEDED_TO_WIN; j++)
                board.add(value);
		Collections.shuffle(board);
		numberPicked = new int[VALUES.length];
		pickedSpaces = new boolean[BOARD_SIZE];
		pinchMode = false;
		multiplier = 1;
		if(enhanced)
		{
			for(int i=0; i<board.size(); i++)
				if(board.get(i) == VALUES[0])
					pickedSpaces[i] = true;
			numberPicked[0] = NEEDED_TO_WIN - 1;
		}
		//Display instructions
		output.add("In Strike it Rich, your objective is to get three of a kind.");
		output.add("Simply keep choosing numbers until you have three of the same value, and the value you've matched three of is what you win.");
		output.add("However, if you pick two of everything, we'll double all the values for your final pick!");
		output.add("The top prize is "+String.format("$%,d!",applyBaseMultiplier(2*VALUES[VALUES.length-1])));
		if(enhanced)
			output.add("ENHANCE BONUS: The $0 spaces have been completely removed.");
		output.add("Make your first pick when you are ready.");
		sendSkippableMessages(output);
		sendMessage(generateBoard());
		getInput();
	}
	
	@Override
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
			output.add(String.format("Space %d selected...",lastSpace+1));
			if(pinchMode)
				output.add("...");
			output.add(String.format("$%,d!",applyBaseMultiplier(multiplier*lastPicked)));
			numberPicked[Arrays.binarySearch(VALUES,lastPicked)] ++;
			if(numberPicked[Arrays.binarySearch(VALUES,lastPicked)] >= (NEEDED_TO_WIN-1))
				pinchMode = true;
			//Check for full count
			if(multiplier == 1)
			{
				multiplier = 2;
				for(int count : numberPicked)
					if(count < (NEEDED_TO_WIN - 1))
					{
						multiplier = 1;
						break;
					}
				//If we do have full count, mention it
				if(multiplier == 2)
					output.add("You got a full count, doubling all values!");
			}
			output.add(generateBoard());
		}
		sendMessages(output);
		if(isGameOver())
		{
			if(multiplier == 2 && lastPicked == VALUES[VALUES.length-1])
				Achievement.STRIKE_JACKPOT.check(getPlayer());
			awardMoneyWon(applyBaseMultiplier(lastPicked*multiplier));
		}
		else
			getInput();
	}
	
	boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < BOARD_SIZE && !pickedSpaces[location]);
	}

	String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("STRIKE IT RICH\n");
		for(int i=0; i<BOARD_SIZE; i++)
		{
			if(pickedSpaces[i])
			{
				display.append("  ");
			}
			else
			{
				display.append(String.format("%02d",(i+1)));
			}
			if((i%VALUES.length) == (VALUES.length-1))
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("\n");
		//Next display how many of each we have (skip the $0 if we're enhanced because it doesn't exist)
		for(int i = enhanced ? 1 : 0; i<VALUES.length; i++)
		{
			display.append(String.format("%1$dx $%2$,d\n",numberPicked[i],applyBaseMultiplier(VALUES[i]*multiplier)));
		}
		display.append("```");
		return display.toString();
	}

	private boolean isGameOver()
	{
		for(int search : numberPicked)
			if(search >= NEEDED_TO_WIN)
				return true;
		return false;
	}
	
	@Override
	public String getBotPick()
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
		//No way to give mercy in this game!
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
	public boolean isBonus()
	{
		return BONUS;
	}
	@Override
	public String getEnhanceText()
	{
		return "The $0 spaces will be removed at the start of the game.";
	}
}
