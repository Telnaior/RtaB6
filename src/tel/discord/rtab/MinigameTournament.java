package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import tel.discord.rtab.GameController.DescendingScoreSorter;
import tel.discord.rtab.board.Game;
import tel.discord.rtab.games.MiniGame;

public class MinigameTournament
{
	public TournamentStatus status;
	public TextChannel channel, resultChannel;
	public ScheduledThreadPoolExecutor timer;
	public MiniGame currentGame;
	int round;
	int enhancements;
	int minimumToQualify;
	int botCount;
	int demoDelay;
	Game[] minigameList;
	LinkedList<Rank> rankList;
	
	public enum TournamentStatus
	{
		LOADING, OPEN, PLAYING, SHUTDOWN;
	}
	
	private class Rank
	{
		private String name;
		private int requirement;
		Rank(String rankString)
		{
			String[] record = rankString.split("#");
			name = record[0];
			requirement = Integer.parseInt(record[1]);
		}
		String getName()
		{
			return name;
		}
		int getRequirement()
		{
			return requirement;
		}
	}

	class MinigameTournamentThreadFactory implements ThreadFactory
	{
		@Override
		public Thread newThread(Runnable r)
		{
			Thread newThread = new Thread(r);
			newThread.setName(String.format("Minigame Tournament - %s", channel.getName()));
			return newThread;
		}
	}
	
	public MinigameTournament(TextChannel channel, String[] record, TextChannel resultChannel)
	{
		status = TournamentStatus.LOADING;
		this.channel = channel;
		this.resultChannel = resultChannel;
		timer = new ScheduledThreadPoolExecutor(1, new MinigameTournamentThreadFactory());
		loadConfigFile();
		botCount = Integer.parseInt(record[4]);
		demoDelay = Integer.parseInt(record[5]);
		if(botCount > 0 && demoDelay > 0)
			timer.schedule(this::runDemo,demoDelay,TimeUnit.MINUTES);
		//Create the savefile if it doesn't already exist
		if(!Files.exists(Paths.get("scores","scores"+channel.getId()+".csv")))
			try
			{
				Files.createFile(Paths.get("scores","scores"+channel.getId()+".csv"));
			}
			catch (IOException e1)
			{
				channel.sendMessage("Couldn't create savefile. Oops.").queue();
				e1.printStackTrace();
				return;
			}
		//And declare ourselves open for business!
		status = TournamentStatus.OPEN;
		channel.sendMessage("Type !ready to play!").queue();
	}
	
	void loadConfigFile()
	{
		List<String> list = null;
		try
		{
			list = Files.readAllLines(Paths.get("scores","tournament"+channel.getId()+".csv"));
		}
		catch (IOException e)
		{
			channel.sendMessage("Failed to load Minigame Tournament configuration.").queue();
			return;
		}
		round = Integer.parseInt(list.get(0));
		enhancements = Integer.parseInt(list.get(1));
		minimumToQualify = Integer.parseInt(list.get(2));
		String[] gameList = list.get(3).split("#");
		minigameList = new Game[gameList.length];
		for(int i=0; i<gameList.length; i++)
			minigameList[i] = Game.valueOf(gameList[i]);
		rankList = new LinkedList<Rank>();
		for(int i=4; i<list.size(); i++)
			rankList.add(new Rank(list.get(i)));
	}
	
	void runDemo()
	{
		//If it's not a good time, try again later
		if(status != TournamentStatus.OPEN)
		{
			timer.schedule(this::runDemo,demoDelay,TimeUnit.MINUTES);
			return;
		}
		//Okay, hold the status and let's find a bot
		try
		{
			List<String> list = Files.readAllLines(Paths.get("scores","scores"+channel.getId()+".csv"));
			status = TournamentStatus.PLAYING;
			GameBot chosenBot;
			boolean goodChoice = false;
			int nextBot = -1;
			int botMoney = 0;
			do
			{
				nextBot ++;
				chosenBot = new GameBot(channel.getGuild().getId(),nextBot);
				//Search for the bot already in the savefile
				boolean botFound = false;
				for(String next : list)
				{
					String[] record = next.split("#");
					if(chosenBot.getBotID().equals(record[0]))
					{
						botFound = true;
						//Validate them if the last round they played was the round before this one and they earned enough to qualify
						goodChoice = (Integer.parseInt(record[3]) == round-1) && (Integer.parseInt(record[2]) >= minimumToQualify);
						botMoney = Integer.parseInt(record[2]);
						break;
					}
				}
				//If we didn't find the bot, validate them if this is round 1
				if(!botFound)
				{
					goodChoice = (round == 1);
					botMoney = 0;
				}
			}
			while(!goodChoice && nextBot < botCount);
			//If no bots are eligible at all, stop scheduling demos
			if(!goodChoice)
			{
				status = TournamentStatus.OPEN;
				return;
			}
			//Otherwise send them in with however much money they already have (if any)
			Player newPlayer = new Player(chosenBot);
			newPlayer.money = botMoney;
			runTournamentRound(newPlayer);
			timer.schedule(this::runDemo,demoDelay,TimeUnit.MINUTES);
		}
		catch(IOException e)
		{
			timer.schedule(this::runDemo,demoDelay,TimeUnit.MINUTES);
		}
	}
	
	public void runHuman(Member member)
	{
		//Check their eligibility
		try
		{
			List<String> list = Files.readAllLines(Paths.get("scores","scores"+channel.getId()+".csv"));
			status = TournamentStatus.PLAYING;
			Player newPlayer = new Player(member);
			boolean playerFound = false;
			for(String next : list)
			{
				String[] record = next.split("#");
				if(newPlayer.uID.equals(record[0]))
				{
					playerFound = true;
					//Confirm eligibility
					int previousWinnings = Integer.parseInt(record[2]);
					int lastPlayedRound = Integer.parseInt(record[3]);
					if(lastPlayedRound == round)
					{
						channel.sendMessage("You have already played this round of the tournament.").queue();
						status = TournamentStatus.OPEN;
						return;
					}
					if(lastPlayedRound < round-1 || previousWinnings < minimumToQualify)
					{
						channel.sendMessage("You have been eliminated from the tournament.").queue();
						status = TournamentStatus.OPEN;
						return;
					}
					//Lock in their money
					newPlayer.money = previousWinnings;
					break;
				}
			}
			//If we didn't find them, they only get to play if it's round 1
			if(!playerFound && round > 1)
			{
				channel.sendMessage("You have been eliminated from the tournament.").queue();
				status = TournamentStatus.OPEN;
				return;
			}
			//We made it here, pass them on to play!
			runTournamentRound(newPlayer);
		}
		catch(IOException e)
		{
			channel.sendMessage("Failed to load save file.").queue();
		}
	}
	
	void runTournamentRound(Player player)
	{
		runTournamentRound(player, new HashSet<Integer>());
	}
	
	@SuppressWarnings("unlikely-arg-type")
	void runTournamentRound(Player player, HashSet<Integer> enhancedGames)
	{
		//Determine enhancements if they aren't already set
		if(enhancedGames.size() < enhancements)
		{
			if(player.isBot)
				while(enhancedGames.size() < enhancements)
					enhancedGames.add((int)(Math.random()*minigameList.length));
			else
			{
				//Prepare the minigame list and tell them they need to enhance something
				StringBuilder enhanceMessage = new StringBuilder();
				enhanceMessage.append(String.format("You have %d enhancement%s to allocate. Please choose a minigame to enhance.%n"
						, enhancements-enhancedGames.size(), enhancements-enhancedGames.size() != 1 ? "s" : ""));
				enhanceMessage.append("Enhancing a minigame will **double** its payout and provide a game-specific effect:\n");
				enhanceMessage.append("```\n");
				for(int i=0; i<minigameList.length; i++)
					if(!enhancedGames.contains(i))
						enhanceMessage.append(String.format("%d - %s: %s%n"
								, i+1, minigameList[i].getName(), minigameList[i].getEnhanceText()));
				enhanceMessage.append("```");
				channel.sendMessage(enhanceMessage.toString()).queue();
				//And ask for their input
				RaceToABillionBot.waiter.waitForEvent(MessageReceivedEvent.class,
						//Right player and channel, they sent a valid choice
						e ->
								(e.getChannel().equals(channel) && e.getAuthor().equals(player.user)
										&& checkValidEnhanceNumber(e.getMessage().getContentStripped(), enhancedGames)),
						//Parse it and call the method that does stuff
						e -> 
						{
							enhancedGames.add(Integer.parseInt(e.getMessage().getContentStripped())-1);
							timer.schedule(() -> runTournamentRound(player, enhancedGames), 500, TimeUnit.MILLISECONDS);
						},
						120,TimeUnit.SECONDS, () ->
						{
							channel.sendMessage(player.getName() + 
									" has gone missing. Resetting...").queue();
							status = TournamentStatus.OPEN;
						});
				return;
			}
		}
		//The enhancements are locked in, let's prepare the variables and get going!
		int pastWinnings = player.money;
		int[] moneyWon = new int[minigameList.length];
		//Start the first minigame
		channel.sendMessage(player.getName() + ", let's begin!").queue();
		try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		runNextTournamentMinigame(player, enhancedGames, pastWinnings, moneyWon, 0);
	}
	
	boolean checkValidEnhanceNumber(String message, HashSet<Integer> enhancedGames)
	{
		try
		{
			int choice = Integer.parseInt(message)-1;
			return (choice >= 0 && choice < minigameList.length && !enhancedGames.contains(choice));
		}
		catch(NumberFormatException e)
		{
			return false;
		}
	}
	
	void runNextTournamentMinigame(Player player, HashSet<Integer> enhancedGames, int pastWinnings, int[] moneyWon, int gameNumber)
	{
		//if we're out of games to run, abort
		if(gameNumber >= minigameList.length)
		{
			endTournamentRound(player, enhancedGames, pastWinnings, moneyWon);
			return;
		}
		//Otherwise set up the next game
		Game game = minigameList[gameNumber];
		boolean enhancedThisGame = enhancedGames.contains(gameNumber);
		player.oldMoney = player.money; //We read this later to determine how much they won from this minigame
		//Print the status display
		try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		StringBuilder statusDisplay = new StringBuilder();
		statusDisplay.append(String.format("**Game %d/%d: %s**%n", gameNumber+1, minigameList.length, game.getName()));
		statusDisplay.append(String.format("Total Cash So Far: **$%,d**", player.money));
		channel.sendMessage(statusDisplay.toString()).queue();
		try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		//Set up the thread for after the game ends
		Thread postGame = new Thread(() -> {
			//Update variables, then recurse to get to the next minigame
			currentGame = null;
			moneyWon[gameNumber] = player.money - player.oldMoney;
			if(status != TournamentStatus.SHUTDOWN) //If we're shutting down, don't keep going wtf
				runNextTournamentMinigame(player, enhancedGames, pastWinnings, moneyWon, gameNumber+1);
		});
		postGame.setName(String.format("Minigame Tournament - %s - %s", player.getName(),game.getName()));
		currentGame = game.getGame();
		currentGame.initialiseGame(channel, true, enhancedThisGame?2:1, 1, 1, Arrays.asList(player), 0, postGame, enhancedThisGame);
	}
	
	void endTournamentRound(Player player, HashSet<Integer> enhancedGames, int pastWinnings, int[] moneyWon)
	{
		//Display result panel
		String resultPanel = getResultPanel(player, enhancedGames, pastWinnings, moneyWon);
		channel.sendMessage(resultPanel).queue();
		if(resultChannel != null)
			resultChannel.sendMessage(resultPanel).queue();
		saveData(player);
		status = TournamentStatus.OPEN;
	}
	
	String getResultPanel(Player player, HashSet<Integer> enhancedGames, int pastWinnings, int[] moneyWon)
	{
		StringBuilder resultString = new StringBuilder();
		resultString.append("```\n");
		resultString.append(player.getName()+" - Round "+round+"\n\n");
		if(pastWinnings > 0)
			resultString.append(String.format("Previous Winnings: $%,d%n%n", pastWinnings));
		for(int i=0; i<minigameList.length; i++)
			resultString.append(String.format("%s%s: $%,d%n", enhancedGames.contains(i)?"*":"", minigameList[i].getName(), moneyWon[i]));
		resultString.append(String.format("%nTOTAL: $%,d%n", player.money));
		resultString.append(String.format("RANK: %s%n", getRank(player.money)));
		resultString.append("```");
		return resultString.toString();
	}
	
	String getRank(int money)
	{
		for(Rank next : rankList)
			if(next.getRequirement() <= money)
				return next.getName();
		return "No Rank";
	}
	
	void saveData(Player player)
	{
		try
		{
			List<String> list = Files.readAllLines(Paths.get("scores","scores"+channel.getId()+".csv"));
			//Find if they already exist in the savefile, and where
			String[] record;
			int location = -1;
			for(int j=0; j<list.size(); j++)
			{
				record = list.get(j).split("#");
				if(record[0].compareToIgnoreCase(player.uID) == 0)
				{
					location = j;
					break;
				}
			}
			StringBuilder toPrint = new StringBuilder();
			toPrint.append(player.uID);
			toPrint.append("#").append(player.getName());
			toPrint.append("#").append(player.money);
			toPrint.append("#").append(round);
			//If they already exist in the savefile then replace their record, otherwise add them
			if(location == -1)
				list.add(toPrint.toString());
			else
				list.set(location,toPrint.toString());
			//Then sort and rewrite the save file
			list.sort(new DescendingScoreSorter());
			Path file = Paths.get("scores","scores"+channel.getId()+".csv");
			Path oldFile = Files.move(file, file.resolveSibling("scores"+channel.getId()+"old.csv"));
			Files.write(file, list);
			Files.delete(oldFile);
		}
		catch(IOException e)
		{
			channel.sendMessage("Could not save data in "+channel.getName()).queue();
			e.printStackTrace();
		}
	}
}
