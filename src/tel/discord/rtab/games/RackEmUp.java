package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.RtaBMath;

public class RackEmUp extends MiniGameWrapper
{
	static final String NAME = "Rack 'em up";
	static final String SHORT_NAME = "Rack";
	static final boolean BONUS = false; 
	
	List<Integer> cardNumbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
	//first 8 will be the single cards, 9-10 and 11-12 will be double cards, 13-15 will be triple
	List<Integer> spaceType = Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17, 20, 21, 3, 8, -1, -2, -3);
	//10-17 = single cards (from 0-7 in cardNumbers)
	//20-21 = double cards (8 and 9, then 10 and 11 in cardNumbers)
	//3 = triple card, 8 = elevator, -1 -2 -3 are numbered bombs
	
	boolean alive; //Player still alive?
	boolean stop; //Has the player called it quits?
	
	boolean[] pickedSpaces;
	//for board spaces picked
	boolean[] pickedCards;
	//for space types picked
	boolean[] pickedBombs;
	//for numbered bombs picked
	boolean[] earnedCards;
	//for cards earned
	int streakMoney = 100_000;
	int elevator = 10_000;
	int roundNumber;
	int lastSpace;
	int lastPick;
	
	/**
	 * Initialises the variables used in the minigame and prints the starting messages.
	 */
	@Override
	void startGame()
	{
		roundNumber = 0;
		lastSpace = -1;
		lastPick = -1;
		alive = true; 
		stop = false;

		pickedSpaces = new boolean[cardNumbers.size()];
		pickedCards = new boolean[cardNumbers.size()];
		pickedBombs = new boolean[3];
		earnedCards = new boolean[cardNumbers.size()];
		Collections.shuffle(cardNumbers);
		
		streakMoney = applyBaseMultiplier(streakMoney);
		elevator = applyBaseMultiplier(elevator);
		
		
		LinkedList<String> output = new LinkedList<>();
		//Give instructions
		output.add("Welcome to Rack 'em Up!");
		output.add("There are fifteen cards, numbered 1 to 15, on this 15 space board.");
		output.add("Eight spaces hold a single card, two hold two cards, and one space holds three cards!");
		output.add("For each consecutive numbered card you have in your largest streak, you'll earn $%,12d%n!", streakMoney);
		output.add("Plus, one space has an Elevator, which increases that amount by $%,12d%n, multiplied by the pick number on which you select it!", elevator);
		output.add("The other three spaces, however, have bombs numbered 1, 2, and 3. If you pick bombs with two consecutive numbers, you win nothing.");
		output.add("Say STOP to end the game and collect your winnings before this happens!");
		
		// Probably edit this part to be the real enhancement.
		if(enhanced)
			output.add("ENHANCE BONUS: JerryEris wins Race to a Billion.");
		
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
	{		LinkedList<String> output = new LinkedList<>();
		if(pick.equalsIgnoreCase("STOP"))
		{
			if (picks == 0)
			{
				output.add("You haven't picked any spaces yet! There's no risk yet, so go ahead and pick one!");
			}
			else
			{
				output.add("You have chosen to stop, congratulations!");
				stop = true;
				output.addAll(generateRevealBoard());
			}
		}
		else if(!isNumber(pick))
		{
			//.'s count as non-numbers, so don't use them
			getInput();
			return;
		}
		else if(!checkValidNumber(pick))
		{
			sendMessage("Invalid pick.");
			getInput();
			return;
		}
		else
		{
			roundNumber++;
			lastPick = Integer.parseInt(pick) - 1;
			output.add(String.format("Space %d selected...",(lastPick+1)));
			if (pickedBombs[0] || pickedBombs[1] || pickedBombs[2] || spaceType.get(lastPick) == 8)
			{
				output.add("...");
			}
			pickedSpaces[lastSpace] = true
			if (spaceType.get() < 0)
			{				
				output.add(String.format("It's Bomb #%d.",-1 * (spaceType.get(lastPick))));
				if ((pickedBombs[0] && pickedBombs[1]) || (pickedBombs[1] && pickedBombs[2]))
				{
					output.add("Unfortunately, you have picked consecutively numbered bombs, so you're out.");
					alive = false;
					output.addAll(generateRevealBoard());
				}
				pickedBombs[-1 * spaceType.get(lastPick)] = true
				//bomb
			}
			else if (spaceType.get() == 8)
			{
				output.add("It's the **Elevator**!");
				streakMoney = streakMoney + (elevator * roundNumber);
				output.add(String.format("You'll now earn $%,12d%n more per card in your highest streak, ", elevator * roundNumber) + String.format("bringing that up to $%,12d%n!", streakMoney));
				//elevator
			}
			else if (spaceType.get() == 3)
			{
				output.add("It's the **Triple**!");					
				output.add("You've earned the " + String.format("%02d",(cardNumbers.get(12))) + ", the " + String.format("%02d",(cardNumbers.get(13))) + ", and the " + String.format("%02d",(cardNumbers.get(14))) + "!");
					earnedCards[cardNumbers.get(12)] = true;
					earnedCards[cardNumbers.get(13)] = true;
					earnedCards[cardNumbers.get(14)] = true;
				//triple
			}
			else if (spaceType.get() >= 20)
			{
				output.add("It's a **Double**!");
				if (spaceType.get(lastPick) == 20)
				{
					output.add("You've earned the " + String.format("%02d",(cardNumbers.get(8))) + " and the " + String.format("%02d",(cardNumbers.get(9))) + "!");
					earnedCards[cardNumbers.get(8)] = true;
					earnedCards[cardNumbers.get(9)] = true;
				}
				else
				{
					output.add("You've earned the " + String.format("%02d",(cardNumbers.get(10))) + " and the " + String.format("%02d",(cardNumbers.get(11))) + "!");
					earnedCards[cardNumbers.get(10)] = true;
					earnedCards[cardNumbers.get(11)] = true;
				}
				//double
			}
			else if (spaceType.get() >= 10)
			{
				output.add("It's a Single!");
				cardNumbers.get(spaceType.get(lastPick) - 10);
				output.add("You've earned the " + String.format("%02d",(cardNumbers.get(spaceType.get(lastPick) - 10))) + ".");
				earnedCards[cardNumbers.get(spaceType.get(lastPick))] = true;
				
				//single
			}
			else if
			{
				output.add("It's an ***Error***! Whoopsie!");
				//uh oh
			}
		var missedACard = false;
		for(int i=1; i < =cardNumbers.size(); i++)
		{
			if(!earnedCards[i])
			{
				missedACard = true;
			}
			if (!missedACard && streakMoney != applyBaseMultiplier(streakMoney))
			{
				output.add("Wow! You picked every safe space, congratulations!");
				stop = true;
			}
			
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
		return (location >= 0 && location < cardNumbers.size() && !pickedSpaces[location]);
	}

	private String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("   RACK 'EM UP   \n");
		for(int i=0; i<cardNumbers.size(); i++)
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
		display.append("Cards:");
		var highStreak = 0;
		var currentStreak = 0;
		var lowCardInCurrentStreak = 0;
		var lowCardInHighStreak = 0;
		for(int i=1; i<=cardNumbers.size(); i++)
		{
			if (earnedCards[i])
			{
				if (lowCardInCurrentStreak == 0)
				{
					lowCardInCurrentStreak = i;
				}
				if (currentStreak > highStreak)
				{
					highStreak = currentStreak;
					lowCardInHighStreak = i;
				}
			}
			else
			{
				currentStreak = 0;
			}
		}
		var cardsLeftInStreak = highStreak;
		for(int i=1; i<=cardNumbers.size(); i++)
		{
			if (earnedCards[i])
			{
				if (lowCardInHighStreak == i)
				{
					display.append(" [" + i.toString());
					cardsLeftInStreak--;
				}
				else if (cardsLeftInStreak == 1)
				{
					display.append(" " + i.toString() + "]");
					cardsLeftInStreak--;
				}
				else if (cardsLeftInStreak == highStreak)
				{
					display.append(" " + i.toString());
				}
				else if (cardsLeftInStreak < highStreak)
				{
					display.append(" " + i.toString());
					cardsLeftInStreak--;
				}
				else
				{
					display.append("x");
				}
			}
		}	

		display.append("\nHigh Streak: " + highStreak);
		display.append(String.format("\nMoney Per Card: $%,12d%n",streakMoney));
		display.append(String.format("\nCurrent Winnings: $%,12d%n",streakMoney * highStreak));
		display.append("\n\nBombs: ");
		for(int i=0; i <= 2; i++)
		{
			if(pickedBombs[i])
			{
				display.append(" " + (i + 1).toString());
			}
		}
		display.append("```");
		return display.toString();
	}

	private String generateRevealBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("   RACK 'EM UP   \n");
		for(int i=0; i<cardNumbers.size(); i++)
		{
			if(pickedSpaces[i])
			{
				if (cardNumbers.get(spaceType.get(lastPick)) < 0)
				{
					display.append("B");
					display.append((cardNumbers.get(spaceType.get(lastPick)) * -1).toString());
				}
				else if (cardNumbers.get(spaceType.get(lastPick)) == 8)
				{
					display.append("E!");
				}
				else if (cardNumbers.get(spaceType.get(lastPick)) == 3)
				{
					display.append("Tr");
				}
				else if (cardNumbers.get(spaceType.get(lastPick)) >= 20)
				{
					display.append("Do");
				}
				else
				{
					display.append("Si");
				}
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
		display.append("Cards:");
		var highStreak = 0;
		var currentStreak = 0;
		var lowCardInCurrentStreak = 0;
		var lowCardInHighStreak = 0;
		for(int i=1; i<=cardNumbers.size(); i++)
		{
			if (earnedCards[i])
			{
				if (lowCardInCurrentStreak == 0)
				{
					lowCardInCurrentStreak = i;
				}
				if (currentStreak > highStreak)
				{
					highStreak = currentStreak;
					lowCardInHighStreak = i;
				}
			}
			else
			{
				currentStreak = 0;
			}
		}
		var cardsLeftInStreak = highStreak;
		for(int i=1; i<=cardNumbers.size(); i++)
		{
			if (earnedCards[i])
			{
				if (lowCardInHighStreak == i)
				{
					display.append(" [" + i.toString());
					cardsLeftInStreak--;
				}
				else if (cardsLeftInStreak == 1)
				{
					display.append(" " + i.toString() + "]");
					cardsLeftInStreak--;
				}
				else if (cardsLeftInStreak == highStreak)
				{
					display.append(" " + i.toString());
				}
				else if (cardsLeftInStreak < highStreak)
				{
					display.append(" " + i.toString());
					cardsLeftInStreak--;
				}
				else
				{
					display.append("x");
				}
			}
		}	

		display.append("\nHigh Streak: " + highStreak.toString());
		display.append(String.format("\nMoney Per Card: $%,12d%n",streakMoney));
		if (alive)
		{
			display.append(String.format("\Winnings: $%,12d%n",streakMoney * highStreak));
		}
		else
		{
			display.append("\Winnings: $0");
		}
		display.append("\n\nBombs: ");
		for(int i=0; i <= 2; i++)
		{
			if(pickedBombs[i])
			{
				display.append(" " + (i + 1).toString());
			}
		}
		display.append("```");
		return display.toString();
	}

	
	/**
	 * Returns true if the minigame has ended
	 */
	private boolean isGameOver()
	{
		return !alive || stop;
	}
	
	@Override
	String getBotPick()
	{
		ArrayList<Integer> openSpaces = new ArrayList<>(cardNumbers.size());
		for(int i=0; i<cardNumbers.size(); i++)
			if(!pickedSpaces[i])
			{
				openSpaces.add(i+1);
			}
		if (!pickedBombs[0] && !pickedBombs[1] && !pickedBombs[2])
		{
			return String.valueOf(openSpaces.get((int)(RtaBMath.random()*openSpaces.size())));
		}
		else
		{
			if(RtaBMath.random()*openSpaces.size() < 2.0)
			{
				return "STOP";
			}
			return String.valueOf(openSpaces.get((int)(RtaBMath.random()*openSpaces.size())));
		}
	}

	@Override
	void abortGame()
	{
		//No mercy here, sorry
		awardMoneyWon(0);
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	//Seriously, replace this part
	@Override public String getEnhanceText() { return "JerryEris wins Race to a Billion."; }