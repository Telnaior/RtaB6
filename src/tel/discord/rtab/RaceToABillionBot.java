package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import tel.discord.rtab.MinigameTournament.TournamentStatus;
import tel.discord.rtab.commands.*;
import tel.discord.rtab.commands.channel.*;
import tel.discord.rtab.commands.hidden.*;
import tel.discord.rtab.commands.mod.*;
import tel.discord.rtab.games.MiniGame;

public class RaceToABillionBot
{
	static JDA betterBot;
	static CommandClient commands;
	public static EventWaiter waiter;
	public static List<GameController> game = new ArrayList<>(5);
	public static List<SuperBotChallenge> challenge = new ArrayList<>(1);
	public static List<MinigameTournament> tournament = new ArrayList<>(1);
	public static List<MiniGame> minigame = new ArrayList<>(10);
	public static int testMinigames = 0;
	public static List<String> testMinigamePlayers = new LinkedList<>();
	static boolean RUN_GAMES = true; //disable this and the bot won't connect to game channels
	
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
				new HistoryCommand(), new ChampionsCommand(), new LevelCommand(), new ListAchievementsCommand(),
				new HiddenCommandCommand(), new TribeTotalsCommand(),
				//Side Mode Commands
				new ReadyCommand(),
				//Mod Commands
				new StartCommand(), new ResetCommand(), new SaveCommand(),
				new ViewBombsCommand(), new GridListCommand(),
				//Channel Management Commands
				new GameChannelAddCommand(), new GameChannelEnableCommand(), new GameChannelDisableCommand(),
				new GameChannelModifyCommand(), new ListGameChannelsCommand(),
				new ResetSeasonCommand(), new ArchiveSeasonCommand(),
				new AddBotCommand(), new DemoCommand(), new AwardCommand(),
				//Owner Commands
				new ReconnectCommand(), new ShutdownCommand(), new RestartCommand(), new SendMessagesCommand(),
				new RecalcLevelCommand(), new ListGuildsCommand(), new LeaveGuildCommand(), new CleanUpChannelsCommand(),
				//Misc Commands
				new PingCommand(), new HelpCommand(), new LockoutCommand(), new RegularCommand(), new JoinTribeCommand(),
				//Joke Commands
				new MemeCommand(), new LuckyNumberCommand(), new MysteryChanceCommand(), new TriforceCommand()
				);
		//Set up the JDA itself
		JDABuilder prepareBot = JDABuilder.createDefault(token);
		prepareBot.enableIntents(GatewayIntent.MESSAGE_CONTENT);
		commands = utilities.build();
		waiter = new EventWaiter(Executors.newScheduledThreadPool(4, new EventWaiterThreadFactory()),true);
		prepareBot.addEventListeners(waiter,commands); //This order is actually important lol
		betterBot = prepareBot.build();
		//Once the bot is ready, move on to setting up game controllers
		betterBot.awaitReady();
		betterBot.getPresence().setActivity(Activity.playing("Type !help"));
		if(RUN_GAMES)
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
		int channelsNotFound = 0;
		for(Guild guild : guildList)
		{
			try
			{
				List<String> list = Files.readAllLines(Paths.get("guilds","guild"+guild.getId()+".csv"));
				for(String nextChannel : list)
					if(!connectToChannel(guild, nextChannel))
						channelsNotFound ++;
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
		System.out.println(channelsNotFound + " channels not found.");
	}
	
	//Return false if the channel couldn't be found
	public static boolean connectToChannel(Guild guild, String channelString)
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
			return false;
		String resultID = record[2];
		TextChannel resultChannel = null;
		if(!resultID.equalsIgnoreCase("null"))
			resultChannel = guild.getTextChannelById(resultID);
		//Check permissions on everything
		Member selfMember = guild.getSelfMember();
		if(!selfMember.hasPermission(gameChannel, Permission.MESSAGE_SEND))
			return false;
		if(resultChannel != null && !selfMember.hasPermission(resultChannel, Permission.MESSAGE_SEND))
			resultChannel = null;
		//Check the channel's enabled status to pass off to the appropriate handler to initialise the channel
		switch (record[1].toLowerCase()) {
			case "sbc" -> {
				SuperBotChallenge challengeHandler = new SuperBotChallenge();
				challenge.add(challengeHandler);
				game.add(challengeHandler.initialise(gameChannel, record, resultChannel));
			}
			case "minigame" -> {
				tournament.add(new MinigameTournament(gameChannel, record, resultChannel));
			}
			case "enabled", "tribes" -> { //Tribal gameplay uses the same game controller
				GameController newGame = new GameController(gameChannel, record, resultChannel);
				if (newGame.initialised())
					game.add(newGame);
				else
					newGame.timer.shutdownNow();
			}
			default -> {
			} //most likely "disabled" - do nothing
		}
		return true;
	}
	
	synchronized public static void addMinigame(MiniGame gameToAdd)
	{
		minigame.add(gameToAdd);
	}
	
	synchronized public static void removeMinigame(MiniGame gameToRemove)
	{
		minigame.remove(gameToRemove);
	}
	
	public static void shutdown(boolean restart)
	{
		//Alert as shutting down
		if(betterBot.getPresence().getStatus() == OnlineStatus.ONLINE)
		{
			betterBot.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
			betterBot.getPresence().setActivity(Activity.playing("Shutting Down..."));
		}
		//oh to be President Madagascar
		for(GameController game : game)
		{
			game.channel.sendMessage("Shutting down...").queue();
			game.timer.purge();
			game.timer.shutdownNow();
		}
		for(SuperBotChallenge challenge : challenge)
		{
			challenge.timer.purge();
			challenge.timer.shutdownNow();
		}
		for(MinigameTournament tournament : tournament)
		{
			tournament.timer.purge();
			tournament.timer.shutdownNow();
			tournament.status = TournamentStatus.SHUTDOWN;
		}
		for(MiniGame minigame : minigame)
		{
			minigame.shutdown();
		}
		minigame.clear();
		betterBot.shutdown();
		if(restart)
		{
			try
			{
				Runtime.getRuntime().exec("shutdown -r -t 10");
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
