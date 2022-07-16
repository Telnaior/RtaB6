package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import tel.discord.rtab.MoneyMultipliersToUse;

public class ColourOfMoney extends PvPMiniGameWrapper
{
	private static final String NAME = "The Colour of Money";
	private static final String SHORT_NAME = "Colour";
	private static final boolean BONUS = false;
	private static final int BASE_VALUE = 10_000;
	private static final int BOARD_SIZE = 20;
	private static final int MAX_TURNS = 5;
	private List<String> colours = Arrays.asList("cobalt", "turquoise", "alabaster", "mauve", "lime", "khaki", "crimson", "coral", "chartreuse",
			"chestnut", "cerulean", "bronze", "lilac", "olive", "auburn", "azure", "amber", "saffron", "taupe", "ebony", "fuchsia", "violet",
			"indigo", "ivory", "jade", "lemon", "periwinkle", "salmon", "mahogany", "maize", "mint", "maroon", "midnight", "rose", "moss",
			"mustard", "burgundy", "ochre", "lavender", "parchment", "peach", "puce", "ruby", "emerald", "sapphire", "sage", "sepia", "silver",
			"viridian", "carmine", "straw", "strawberry", "terracotta", "ultramarine", "verdigris", "sunshine"); //variety!
	ArrayList<Integer> values = new ArrayList<>();
	ArrayList<Integer> remainingValues = new ArrayList<>();
	boolean[] pickedSpaces = new boolean[BOARD_SIZE];
	int playerBank, opponentBank;
	int playerExcess, opponentExcess;
	int playerTurns, opponentTurns;
	int adjustedBase;
	
	@Override
	LinkedList<String> getInstructions()
	{
		adjustedBase = applyBaseMultiplier(BASE_VALUE);
		LinkedList<String> output = new LinkedList<>();
		output.add("This minigame features twenty coloured banks, "
				+ String.format("each containing an amount of money that ranges from $%1$,d to $%2$,d in $%1$,d increments.",
						adjustedBase, BOARD_SIZE*adjustedBase));
		output.add("You and your opponent will take turns attempting to withdraw from these banks.");
		output.add("On your turn, you must choose a bank and an amount you want to withdraw. If the bank doesn't have the amount you asked for, you get nothing.");
		output.add("Each player gets five chances to withdraw money, then whoever earned the highest total wins both player's totals! The loser gets nothing.");
		if(enhanced)
			output.add("ENHANCE BONUS: If you win, you also win the excess amounts from the banks you successfully withdrew from.");
		return output;
	}

	@Override
	void startPvPGame()
	{
		//Prepare value list
		for(int i=0; i<BOARD_SIZE; i++)
		{
			int nextValue = adjustedBase * (i+1);
			values.add(nextValue);
			remainingValues.add(nextValue);
		}
		Collections.shuffle(values);
		//Prepare colour list
		Collections.shuffle(colours); //We only use the first 20 of these so the rest sitting at the end aren't a problem
		//and figure out turn order
		playerTurn = true;
		if(players.get(player).isBot)
		{
			playerTurn = Math.random() < 0.2; //going second is an advantage so the AI favours it
			sendMessage(getCurrentPlayer().getName() + " elected to go " + (playerTurn ? "first." : "second."));
			gameStatus = Status.MID_GAME;
			runTurn();
		}
		else
		{
			sendMessage(getCurrentPlayer().getSafeMention() + ", would you like to go FIRST or SECOND?");
			getInput();
		}
	}
	private void runTurn()
	{
		LinkedList<String> output = new LinkedList<>();
		if(!getCurrentPlayer().isBot)
			output.add(getCurrentPlayer().getSafeMention() + ", your turn. Choose a bank and how much you would like to withdraw.");
		output.add(generateBoard());
		sendMessages(output);
		getCurrentPlayerInput();
	}

	@Override
	void playNextTurn(String input)
	{
		switch(gameStatus)
		{
		//This case will tell us if they picked to go first or second, as bomb placement itself is handled elsewhere
		case PRE_GAME:
			if(input.equalsIgnoreCase("FIRST"))
			{
				playerTurn = true;
				gameStatus = Status.MID_GAME;
				runTurn();
			}
			else if(input.equalsIgnoreCase("SECOND"))
			{
				playerTurn = false;
				gameStatus = Status.MID_GAME;
				runTurn();
			}
			else
				getInput();
			return;
		case MID_GAME:
			String pick = input.toLowerCase();
			pick = pick.replaceAll(",", "");
			String[] tokens = pick.split("\\s");
			if(tokens.length > 2)
			{
				//they're probably just saying some nonsense, return
				getCurrentPlayerInput();
				return;
			}
			if(tokens.length < 2)
			{
				sendMessage("Please choose a bank and a withdrawal amount on the same line.");
				getCurrentPlayerInput();
				return;
			}
			//Okay, we now know we have two tokens
			//Swap the inputs if they put the amount second
			if (colours.contains(tokens[0]) || tokens[0].length() == 1)
			{
				String temp = tokens[1];
				tokens[1] = tokens[0];
				tokens[0] = temp;
			}
			//Parse 'K' shorthand
			if (tokens[0].charAt(tokens[0].length() - 1) == 'k')
			{
				tokens[0] = tokens[0].substring(0, tokens[0].length() - 1) + "000";
			}
			//Get usable values out of all this
			int withdrawalAmount = 0;
			try
			{
				withdrawalAmount = Integer.parseInt(tokens[0]);
			}
			catch(NumberFormatException e)
			{
				//not a number they're talking nonsense
				getCurrentPlayerInput();
				return;
			}
			if(withdrawalAmount % adjustedBase != 0)
			{
				sendMessage(String.format("Please withdraw in multiples of $%,d.",adjustedBase));
				getCurrentPlayerInput();
				return;
			}
			if(withdrawalAmount <= 0)
			{
				sendMessage("You must withdraw something.");
				getCurrentPlayerInput();
				return;
			}
			if(withdrawalAmount / adjustedBase > BOARD_SIZE)
			{
				sendMessage("No bank in the world has that much money.");
				getCurrentPlayerInput();
				return;
			}
			int chosenBank = -1;
			if(tokens[1].length() == 1)
				chosenBank = tokens[1].charAt(0) - 'a';
			else
			{
				for(int i=0; i<BOARD_SIZE; i++)
					if(tokens[1].equals(colours.get(i)))
					{
						chosenBank = i;
						break;
					}
				//If we didn't find the bank they said, get out of here
				if(chosenBank == -1)
				{
					getCurrentPlayerInput();
					return;
				}
			}
			if(chosenBank < 0 || chosenBank >= BOARD_SIZE)
			{
				//they're still talking nonsense geeze so much data validation on this one
				getCurrentPlayerInput();
				return;
			}
			if(pickedSpaces[chosenBank])
			{
				sendMessage("That bank has already been taken.");
				getCurrentPlayerInput();
				return;
			}
			LinkedList<String> output = new LinkedList<>();
			if(getCurrentPlayer().isBot)
				output.add(String.format("%s withdraws $%,d from the %s bank, which contains...",
						getCurrentPlayer().getName(), withdrawalAmount, colours.get(chosenBank)));
			else
				output.add(String.format("Withdrawing $%,d from the %s bank. It contains...", withdrawalAmount, colours.get(chosenBank)));
			pickedSpaces[chosenBank] = true;
			remainingValues.remove(Integer.valueOf(values.get(chosenBank))); //valueOf trickery needed to avoid value being read as an index
			incrementTurnCount();
			//Suspense if you're playing it risky
			if(withdrawalAmount > adjustedBase*BOARD_SIZE/2)
				output.add("...");
			if(values.get(chosenBank) >= withdrawalAmount)
			{
				output.add(String.format("$%,d!", values.get(chosenBank)));
				addToMyBank(withdrawalAmount);
				addToMyExcess(values.get(chosenBank)-withdrawalAmount);
				output.add(String.format("Withdrawal successful! %s now has a total of **$%,d**.",getCurrentPlayer().getName(), getMyBank()));
			}
			else
			{
				output.add(String.format("$%,d.", values.get(chosenBank)));
				output.add("Withdrawal failed.");
			}
			sendMessages(output);
			advanceTurn();
			if(getMyTurnCount() >= MAX_TURNS)
				endGame();
			else
				runTurn();
			return;
		default:
			//um
			endGame();
			return;
		}
	}
	
	void endGame()
	{
		int totalWin = playerBank + opponentBank;
		int winner = playerBank - opponentBank;
		//super messy code duplication we definitely never have to refactor this riiiiiiiiiiiiiiight?
		if(winner > 0)
		{
			sendMessage("Game over. "+players.get(player).getName()+" wins!");
			if(enhanced)
			{
				sendMessage(String.format("Excess $%,d from banks added to prize.", playerExcess));
				totalWin += playerExcess;
			}
			StringBuilder resultString = new StringBuilder();
			resultString.append(players.get(player).getName() + " won ");
			resultString.append(String.format("**$%,d** from ",totalWin));
			if(gameMultiplier > 1)
				resultString.append(String.format("%d copies of ",gameMultiplier));
			resultString.append(getName() + ".");
			StringBuilder extraResult = null;
			extraResult = players.get(player).addMoney(totalWin, MoneyMultipliersToUse.BOOSTER_OR_BONUS);
			//We want the endgame result to show up unconditionally
			sendMessages = true;
			sendMessage(resultString.toString());
			if(extraResult != null)
				sendMessage(extraResult.toString());
		}
		else if(winner < 0)
		{
			sendMessage("Game over. "+players.get(opponent).getName()+" wins!");
			if(opponentEnhanced)
			{
				sendMessage(String.format("Excess $%,d from banks added to prize.", opponentExcess));
				totalWin += opponentExcess;
			}
			StringBuilder resultString = new StringBuilder();
			resultString.append(players.get(opponent).getName() + " won ");
			resultString.append(String.format("**$%,d** from ",totalWin));
			if(gameMultiplier > 1)
				resultString.append(String.format("%d copies of ",gameMultiplier));
			resultString.append(getName() + ".");
			StringBuilder extraResult = null;
			extraResult = players.get(opponent).addMoney(totalWin, MoneyMultipliersToUse.BOOSTER_OR_BONUS);
			//We want the endgame result to show up unconditionally
			sendMessages = true;
			sendMessage(resultString.toString());
			if(extraResult != null)
				sendMessage(extraResult.toString());
		}
		else
		{
			sendMessage("Game over. It's a tie!");
			LinkedList<String> output = new LinkedList<>();
			output.add(players.get(player).getName() + String.format(" won **$%,d**,",playerBank));
			StringBuilder extraResult = players.get(player).addMoney(playerBank,MoneyMultipliersToUse.BOOSTER_OR_BONUS);
			if(extraResult != null)
				output.add(extraResult.toString());
			//And the opponent
			output.add("and " + players.get(opponent).getName() + String.format(" won **$%,d**,",opponentBank));
			extraResult = players.get(opponent).addMoney(opponentBank,MoneyMultipliersToUse.BOOSTER_OR_BONUS);
			if(extraResult != null)
				output.add(extraResult.toString());
			//and the closer
			output.add(" from " + (gameMultiplier > 1 ? String.format("%d copies of ", gameMultiplier) : "")
					+ "Tic Tac Bomb.");
			sendMessages = true;
			sendMessages(output);
		}
		gameOver(winner);
	}
	
	void addToMyBank(int amount)
	{
		if(playerTurn) playerBank += amount;
		else opponentBank += amount;
	}
	
	void addToMyExcess(int amount)
	{
		if(playerTurn) playerExcess += amount;
		else opponentExcess += amount;
	}
	
	int getMyBank()
	{
		return playerTurn ? playerBank : opponentBank;
	}
	
	int getOtherBank()
	{
		return playerTurn ? opponentBank : playerBank;
	}
	
	int getMyTurnCount()
	{
		return playerTurn ? playerTurns : opponentTurns;
	}
	
	int getOtherTurnCount()
	{
		return playerTurn ? opponentTurns : playerTurns;
	}
	
	void incrementTurnCount()
	{
		if(playerTurn)
			playerTurns ++;
		else
			opponentTurns ++;
	}
	
	String generateBoard()
	{
		StringBuilder board = new StringBuilder();
		board.append("```\n");
		//Start by figuring out the lengths of things and printing the header
		int nameLength = Math.max(players.get(player).getName().length(), players.get(opponent).getName().length());
		int moneyLength = String.valueOf(String.format("%,d",adjustedBase*BOARD_SIZE*MAX_TURNS/2)).length();
		int headerSpaces = (nameLength + moneyLength - 10)/2;
        board.append(" ".repeat(Math.max(0, headerSpaces)));
		board.append("THE COLOUR OF MONEY\n");
		//Now the status lines, starting with the player
		board.append(playerTurn ? "> " : "  ");
		board.append(String.format("%-"+nameLength+"s", players.get(player).getName()));
		board.append(String.format(" $%,"+moneyLength+"d ", playerBank));
		for(int i=0; i<5; i++)
			board.append(i < playerTurns ? "\u25a0" : "\u25a1");
		board.append("\n");
		//Opponent status line
		board.append(!playerTurn ? "> " : "  ");
		board.append(String.format("%-"+nameLength+"s", players.get(opponent).getName()));
		board.append(String.format(" $%,"+moneyLength+"d ", opponentBank));
		for(int i=0; i<5; i++)
			board.append(i < opponentTurns ? "\u25a0" : "\u25a1");
		board.append("\n\n");
		//Now the banks themselves
		board.append("       REMAINING BANKS\n");
		int nextValue = 0;
		for(int i=0; i<BOARD_SIZE; i++)
		{
			if(!pickedSpaces[nextValue])
			{
				board.append((char)('a' + nextValue));
				board.append(String.format(" - %-11s",colours.get(nextValue)));
			}
			else
			{
				board.append(String.format("  $%,"+moneyLength+"d",values.get(nextValue)));
                board.append(" ".repeat(Math.max(0, 13 - moneyLength - 1)));
			}
			//Order is 0, 10, 1, 11, ..., 9, 19
			nextValue += BOARD_SIZE/2;
			if(nextValue >= BOARD_SIZE)
				nextValue -= BOARD_SIZE - 1;
			//Space appropriately
			board.append(i%2==0 ? "   " : "\n");
		}
		board.append("```");
		return board.toString();
	}

	@Override
	String getBotPick()
	{
		boolean debug = false;
		int withdrawal;
		//Position factor is whatever we would currently need to win plus 1
		int positionFactor = getOtherBank() - getMyBank() + adjustedBase;
		if(debug)
			sendMessage(String.format("AI position factor = $%,d", positionFactor));
		//Average factor is based on the remaining values plus a bit of random variance
		int averageFactor = 0;
		for(int next : remainingValues)
			averageFactor += next;
		averageFactor /= remainingValues.size();
		//Throw in a random factor to keep it interesting
		averageFactor += 4*(Math.random()-0.5) * adjustedBase;
		if(debug)
			sendMessage(String.format("AI average factor = $%,d", averageFactor));
		//Now combine the two factors into something suitable for the current turn
		//We want to focus mostly average factor until the endgame, where suddenly position factor is all-important
		int factorRatio = (int)Math.pow(getOtherTurnCount(), 2);
		withdrawal = positionFactor * factorRatio / 25 + averageFactor * (25-factorRatio) / 25;
		if(debug)
			sendMessage(String.format("AI withdrawal target  = $%,d", withdrawal));
		//If we're so far ahead that we don't think we need anything at all, try some backup ideas
		if(withdrawal < 0)
		{
			//If we've mathematically clinched victory, just play solitaire 
			if(positionFactor + (adjustedBase * BOARD_SIZE * (5-getOtherTurnCount())) < 0)
			{
				withdrawal = averageFactor;
				if(debug)
					sendMessage("AI victory certain. Using average factor only.");
			}
			//Otherwise, engage Protocol Killshot and let's get 21k ahead of them
			else
			{
				withdrawal = positionFactor + (adjustedBase * BOARD_SIZE);
				if(debug)
					sendMessage("AI attempting a killshot.");
			}
		}
		//Round our withdrawal off as required
		int remainder = withdrawal % adjustedBase;
		if(Math.random() < (double)remainder / adjustedBase)
			withdrawal += (adjustedBase - remainder);
		else
			withdrawal -= remainder;
		//Then push it up to the next value if we've found a gap
		while(!remainingValues.contains(withdrawal))
		{
			//Don't go for something totally impossible lolololol
			if(withdrawal > remainingValues.get(remainingValues.size()-1))
				withdrawal = remainingValues.get(remainingValues.size()-1);
			else
				withdrawal += adjustedBase;
		}
		//Alright, now we have our target let's choose a bank
		ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
		for(int i=0; i<BOARD_SIZE; i++)
			if(!pickedSpaces[i])
				openSpaces.add(i);
		return colours.get(openSpaces.get((int)(Math.random()*openSpaces.size()))) + " " + withdrawal;
	}

	@Override
	void abortGame()
	{
		//Award 10k for each turn remaining to the player who didn't timeout, then end the game
		//This can award victory to the player who timed out, but only if it was mathematically impossible for them to lose already
		advanceTurn();
		addToMyBank(adjustedBase * (BOARD_SIZE / 2) * ((MAX_TURNS*2) - (playerTurns+opponentTurns)));
		endGame();
	}


	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "If you win, the excess amounts from the banks you successfully withdrew from are added to your prize."; }
	
}
