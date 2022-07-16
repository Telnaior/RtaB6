package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MinefieldMulti extends MiniGameWrapper
{
	static final String NAME = "Minefield Multiplier";
	static final String SHORT_NAME = "Multi";
	static final boolean BONUS = false; 
	int total;
	int stageAmount;
	int stage;
	List<Integer> numbers = Arrays.asList(-1,1,1,1,1,1,2,2,2,2,2,3,3,3,3,4,4,4,5,5,10);
	List<Integer> bombs;
	int maxBombs;
	boolean alive; //Player still alive?
	boolean stop; //Running away with the Money
	boolean[] pickedSpaces;
	int lastSpace;
	int lastPick;
	
	
	/**
	 * Initialises the variables used in the minigame and prints the starting messages.
	 */
	@Override
	void startGame()
	{
		bombs = Arrays.asList(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
		total = 0;
		stage = 1;
		maxBombs = 1;
		stageAmount = stageTable(stage);
		alive = true; 
		stop = false;

		pickedSpaces = new boolean[numbers.size()];
		Collections.shuffle(numbers);

		LinkedList<String> output = new LinkedList<>();
		//Give instructions
		output.add("Welcome to Minefield Multiplier!");
		output.add("There is a multiplier hiding in each space of this 21-space board ranging from x1 to x5, along witha single x10.");
		output.add("In each stage of the game, you will be given a cash value which you can multiply by choosing a space off the board.");
		output.add("There are eight stages, and each stage offers a higher cash value to multiply...");
		output.add("But more bombs will be added to the board in every stage as well!");
		output.add("Bombs will be randomly placed anywhere on the board, "+
			  "including on top of other bombs or already picked spaces.");
		output.add("You can **STOP** after each round with your current total bank,"
				+ "or **PASS** to skip the current stage without gaining anything and advance to the next one without risk.");
		output.add("Of course, if you pick a bomb at any point, you lose everything."); //~Duh
		sendSkippableMessages(output);
		sendMessage(generateBoard());
		getInput();
	}

	/**
	 * Takes the next player input and uses it to play the next "turn" - up until the next input is required.
	 * @param pick The next input sent by the player.
	 */
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		

		String choice = pick.toUpperCase();
		choice = choice.replaceAll("\\s","");
		if(choice.equals("ACCEPT") || choice.equals("DEAL") || choice.equals("TAKE") || choice.equals("STOP"))
		{
			// Player stops 
			stop = true;
			output.add(String.format("Very well! You escaped with your bank of $%,d.",total));
			output.add(generateRevealBoard());
		}
		else if(choice.equals("PASS")){
			if (stage >= 8) {
				output.add("You can't pass the last stage. Either **STOP** or pick your space!");
			}
			else {
				output.add(String.format("Stage passed. **%d** bomb"+ (bombTable(stage+1) != 1 ? "s were" : " was")
						+" added!", bombTable(stage + 1)));
				increaseStage();
				output.add(generateBoard());
			}
		}
		else if(!isNumber(choice))
		{
			//Still don't say anything for random strings
		}
		else if(!checkValidNumber(choice))
		{
			// EASTER EGG! Take the RTaB Challenge!
			// Hit this message 29,998,559,671,349 times in a row
			output.add("Invalid pick.");
			// and you win a free kick from the server
		}
		else
		{
			lastSpace = Integer.parseInt(pick)-1;
			pickedSpaces[lastSpace] = true;
			lastPick = numbers.get(lastSpace);
			//Start printing output
			output.add(String.format("Space %d selected...",lastSpace+1));
			if(bombs.get(lastSpace) == 1 || lastPick == -1) // If it's a Bomb
			{
				output.add("**BOOM**");
				output.add("Sorry, you lose!");
				output.add(generateRevealBoard());
				alive=false;

			}
			else // If it's NOT a Bomb
			{
				int win = 0;
				win = lastPick * stageAmount;
				total = win + total; // Either way, put the total on the board.

				output.add("It's a " + String.format("**x%d** Multiplier!", lastPick));
				output.add(String.format("That makes **$%,d** for a total of **$%,d!**", win, total));
				if(stage >= 8)
				{
					stage++; //Allowing the game to detect that you've reached the end
					output.add(generateRevealBoard());
				}
				else
				{
					output.add(String.format("**%d** bomb"+ (bombTable(stage+1) != 1 ? "s have" : " has")
							+" been added!", bombTable(stage + 1)));
					increaseStage();
					output.add(generateBoard());
				}
			}
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
		return (location >= 0 && location < numbers.size() && !pickedSpaces[location]);
	}

	private String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("MINEFIELD MULTIPLIER\n");
		for(int i=0; i<numbers.size(); i++)
		{
			if(pickedSpaces[i])
			{
				display.append("  ");
			}
			else
			{
				display.append(String.format("%02d",(i+1)));
			}
			if(i%7 == 6)
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("\n");
		//Next display Bank, StageAmount and Number of Max Bombs 
		display.append(String.format("Bank: $%,d\n",total));
		display.append(String.format("Next pick: $%,d\n",stageAmount));
		display.append(String.format("Max bombs: %d\n",maxBombs));
		display.append("```");
		return display.toString();
	}

	private String generateRevealBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("MINEFIELD MULTIPLIER\n");
		for(int i=0; i<numbers.size(); i++)
		{
			if(pickedSpaces[i])
			{
				display.append("  ");
			}
			else if(bombs.get(i) == 1 || numbers.get(i)== -1)
			{
				display.append("XX");
			}
			else
			{
				if (numbers.get(i) == 10){
					display.append(String.format("%d",numbers.get(i)));
				}
				else{
					display.append(String.format("x%d",numbers.get(i)));
				}
			}
			if(i%7 == 6)
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("```");
		return display.toString();
	}

	private int stageTable(int stage)
	{
		int value = 0;
		switch (stage)
		{
			case 1:
				value = 10000;
				break;
			case 2:
				value = 20000;
				break;
			case 3:
				value = 30000;
				break;
			case 4:
				value = 40000;
				break;
			case 5:
				value = 50000;
				break;
			case 6:
				value = 75000;
				break;
			case 7:
				value = 100000;
				break;
			case 8:
				value = 200000;
				break;
		}
		return(applyBaseMultiplier(value));
	}

	private int bombTable(int stage)
	{
		int value = 0;
		switch (stage)
		{
			case 6:
				value = 2;
				break;
			case 7:
				value = 3;
				break;
			case 8:
				value = 5;
				break;
			default:
				value = 1;
				break;
		}
		return(value);
	}

	private void increaseStage(){
		stage++;
		stageAmount = stageTable(stage);
		maxBombs = maxBombs + bombTable(stage);
		for(int i=0; i<bombTable(stage); i++)
		{
			int rand = (int) (Math.random()*numbers.size()); //0-20 (21 Spaces in the Array, 0 is included*)
			bombs.set(rand, 1);
		}
	}
	/**
	 * Returns true if the minigame has ended
	 */
	private boolean isGameOver(){
		return stop || !alive || stage >=9;
	}


	/**
	 * Returns an int containing the player's winnings, pre-booster.
	 */
	private int getMoneyWon()
	{
		return alive ? total : 0;
	}
	
	@Override
	public String getBotPick()
	{
		//If there are more than 9 Bombs and he won more than 100k
		//Let him flip a coin to decide if he wants to continue
		if(maxBombs > 5 || total > 100000)
		{
			if((int)(Math.random()*2)< 1)
				return "STOP";
		}
		else if (stageAmount<=40000){
			if((int)(Math.random()*2)<1)
				return "PASS";
		}
		//If we aren't going to stop, let's just pick our next space

		ArrayList<Integer> openSpaces = new ArrayList<>(numbers.size());
		for(int i=0; i<numbers.size(); i++)
			if(!pickedSpaces[i])
				openSpaces.add(i+1);
		return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size())));
	}

	@Override
	void abortGame()
	{
		//Push your luck game = auto-stop
		awardMoneyWon(getMoneyWon());
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
