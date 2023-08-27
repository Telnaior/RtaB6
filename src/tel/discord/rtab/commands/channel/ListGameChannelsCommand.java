package tel.discord.rtab.commands.channel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class ListGameChannelsCommand extends Command
{
	
	public ListGameChannelsCommand()
	{
		this.name = "listchannels";
		this.help = "lists the game channels within this server";
		this.hidden = true;
		this.userPermissions = new Permission[] {Permission.MANAGE_SERVER};
	}
	
	@Override
	protected void execute(CommandEvent event)
	{
		try
		{
			Guild guild = event.getGuild();
			event.reply("**Game Channels on "+guild.getName()+"**");
			//Get this guild's settings file
			List<String> list = Files.readAllLines(Paths.get("guilds","guild"+guild.getId()+".csv"));
			//Then loop through each channel in turn
			for(String nextChannel : list)
			{
				String[] record = nextChannel.split("#");
				//Get the channel and add the basics to our output string
				TextChannel channel = guild.getTextChannelById(record[0]);
				StringBuilder output = new StringBuilder().append(channel.getAsMention()).append(": ").append(record[1]);
				//If there's a result channel set up, mention that too
				if(!record[2].equalsIgnoreCase("null"))
					output.append(", result channel: "+guild.getTextChannelById(record[2]).getAsMention());
				//Then scan for history files to get completed seasons
				int seasons = 0;
				while(Files.exists(Paths.get("scores","history"+channel.getId()+"s"+(seasons+1)+".csv")))
					seasons++;
				if(seasons > 0)
					output.append(" (").append(seasons).append(" Season").append(seasons > 1 ? "s" : "").append(" Completed)");
				//Then send the message
				event.reply(output.toString());
			}
		}
		catch (IOException e)
		{
			event.reply("Failed to read channel list. Try again later.");
			e.printStackTrace();
		}
	}
}