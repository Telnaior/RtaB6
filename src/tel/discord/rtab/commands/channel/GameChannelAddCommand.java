package tel.discord.rtab.commands.channel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class GameChannelAddCommand extends Command
{
	
	public GameChannelAddCommand()
	{
		this.name = "addchannel";
		this.help = "sets up a channel as a game channel";
		this.hidden = true;
		this.ownerCommand = true;
	}
	
	@Override
	protected void execute(CommandEvent event)
	{	
		try
		{
			String channelID = event.getChannel().getId();
			//Get this guild's settings file
			List<String> list = Files.readAllLines(Paths.get("guilds","guild"+event.getGuild().getId()+".csv"));
			//Make sure this channel isn't in the list already
			for(String nextChannel : list)
			{
				String[] record = nextChannel.split("#");
				if(record[0].equals(channelID))
				{
					event.reply("Channel has already been added, use !modifychannel or !enablechannel instead.");
					return;
				}
			}
			//Good, now add it
			StringBuilder fullLine = new StringBuilder().append(channelID).append("#disabled");
			for(ChannelSetting nextSetting : ChannelSetting.values())
			{
				fullLine.append("#");
				fullLine.append(nextSetting.getDefault());
			}
			list.add(fullLine.toString());
			event.reply("Channel added. Use !modifychannel to set it up appropriately, then !enablechannel to open it up to play.");
			//Finally, save the settings file
			Path file = Paths.get("guilds","guild"+event.getGuild().getId()+".csv");
			Path oldFile = Files.move(file, file.resolveSibling("guild"+event.getGuild().getId()+"old.csv"));
			Files.write(file, list);
			Files.delete(oldFile);
		}
		catch (IOException e)
		{
			event.reply("Save failed. Try again later.");
			e.printStackTrace();
		}	
	}
	
}