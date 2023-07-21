package tel.discord.rtab.games;

import static tel.discord.rtab.RaceToABillionBot.waiter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.board.Game;
import tel.discord.rtab.board.WeightedSpace;

public class TicTacBomb extends PvPMiniGameWrapper
{
	private static final String NAME = "Tic Tac Bomb";
	private static final String SHORT_NAME = "TicTac";
	private static final boolean BONUS = false;
	private static final int PRIZE_PER_SAFE_SPACE = 50_000;
	private static final int PRIZE_FOR_MINOR_WIN = 1_000_000;
	private static final int PRIZE_FOR_MAJOR_WIN = 2_500_000;
	private static final int[][] LINES = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
	private final int[] spaces = new int[9];
	private int playerBomb = -1;
	private int opponentBomb = -1;
	
	//We want the AI to see the centre space as most important, then corners, with side spaces the least valuable
	private enum TicTacBombSpace implements WeightedSpace
	{
		//No points for figuring out the naming scheme
		LG(0,2),NG(1,1),CG(2,2),
		LN(3,1),TN(4,4),CN(5,1),
		LE(6,2),NE(7,1),CE(8,2);

		final int weight;
		final int spaceNumber;
		TicTacBombSpace(int spaceNumber, int weight)
		{
			this.weight = weight;
			this.spaceNumber = spaceNumber;
		}
		@Override
		public int getWeight(int playerCount)
		{
			return weight;
		}
		int getSpace()
		{
			return spaceNumber;
		}
	}
	
	@Override
	LinkedList<String> getInstructions()
	{
		LinkedList<String> output = new LinkedList<>();
		output.add("In this minigame, you and your opponent will be playing Tic-Tac-Toe...");
		output.add("But before the game starts, you'll each place a bomb.");
		output.add(String.format("If your opponent hits your bomb, you win **$%,d**.",applyBaseMultiplier(PRIZE_FOR_MINOR_WIN)));
		output.add(String.format("But if you win by making three in a row, you get **$%,d**!",applyBaseMultiplier(PRIZE_FOR_MAJOR_WIN)));
		output.add(String.format("Win or lose, you also earn $%,d for every safe space you pick.", applyBaseMultiplier(PRIZE_PER_SAFE_SPACE)));
		if(enhanced)
			output.add("ENHANCE BONUS: Safe spaces you pick are worth five times that value.");
		return output;
	}
	
	void startPvPGame()
	{
		StringBuilder output = new StringBuilder().append(players.get(player).getSafeMention()).append(", ");
		output.append(players.get(opponent).getSafeMention()).append(", ");
		output.append("both of you must now place your bombs on this grid.\n");
		output.append(generateBoard());
		sendMessage(output.toString());
		askForBomb(player, true);
		askForBomb(opponent, false);
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
		//Figure out what they just did and decide where to go from here
		case MID_GAME:
			if(!isNumber(input))
				getCurrentPlayerInput();
			else if(!checkValidNumber(input))
			{
				sendMessage("Invalid space.");
				getCurrentPlayerInput();
			}
			else
				resolveTurn(Integer.parseInt(input)-1);
			return;
		//We shouldn't be here
		case END_GAME:
			endGame(false);
		}
	}
	
	private void runTurn()
	{
		LinkedList<String> output = new LinkedList<>();
		if(!getCurrentPlayer().isBot)
			output.add(getCurrentPlayer().getSafeMention() + ", your turn. Choose a space on the board.");
		output.add(generateBoard());
		sendMessages(output);
		getCurrentPlayerInput();
	}
	
	private boolean checkValidNumber(String input)
	{
		if(!isNumber(input))
			return false;
		int spacePicked = Integer.parseInt(input)-1;
		return (spacePicked >= 0 && spacePicked < 9 && spaces[spacePicked] == 0);
	}
	
	private void askForBomb(int playerID, boolean playerTurn)
	{
		if(players.get(playerID).isBot)
		{
			int spaceNumber = Board.generateSpaces(1,2,TicTacBombSpace.values()).get(0).getSpace();
			if(playerTurn)
				playerBomb = spaceNumber;
			else
				opponentBomb = spaceNumber;
			checkReady();
		}
		else
		{
			players.get(playerID).user.openPrivateChannel().queue(
					(channel) -> channel.sendMessage("Please place your bomb within the next 60 seconds "
							+ "by sending a number 1-9").queue());
			waiter.waitForEvent(MessageReceivedEvent.class,
					//Check if right player, and valid bomb pick
					e -> (e.getAuthor().equals(players.get(playerID).user)
							&& e.getChannel().getType() == ChannelType.PRIVATE
							&& checkValidNumber(e.getMessage().getContentStripped())),
					//Parse it and update the bomb board
					e -> 
					{
						int spaceSelected = Integer.parseInt(e.getMessage().getContentStripped())-1;
						if(playerTurn)
							playerBomb = spaceSelected;
						else
							opponentBomb = spaceSelected;
						players.get(playerID).user.openPrivateChannel().queue(
								(channel) -> channel.sendMessage("Bomb placement confirmed.").queue());
						checkReady();
					},
					//Or timeout the prompt after a minute (nothing needs to be done here)
					playerTurn ? 60 : 61, TimeUnit.SECONDS, () -> timeoutBomb());
		}
	}
	
	private synchronized void timeoutBomb()
	{
		if(gameStatus == Status.END_GAME)
			return;
		else if(playerBomb != -1 && opponentBomb != -1)
			return;
		//Abort the game, figure out who didn't place, and award the other guy
		gameStatus = Status.END_GAME;
		//If neither placed, no one gets anything
		if(playerBomb == -1 && opponentBomb == -1)
		{
			sendMessage("Game Over. Neither player placed their bomb.");
			gameOver(0);
		}
		else if(playerBomb == -1)
		{
			playerTurn = false;
			endGame(false);
		}
		else if(opponentBomb == -1)
		{
			playerTurn = true;
			endGame(false);
		}
	}
	
	private void checkReady()
	{
		if(playerBomb != -1 && opponentBomb != -1)
			getFirstPlayer();
	}
	
	private void getFirstPlayer()
	{
		if(players.get(player).isBot)
		{
			playerTurn = Math.random() < 0.5;
			sendMessage(getPlayer().getName() + " elected to go " + (playerTurn ? "first." : "second."));
			gameStatus = Status.MID_GAME;
			runTurn();
		}
		else
		{
			playerTurn = true;
			sendMessage(getPlayer().getSafeMention() + ", would you like to go FIRST or SECOND?");
			getInput();
		}
	}
	
	private String generateBoard()
	{
		StringBuilder output = new StringBuilder().append("```\n");
		output.append("TIC  \n TAC \n BOMB\n");
		for(int i=0; i<9; i++)
		{
			if(spaces[i] == 1)
				output.append("X");
			else if(spaces[i] == -1)
				output.append("O");
			else
				output.append(i+1);
			if(i%3 == 2)
				output.append("\n");
			else
				output.append(" ");
		}
		output.append("```");
		return output.toString();
	}

	@Override
	String getBotPick()
	{
		//Decide which space to pick based on if there are any urgent lines
		ArrayList<int[]> urgentLines = new ArrayList<>();
		for(int[] nextLine : LINES)
		{
			int sum = 0;
			boolean filledLine = true;
			for(int nextSpace : nextLine)
			{
				sum += spaces[nextSpace];
				if(spaces[nextSpace] == 0 && !isMyBomb(nextSpace))
					filledLine = false;
			}
			//If the last space is our bomb, we don't add it to the list
			if(!filledLine && Math.abs(sum) == 2)
				urgentLines.add(nextLine);
		}
		//Now start with the priority 2 lines - these are lines that either player could complete on their next turn
		if(urgentLines.size() > 1)
			//If there are two or more such lines, we have to pick one and fill it
			return String.valueOf(findEmptySpaceInLine(urgentLines.get((int)(Math.random()*urgentLines.size())))+1);
		else if(urgentLines.size() > 0)
			//If there's one line, we'll probably fill it but maybe not in case it's their bomb (or to bluff that it's ours)
			if(Math.random() < 0.75)
				return String.valueOf(findEmptySpaceInLine(urgentLines.get(0))+1);
		//If there are no urgent lines or we decided to leave them alone, pick a space at random
		ArrayList<Integer> openSpaces = new ArrayList<>();
		for(int i=0; i<spaces.length; i++)
			if(spaces[i] == 0 && !isMyBomb(i))
				openSpaces.add(i);
		if(openSpaces.size() > 0)
			return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size()))+1);
		//If all the spaces are gone, the only one left is our bomb...
		else
			return String.valueOf(getMyBomb()+1);
	}
	
	private int findEmptySpaceInLine(int[] line)
	{
		for(int nextSpace : line)
			if(spaces[nextSpace] == 0)
				return nextSpace;
		//This should never happen
		return -1;
	}
	
	private void resolveTurn(int space)
	{
		LinkedList<String> output = new LinkedList<>();
		if(getCurrentPlayer().isBot)
			output.add(getCurrentPlayer().getName() + " selects space " + (space+1) + "...");
		else
			output.add("Space " + (space+1) + " selected...");
		//Blow them up and end the minigame on a bomb
		if(isBomb(space))
		{
			output.add("...");
			output.add("It's a **BOMB**. It goes **BOOM**.");
			advanceTurn();
			sendMessages(output);
			endGame(false);
		}
		//Otherwise, check if they won and move on
		else
		{
			if(Math.random() < 0.5)
				output.add("...");
			int safeValue = applyBaseMultiplier(PRIZE_PER_SAFE_SPACE);
			if((enhanced && playerTurn) || (opponentEnhanced && !playerTurn))
				safeValue *= 5;
			output.add(String.format("**$%,d**!", safeValue));
			if(playerTurn)
				spaces[space] ++;
			else
				spaces[space] --;
			if(checkMajorWin())
			{
				output.add("That's three in a row! Congratulations!");
				sendMessages(output);
				endGame(true);
			}
			else
			{
				sendMessages(output);
				advanceTurn();
				runTurn();
			}
		}
	}
	
	private boolean isBomb(int space)
	{
		return space == playerBomb || space == opponentBomb;
	}
	
	private int getMyBomb()
	{
		return playerTurn ? playerBomb : opponentBomb;
	}
	
	private boolean isMyBomb(int space)
	{
		if(playerTurn)
			return space == playerBomb;
		else
			return space == opponentBomb;
	}
	
	private boolean checkMajorWin()
	{
		//Loop through each line and check if all three spaces are claimed by the same player
		for(int[] nextLine : LINES)
		{
			int sum = 0;
			for(int nextSpace : nextLine)
				sum += spaces[nextSpace];
			if(Math.abs(sum) == 3)
				return true;
		}
		return false;
	}

	@Override
	void abortGame()
	{
		switch(gameStatus)
		{
		case PRE_GAME:
			//This only triggers on a timeout on the minigame winner deciding who goes first
			playerTurn = false;
			endGame(false);
			break;
		case MID_GAME:
			advanceTurn();
			endGame(false);
		case END_GAME:
			//How are we here?
			endGame(false);
		}
	}
	
	private void endGame(boolean majorWin)
	{
		LinkedList<String> output = new LinkedList<>();
		sendMessage("Game Over. " + players.get(playerTurn?player:opponent).getName() + " wins" 
				+ (majorWin ? " through tic-tac-toe!" : "!"));
		int playerTotal = 0;
		int opponentTotal = 0;
		//Count up safe spaces picked
		for(int nextSpace : spaces)
		{
			if(nextSpace == 1)
				playerTotal += PRIZE_PER_SAFE_SPACE;
			else if(nextSpace == -1)
				opponentTotal += PRIZE_PER_SAFE_SPACE;
		}
		if(enhanced)
			playerTotal *= 5;
		if(opponentEnhanced)
			opponentTotal *= 5;
		//Award winner bonus
		if(playerTurn)
			playerTotal += majorWin ? PRIZE_FOR_MAJOR_WIN : PRIZE_FOR_MINOR_WIN;
		else
			opponentTotal += majorWin ? PRIZE_FOR_MAJOR_WIN : PRIZE_FOR_MINOR_WIN;
		//Deploy base multiplier
		playerTotal = applyBaseMultiplier(playerTotal);
		opponentTotal = applyBaseMultiplier(opponentTotal);
		//Credit the player their winnings
		output.add(players.get(player).getName() + String.format(" won **$%,d**,",playerTotal));
		StringBuilder extraResult = players.get(player).addMoney(playerTotal,MoneyMultipliersToUse.BOOSTER_OR_BONUS);
		if(extraResult != null)
			output.add(extraResult.toString());
		//And the opponent
		output.add("and " + players.get(opponent).getName() + String.format(" won **$%,d**,",opponentTotal));
		extraResult = players.get(opponent).addMoney(opponentTotal,MoneyMultipliersToUse.BOOSTER_OR_BONUS);
		if(extraResult != null)
			output.add(extraResult.toString());
		//and the closer
		output.add(" from " + (gameMultiplier > 1 ? String.format("%d copies of ", gameMultiplier) : "")
				+ "Tic Tac Bomb.");
		sendMessages = true;
		sendMessages(output);
		gameOver(playerTurn ? 1 : -1);
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "Safe spaces you pick are worth five times as much."; }
	@Override boolean isOpponentEnhanced()
	{
		if(opponent == -1)
			return false;
		return players.get(opponent).enhancedGames.contains(Game.TIC_TAC_BOMB);
	}
	
}
