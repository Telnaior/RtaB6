package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.MessageChannel;
import tel.discord.rtab.Player;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.board.Game;

public class TheCommissioner extends MiniGameWrapper
{
	ArrayList<Game> gameList = new ArrayList<>();
	
	//We're overriding this to remove the announcement code so that it doesn't display this as a minigame itself
	@Override
	public void initialiseGame(MessageChannel channel, boolean sendMessages, int baseNumerator, int baseDenominator,
			int gameMultiplier, List<Player> players, int player, Thread callWhenFinished, boolean enhanced)
	{
		//Initialise variables
		this.channel = channel;
		this.sendMessages = sendMessages;
		this.baseNumerator = baseNumerator;
		this.baseDenominator = baseDenominator;
		this.gameMultiplier = gameMultiplier;
		this.players = players;
		this.player = player;
		this.callWhenFinished = callWhenFinished;
		this.enhanced = enhanced;
		//Set up the threadpool
		timer = new ScheduledThreadPoolExecutor(1, new MinigameThreadFactory());
		//Then pass over to minigame-specific code
		timer.schedule(this::startGame, 1000, TimeUnit.MILLISECONDS);
	}

	@Override
	void startGame()
	{
		//Roll some minigames!
		HashSet<Game> gameList = new HashSet<>();
		//Two enhance-weighted games
		for(int i=0; i<2; i++)
			gameList.add(getPlayer().generateEventMinigame());
		//and 3-5 random games
		gameList.addAll(Board.generateSpaces(enhanced ? 5 : 3, 4, Game.values()));
		//Remove anything we already have in our minigame queue
		getPlayer().games.forEach(gameList::remove);
		//Then if we don't have enough minigames to choose from, roll some more
		while(gameList.size() < (enhanced ? 5 : 3))
		{
			gameList.add(Board.generateSpace(4, Game.values()));
		}
		//Also remove ourselves so we don't get stuck in an endless loop
		gameList.remove(Game.COMMISSIONER);
		//Then put everything into our list
		this.gameList.addAll(gameList);
		//Game list is now finalised, show it to them!
		sendMessages(giveInstructions());
		getInput();
	}
	
	private LinkedList<String> giveInstructions()
	{
		LinkedList<String> output = new LinkedList<>();
		output.add(getPlayer().getSafeMention() + ", The Commissioner offers you the following choice of minigames:");
		if(gameMultiplier > 1)
			output.add(String.format("(You have %1$d commissions, so your minigame will be played for x%1$d stakes)", gameMultiplier));
		output.add(generateBoard());
		output.add("Please state the number corresponding to the minigame you would like.");
		return output;
	}
	
	private String generateBoard()
	{
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		for(int i=0; i<gameList.size(); i++)
			output.append(String.format("%d - %s%n", (i+1), gameList.get(i).getName()));
		output.append("```");
		return output.toString();
	}

	@Override
	void playNextTurn(String input)
	{
		//Let's figure out what they just picked
		Game chosenGame = null;
		//Start by checking for the name
		for(Game next : gameList)
			if(input.equalsIgnoreCase(next.getName()) || input.equalsIgnoreCase(next.getShortName()))
			{
				chosenGame = next;
				break;
			}
		//If they didn't give a name (I mean we did tell them to give a number), look by number
		if(chosenGame == null)
		{
			try
			{
				int number = Integer.parseInt(input);
				if(number > 0 && number <= gameList.size())
					chosenGame = gameList.get(number-1);
			}
			catch(NumberFormatException ignored) {}//Ignore if it's not a number because we're about to catch it anyway
		}
		//If they didn't give a valid number either, try asking again
		if(chosenGame == null)
		{
			getInput();
			return;
		}
		//Okay, we got a game from them, let's set it up to play!
		chosenGame.getGame().initialiseGame(channel, sendMessages, baseNumerator, baseDenominator, gameMultiplier,
				players, player, new Thread(this::gameOver), getPlayer().enhancedGames.contains(chosenGame));
	}

	@Override
	void abortGame()
	{
		//Just forget about it lol
		gameOver();
	}

	@Override
	String getBotPick()
	{
		//Pick the top one!
		return "1";
	}

	@Override
	public String getName()
	{
		return "The Commissioner";
	}

	@Override
	public String getShortName()
	{
		return "Commission";
	}

	@Override
	public boolean isBonus()
	{
		return true;
	}
	
	@Override
	public String getEnhanceText()
	{
		return "Two more minigames will be offered for you to choose from.";
	}
}
