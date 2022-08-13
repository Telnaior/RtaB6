package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import tel.discord.rtab.Achievement;

public class BumperGrab extends MiniGameWrapper
{
	private static final String NAME = "Bumper Grab";
	private static final String SHORT_NAME = "Bumper";
	private static final boolean BONUS = false;
	private int boardWidth;
	private int boardHeight;
	private String boardHint;
	private Space[][] board;
	private int playerX;
	private int playerY;
	private boolean isFirstMove = true;
	private boolean gameOver = false;
	private boolean boardGenerated = false;
	private int winnings = 0;
	private int maxWinnings, exitsLeft;
	//Amount of winnings after which a bot will try to escape rather than going for more
	private int botWinningsTarget;
	
	private enum Direction
	{
		LEFT	('<',-1, 0, "WEST"),
		DOWN	('v', 0, 1, "SOUTH"),
		UP		('^', 0,-1, "NORTH"),
		RIGHT	('>', 1, 0, "EAST");
		
		final char arrow;
		final int deltaX;
		final int deltaY;
		final String alias;
		Direction(char arrow, int deltaX, int deltaY, String alias)
		{
			this.arrow = arrow;
			this.deltaX = deltaX;
			this.deltaY = deltaY;
			this.alias = alias;
		}
	}
	
	private enum SpaceType
	{
		ICE, EXIT, BUMPER, CASH, HOLE, USED_EXIT
	}
	private interface Space
	{
		SpaceType getType();
		Direction getDirection();
		int getValue();
		boolean isExit();
	}
	private static class Ice implements Space
	{
		public SpaceType getType() { return SpaceType.ICE; }
		public Direction getDirection() { throw new UnsupportedOperationException(); }
		public int getValue() { throw new UnsupportedOperationException(); }
		public boolean isExit() { return false; }
	}
	private static class Hole implements Space
	{
		public SpaceType getType() { return SpaceType.HOLE; }
		public Direction getDirection() { throw new UnsupportedOperationException(); }
		public int getValue() { throw new UnsupportedOperationException(); }
		public boolean isExit() { return false; }
	}
	private static class Exit implements Space
	{
		public SpaceType getType() { return SpaceType.EXIT; }
		public Direction getDirection() { throw new UnsupportedOperationException(); }
		public int getValue() { throw new UnsupportedOperationException(); }
		public boolean isExit() { return true; }
	}
	private static class UsedExit implements Space
	{
		public SpaceType getType() { return SpaceType.USED_EXIT; }
		public Direction getDirection() { throw new UnsupportedOperationException(); }
		public int getValue() { throw new UnsupportedOperationException(); }
		public boolean isExit() { return true; }
	}
	private static class Bumper implements Space
	{
		Direction direction;
		Bumper(String direction) { this.direction = Direction.valueOf(direction); }
		public SpaceType getType() { return SpaceType.BUMPER; }
		public Direction getDirection() { return direction; }
		public int getValue() { throw new UnsupportedOperationException(); }
		public boolean isExit() { return false; }
	}
	private class Cash implements Space
	{
		int value;
		Cash(int value) { this.value = value; }
		public SpaceType getType() { return SpaceType.CASH; }
		public Direction getDirection() { throw new UnsupportedOperationException(); }
		public int getValue() { return applyBaseMultiplier(value); }
		public boolean isExit() { return false; }
	}

	@Override
	void startGame()
	{
		if(channel.getType() == ChannelType.PRIVATE)
		{
			sendMessage("""
					Choose which board you'd like to play:
					1) Square
					2) Plus
					""");
			getInput();
		}
		else
		{
			generateRandomBoard();
			turnOne();
		}
	}
	
	private void generateRandomBoard()
	{
		switch ((int) (Math.random() * 2)) {
			case 0 -> generateBoardSquare();
			case 1 -> generateBoardPlus();
		}
	}
	
	private void chooseBoard(String input)
	{
		switch (input.toUpperCase()) {
			case "1", "SQUARE" -> {
				generateBoardSquare();
				turnOne();
			}
			case "2", "PLUS" -> {
				generateBoardPlus();
				turnOne();
			}
			default -> getInput();
		}
	}
	
	private void turnOne()
	{
	    LinkedList<String> output = new LinkedList<>();
	    output.add("In Bumper Grab, your objective is to navigate an icy floating platform.");
	    output.add("Slide around, bounce off bumpers, and grab as much cash as you can!");
	    output.add("You're represented as an 'X', and exits are represented as 'O's.");
	    output.add("Other spaces are either cash or bumpers, but you won't know which until you hit them.");
	    output.add("Each move, you'll pick a direction (UP, LEFT, RIGHT, or DOWN), and " +
	    		"you'll slide in that direction until you hit a space you haven't been to.");
	    output.add("If it's cash, you grab it, and choose a new direction.");
	    output.add("If it's a bumper, you'll be pushed in a new direction.");
	    output.add("And if it's an exit, you're allowed to EXIT and escape with your loot!");
	    output.add("Or you can move again, but you won't be able to use that same exit later.");
	    output.add("Oh, and if you slide off the edge, you fall to your doom and lose everything.");
	    if(enhanced)
	    	output.add("ENHANCE BONUS: Exits can now be reused.");
	    output.add("P.S. " + boardHint + " Good luck!");
	    sendSkippableMessages(output);
	    sendMessage(drawScoreboard(false));
	    getInput();
	}
	
	private void generateBoardSquare()
	{
		ArrayList<Space> inner = new ArrayList<>(Arrays.asList(
				new Bumper("LEFT"), new Bumper("LEFT"), new Bumper("LEFT"),
				new Bumper("DOWN"), new Bumper("DOWN"), new Bumper("DOWN"),
				new Bumper("UP"), new Bumper("UP"), new Bumper("UP"),
				new Bumper("RIGHT"), new Bumper("RIGHT"), new Bumper("RIGHT"),
				new Cash(50_000), new Cash(100_000), new Cash(100_000),
				new Cash(250_000), new Cash(250_000), new Cash(250_000),
				new Cash(375_000), new Cash(500_000)));
		ArrayList<Space> outer = new ArrayList<>(Arrays.asList(
				new Bumper("LEFT"), new Bumper("LEFT"), new Bumper("LEFT"),
				new Bumper("DOWN"), new Bumper("DOWN"), new Bumper("DOWN"),
				new Bumper("UP"), new Bumper("UP"), new Bumper("UP"),
				new Bumper("RIGHT"), new Bumper("RIGHT"), new Bumper("RIGHT"),
				new Cash(250_000), new Cash(250_000), new Cash(250_000),
				new Cash(250_000), new Cash(375_000), new Cash(375_000),
				new Cash(375_000), new Cash(500_000), new Cash(500_000),
				new Cash(1_250_000), new Cash(1_250_000), new Cash(2_500_000)));
		Collections.shuffle(inner);
		Collections.shuffle(outer);
		
		boardWidth = 7;
		boardHeight = 7;
		playerX = 3;
		playerY = 3;
		boardHint = "The largest cash can only be found on the outer rim of the board.";
		
		board = new Space[][]
				{{outer.get( 0), outer.get( 1), outer.get( 2), outer.get( 3), outer.get( 4), outer.get( 5), outer.get( 6)},
				 {outer.get( 7), new Exit(),    inner.get( 0), inner.get( 1), inner.get( 2), new Exit(),    outer.get( 8)},
				 {outer.get( 9), inner.get( 3), inner.get( 4), inner.get( 5), inner.get( 6), inner.get( 7), outer.get(10)},
				 {outer.get(11), inner.get( 8), inner.get( 9), new Ice(),     inner.get(10), inner.get(11), outer.get(12)},
				 {outer.get(13), inner.get(12), inner.get(13), inner.get(14), inner.get(15), inner.get(16), outer.get(14)},
				 {outer.get(15), new Exit(),    inner.get(17), inner.get(18), inner.get(19), new Exit(),    outer.get(16)},
				 {outer.get(17), outer.get(18), outer.get(19), outer.get(20), outer.get(21), outer.get(22), outer.get(23)}};
				 
		maxWinnings = applyBaseMultiplier(10_000_000);
		botWinningsTarget = applyBaseMultiplier(2_500_000);
		exitsLeft = 4;
		boardGenerated = true;
	}
	
	private void generateBoardPlus()
	{
		ArrayList<Space> inner = new ArrayList<>(Arrays.asList(
				new Bumper("LEFT"), new Bumper("LEFT"), new Bumper("LEFT"), new Bumper("LEFT"),
				new Bumper("DOWN"), new Bumper("DOWN"), new Bumper("DOWN"), new Bumper("DOWN"),
				new Bumper("UP"), new Bumper("UP"), new Bumper("UP"), new Bumper("UP"),
				new Bumper("RIGHT"), new Bumper("RIGHT"), new Bumper("RIGHT"), new Bumper("RIGHT"),
				new Cash(50_000), new Cash(50_000), new Cash(100_000), new Cash(100_000),
				new Cash(150_000), new Cash(150_000), new Cash(200_000), new Cash(200_000),
				new Cash(250_000), new Cash(250_000), new Cash(375_000), new Cash(500_000)));
		ArrayList<Space> outer = new ArrayList<>(Arrays.asList(
				new Bumper("LEFT"), new Bumper("LEFT"), new Bumper("LEFT"), new Bumper("LEFT"),
				new Bumper("DOWN"), new Bumper("DOWN"), new Bumper("DOWN"), new Bumper("DOWN"),
				new Bumper("UP"), new Bumper("UP"), new Bumper("UP"), new Bumper("UP"),
				new Bumper("RIGHT"), new Bumper("RIGHT"), new Bumper("RIGHT"), new Bumper("RIGHT"),
				new Cash(250_000), new Cash(250_000), new Cash(250_000), new Cash(250_000),
				new Cash(250_000), new Cash(375_000), new Cash(375_000), new Cash(375_000),
				new Cash(375_000), new Cash(500_000), new Cash(500_000), new Cash(500_000),
				new Cash(750_000), new Cash(1_125_000), new Cash(300_000), new Cash(2_500_000)));
		Collections.shuffle(inner);
		Collections.shuffle(outer);
		
		boardWidth = 9;
		boardHeight = 9;
		playerX = 4;
		playerY = 4;
		boardHint = "The largest cash can only be found on the outer rim of the board.";
		
		board = new Space[][]
		{{new Hole(),    new Hole(),    outer.get( 0), outer.get( 1), outer.get( 2), outer.get( 3), outer.get( 4), new Hole(),    new Hole()},
		 {new Hole(),    new Hole(),    outer.get( 5), inner.get( 0), new Exit(),    inner.get( 1), outer.get( 6), new Hole(),    new Hole()},
		 {outer.get( 7), outer.get( 8), outer.get( 9), inner.get( 2), inner.get( 3), inner.get( 4), outer.get(10), outer.get(11), outer.get(12)},
		 {outer.get(13), inner.get( 5), inner.get( 6), inner.get( 7), inner.get( 8), inner.get( 9), inner.get(10), inner.get(11), outer.get(14)},
		 {outer.get(15), new Exit(),    inner.get(12), inner.get(13), new Ice(),     inner.get(14), inner.get(15), new Exit(),    outer.get(16)},
		 {outer.get(17), inner.get(16), inner.get(17), inner.get(18), inner.get(19), inner.get(20), inner.get(21), inner.get(22), outer.get(18)},
		 {outer.get(19), outer.get(20), outer.get(21), inner.get(23), inner.get(24), inner.get(25), outer.get(22), outer.get(23), outer.get(24)},
		 {new Hole(),    new Hole(),    outer.get(25), inner.get(26), new Exit(),    inner.get(27), outer.get(26), new Hole(),    new Hole()},
		 {new Hole(),    new Hole(),    outer.get(27), outer.get(28), outer.get(29), outer.get(30), outer.get(31), new Hole(),    new Hole()}};
				 
		maxWinnings = applyBaseMultiplier(12_500_000);
		botWinningsTarget = applyBaseMultiplier(3_125_000);
		exitsLeft = 4;
		boardGenerated = true;
	}

	@Override
	void playNextTurn(String input)
	{
		if(!boardGenerated)
		{
			chooseBoard(input);
			return;
		}
		LinkedList<String> output = new LinkedList<>();
		input = input.toUpperCase();
		for(Direction direction : Direction.values())
		{
			//Match the name itself, its compass alias, or the first letter of either
			if(input.equals(direction.toString()) || input.equals(direction.alias)
				|| (input.length() == 1 && (direction.toString().startsWith(input) || direction.alias.startsWith(input))))
			{
				output.add(direction +"...");
				move(direction, output);
				break;
			}
		}
		if(input.equals("QUIT") || input.equals("EXIT") || input.equals("STOP") || input.equals("Q"))
		{
			if(getSpace(playerX, playerY).isExit())
				escape();
			else
				output.add("There's no exit there, you have to pick a direction!");
		}
		sendMessages(output);
		if(gameOver)
		{
			sendMessage(drawScoreboard(true));
			awardMoneyWon(winnings);
		}
		else
			getInput();
	}
	
	private LinkedList<String> move(Direction direction, LinkedList<String> output)
	{
		LinkedList<Pair<Integer,Integer>> currentSegment = new LinkedList<Pair<Integer,Integer>>();
		currentSegment.add(Pair.of(playerX, playerY));
		return move(direction, output, currentSegment);
	}
	
	//Turn the current space to ice and move past in the specified direction, continuing across ice and
	//bouncing off bumpers until hitting a non-ice, non-bumper space. Builds up drawings of the board
	//with the path overlaid after each bumper, and returns a list of strings to be sent to the player
	private LinkedList<String> move(Direction direction, LinkedList<String> output, 
			LinkedList<Pair<Integer,Integer>> currentSegment)
	{
		//If an exit has been used in the enhanced game, don't turn it to ice thanks
		if(getSpace(playerX,playerY).getType() != SpaceType.USED_EXIT)
			turnToIce(playerX, playerY);
		playerX += direction.deltaX;
		playerY += direction.deltaY;
		switch(getSpace(playerX,playerY).getType())
		{
		case ICE:
			move(direction, output, currentSegment);
			break;
		case BUMPER:
			currentSegment.add(Pair.of(playerX, playerY));
			StringBuilder bumperMessage = new StringBuilder();
			bumperMessage.append("```\n");
			if(direction.deltaX != 0)
				bumperMessage.append(drawHorizontalLine(currentSegment, getSpace(playerX,playerY).getDirection().arrow));
			else
				bumperMessage.append(drawVerticalLine(currentSegment, getSpace(playerX,playerY).getDirection().arrow));
			bumperMessage.append("```\n");
			bumperMessage.append(getBumperMessage());
			output.add(bumperMessage.toString());
			move(getSpace(playerX,playerY).getDirection(), output);
			break;
		case CASH:
			isFirstMove = false;
			winnings += getSpace(playerX,playerY).getValue();
			currentSegment.add(Pair.of(playerX, playerY));
			StringBuilder cashMessage = new StringBuilder();
			cashMessage.append("```\n");
			if(direction.deltaX != 0)
				cashMessage.append(drawHorizontalLine(currentSegment, '$'));
			else
				cashMessage.append(drawVerticalLine(currentSegment, '$'));
			cashMessage.append("```\n");
			cashMessage.append(String.format("**$%,d**", getSpace(playerX,playerY).getValue()));
			output.add(cashMessage.toString());
			output.add(drawScoreboard(false));
			break;
		case EXIT:
			exitsLeft --;
			if(enhanced)
				board[playerY][playerX] = new UsedExit();
			//Fall through
		case USED_EXIT:
			isFirstMove = false;
			if(!enhanced && exitsLeft == 0)
			{ //Foolproof if this is their last chance to actually leave
				output.add("You reached the last exit! It's time for you to escape!");
				escape();
			}
			else
			{
				output.add("You reached an exit! You can EXIT now, or keep going by picking another direction.");
				output.add(drawScoreboard(false));
			}
			break;
		case HOLE:
			gameOver = true;
			if(isFirstMove)
			{
				output.add("Geeze, you fell off on your first move? Here, have $100 on the house.");
				winnings = 100;
			}
			else
			{
				output.add("Oh no, you fell off the edge!");
				winnings = 0;
			}
			break;
		}
		return output;
	}
	
	private Space getSpace(int xCoord, int yCoord)
	{
		//x = 0 is the left column, y = 0 is the top row
		//If they asked for a coordinate off the map, give them a hole
		if(xCoord < 0 || xCoord >= boardWidth || yCoord < 0 || yCoord >= boardHeight)
			return new Hole();
		else
			return board[yCoord][xCoord];
	}
	private void turnToIce(int xCoord, int yCoord)
	{
		board[yCoord][xCoord] = new Ice();
	}
	private ArrayList<String> drawBoard(boolean showPlayer, boolean revealAll)
	{
		ArrayList<String> rows = new ArrayList<>(boardHeight);
		for(int y=0; y<boardHeight; y++)
		{
			StringBuilder output = new StringBuilder();
			for(int x=0; x<boardWidth; x++)
			{
				if(showPlayer && playerX == x && playerY == y)
					output.append("X");
				else
					switch (getSpace(x, y).getType()) {
						case BUMPER -> output.append(revealAll ? getSpace(x, y).getDirection().arrow : "?");
						case CASH -> output.append(revealAll ? "$" : "?");
						case EXIT, USED_EXIT -> output.append("O");
						case ICE -> output.append("-");
						case HOLE -> output.append(" ");
					}
				output.append(" ");
			}
			rows.add(output.toString());
		}
		return rows;
	}
	private String connectRows(ArrayList<String> rows)
	{
		StringBuilder output = new StringBuilder();
		for(String nextRow : rows)
		{
			output.append(nextRow);
			output.append("\n");
		}
		return output.toString();
	}
	private String drawScoreboard(boolean reveal)
	{
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		output.append(" ".repeat(Math.max(0, boardWidth - 6)));
		output.append("BUMPER GRAB\n");
		output.append(connectRows(drawBoard(true, reveal)));
		output.append(String.format("Total: $%,9d\n", winnings));
		output.append(String.format("      /$%,9d\n", maxWinnings));
		output.append("```");
		return output.toString();
	}
	private String drawHorizontalLine(LinkedList<Pair<Integer,Integer>> coords, char endCharacter)
	{
		//There's a space between each character, so x=n on the board maps to x=2n on the display
		ArrayList<String> rows = drawBoard(false, false);
		int xStart = coords.getFirst().getLeft() * 2;
		int xEnd = coords.getLast().getLeft() * 2;
		int yCoord = coords.getFirst().getRight();
		StringBuilder newRow = new StringBuilder();
		for(int x=0; x<boardWidth*2-1; x++)
		{
			if(x == xEnd)
				newRow.append(endCharacter);
			else if(x >= xStart && x <= xEnd || x >= xEnd && x <= xStart)
				newRow.append('-');
			else
				newRow.append(rows.get(yCoord).charAt(x));
		}
		rows.set(yCoord, newRow.toString());
		return connectRows(rows);
	}
	private String drawVerticalLine(LinkedList<Pair<Integer,Integer>> coords, char endCharacter)
	{
		ArrayList<String> rows = drawBoard(false, false);
		int yStart = coords.getFirst().getRight();
		int yEnd = coords.getLast().getRight();
		int xCoord = coords.getFirst().getLeft() * 2;
		for(int y=0; y<boardHeight; y++)
		{
			if(y == yEnd)
				rows.set(y,rows.get(y).substring(0,xCoord)+endCharacter+rows.get(y).substring(xCoord+1));
			else if(y >= yStart && y <= yEnd || y >= yEnd && y <= yStart)
				rows.set(y,rows.get(y).substring(0,xCoord)+'|'+rows.get(y).substring(xCoord+1));
		}
		return connectRows(rows);
	}
	private String getBumperMessage()
	{
		final String[] BUMPER_MESSAGES = {"PING","PONG","BOING","F'TAANG"};
		return "**" +
				BUMPER_MESSAGES[(int) (Math.random() * BUMPER_MESSAGES.length)] +
				"**";
	}
	
	private void escape()
	{
		gameOver = true;
		if(exitsLeft == 0)
		{
			if(winnings * 2 >= maxWinnings)
			{
				Achievement.BUMPER_JACKPOT.check(getCurrentPlayer());
			}
			else if(winnings == 0)
			{
				sendMessage("...You hit EVERY SINGLE EXIT but no cash?! You know what, just have all the cash. You deserve it.");
				winnings = maxWinnings;
				Achievement.BUMPER_JACKPOT.check(getCurrentPlayer());
			}
		}
		else if(winnings != 0)
			sendMessage("You made it out!");
		else
			sendMessage("You made it out...with no cash? You know there was cash there, right?");
	}

	@Override
	String getBotPick()
	{
		ArrayList<Direction> exitMoves = new ArrayList<>(4);
		ArrayList<Direction> nonExitMoves = new ArrayList<>(4);
		for(Direction direction : Direction.values())
		{
			Pair<Integer,Integer> newPosition = firstNonIceTile(direction, playerX, playerY);
			switch (getSpace(newPosition.getLeft(), newPosition.getRight()).getType()) {
				case BUMPER, CASH -> nonExitMoves.add(direction);
				case EXIT -> exitMoves.add(direction);
				default -> {
				}
				//Do nothing
			}
		}
		//Check if we can exit, and quit if we either have enough money or there's nowhere we can go to get more
		if(getSpace(playerX,playerY).isExit() &&
				(winnings > botWinningsTarget || nonExitMoves.size() == 0))
				return "EXIT";
		//Otherwise, check if we're screwed and pick randomly
		if(nonExitMoves.size() == 0 && exitMoves.size() == 0)
			return Direction.values()[(int)(Math.random()*4)].toString();
		//Decide whether we want to go toward or away from an exit
		if(nonExitMoves.size() == 0 || (winnings >= botWinningsTarget && exitMoves.size() != 0))
			return exitMoves.get((int)(Math.random()*exitMoves.size())).toString();
		else
			return nonExitMoves.get((int)(Math.random()*nonExitMoves.size())).toString();
	}
	
	private Pair<Integer,Integer> firstNonIceTile(Direction direction, int startX, int startY)
	{
		int currentX = startX + direction.deltaX;
		int currentY = startY + direction.deltaY;
		if(getSpace(currentX,currentY).getType() != SpaceType.ICE)
			return Pair.of(currentX, currentY);
		else
			return firstNonIceTile(direction, currentX, currentY);
	}

	@Override
	void abortGame()
	{
		//If they have the option to quit right now, they quit
		if(getSpace(playerX,playerY).getType() == SpaceType.EXIT)
			awardMoneyWon(winnings);
		//Otherwise they fall
		else
			awardMoneyWon(0);
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "Exits will not vanish and can be revisited."; }
}
