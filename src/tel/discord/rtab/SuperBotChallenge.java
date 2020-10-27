package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static tel.discord.rtab.RaceToABillionBot.waiter;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;

public class SuperBotChallenge
{
	GameController gameHandler;
	public TextChannel channel;
	public ScheduledThreadPoolExecutor timer;
	public boolean loadingHumanGame;
	List<int[]> gameList = new LinkedList<>();
	LinkedList<Pair<String,Integer>> humanCache = new LinkedList<>();
	int runDemos;
	int gamesRun;
	int totalGames;
	int gameToLoad;
	List<String> missingPlayers;
	
	class HandlerThreadFactory implements ThreadFactory
	{
		@Override
		public Thread newThread(Runnable r)
		{
			Thread newThread = new Thread(r);
			newThread.setName(String.format("Challenge Handler - %s", channel.getName()));
			return newThread;
		}
	}
	
	public GameController initialise(TextChannel gameChannel, String[] record, TextChannel resultChannel)
	{
		channel = gameChannel;
		timer = new ScheduledThreadPoolExecutor(1, new HandlerThreadFactory());
		gameHandler = new GameController(gameChannel, record, resultChannel);
		gameHandler.playersCanJoin = false;
		return gameHandler;
	}
	public void loadGames(int demoDelay)
	{
		//Format is "~ CHALLENGE CHANNEL ~ XX Players Remain"
		int playersLeft = Integer.parseInt(channel.getTopic().substring(22,24));
		int multiplier = getMultiplier(playersLeft);
		gameHandler.baseNumerator *= multiplier;
		timer.shutdownNow();
		gameList.clear();
		loadingHumanGame = false;
		timer = new ScheduledThreadPoolExecutor(1);
		List<String> list = null;
		try
		{
			list = Files.readAllLines(Paths.get("schedule"+channel.getId()+".csv"));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		ListIterator<String> schedule = list.listIterator(0);
		gamesRun = 0;
		totalGames = 0;
		while(schedule.hasNext())
		{
			String[] record = schedule.next().split("	");
			int[] players = new int[record.length];
			for(int i=0; i<record.length;i++)
				players[i] = Integer.parseInt(record[i]);
			gameList.add(players);
			totalGames ++;
		}
		if(totalGames > 0)
		{
			runDemos = demoDelay;
			timer.schedule(() -> loadDemoGame(), demoDelay, TimeUnit.MINUTES);
			channel.sendMessage(totalGames + " games loaded.").queue();
		}
	}
	
	void loadDemoGame()
	{
		//If the season's over, just abort this and don't schedule another
		if(gameHandler.gameStatus == GameStatus.SEASON_OVER)
			return;
		//If there's already a game running or being set up, reschedule for another time
		if(loadingHumanGame || gameHandler.players.size() > 0)
		{
			timer.schedule(() -> loadDemoGame(), runDemos, TimeUnit.MINUTES);
			return;
		}
		//Run through the list of games
		ListIterator<int[]> currentGame = gameList.listIterator(0);
		while(currentGame.hasNext())
		{
			boolean botsOnly = true;
			int[] players = currentGame.next();
			try
			{
				//Check to make sure there's only bots in this game
				for(int next : players)
					if(new GameBot(channel.getGuild().getId(),next).getHuman() != null)
					{
						botsOnly = false;
						break;
					}
			}
			catch(IOException e)
			{
				channel.sendMessage("Bot creation failed.").queue();
				e.printStackTrace();
				return;
			}
			//If there are, load it, pop it off the list, and schedule to look for another demo later
			if(botsOnly)
			{
				prepGame(players);
				currentGame.remove();
				timer.schedule(() -> loadDemoGame(), runDemos, TimeUnit.MINUTES);
				break;
			}
		}
		//If we never found a good demo game that means we're out of demos, so we don't schedule another
	}
	
	public void searchForHumanGame(String humanID)
	{
		//If the season's over, just tell them and exit
		if(gameHandler.gameStatus == GameStatus.SEASON_OVER)
		{
			channel.sendMessage("The season is already over!").queue();
			return;
		}
		//If there's already a game running, make them wait
		if(gameHandler.players.size() > 0)
		{
			channel.sendMessage("Wait until after the current game.").queue();
			return;
		}
		//Check which bot they represent, and cut it off early if they aren't any of them
		int botNumber = getBotFromHuman(humanID);
		if(botNumber == -1)
		{
			channel.sendMessage("You are not a player in the Super Bot Challenge.").queue();
			return;
		}
		//Now we've passed all the initial checks, mark that we're in the human process so that a demo doesn't cut us off
		loadingHumanGame = true;
		//Run through the list of games
		ListIterator<int[]> currentGame = gameList.listIterator(0);
		List<Integer> gamesWithPlayer = new ArrayList<>(4);
		while(currentGame.hasNext())
		{
			//Check to see if the command caller is here
			boolean playerInGame = false;
			int[] players = currentGame.next();
			for(int next : players)
				if(next == botNumber)
				{
					playerInGame = true;
					break;
				}
			//If they're here, add them to the set
			if(playerInGame)
			{
				gamesWithPlayer.add(currentGame.previousIndex());
			}
		}
		//Now we've got a list of games with the command caller, switch based on how many we found
		switch(gamesWithPlayer.size())
		{
		case 0:
			//If we didn't find any, what are they doing? Just exit
			channel.sendMessage("No scheduled games found.").queue();
			loadingHumanGame = false;
			break;
		case 1:
			//If we found exactly one, load it up right away
			loadHumanGame(gamesWithPlayer.get(0), humanID);
			break;
		default:
			//If we found multiple games, list them and ask which they want to run
			try
			{
				channel.sendMessage("Multiple games found. Which one would you like to play now?").queue();
				for(int i=0; i<gamesWithPlayer.size(); i++)
				{
					StringBuilder output = new StringBuilder();
					output.append(i+1);
					int[] botList = gameList.get(gamesWithPlayer.get(i));
					for(int next : botList)
					{
						output.append(" | ");
						output.append(new GameBot(channel.getGuild().getId(),next).getName());
					}
					channel.sendMessage(output).queue();
				}
			}
			catch(IOException e)
			{
				channel.sendMessage("Bot creation failed.").queue();
				e.printStackTrace();
				return;
			}
			waiter.waitForEvent(MessageReceivedEvent.class,
					//Right player and channel
					e ->
					{
						if(e.getAuthor().getId().equals(humanID) && e.getChannel().equals(channel))
						{
							//Make sure it's a number and actually fits the range
							try
							{
								int index = Integer.parseInt(e.getMessage().getContentRaw());
								return index > 0 && index <= gamesWithPlayer.size();
							}
							catch(NumberFormatException ex)
							{
								return false;
							}
						}
						return false;
					},
					e -> loadHumanGame(gamesWithPlayer.get(Integer.parseInt(e.getMessage().getContentRaw())-1),humanID),
					30,TimeUnit.SECONDS, () -> 
					{
						channel.sendMessage("Timed out. !ready again when you decide.").queue();
						loadingHumanGame = false;
					});
			break;
		}
	}
	
	private int getBotFromHuman(String humanID)
	{
		//First, check the cache
		for(Pair<String,Integer> nextCachedPlayer : humanCache)
			if(humanID.equals(nextCachedPlayer.getLeft()))
				return nextCachedPlayer.getRight();
		//If we haven't found them there, search through every bot until we find it or hit an exception
		try
		{
			int i = -1;
			while(true)
			{
				i++;
				if(humanID.equals(new GameBot(channel.getGuild().getId(),i).getHuman()))
				{
					//Add them to the cache, then get out of here
					humanCache.add(Pair.of(humanID, i));
					return i;
				}
			}
		}
		catch(IOException e)
		{
			//If we run out of bots to check we'll end up here, so just return a failure
			return -1;
		}
	}
	
	void loadHumanGame(int index, String humanID)
	{
		//Get a list of other humans in the game
		missingPlayers = new ArrayList<>(3);
		int[] players = gameList.get(index);
		for(int next : players)
		{
			try
			{
				String playerID = new GameBot(channel.getGuild().getId(),next).getHuman();
				if(playerID != null && !playerID.equals(humanID))
					missingPlayers.add(playerID);
			}
			catch(IOException e)
			{
				channel.sendMessage("Bot creation failed.").queue();
				e.printStackTrace();
				return;
			}
		}
		//If there aren't any, just load it up
		if(missingPlayers.size() == 0)
		{
			gameList.remove(index);
			prepGame(players);
		}
		//If there are, give them 30 seconds to confirm that they're here too
		else
		{
			gameToLoad = index;
			//Ping everyone missing
			for(String nextPlayer : missingPlayers)
			{
				channel.sendMessage(String.format("<@!%s>, are you there? Type !ready if you are!", nextPlayer)).queue();
			}
			//Then if they aren't here, reset the whole thing
			timer.schedule(() -> 
			{
				if(loadingHumanGame)
				{
					channel.sendMessage("Other players aren't here. Game aborted.").queue();
					loadingHumanGame = false;
				}
			}, 30, TimeUnit.SECONDS);
		}
	}
	
	public void readyUp(String humanID)
	{
		//Strike them off the list of people we're waiting on, then start the game if everyone's here
		for(int i=0; i<missingPlayers.size(); i++)
			if(missingPlayers.get(i).equals(humanID))
			{
				missingPlayers.remove(i);
				if(missingPlayers.size() == 0)
				{
					prepGame(gameList.get(gameToLoad));
					gameList.remove(gameToLoad);
				}
				break;
			}
	}
	
	void prepGame(int[] players)
	{
		loadingHumanGame = false;
		for(int next : players)
			gameHandler.addBot(next);
		channel.sendMessage("Next game starting in five minutes:").queue();
		gamesRun++;
		channel.sendMessage(String.format("**Game %02d/%02d**", gamesRun, totalGames)).queue();
		channel.sendMessage(gameHandler.listPlayers(false)).queue();
		timer.schedule(() -> gameHandler.startTheGameAlready(), 5, TimeUnit.MINUTES);
	}
	
	int getMultiplier(int playersLeft)
	{
		int multiplier;
		switch(playersLeft)
		{
		case 4:
			multiplier = 10;
			break;
		case 8:
			multiplier = 9;
			break;
		case 12:
			multiplier = 8;
			break;
		case 16:
		case 20:
			multiplier = 7;
			break;
		case 24:
		case 28:
			multiplier = 6;
			break;
		case 32:
		case 36:
			multiplier = 5;
			break;
		case 40:
		case 44:
			multiplier = 4;
			break;
		case 48:
		case 52:
		case 56:
			multiplier = 3;
			break;
		case 60:
		case 64:
		case 68:
			multiplier = 2;
			break;
		case 72:
		case 76:
		case 80:
			multiplier = 1;
			break;
		default:
			multiplier = 1;
			channel.sendMessage("Multiplier not initialised properly!").queue();
		}
		return multiplier;
	}
}