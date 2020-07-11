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
import com.jagrosh.jdautilities.examples.command.ShutdownCommand;

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
	 * main - Load the JDA and log in to the bot's account, then pass to connectToChannels(). 
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
				//Channel Management Commands
				new GameChannelAddCommand(), new GameChannelEnableCommand(), new GameChannelDisableCommand(), new GameChannelModifyCommand(),
				//Owner Commands
				new ReconnectCommand(), new ShutdownCommand(),
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
	 * scanGuilds - run through each guild the bot is connected to, check its settings file, and send channels to connectToChannel()
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
		 * record[2] = base multiplier (expressed as fraction)
		 */
		String[]record = channelString.split("#");
		//If the channel is disabled, we don't need to do anything here
		if(record[1].equals("enabled"))
		{
			String channelID = record[0];
			//Make sure the channel actually exists
			TextChannel gameChannel = guild.getTextChannelById(channelID);
			if(gameChannel == null)
			{
				System.out.println("Channel "+channelID+" does not exist.");
				return;
			}
			//If there are any missing settings, let them know
			try
			{
				String[] baseMultiplier = record[2].split("/");
				int baseNumerator = Integer.parseInt(baseMultiplier[0]);
				int baseDenominator;
				//If no denominator supplied, treat it as 1
				if(baseMultiplier.length < 2)
					baseDenominator = 1;
				else
					baseDenominator = Integer.parseInt(baseMultiplier[1]);
				//Finally, create a game channel with all the settings as instructed
				game.add(new GameController(gameChannel,baseNumerator,baseDenominator));
			}
			catch(ArrayIndexOutOfBoundsException e1)
			{
				gameChannel.sendMessage("A fatal error has occurred.").queue();
				e1.printStackTrace();
			}
		}
	}
	
}
