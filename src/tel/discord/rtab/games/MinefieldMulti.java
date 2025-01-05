package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import tel.discord.rtab.RtaBMath;

public class MinefieldMulti extends MiniGameWrapper
{
	static final String NAME = "Minefield Multiplier";
	static final String SHORT_NAME = "Multi";
	static final boolean BONUS = false; 
	int total;
	int stageAmount;
	int stage;
	int picks;
	List<Integer> numbers = Arrays.asList(-1,1,1,1,1,1,2,2,2,2,3,3,3,-10,4,4,4,5,6,-20,12);
	List<Integer> bombs;
	int maxBombs;
	boolean alive; //Player still alive?
	boolean stop; //Running away with the Money
	boolean[] pickedSpaces;
	
	
	/**
	 * Initialises the variables used in the minigame and prints the starting messages.
	 */
	@Override
	void startGame()
	{
		bombs = Arrays.asList(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
		total = 0;
		stage = 1;
		picks = 0;
		maxBombs = 1;
		stageAmount = stageTable(stage);
		alive = true; 
		stop = false;

		pickedSpaces = new boolean[numbers.size()];
		Collections.shuffle(numbers);

		LinkedList<String> output = new LinkedList<>();
		//Give instructions
		output.add("Welcome to Minefield Multiplier!");
		output.add("There is a multiplier hiding in each space of this 21-space board ranging from x1 to x6, with a single x12.");
		output.add("Two of the multipliers will also increase with every pick you make, and could go as high as x16.");
		output.add("In each stage of the game, you will be given a cash value which you can multiply by choosing a space off of the board.");
		output.add("There are eight stages, and each stage offers a higher cash value to multiply...");
		output.add("But more bombs will be added to the board with every stage!");
		output.add("Bombs will be randomly placed anywhere on the board, "+
			  "including on top of other bombs or already picked spaces.");
		if(enhanced)
			output.add("ENHANCE BONUS: Bombs cannot be placed on spaces x4 or greater, or on the Double Pick Count multiplier.");
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
			int lastSpace = Integer.parseInt(pick)-1;
			pickedSpaces[lastSpace] = true;
			picks ++;
			int lastPick = numbers.get(lastSpace);
			//Start printing output
			output.add(String.format("Space %d selected...",lastSpace+1));
			if(bombs.get(lastSpace) == 1 || lastPick == -1) // If it's a Bomb
			{
				output.add("...");
				output.add("**BOOM**");
				output.add("Sorry, you lose!");
				output.add(generateRevealBoard());
				alive=false;

			}
			else // If it's NOT a Bomb
			{
				if(lastPick > 3 || lastPick < 0)
					output.add("...");
				int win;
				switch(lastPick)
				{
				case -10:
					output.add("It's a **PICK COUNT** Multiplier!");
					output.add(String.format("You've made %1$d pick%2$s, so this is **x%1$d**.", picks, picks==1?"":"s"));
					win = picks * stageAmount;
					break;
				case -20:
					output.add("It's a **DOUBLE PICK COUNT** Multiplier!");
					output.add(String.format("You've made %1$d pick%2$s, so this is **x%3$d**!", picks, picks==1?"":"s", picks*2));
					win = 2 * picks * stageAmount;
					break;
				default:
					output.add("It's a " + String.format("**x%d** Multiplier!", lastPick));
					win = lastPick * stageAmount;
				}
				total = win + total; // Either way, put the total on the board.
				output.add(String.format("That makes **$%,d** for a total of **$%,d!**", win, total));
				if(stage >= 8)
				{
					stage++; //Allowing the game to detect that you've reached the end
					output.add(generateRevealBoard());
				}
				else
				{
					if(bombTable(stage+1) > 0)
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
		display.append(String.format("Bank: $%,d%n",total));
		display.append(String.format("Next pick: $%,d%n",stageAmount));
		display.append(String.format("Max bombs: %d%n",maxBombs));
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
				display.append(switch(numbers.get(i))
				{
				case -10 -> " P";
				case -20 -> "DP";
				case 12 -> "12";
				default -> String.format("x%d",numbers.get(i));
				});
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
		int value = switch (stage) {
			case 1 -> 10_000;
			case 2 -> 25_000;
			case 3 -> 50_000;
			case 4 -> 75_000;
			case 5 -> 100_000;
			case 6 -> 150_000;
			case 7 -> 200_000;
			case 8 -> 250_000;
			default -> 0;
		};
		return(applyBaseMultiplier(value));
	}

	private int bombTable(int stage)
	{
		int value = switch (stage) {
			case 1 -> 0;
			case 2 -> 0;
			case 3 -> 1;
			case 4 -> 1;
			case 5 -> 1;
			case 6 -> 2;
			case 7 -> 3;
			case 8 -> 5;
			default -> 1;
		};
		return(value);
	}

	private void increaseStage(){
		stage++;
		stageAmount = stageTable(stage);
		maxBombs = maxBombs + bombTable(stage);
		int rand;
		for(int i=0; i<bombTable(stage); i++)
		{
			do
			{
				rand = (int) (RtaBMath.random()*numbers.size()); //0-20 (21 Spaces in the Array, 0 is included*)
			}
			while(enhanced && !pickedSpaces[rand] && (numbers.get(rand) >= 4 || numbers.get(rand) == -20)); //reroll if enhance-protected
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
		//If we have too much to risk, consider stopping
		//Otherwise, consider skipping early stages
		if(total > (18-maxBombs) * stageAmount) //We picked 18 to leave a $1m threshold on the last stage
		{
			if((int)(RtaBMath.random()*10) < stage)
				return "STOP";
		}
		else if (stage < 5){
			if((int)(RtaBMath.random()*2)<1)
				return "PASS";
		}
		//If we aren't going to stop, let's just pick our next space

		ArrayList<Integer> openSpaces = new ArrayList<>(numbers.size());
		for(int i=0; i<numbers.size(); i++)
			if(!pickedSpaces[i])
				openSpaces.add(i+1);
		return String.valueOf(openSpaces.get((int)(RtaBMath.random()*openSpaces.size())));
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
	@Override
	public String getEnhanceText()
	{
		return "Bombs will not be placed on spaces x4 or greater, or the Double Pick Bonus multiplier.";
	}
}
