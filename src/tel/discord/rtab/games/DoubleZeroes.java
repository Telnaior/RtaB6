package tel.discord.rtab.games;

import java.security.SecureRandom;
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
	static final int PER_ZERO_PRICE = 5;
	static final int MAX_DIGITS = 4;
	int total;
	int digitsPicked;
	int zeroesLeft;
	List<Integer> numbers = Arrays.asList(-2,-1,-1,-1,-1,-1,-1,-1,-1,-1,0,1,2,3,4,5,6,7,8,9);
	// -1 = Double Zero, -2 = Joker Zero
	boolean alive;
	boolean[] pickedSpaces;
	int lastSpace;
	int lastPick;
	boolean jokerHit;
	
	/**
	 * Initializes the variables used in the minigame and prints the starting messages.
	 */
	@Override
	void startGame()
	{
		alive = true;
		total = 0;
		digitsPicked = 0;
		zeroesLeft = 9; //Don't forget to update this when you change the board
		pickedSpaces = new boolean[numbers.size()];
		jokerHit = false;
		if(enhanced)
		{
			numbers.set(10, -1);
			zeroesLeft ++;
		}
		Collections.shuffle(numbers);
		// Give 'em the run down
		LinkedList<String> output = new LinkedList<>();
		output.add("In Double Zeroes, you will see twenty spaces.");
		output.add("Nine of these are Double Zeroes, and ten are digits from 0 to 9.");
		output.add("There's also a Joker Zero, which if hit will add a 0 to your total WITHOUT counting as a digit.");
		output.add("You'll pick spaces, one at a time, until you uncover four single digits.");
		output.add("These digits will be put on the board as your bank"
				+ (applyBaseMultiplier(1_000_000) == 1_000_000 ? "." : ", which then has the base multiplier applied to it."));
		output.add("At this point, everything but the Double Zeroes turn into BOMBs!");
		output.add(String.format("You can then choose to 'STOP' and multiply your bank by %d for each Double Zero remaining...",PER_ZERO_PRICE));
		output.add("...or try to hit a Double Zero to stick that Double Zero at the end of your bank, "
				+ String.format("multiplying it by %d! Good luck!",100));
		if(enhanced)
			output.add("ENHANCE BONUS: The single 0 has been replaced by a tenth Double Zero.");
		sendSkippableMessages(output);
		sendMessage(generateBoard());
		getInput();
	}

	/**
	 * Takes the next player input and uses it to play the next "turn" - up until the next input is required.
	 * @param pick  The next input sent by the player.
	 */
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		if(pick.equalsIgnoreCase("STOP"))
		{
			if(digitsPicked != getMaxDigits()) // Don't stop 'til you get enough, keep on!
			{
				output.add("Can't stop yet, you must pick four non-zero values first!");
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
				if(digitsPicked == getMaxDigits()) // ...and you decided to go on, you win!
				{
					output.add("It's a **Double Zero**!");
					output.add("Congratulations, you've won the game!");
					if(total >= applyBaseMultiplier(90_000))
					{
						sendMessages(output);
						output.clear();
						Achievement.ZEROES_JACKPOT.check(getPlayer());
					}
					alive = false;
					total *= 100;
					// No need to subtract a zero because the game's over
					// And no need to show the total because that happens at the Game Over message outside of this file
				}
				else // ...and it's still in the first phase, keep going and remember that there's one less Double Zero.
				{
					output.add("It's a **Double Zero**.");
					zeroesLeft--;
				}
			}
			else // If it's NOT a Double Zero...
			{
				if(digitsPicked == getMaxDigits()) // ...and you decided to go...
				{
						alive = false; // BOMB, shoulda taken the bribe!
						total = 0;
						output.add("It's a **BOMB**.");
						output.add("Sorry, you lose.");
				}
				else if(numbers.get(lastSpace) == -2) //Joker Zero!
				{
					output.add("It's the **JOKER ZERO**!");
					jokerHit = true;
					digitsPicked++;
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
					//If they just put a 0 in the most important space, just... ignore that
					if(digitsPicked == (getMaxDigits() - 1) && numbers.get(lastSpace) == 0)
						output.add("...let's just pretend you didn't pick that.");
					else
						digitsPicked++;
				}
			
			}
			if(alive)
			{

				if(digitsPicked == getMaxDigits() && zeroesLeft > 0) // If we just hit the 4th number, tell 'em about the DECISION~!
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
				else if(digitsPicked == getMaxDigits() && zeroesLeft == 0) // uhhhhhhhhhhhhhhhh
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
	
	private int getMaxDigits()
	{
		return jokerHit ? MAX_DIGITS+1 : MAX_DIGITS;
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
		display.append(String.format("%d Double Zeroes left%n",zeroesLeft));
		display.append("```");
		return display.toString();
	}
	
	@Override
	String getBotPick()
		{
			SecureRandom r = new SecureRandom();
			//If the game's at its decision point, make the decision
			//There should be (6 + zeroesLeft) spaces left here
			if(digitsPicked == getMaxDigits())
			{
				int goChance = (100 * zeroesLeft) / (6 + zeroesLeft);
				if(r.nextDouble(100)>goChance)
					return "STOP";
			}
			//If we aren't going to stop, let's just pick our next space
			ArrayList<Integer> openSpaces = new ArrayList<>(numbers.size());
			for(int i=0; i<numbers.size(); i++)
				if(!pickedSpaces[i])
					openSpaces.add(i+1);
			return String.valueOf(openSpaces.get(r.nextInt(openSpaces.size())));
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
	@Override public String getEnhanceText() { return "The single 0 is replaced by an extra Double Zero."; }
}
