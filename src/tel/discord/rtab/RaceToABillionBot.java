package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginException;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import tel.discord.rtab.commands.*;
import tel.discord.rtab.commands.channel.*;

public class RaceToABillionBot
{
	static JDA betterBot;
	public static EventWaiter waiter;
	public static ArrayList<GameController> game = new ArrayList<>(3);

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
		utilities.useDefaultGame();
		utilities.addCommands(
				//Basic Game Commands
				new JoinCommand(), new QuitCommand(),
				//Info Commands
				new PlayersCommand(),
				//Extra Mod Commands
				new StartCommand(), new ResetCommand(),
				//Channel Management Commands
				new GameChannelAddCommand(), new GameChannelEnableCommand(), new GameChannelDisableCommand(), new GameChannelModifyCommand(),
				//Owner Commands
				new ReconnectCommand(), new ShutdownBotCommand(), new AddBotCommand(), new DemoCommand(),
				//Misc Commands
				new PingCommand()
				);
		//Set up the JDA itself
		JDABuilder prepareBot = JDABuilder.createDefault(token);
		waiter = new EventWaiter();
		prepareBot.addEventListeners(utilities.build(),waiter);
		betterBot = prepareBot.build();
		//Give the bot a second to finish connecting to discord, then move on to setting up channels
		Thread.sleep(5000);
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
		//First, check if the channel is actually enabled
		if(record[1].equalsIgnoreCase("disabled"))
			return;
		//Then make sure the channel actually exists
		String channelID = record[0];
		TextChannel gameChannel = guild.getTextChannelById(channelID);
		String resultID = record[2];
		TextChannel resultChannel = null;
		if(!resultID.equalsIgnoreCase("null"))
			resultChannel = guild.getTextChannelById(resultID);
		if(gameChannel == null)
		{
			System.out.println("Channel "+channelID+" does not exist.");
			return;
		}
		//Alright, now we pass it over to the controller to finish initialisation
		GameController newGame = new GameController(gameChannel,record,resultChannel);
		if(newGame.initialised())
			game.add(newGame);
		else
			newGame.timer.shutdownNow();
	}
	
}
