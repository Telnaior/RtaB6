package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import tel.discord.rtab.Achievement;

public class DoubleZeroes extends MiniGameWrapper
{
	static final String NAME = "Double Zero";
	static final String SHORT_NAME = "00";
	static final boolean BONUS = false;
	static final int PER_ZERO_PRICE = 4;
	int total;
	int digitsPicked;
	int zeroesLeft;
	List<Integer> numbers = Arrays.asList(-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,0,1,2,3,4,5,6,7,8,9);
	// -1 = Double Zero
	boolean alive;
	boolean[] pickedSpaces;
	int lastSpace;
	int lastPick;
	boolean secondChance;
	boolean zeroHit;
	
	/**
	 * Initializes the variables used in the minigame and prints the starting messages.
	 * @return A list of messages to send to the player.
	 */
	@Override
	void startGame()
	{
		alive = true;
		total = 0;
		digitsPicked = 0;
		zeroesLeft = 10; //Don't forget to update this when you change the board
		pickedSpaces = new boolean[numbers.size()];
		secondChance = false;
		Collections.shuffle(numbers);
		// Give 'em the run down
		LinkedList<String> output = new LinkedList<>();
		output.add("In Double Zeroes, you will see twenty spaces.");
		output.add("Ten of these are Double Zeroes, and the other ten are digits from 0 to 9.");
		output.add("You'll pick spaces, one at a time, until you uncover four single digits.");
		output.add("These digits will be put on the board as your bank"
				+ (applyBaseMultiplier(1_000_000) == 1_000_000 ? "." : ", which then has the base multiplier applied to it."));
		output.add("At this point, everything but the Double Zeroes turn into BOMBs!");
		output.add(String.format("You can then choose to 'STOP' and multiply your bank by %d for each Double Zero remaining...",PER_ZERO_PRICE));
		output.add("...or try to hit a Double Zero to stick that Double Zero at the end of your bank, "
				+ String.format("multiplying it by %d! Good luck!",100));
		if(enhanced)
			output.add("ENHANCE BONUS: If you miss the Double Zeroes, you'll have a second chance to find a Single Zero.");
		sendSkippableMessages(output);
		sendMessage(generateBoard());
		getInput();
	}

	/**
	 * Takes the next player input and uses it to play the next "turn" - up until the next input is required.
	 * @param  The next input sent by the player.
	 * @return A list of messages to send to the player.
	 */
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		if(pick.toUpperCase().equals("STOP"))
		{
			if(digitsPicked != 4) // Don't stop 'til you get enough, keep on!
			{
				output.add("Can't stop yet, you must pick four non-zero values first!");
			}
			else if(secondChance) //ha ha ha yeah right
			{
				output.add("You already had your chance to stop!");
			}
			else
			{
				// Player stops at the decision point? Tell 'em what they've won and end the game!
				alive = false;
				total = total * zeroesLeft * PER_ZERO_PRICE;
					output.add("Very well! Your bank is multiplied by " + String.format("%,d",zeroesLeft*PER_ZERO_PRICE)
					+ ", which means...");
			}
		}
		else if(!isNumber(pick))
		{
			//Still don't say anything for random strings
		}
		else if(!checkValidNumber(pick))
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
			if(numbers.get(lastSpace) == -1) // If it's a Double Zero...
			{
				if(digitsPicked == 4) // ...and you decided to go on, you win!
				{
					if(!secondChance)
					{
						output.add("It's a **Double Zero**!");
						output.add("Congratulations, you've won the game!");
						if(total >= applyBaseMultiplier(9000))
							Achievement.ZEROES_JACKPOT.check(getCurrentPlayer());
						alive = false;
						total *= 100;
						// No need to subtract a zero because the game's over
						// And no need to show the total because that happens at the Game Over message outside of this file
					}
					else
					{
						output.add("It's a **Single Zero**!");
						output.add("Congratulations, you're winning after all!");
						alive = false;
						total *= 10;
					}
				}
				else // ...and it's still in the first phase, keep going and remember that there's one less Double Zero.
				{
					output.add("It's a **Double Zero**.");
					zeroesLeft--;
				}
			}
			else // If it's NOT a Double Zero...
			{
				if(digitsPicked == 4) // ...and you decided to go...
				{
					//The 0 is, incidentally, also a Single Zero
					if(enhanced && numbers.get(lastSpace) == 0)
					{
						output.add("It's a **0**. Not a double zero, but good enough!");
						alive = false;
						total *= 10;
					}
					else
					{
						output.add("It's a **BOMB**.");
						if(enhanced && !secondChance)
						{
							secondChance = true;
							output.add("Now the Double Zeroes become Single Zeroes, but you've got another chance to pick one.");
							if(!zeroHit)
								zeroesLeft ++;
						}
						else
						{
							alive = false; // BOMB, shoulda taken the bribe!
							total = 0;
							output.add("Sorry, you lose.");
						}
					}
				}
				else
				{
					if (numbers.get(lastSpace) == 8) // ... and it's an 8, use an 'an'
					{
					output.add("It's an " + String.format("**%,d**!",numbers.get(lastSpace)));
					}
					else // ... and it's not an 8, use an 'a'
					{
					output.add("It's a " + String.format("**%,d**!",numbers.get(lastSpace)));
					}
					// Either way, put the total on the board by placing it in the next-left-most position, then increment
					total += Math.pow(10, digitsPicked) * numbers.get(lastSpace);
					//Remember if they hit a 0 for later when it turns into a Single Zero
					if(numbers.get(lastSpace) == 0)
						zeroHit = true;
					//If they just put a 0 in the most important space, just... ignore that
					if(digitsPicked == 3 && numbers.get(lastSpace) == 0)
						output.add("...let's just pretend you didn't pick that.");
					else
						digitsPicked++;
				}
			
			}
			if(alive)
			{

				if(digitsPicked == 4 && zeroesLeft > 0 && !secondChance) // If we just hit the 4th number, tell 'em about the DECISION~!
				{
					if(applyBaseMultiplier(total) != total)
					{
						total = applyBaseMultiplier(total);
						output.add(String.format("According to the base multiplier, your bank has been amended to $%,d.",total));
					}
					output.add("You can now choose to continue by picking a number, "
							+ "or you can type STOP to stop with your bank of " + String.format("**$%,d**",total)
							+ String.format(", times %d for the remaining Double Zeroes, "
									+ "which would give you **$%,d!**",zeroesLeft*PER_ZERO_PRICE,total*zeroesLeft*PER_ZERO_PRICE));
					output.add(generateBoard());
				}
				else if(digitsPicked == 4 && zeroesLeft == 0) // uhhhhhhhhhhhhhhhh
				{
					output.add("That's all four digits, but, uh...");
					output.add("You picked all the Double Zeroes!");
					output.add("So I hope you like the total you've got.");
					alive = false;
				}
				else // Otherwise let 'em pick another space.
				{
					output.add(generateBoard());
				}
			}
		}
		sendMessages(output);
		if(!alive)
			awardMoneyWon(total);
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
		display.append(" DOUBLE ZERO \n");
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
			if(i%5 == 4)
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("\n");
		//Next display our bank and number of Double Zeroes left
		display.append(String.format("Bank: $%,0"+(Math.max(digitsPicked,1))+"d\n",total));
		//If they're on the second chance, these are actually Single Zeroes now lolololol
		if(secondChance)
			display.append(String.format("%d Single Zeroes left\n",zeroesLeft));
		else
			display.append(String.format("%d Double Zeroes left\n",zeroesLeft));
		display.append("```");
		return display.toString();
	}
	
	@Override
	String getBotPick()
		{
			//If the game's at its decision point, make the decision
			//There should be (11 + zeroesLeft) spaces left here
			if(digitsPicked == 4 && !secondChance)
			{
				int goChance = (100 * zeroesLeft) / (11 + zeroesLeft);
				if(Math.random()*100>goChance)
					return "STOP";
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
		//Just... take the total you've got lol
		awardMoneyWon(total);
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "If you miss the Double Zeroes at the end, they become Single Zeroes for a second chance."; }
}
