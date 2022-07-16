package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import tel.discord.rtab.Achievement;

public class Spectrum extends MiniGameWrapper
{
	static final String NAME = "Spectrum";
	static final boolean BONUS = true;
	static final int BOARD_SIZE = 25;
	static final int[] VALUES = {0,1_000_000,2_000_000,3_000_000,4_000_000,5_000_000,6_000_000,
		8_000_000,11_000_000,15_000_000,20_000_000,25_000_000}; //Bad things happen if this isn't sorted
	static final int NEEDED_TO_WIN = (BOARD_SIZE/VALUES.length); //Integer division lol, 25/12 = 2
	int[] numberPicked = new int[VALUES.length];
	ArrayList<Integer> board = new ArrayList<>(BOARD_SIZE);
	int totalSum = 0;
	int lastSpace;
	int lastPicked;
	int total;
	boolean[] pickedSpaces = new boolean[BOARD_SIZE];
	
	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		//Initialise board
		board.clear();
        for (int value : VALUES) {
            totalSum += value;
            for (int j = 0; j < NEEDED_TO_WIN; j++)
                board.add(value);
        }
		//Add an extra bomb
		board.add(0);
		Collections.shuffle(board);
		numberPicked = new int[VALUES.length];
		pickedSpaces = new boolean[BOARD_SIZE];
		total = 0;
		//Streak bonus achievement
		Achievement.TWELVE.check(getCurrentPlayer());
		//Display instructions
		output.add("For reaching a streak bonus of x12, you have earned the right to play the third bonus game!");
		output.add(String.format("In Spectrum, you can win up to **$%,d**!",applyBaseMultiplier(totalSum)));
		output.add("Pairs of money are hidden on the board, along with three bombs.");
		output.add("If you make a pair, you win that amount and get to keep picking!");
		output.add("The game only ends when you make a pair of bombs.");
		output.add("Good luck, do your best to clean up the board!");
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
			numberPicked[Arrays.binarySearch(VALUES,lastPicked)] ++;
			//Print output
			output.add(String.format("Space %d selected...",lastSpace+1));
			if(numberPicked[0] >= (NEEDED_TO_WIN-1))
				output.add("...");
			if(lastPicked == 0)
				output.add("**BOMB**");
			else
				output.add(String.format("$%,d!",applyBaseMultiplier(lastPicked)));
			//Check for pairs
			if(numberPicked[Arrays.binarySearch(VALUES,lastPicked)] >= NEEDED_TO_WIN)
			{
				total += applyBaseMultiplier(lastPicked);
				if(total >= applyBaseMultiplier(totalSum))
				{
					Achievement.SPECTRUM_JACKPOT.check(getCurrentPlayer());
				}
			}
			//Print the board
			output.add(generateBoard());
		}
		sendMessages(output);
		if(isGameOver())
			awardMoneyWon(total);
		else
			getInput();
	}

	private boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < BOARD_SIZE && !pickedSpaces[location]);
	}
	
	private boolean isGameOver()
	{
		return numberPicked[0] >= NEEDED_TO_WIN;
	}
	
	private String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("   SPECTRUM   \n");
		for(int i=0; i<BOARD_SIZE; i++)
		{
			if(pickedSpaces[i])
			{
				display.append("  ");
			}
			else if(isGameOver())
			{ //Reveal what was behind each space, though this may break with large base multipliers
				if(board.get(i) == 0)
					display.append("XX");
				else
					display.append(String.format("%02d", applyBaseMultiplier(board.get(i)/1_000_000)));
			}
			else
			{
				display.append(String.format("%02d",(i+1)));
			}
			if((i%5) == 4)
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("\n");
		//Next display how many of each we have, and our total
		display.append("Total So Far: \n");
		display.append(String.format("$%,11d\n",total));
		display.append("\n");
		display.append(String.format("%dx BOMB\n",numberPicked[0]));
		for(int i=1; i<VALUES.length; i++)
		{
			if(numberPicked[i] > 0 && numberPicked[i] < NEEDED_TO_WIN)
				display.append(String.format("%1$dx $%2$,d\n",numberPicked[i],applyBaseMultiplier(VALUES[i])));
		}
		display.append("```");
		return display.toString();
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
		//Assume they bomb out immediately
		awardMoneyWon(total);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
	@Override
	public String getShortName()
	{
		return NAME; //Spectrum is already a short name
	}
	@Override
	public boolean isBonus()
	{
		return BONUS;
	}
}