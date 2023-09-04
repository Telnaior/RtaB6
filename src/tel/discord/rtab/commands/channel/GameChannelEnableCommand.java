package tel.discord.rtab.commands.channel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;
import tel.discord.rtab.RaceToABillionBot;

public class GameChannelEnableCommand extends Command
{
	
	public GameChannelEnableCommand()
	{
		this.name = "enablechannel";
		this.help = "enables a game channel, allowing the game to begin!";
		this.hidden = true;
		this.userPermissions = new Permission[] {Permission.MANAGE_SERVER};
	}
	
	@Override
	protected void execute(CommandEvent event)
	{	
		try
		{
			String channelID = event.getChannel().getId();
			//Get this guild's settings file
			List<String> list = Files.readAllLines(Paths.get("guilds","guild"+event.getGuild().getId()+".csv"));
			StringBuilder fullLine = new StringBuilder();
			//Find this channel in the list
			for(int i=0; i<list.size(); i++)
			{
				String[] record = list.get(i).split("#");
				if(record[0].equals(channelID))
				{
					if(!record[1].equals("disabled"))
					{
						event.reply("Channel is already enabled.");
						return;
					}
					String enableType = event.isOwner() ? event.getArgs() : "";
					switch (enableType) {
						case "sbc" -> record[1] = "sbc";
						case "tribes" -> record[1] = "tribes";
						default -> record[1] = "enabled";
					}
					//Cool, we found it, now remake the entry with the flipped bit
					for(String next : record)
					{
						fullLine.append("#");
						fullLine.append(next);
					}
					fullLine.deleteCharAt(0); //Remove the opening #
					list.set(i, fullLine.toString());
					//Then start the game using the updated channel string
					RaceToABillionBot.connectToChannel(event.getGuild(),fullLine.toString());
					break;
				}
			}
			//If we never found it, fullLine will have never been written to
			if(fullLine.length() == 0)
			{
				event.reply("Channel not found in database. Try !addchannel instead.");
				return;
			}
			//Next, save the settings file
			Path file = Paths.get("guilds","guild"+event.getGuild().getId()+".csv");
			Path oldFile = Files.move(file, file.resolveSibling("guild"+event.getGuild().getId()+"old.csv"));
			Files.write(file, list);
			Files.delete(oldFile);
			//The game controller itself will confirm the success of this command when it starts up
		}
		catch (IOException e)
		{
			event.reply("Save failed. Try again later.");
			e.printStackTrace();
		}	
	}
	
}