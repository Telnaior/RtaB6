package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.security.auth.login.LoginException;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import com.jagrosh.jdautilities.examples.command.ShutdownCommand;

import net.dv8tion.jda.api.JDABuilder;

public class RaceToABillionBot
{
	public static EventWaiter waiter;

	public static void main(String[] args) throws IOException, LoginException
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
				new PingCommand(), new ShutdownCommand()
				);
		//Set up the JDA itself
		JDABuilder prepareBot = JDABuilder.createDefault(token);
		waiter = new EventWaiter();
		prepareBot.addEventListeners(utilities.build(),waiter);
		prepareBot.build();
	}
	
}
