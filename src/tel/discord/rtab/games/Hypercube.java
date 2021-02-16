package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import tel.discord.rtab.Achievement;

public class Hypercube extends MiniGameWrapper
{
	static final String NAME = "Hypercube";
	static final String SHORT_NAME = "Hyper^3";
	static final boolean BONUS = true;
	static final int MAX_PICKS = 10;
	static final int MIN_NUM = 51;
	static final int MAX_NUM = 99;
	static final int BOMBS = 15;
	static final int BOARD_SIZE = (MAX_NUM-MIN_NUM+1)+BOMBS;
	int picksUsed;
	int total;
	ArrayList<Integer> board = new ArrayList<Integer>(BOARD_SIZE);
	boolean[] pickedSpaces = new boolean[BOARD_SIZE];
	int lastSpace;
	int lastPicked;

	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		//Initialise board
		board.clear();
		//Add the numbers
		for(int i=MIN_NUM; i<=MAX_NUM; i++)
			board.add(i);
		//Add the zeroes and bombs too
		Integer[] bombBlock = new Integer[BOMBS];
		Arrays.fill(bombBlock,0);
		board.addAll(Arrays.asList(bombBlock));
		Collections.shuffle(board);
		pickedSpaces = new boolean[BOARD_SIZE];
		picksUsed = 0;
		total = 0;
		//Display instructions
		output.add("For reaching a streak bonus of x16, you have earned the right to play the fourth bonus game!");
		output.add("In Hypercube, you can win hundreds of millions of dollars!");
		output.add("You have ten picks to build up the largest total you can by selecting the largest numbers.");
		output.add("But if you find one of the fifteen bombs, your total will be reset to zero!");
		output.add("Once you've made all ten picks, your total is cubed and you win the result in cash.");
		output.add("Good luck, go for a top score!");
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
			if(lastPicked == 0)
				total = 0;
			else
			{
				total += lastPicked;
				picksUsed ++;
			}
			//Print output
			output.add(String.format("Space %d selected...",lastSpace+1));
			output.add("...");
			if(lastPicked == 0)
				output.add("**BOOM**");
			else
				output.add(String.format("**%2d**",lastPicked));
			output.add(generateBoard());
		}
		sendMessages(output);
		if(isGameOver())
		{
			if(total >= 500)
				Achievement.HYPERCUBE_JACKPOT.award(getCurrentPlayer());
			awardMoneyWon(getMoneyWon());
		}
		else
			getInput();
	}
	
	private boolean isGameOver()
	{
		return picksUsed >= MAX_PICKS;
	}

	private boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < BOARD_SIZE && !pickedSpaces[location]);
	}

	private String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("   H Y P E R C U B E   \n");
		for(int i=0; i<BOARD_SIZE; i++)
		{
			if(pickedSpaces[i])
			{
				display.append("  ");
			}
			else if(isGameOver())
			{ //Reveal everything once the game is over
				if(board.get(i) == 0)
					display.append("XX");
				else
					display.append(String.format("%02d", board.get(i)));
			}
			else
			{
				display.append(String.format("%02d", (i+1)));
			}
			if((i%Math.sqrt(BOARD_SIZE)) == (Math.sqrt(BOARD_SIZE)-1))
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("\n");
		//Next display our total and the cash it converts to
		display.append(String.format("   Total So Far: %03d   \n",total));
		display.append(String.format("     $ %,11d     \n", getMoneyWon()));
		if(picksUsed == (MAX_PICKS-1))
			display.append(String.format("    %02d Pick Remains    \n",(MAX_PICKS-picksUsed)));
		else
			display.append(String.format("    %02d Picks Remain    \n",(MAX_PICKS-picksUsed)));
		display.append("```");
		return display.toString();
	}

	private int getMoneyWon()
	{
		return applyBaseMultiplier((int)Math.pow(total,3));
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
		awardMoneyWon(0); //Unfortunate
	}

	@Override
	public String getName()
	{
		return NAME;
	}
	@Override
	public String getShortName()
	{
		return SHORT_NAME; //Hypercube is already a short name, and the old one "H^3" wasn't searchable
	}
	@Override
	public boolean isBonus()
	{
		return BONUS;
	}
}
