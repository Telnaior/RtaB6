package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.LinkedList;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.board.WeightedSpace;
import tel.discord.rtab.games.objs.Jackpots;

public class BowserTicTacBomb extends MiniGameWrapper
{
	static final String NAME = "Bowser's Tic Tac Bomb";
	static final String SHORT_NAME = "TicTac";
	static final boolean BONUS = false;
	static final int PRIZE_PER_SAFE_SPACE = 100_000;
	static final int PRIZE_FOR_MINOR_WIN = 1_000_000;
	static final int PRIZE_FOR_MAJOR_WIN = 2_500_000;
	static final int[][] LINES = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
	final int[] spaces = new int[9];
	int playerBomb = -1;
	int opponentBomb = -1;
	boolean playerTurn;
	
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
	void startGame()
	{
		//Display instructions
		LinkedList<String> output = new LinkedList<>();
		output.add("Wah, hah, hah, let's play a game of Tic-Tac-Toe!");
		output.add("But before the game starts, we'll each place a bomb.");
		output.add(String.format("Your penalty starts at $%,d and every safe space we uncover will cost you another $%,d..."
				,applyBaseMultiplier(PRIZE_FOR_MINOR_WIN),applyBaseMultiplier(PRIZE_PER_SAFE_SPACE)));
		output.add("But if I pick your bomb or you make three in a row, then you'll get away scot free!");
		output.add("However, if you pick **my** bomb, you'll be paying up!");
		output.add(String.format("And if you let me make three in a row, I'll add **$%,d** to your penalty!"
				,applyBaseMultiplier(PRIZE_FOR_MAJOR_WIN-PRIZE_FOR_MINOR_WIN)));
		sendSkippableMessages(output);
		//Place Bowser's bomb and ask for the player's
		opponentBomb = Board.generateSpaces(1,2,TicTacBombSpace.values()).get(0).getSpace();
		sendMessage(String.format("Good luck, %s! I've placed my bomb, so go ahead and place yours on this board...",getPlayer().getName()));
		sendMessage(generateBoard());
		getInput();
	}

	@Override
	void playNextTurn(String input)
	{
		if (!isNumber(input))
			getInput();
		else if (!checkValidNumber(input))
		{
			sendMessage("Invalid space.");
			getInput();
		}
		else if(playerBomb == -1) //bomb placement
		{
			playerBomb = Integer.parseInt(input) - 1;
			decideFirstTurn();
		}
		else //main game
			resolveTurn(Integer.parseInt(input) - 1);
	}
	
	private void decideFirstTurn()
	{
		playerTurn = Math.random() < 0.5;
		sendMessage("Let's see here... I think I'll go " + (playerTurn ? "SECOND." : "FIRST."));
		runTurn();
	}
	
	private boolean checkValidNumber(String input)
	{
		if(!isNumber(input))
			return false;
		int spacePicked = Integer.parseInt(input)-1;
		return (spacePicked >= 0 && spacePicked < 9 && spaces[spacePicked] == 0);
	}
	
	private void runTurn()
	{
		if(playerTurn)
		{
			if(!getPlayer().isBot)
			{
				LinkedList<String> output = new LinkedList<>();
				output.add(getPlayer().getSafeMention() + ", your turn. Choose a space on the board.");
				output.add(generateBoard());
				sendMessages(output);
			}
			getInput();
		}
		else
			playNextTurn(getBotPick());
	}
	
	private void resolveTurn(int space)
	{
		LinkedList<String> output = new LinkedList<>();
		if(!playerTurn)
			output.add("Bowser selects space " + (space+1) + "...");
		else if(getPlayer().isBot)
			output.add(getPlayer().getName() + " selects space " + (space+1) + "...");
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
			output.add(String.format("**-$%,d**!", safeValue));
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
	
	void advanceTurn()
	{
		playerTurn = !playerTurn;
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
	
	private String generateBoard()
	{
		StringBuilder output = new StringBuilder().append("```\n");
		output.append("TIC  \n TAC \n BOMB\n");
		for(int i=0; i<9; i++)
		{
			if(spaces[i] == 1)
				output.append("O");
			else if(spaces[i] == -1)
				output.append("X");
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
		if(playerBomb == -1) //Place bomb if needed
			return String.valueOf(Board.generateSpaces(1,2,TicTacBombSpace.values()).get(0).getSpace() + 1);
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
		else if(!urgentLines.isEmpty())
			//If there's one line, we'll probably fill it but maybe not in case it's their bomb (or to bluff that it's ours)
			if(Math.random() < 0.75)
				return String.valueOf(findEmptySpaceInLine(urgentLines.get(0))+1);
		//If there are no urgent lines or we decided to leave them alone, pick a space at random
		ArrayList<Integer> openSpaces = new ArrayList<>();
		for(int i=0; i<spaces.length; i++)
			if(spaces[i] == 0 && !isMyBomb(i))
				openSpaces.add(i);
		if(!openSpaces.isEmpty())
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

	@Override
	void abortGame()
	{
		playerTurn = false;
		endGame(true);
	}
	
	private void endGame(boolean majorWin)
	{
		LinkedList<String> output = new LinkedList<>();
		output.add((playerTurn ? getPlayer().getName() : "Bowser") + " wins" + (majorWin ? " through tic-tac-toe!" : "!"));
		//Calculate how much money is at stake
		int playerTotal = 0;
		//Count up penalty from picked spaces
		for(int nextSpace : spaces)
			if(nextSpace != 0)
				playerTotal += PRIZE_PER_SAFE_SPACE;
		//Increase penalty based on win type
		playerTotal += majorWin ? PRIZE_FOR_MAJOR_WIN : PRIZE_FOR_MINOR_WIN;
		//Deploy base multiplier
		playerTotal = applyBaseMultiplier(playerTotal);
		//If you won, Bowser sends you on your way
		if(playerTurn)
		{
			output.add("What? You won?! Well, you got lucky this time.");
			sendMessages(output);
			if(getPlayer().isBot)
			{
				sendMessages = true;
				sendMessage(getPlayer().getName() + " escaped unscathed from Bowser's Tic Tac Bomb.");
			}
			checkLuckyCharm(getPlayer(), playerTotal);
		}
		//If you lost, it's punishment time
		else
		{
			//Bowser takes it home
			Jackpots.BOWSER.addToJackpot(channel, playerTotal);
			output.add("I won, so now your money is MINE! Wah, hah, HAH!");
			sendMessages(output);
			//Build result string
			StringBuilder resultString = new StringBuilder();
			if(getPlayer().isBot)
				resultString.append(getPlayer().getName()).append(" lost ");
			else
				resultString.append("Game Over. You lost ");
			resultString.append(String.format("**$%,d** from ",playerTotal));
			if(gameMultiplier > 1)
				resultString.append(String.format("%d copies of ",gameMultiplier));
			resultString.append(getName()).append(".");
			//and goodbye money
			StringBuilder extraResult = null;
			extraResult = getPlayer().addMoney(-1*playerTotal, MoneyMultipliersToUse.BOOSTER_OR_BONUS);
			sendMessages = true;
			sendMessage(resultString.toString());
			if(extraResult != null)
				sendMessage(extraResult.toString());
		}
		gameOver();
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public boolean isNegativeMinigame() { return true; }
}
