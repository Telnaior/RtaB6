package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.security.auth.login.LoginException;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import tel.discord.rtab.commands.*;
import tel.discord.rtab.commands.channel.*;
import tel.discord.rtab.commands.hidden.*;
import tel.discord.rtab.commands.mod.*;

public class RaceToABillionBot
{
	static JDA betterBot;
	static CommandClient commands;
	public static EventWaiter waiter;
	public static ArrayList<GameController> game = new ArrayList<>(5);
	public static ArrayList<SuperBotChallenge> challenge = new ArrayList<>(1);
	public static int testMinigames = 0;
	
	static class EventWaiterThreadFactory implements ThreadFactory
	{
		int nextID = 0;
		@Override
		public Thread newThread(Runnable r)
		{
			Thread newThread = new Thread(r);
			nextID ++;
			newThread.setName("Event Waiter Thread "+nextID);
			return newThread;
		}
	}

	/**
	 * Load the JDA and log in to the bot's account, then pass to connectToChannels().
	 * 
	 * @param args - ignored!
	 * @throws IOException - if config.txt doesn't exist or cannot be read
	 * @throws LoginException - if we can't log in to the bot's account
	 * @throws InterruptedException - if the bot can't sleep
	 */
	public static void main(String[] args) throws IOException, LoginException, InterruptedException
	{
		//Read initial config file to get important things
		List<String> list = Files.readAllLines(Paths.get("config.txt"));
		String token = list.get(0);
		String owner = list.get(1);
		//Set up JDA Utilities with the command list
		CommandClientBuilder utilities = new CommandClientBuilder();
		utilities.setOwnerId(owner);
		utilities.setPrefix("!");
		utilities.setHelpWord("commands");
		utilities.addCommands(
				//Basic Game Commands
				new JoinCommand(), new QuitCommand(), new PeekCommand(),
				//Hidden Commands
				new FoldCommand(), new RepelCommand(), new BlammoCommand(),
				new DefuseCommand(), new WagerCommand(), new BonusCommand(),
				new TruesightCommand(), new FailsafeCommand(), new MinesweeperCommand(),
				//Minigame Commands
				new EnhanceCommand(), new SkipCommand(), new TestMinigameCommand(),
				//Info Commands
				new PlayersCommand(), new BoardCommand(), new TotalsCommand(), new NextCommand(), new ViewPeeksCommand(),
				new RankCommand(), new TopCommand(), new LivesCommand(), new StatsCommand(), new AnnuitiesCommand(),
				new HistoryCommand(), new LevelCommand(), new ListAchievementsCommand(), new HiddenCommandCommand(),
				//Side Mode Commands
				new ReadyCommand(),
				//Mod Commands
				new StartCommand(), new ResetCommand(), new SaveCommand(),
				new ViewBombsCommand(), new GridListCommand(),
				//Channel Management Commands
				new GameChannelAddCommand(), new GameChannelEnableCommand(), new GameChannelDisableCommand(),
				new GameChannelModifyCommand(), new ListGameChannelsCommand(),
				new ResetSeasonCommand(), new ArchiveSeasonCommand(),
				new AddBotCommand(), new DemoCommand(),
				//Owner Commands
				new ReconnectCommand(), new ShutdownCommand(), new SendMessagesCommand(), new RecalcLevelCommand(),
				new ListGuildsCommand(), new LeaveGuildCommand(),
				//Misc Commands
				new PingCommand(), new HelpCommand(), new LockoutCommand(),
				//Joke Commands
				new MemeCommand(), new LuckyNumberCommand(), new MysteryChanceCommand(), new TriforceCommand()
				);
		//Set up the JDA itself
		JDABuilder prepareBot = JDABuilder.createDefault(token);
		commands = utilities.build();
		waiter = new EventWaiter(Executors.newScheduledThreadPool(4, new EventWaiterThreadFactory()),true);
		prepareBot.addEventListeners(waiter,commands); //This order is actually important lol
		betterBot = prepareBot.build();
		//Once the bot is ready, move on to setting up game controllers
		betterBot.awaitReady();
		betterBot.getPresence().setActivity(Activity.playing("Type !help"));
		scanGuilds();
	}
	
	/**
	 * Run through each guild the bot is connected to, check its settings file, and send channels to <code>connectToChannel</code>.
	 */
	public static void scanGuilds()
	{
		//Get all the guilds we're in
		List<Guild> guildList = betterBot.getGuilds();
		System.out.println(guildList);
		//For each guild, get that guild's settings file and set up channels accordingly
		for(Guild guild : guildList)
		{
			try
			{
				List<String> list = Files.readAllLines(Paths.get("guilds","guild"+guild.getId()+".csv"));
				for(String nextChannel : list)
					connectToChannel(guild, nextChannel);
			}
			catch(IOException e)
			{
				System.out.println("No settings file found for "+guild.getName()+", creating.");
				try
				{
					Files.createFile(Paths.get("guilds","guild"+guild.getId()+".csv"));
				}
				catch (IOException e1)
				{
					System.err.println("Couldn't create it either. Oops.");
					e1.printStackTrace();
				}
			}
		}
	}
	
	public static void connectToChannel(Guild guild, String channelString)
	{
		/*
		 * Guild settings file format:
		 * record[0] = channel ID
		 * record[1] = enabled
		 * record[2] = result channel ID
		 */
		String[] record = channelString.split("#");
		//Make sure the channel actually exists
		String channelID = record[0];
		TextChannel gameChannel = guild.getTextChannelById(channelID);
		if(gameChannel == null)
		{
			System.out.println("Channel "+channelID+" does not exist.");
			return;
		}
		String resultID = record[2];
		TextChannel resultChannel = null;
		if(!resultID.equalsIgnoreCase("null"))
			resultChannel = guild.getTextChannelById(resultID);
		//Check the channel's enabled status to decide what to do next
		switch(record[1].toLowerCase())
		{
		case "sbc":
			//Alright, now we pass it over to the controller to finish initialisation
			SuperBotChallenge challengeHandler = new SuperBotChallenge();
			challenge.add(challengeHandler);
			game.add(challengeHandler.initialise(gameChannel,record,resultChannel));
			break;
		case "tribes":
			//TODO
		case "enabled":
			//Alright, now we pass it over to the controller to finish initialisation
			GameController newGame = new GameController(gameChannel,record,resultChannel);
			if(newGame.initialised())
				game.add(newGame);
			else
				newGame.timer.shutdownNow();
			break;
		default: //most likely "disabled" - do nothing
		}
	}
	
	public static void shutdown()
	{
		//Alert as shutting down
		boolean firstShutdown = false;
		if(betterBot.getPresence().getStatus() == OnlineStatus.ONLINE)
		{
			firstShutdown = true;
			betterBot.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
			betterBot.getPresence().setActivity(Activity.playing("Shutting Down..."));
		}
		//Stop game controllers immediately
		for(GameController game : game)
		{
			game.channel.sendMessage("Shutting down...").queue();
			game.timer.purge();
			game.timer.shutdownNow();
			if(game.currentGame != null)
				game.currentGame.gameOver();
		}
		for(SuperBotChallenge challenge : RaceToABillionBot.challenge)
		{
			challenge.timer.purge();
			challenge.timer.shutdownNow();
		}
		//If there are test minigames, wait for them and disable new ones
		if(testMinigames > 0)
		{
			System.out.println("Waiting on " + testMinigames + " test minigames...");
			//We can't remove the command if we already have
			if(firstShutdown)
				commands.removeCommand("practice");
		}
		//Otherwise we're good to close
		else
			betterBot.shutdown();
	}
}
