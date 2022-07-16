package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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
	int playersPerGame;
	final static int DEMO_DELAY = 45;
	public TextChannel channel;
	public ScheduledThreadPoolExecutor timer;
	public boolean loadingHumanGame;
	LinkedList<Integer> playerList = new LinkedList<>(); //Kept sorted, size should always be divisible by PLAYERS_PER_GAME
	LinkedList<int[]> gameList = new LinkedList<>();
	LinkedList<Pair<String,Integer>> humanCache = new LinkedList<>();
	int baseNumerator, baseDenominator;
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
		//Figure out the settings
		playersPerGame = Math.min(Math.max(4, gameHandler.minPlayers), gameHandler.maxPlayers);
		baseNumerator = gameHandler.baseNumerator;
		baseDenominator = gameHandler.baseDenominator;
		loadGames();
		return gameHandler;
	}
	public void loadGames()
	{
		List<String> list = null;
		try
		{
			list = Files.readAllLines(Paths.get("scores","schedule"+channel.getId()+".csv"));
		}
		catch (IOException e)
		{
			System.out.println("No SBC savefile found in "+channel.getId()+", commencing new Challenge.");
			startCampaign();
			return;
		}
		//Clear the existing challenge status
		loadingHumanGame = false;
		playerList.clear();
		gameList.clear();
		//Parse the player list
		String[] lineOne = list.get(0).split(" ");
		for(String next : lineOne)
			playerList.add(Integer.parseInt(next));
		gameHandler.baseNumerator = getMultiplier(playerList.size())*baseNumerator;
		gameHandler.baseDenominator = baseDenominator;
		//Parse the game list
		ListIterator<String> schedule = list.listIterator(2);
		gamesRun = 0;
		totalGames = 0;
		while(schedule.hasNext())
		{
			String[] record = schedule.next().split(" ");
			int[] players = new int[record.length];
			for(int i=0; i<record.length;i++)
				players[i] = Integer.parseInt(record[i]);
			gameList.add(players);
			totalGames ++;
		}
		//Deal with the result
		if(totalGames > 0)
		{
			runDemos = DEMO_DELAY;
			timer.schedule(() -> loadDemoGame(), runDemos, TimeUnit.MINUTES);
			channel.sendMessage(String.format("%d Players Remain, %d Games to Play", playerList.size(), totalGames)).queue();
		}
		else
			startRoundCycle(); //No games to play? New round cycle!
	}
	
	private void startCampaign()
	{
		for(int i=0; i<gameHandler.botCount; i++)
			playerList.add(i);
		startRoundCycle();
	}
	
	private void startRoundCycle()
	{
		//Set multiplier by playercount
		gameHandler.baseNumerator = getMultiplier(playerList.size())*baseNumerator;
		gameHandler.baseDenominator = baseDenominator;
		gameList.clear();
		loadingHumanGame = false;
		totalGames = playerList.size();
		gamesRun = 0;
		//Make deep copy of playerlist
		LinkedList<Integer> playerShuffle = new LinkedList<>();
		for(int next : playerList)
			playerShuffle.add(next);
		for(int i=0; i<playersPerGame; i++) //We run N rounds and then eliminate N players so the playercount remains divisible
		{
			//Skip scheduling the very final game (the 80th under default settings) so we can run an epic finale manually if need be
			if(i == (playerList.size() - 1))
			{
				totalGames --;
				break;
			}
			//Randomly shuffle all the players into games
			Collections.shuffle(playerShuffle);
			int[][] next = new int[playerList.size()/playersPerGame][playersPerGame];
			for(int j=0; j<playerShuffle.size(); j++)
			{
				int positionInGame = j%playersPerGame;
				next[j/playersPerGame][positionInGame] = playerShuffle.get(j);
				if(positionInGame+1 == playersPerGame)
				{
					gameList.add(next[j/playersPerGame]);
				}
			}
		}
		saveData();
		if(channel.getId().equals("485729867275436032")) //Lazily hardcoding in the main SBC channel + Challenger role for the ping
		{
			channel.sendMessage("<@&586732055166189568> A new Round Cycle is beginning! Use !ready to see your games.").queue();
			channel.getManager().setTopic(String.format("~ CHALLENGE CHANNEL ~ %d Players Remain ~ x%d Multiplier", 
					playerList.size(), getMultiplier(playerList.size()))).queue();
		}
		else
		{
			channel.sendMessage("A new Round Cycle is beginning! Use !ready to see your games.").queue();
		}
		runDemos = DEMO_DELAY;
		timer.schedule(() -> loadDemoGame(), runDemos, TimeUnit.MINUTES);
	}
	
	private void endRoundCycle()
	{
		//If it's the very last round cycle, just abort
		if(playerList.size() == playersPerGame)
		{
			saveData();
			return;
		}
		channel.sendMessage("**ROUND CYCLE COMPLETE!**").queue();
		//Find the lowest-scoring players to eliminate
		try
		{
			Path gameSavefile = Paths.get("scores","scores"+channel.getId()+".csv");
			Path oldGameSavefile = gameSavefile.resolveSibling("scores"+channel.getId()+"old.csv");
			Path eliminatedSavefile = Paths.get("scores","eliminated"+channel.getId()+".csv");
			Path oldEliminatedSavefile = eliminatedSavefile.resolveSibling("eliminated"+channel.getId()+"old.csv");
			List<String> aliveScores = Files.readAllLines(gameSavefile);
			String nextRecord;
			String[] record;
			//We start one record before we need to so we can check for a tie, which shouldn't be broken arbitrarily
			ListIterator<String> nextScore = aliveScores.listIterator(playerList.size()-(playersPerGame+1));
			record = nextScore.next().split("#");
			int benchmarkScore = Integer.parseInt(record[2]); //Get the last safe player's money
			//Then start going through
			List<String> eliminatedScores = new LinkedList<>();
			List<String> eliminatedNames = new LinkedList<>();
			StringBuilder output = new StringBuilder();
			output.append("```\n");
			int nextRank = nextScore.nextIndex();
			while(nextScore.hasNext())
			{
				nextRecord = nextScore.next();
				nextRank ++;
				record = nextRecord.split("#");
				if(Integer.parseInt(record[2]) >= benchmarkScore)
				{
					channel.sendMessage("Elimination failed: There's a tie?!?!").queue();
					return;
				}
				eliminatedNames.add(record[1]);
				//Add their score to the message
				output.append("#");
				output.append(nextRank);
				output.append(String.format(": $%,13d", Integer.parseInt(record[2])));
				output.append(" - " + record[1]);
				output.append("\n");
				//And remove the eliminated player from the scoreboard
				eliminatedScores.add(nextRecord);
				nextScore.remove();
			}
			//So now eliminatedScores has the records of the players that are going home
			//and output has the eliminated players to send off
			channel.sendMessage("ELIMINATED PLAYERS:").queue();
			output.append("```");
			channel.sendMessage(output.toString()).queue();
			channel.sendMessage(aliveScores.size() + " Players Remain.").queue();
			ListIterator<Integer> nextPlayer = playerList.listIterator();
			//Figure out who they were
			while(nextPlayer.hasNext())
			{
				int next = nextPlayer.next();
				GameBot nextBot = new GameBot(channel.getGuild().getId(), next);
				//If they're in the list of casualties, excise them
				if(eliminatedNames.contains(nextBot.getName()))
					nextPlayer.remove();
			}
			//Add the previously-eliminated-players to the big bad list
			try
			{
				List<String> list = Files.readAllLines(eliminatedSavefile);
				eliminatedScores.addAll(list);
			}
			catch(IOException e)
			{
				//Swallow this because we really don't care if it doesn't exist and if there's a bigger error everything else will pick it up
				//Plus even if we somehow seriously manage to lose this data we can reconstruct it
			}
			//Send them away, death-sensei
			Files.move(gameSavefile, oldGameSavefile);
			Files.write(gameSavefile, aliveScores);
			Files.delete(oldGameSavefile);
			if(Files.exists(eliminatedSavefile))
			{
				Files.move(eliminatedSavefile, oldEliminatedSavefile);
				Files.write(eliminatedSavefile, eliminatedScores);
				Files.delete(oldEliminatedSavefile);
			}
			else
				Files.write(eliminatedSavefile, eliminatedScores);
			//AGAIN, AGAIN!
			startRoundCycle();
		}
		catch(IOException e)
		{
			channel.sendMessage("Failed to execute players eliminated.").queue(); //puhuhuhuhu
			saveData();
			return;
		}
	}
	
	private void saveData()
	{
		//Build the schedule file
		LinkedList<String> list = new LinkedList<>();
		//First the list of non-eliminated players
		StringBuilder players = new StringBuilder();
		for(int next : playerList)
		{
			players.append(String.format("%02d ", next));
		}
		list.add(players.toString());
		list.add("--------");
		//Now each game to play, one per line
		for(int[] nextGame : gameList)
		{
			StringBuilder game = new StringBuilder();
			for(int next : nextGame)
				game.append(String.format("%02d ", next));
			list.add(game.toString());
		}
		//and save!
		try
		{
			Path file = Paths.get("scores","schedule"+channel.getId()+".csv");
			if(Files.exists(file))
			{
				Path oldFile;
				oldFile = Files.move(file, file.resolveSibling("schedule"+channel.getId()+"old.csv"));
				Files.write(file, list);
				Files.delete(oldFile);
			}
			else
				Files.write(file, list);
		}
		catch(IOException e)
		{
			System.err.println("Could not save SBC data in "+channel.getName());
			e.printStackTrace();
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
					if(!new GameBot(channel.getGuild().getId(),next).getHuman().equals("null"))
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
				prepGame(currentGame.previousIndex());
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
		/*
		if(gameHandler.players.size() > 0)
		{
			channel.sendMessage("Wait until after the current game.").queue();
			return;
		}
		*/
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
			/* We want it to always ask them for now
			 * since !ready is how they check their matchups
		case 1:
			//If we found exactly one, load it up right away
			loadHumanGame(gamesWithPlayer.get(0), humanID);
			break;
			*/
		default:
			//If we found multiple games, list them and ask which they want to run
			try
			{
				channel.sendMessage("Which game would you like to play?").queue();
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
				channel.sendMessage("0 | Don't play now").queue();
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
								return index >= 0 && index <= gamesWithPlayer.size();
							}
							catch(NumberFormatException ex)
							{
								return false;
							}
						}
						return false;
					},
					e -> 
					{
						if(e.getMessage().getContentRaw().equals("0"))
						{
							channel.sendMessage("Very well.").queue();
							loadingHumanGame = false;
						}
						else
							loadHumanGame(gamesWithPlayer.get(Integer.parseInt(e.getMessage().getContentRaw())-1),humanID);
					},
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
		catch(Exception e)
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
				if(!playerID.equals("null") && !playerID.equals(humanID))
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
			prepGame(index);
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
					prepGame(gameToLoad);
				}
				break;
			}
	}
	
	void prepGame(int gameToPlay)
	{
		Thread endOfGameTasks = new Thread(() -> {
			gameList.remove(gameToPlay);
			saveData();
			if(gameList.size() == 0)
				endRoundCycle();
		});
		int[] players = gameList.get(gameToPlay);
		loadingHumanGame = false;
		for(int next : players)
			gameHandler.addBot(next);
		gameHandler.runAtGameEnd = endOfGameTasks;
		gamesRun++;
		channel.sendMessage(String.format("**Game %02d/%02d**", gamesRun, totalGames)).queue();
		channel.sendMessage(gameHandler.listPlayers(false)).queueAfter(2, TimeUnit.SECONDS);
		timer.schedule(() -> gameHandler.startTheGameAlready(), 5, TimeUnit.SECONDS);
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