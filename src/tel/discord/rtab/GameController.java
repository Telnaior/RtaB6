package tel.discord.rtab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import tel.discord.rtab.board.HiddenCommand;
import net.dv8tion.jda.api.entities.Member;

public class GameController
{
	//Basic stuff
	public ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
	public ScheduledFuture<?> demoMode;
	public TextChannel channel;
	//Settings that can be customised
	int baseNumerator, baseDenominator, botCount, runDemo, minPlayers, maxPlayers;
	boolean playersCanJoin = true;
	//Game variables
	public final List<Player> players = new ArrayList<>(16);
	int currentTurn, playersAlive, repeatTurn, boardSize, spacesLeft;
	boolean[] pickedSpaces, bombs;
	GameStatus gameStatus = GameStatus.LOADING;
	
	public GameController(TextChannel gameChannel, String[] record)
	{
		/*
		 * Guild settings file format:
		 * record[2] = base multiplier (expressed as fraction)
		 * record[3] = how many bots (0+)
		 */
		channel = gameChannel;
		//Let them know if anything goes wrong
		try
		{
			//Base multiplier is kinda complex
			String[] baseMultiplier = record[2].split("/");
			baseNumerator = Integer.parseInt(baseMultiplier[0]);
			//If no denominator supplied, treat it as 1
			if(baseMultiplier.length < 2)
				baseDenominator = 1;
			else
				baseDenominator = Integer.parseInt(baseMultiplier[1]);
			//Other settings just simple imports
			botCount = Integer.parseInt(record[3]);
			runDemo = Integer.parseInt(record[4]);
			minPlayers = Integer.parseInt(record[5]);
			maxPlayers = Integer.parseInt(record[6]);
			//Finally, create a game channel with all the settings as instructed
		}
		catch(Exception e1)
		{
			gameChannel.sendMessage("A fatal error has occurred.").queue();
			e1.printStackTrace();
			return;
		}
		reset();
	}
	
	public void reset()
	{
		players.clear();
		currentTurn = -1;
		playersAlive = 0;
		if(gameStatus != GameStatus.SEASON_OVER)
			gameStatus = GameStatus.SIGNUPS_OPEN;
		boardSize = 0;
		repeatTurn = 0;
		timer.shutdownNow();
		timer = new ScheduledThreadPoolExecutor(1);
		if(runDemo != 0 && botCount >= 4)
		{
			demoMode = timer.schedule(() -> 
			{
				for(int i=0; i<4; i++)
					addRandomBot();
				startTheGameAlready();
			},runDemo,TimeUnit.MINUTES);
		}
	}
	
	boolean initialised()
	{
		return gameStatus == GameStatus.SIGNUPS_OPEN;
	}

	public int findPlayerInGame(String playerID)
	{
		for(int i=0; i < players.size(); i++)
			if(players.get(i).uID.equals(playerID))
				return i;
		return -1;
	}

	/**
	 * addPlayer - adds a player to the game, or updates their name if they're already in.
	 * MessageChannel channelID - channel the request took place in (only used to know where to send game details to)
	 * String playerID - ID of player to be added.
	 * Returns true if the join attempt succeeded, or false if it failed.
	 */
	public boolean addPlayer(Member playerID)
	{
		//Are player joins even *allowed* here?
		if(!playersCanJoin)
		{
			channel.sendMessage("Cannot join game: Joining is not permitted in this channel.").queue();
			return false;
		}
		//Make sure game isn't already running
		if(gameStatus != GameStatus.SIGNUPS_OPEN)
		{
			channel.sendMessage("Cannot join game: "+
					(gameStatus == GameStatus.SEASON_OVER?"There is no season currently running.":"Game already running.")).queue();
			return false;
		}
		//Watch out for too many players
		if(players.size() >= maxPlayers)
		{
			channel.sendMessage("Cannot join game: Too many players.").queue();
			return false;
		}
		//Create player object
		Player newPlayer = new Player(playerID,this,null);
		if(newPlayer.name.contains(":") || newPlayer.name.contains("#") || newPlayer.name.startsWith("!"))
		{
			channel.sendMessage("Cannot join game: Illegal characters in name.").queue();
			return false;
		}
		//Dumb easter egg
		if(newPlayer.money <= -1000000000)
		{
			channel.sendMessage("Cannot join game: You have been eliminated from Race to a Billion.").queue();
			return false;
		}
		//If they're out of lives, charge them and let them know
		//Fee is 1% plus an extra 0.2% per additional life spent while already out
		if(newPlayer.lives <= 0 && newPlayer.newbieProtection <= 0)
		{
			int entryFee = calculateEntryFee(newPlayer.money, newPlayer.lives);
			newPlayer.addMoney(-1*entryFee,MoneyMultipliersToUse.NOTHING);
			newPlayer.oldMoney = newPlayer.money;
			channel.sendMessage(newPlayer.getSafeMention() + String.format(", you are out of lives. "
					+ "Playing this round will incur an entry fee of $%,d.",entryFee)).queue();
		}
		//Look for match already in player list
		int playerLocation = findPlayerInGame(newPlayer.uID);
		if(playerLocation != -1)
		{
			//Found them, check if we should update their name or just laugh at them
			if(players.get(playerLocation).name == newPlayer.name)
			{
				channel.sendMessage("Cannot join game: You have already joined the game.").queue();
				return false;
			}
			else
			{
				players.set(playerLocation,newPlayer);
				channel.sendMessage("Updated in-game name.");
				return false;
			}
		}
		//Haven't found one, add them to the list
		players.add(newPlayer);
		if(newPlayer.hiddenCommand != HiddenCommand.NONE)
		{
			StringBuilder commandHelp = new StringBuilder();
			switch(newPlayer.hiddenCommand)
			{
			case FOLD:
				commandHelp.append("You are carrying over a **FOLD** from a previous game.\n"
						+ "You may use it at any time by typing **!fold**.");
				break;
			case REPELLENT:
				commandHelp.append("You are carrying over **BLAMMO REPELLENT** from a previous game.\n"
						+ "You may use it when a blammo is in play by typing **!repel**.");
				break;
			case BLAMMO:
				commandHelp.append("You are carrying over a **BLAMMO SUMMONER** from a previous game.\n"
						+ "You may use it at any time by typing **!blammo**.");
				break;
			case DEFUSE:
				commandHelp.append("You are carryng over a **DEFUSER** from a previous game.\n"
						+ "You may use it at any time by typing **!defuse** _followed by the space you wish to defuse_.");
				break;
			case WAGER:
				commandHelp.append("You are carrying over a **WAGERER** from a previous game.\n"
						+ "You may use it at any time by typing **!wager**.");
				break;
			case BONUS:
				commandHelp.append("You are carrying over the **BONUS BAG** from a previous game.\n"
						+ "You may use it at any time by typing **!bonus** followed by 'cash', 'boost', 'game', or 'event'.");
				break;
			default:
				break;
			}
			newPlayer.user.openPrivateChannel().queue(
					(channel) -> channel.sendMessage(commandHelp.toString()).queueAfter(5,TimeUnit.SECONDS));
		}
		if(newPlayer.money > 900000000)
		{
			channel.sendMessage(String.format("%1$s needs only $%2$,d more to reach the goal!",
					newPlayer.name,(1000000000-newPlayer.money))).queue();
		}
		//If there's only one player right now, that means we're starting a new game so schedule the relevant things
		if(players.size() == 1)
		{
			if(runDemo != 0)
				demoMode.cancel(false);
			timer.schedule(() -> 
			{
			channel.sendMessage("Thirty seconds before game starts!").queue();
			channel.sendMessage(listPlayers(false)).queue();
			}, 90, TimeUnit.SECONDS);
			timer.schedule(() -> startTheGameAlready(), 120, TimeUnit.SECONDS);
			channel.sendMessage("Starting a game of Race to a Billion in two minutes. Type !join to sign up.").queue();
		}
		//Finally, wrap up by saying they actually managed to join
		channel.sendMessage(newPlayer.name + " successfully joined the game.").queue();
		return true;
	}
	

	/**
	 * removePlayer - removes a player from the game.
	 * String playerID - ID of player to be removed.
	 * Returns true if the quit attempt succeeded, or false if it failed.
	 */
	public boolean removePlayer(Member playerID)
	{
		//Make sure game isn't running, too late to quit now
		if(gameStatus != GameStatus.SIGNUPS_OPEN)
		{
			channel.sendMessage("The game cannot be left after it has started.").queue();
			return false;
		}
		//Search for player
		int playerLocation = findPlayerInGame(playerID.getId());
		if(playerLocation != -1)
		{
			players.remove(playerLocation);
			//Abort the game if everyone left
			if(players.size() == 0)
				reset();
			channel.sendMessage(playerID.getEffectiveName() + " left the game.").queue();
			return true;
		}
		//Didn't find them, why are they trying to quit in the first place?
		channel.sendMessage(playerID.getEffectiveName() + 
				" could not leave the game because they were never in the game. :thinking:").queue();
		return false;
	}
	
	public void addBot(int botNumber)
	{
		//Only do this if we're in signups!
		if(gameStatus != GameStatus.SIGNUPS_OPEN)
			return;
		GameBot chosenBot;
		try
		{
			chosenBot = new GameBot(channel.getGuild().getId(),botNumber);
		}
		catch (IOException e)
		{
			channel.sendMessage("Bot creation failed.").queue();
			e.printStackTrace();
			return;
		}
		Player newPlayer;
		newPlayer = new Player(chosenBot,this);
		players.add(newPlayer);
		if(newPlayer.money > 900_000_000)
		{
			channel.sendMessage(String.format("%1$s needs only $%2$,d more to reach the goal!",
					newPlayer.name,(1_000_000_000-newPlayer.money)));
		}
		return;
	}
	
	public void addRandomBot()
	{
		//Only do this if the game hasn't started!
		if(gameStatus != GameStatus.SIGNUPS_OPEN && gameStatus != GameStatus.ADD_BOT_QUESTION
				&& gameStatus != GameStatus.BOMB_PLACEMENT)
			return;
		GameBot chosenBot;
		int nextBot = (int)(Math.random()*botCount);
		int triesLeft = botCount;
		//Start looping through until we find a valid bot (one that isn't already in the round)
		boolean goodPick;
		do
		{
			nextBot = (nextBot + 1) % botCount;
			triesLeft --;
			try
			{
				chosenBot = new GameBot(channel.getGuild().getId(),nextBot);
			}
			catch (IOException e)
			{
				channel.sendMessage("Bot generation failed.").queue();
				e.printStackTrace();
				return;
			}
			goodPick = (findPlayerInGame(chosenBot.botID) != -1);
		}
		while(!goodPick && triesLeft > 0);
		if(!goodPick)
		{
			//If we've checked EVERY bot...
			channel.sendMessage("Bot generation failed.").queue();
			return;
		}
		else
		{
			//But assuming we found one, pass them to the method that actually adds them
			addBot(nextBot);
		}
	}
	
	/**
	 * startTheGameAlready - close signups and run game initialisation stuff
	 */
	public void startTheGameAlready()
	{
		//If the game's already running or no one's in it, just don't
		if((gameStatus != GameStatus.SIGNUPS_OPEN && gameStatus != GameStatus.ADD_BOT_QUESTION) || players.size() == 0)
		{
			return;
		}
		//Potentially ask to add bots
		if(gameStatus == GameStatus.SIGNUPS_OPEN && botCount > 0 &&
				(players.size() < minPlayers || (players.size() < 4 && Math.random() < 0.2) || (players.size() < 16 && Math.random() < 0.1)))
		{
			addBotQuestion();
			return;
		}
		//If we don't have enough players and for some reason didn't add bots, call the game off
		else if(players.size() < minPlayers)
		{
			channel.sendMessage("Not enough players. Game aborted.").queue();
			reset();
		}
		//Declare game in progress so we don't get latecomers
		channel.sendMessage("Starting game...").queue();
		gameStatus = GameStatus.BOMB_PLACEMENT;
		//Initialise stuff that needs initialising
		boardSize = 5 + (5*players.size());
		spacesLeft = boardSize;
		pickedSpaces = new boolean[boardSize];
		bombs = new boolean[boardSize];
		//Then do bomb placement
		sendBombPlaceMessages();
	}
	
	/**
	 * addBotQuestion - ask the players if they want a bot (or more) in their game, and add them if agreed to
	 * This method should only be called from startTheGameAlready, and will recurse back to it once it's done its work.
	 */
	private void addBotQuestion()
	{
		//Didn't get players? How about a bot?
		if(players.size() == 1)
			channel.sendMessage(players.get(0).getSafeMention()+", would you like to play against a bot? (Y/N)").queue();
		else
			channel.sendMessage("Would you like to play against a bot? (Y/N)").queue();
		gameStatus = GameStatus.ADD_BOT_QUESTION;
		RaceToABillionBot.waiter.waitForEvent(MessageReceivedEvent.class,
				//Accept if it's a player in the game, they're in the right channel, and they've given a valid response
				e ->
				{
					if(findPlayerInGame(e.getAuthor().getId()) != -1 && e.getChannel().equals(channel))
					{
						String firstLetter = e.getMessage().getContentRaw().toUpperCase().substring(0,1);
						return(firstLetter.startsWith("Y") || firstLetter.startsWith("N"));
					}
					return false;
				},
				//Parse it and call the method that does stuff
				e -> 
				{
					if(e.getMessage().getContentRaw().toUpperCase().startsWith("Y"))
					{
						int botsAdded = 0;
						do
						{
							addRandomBot();
							botsAdded++;
						}
						while(players.size() < 4 && Math.random() < 0.2 && botCount > botsAdded);
						startTheGameAlready();
					}
					else
					{
						channel.sendMessage("Very well.").queue();
						startTheGameAlready();
					}
				},
				30,TimeUnit.SECONDS, () ->
				{
					startTheGameAlready();
				});
	}
	
	private void sendBombPlaceMessages()
	{
		//TODO - next time on RtaB6!
	}
	
	public int calculateEntryFee(int money, int lives)
	{
		int entryFee = Math.max(money/500,20000);
		entryFee *= 5 - lives;
		return entryFee;
	}

	public String listPlayers(boolean waitingOn)
	{
		StringBuilder resultString = new StringBuilder();
		if(waitingOn)
			resultString.append("**WAITING ON**");
		else
			resultString.append("**PLAYERS**");
		for(Player next : players)
		{
			if(!waitingOn || (waitingOn && next.status == PlayerStatus.OUT))
			{
				resultString.append(" | ");
				resultString.append(next.name);
			}
		}
		return resultString.toString();
	}
}
