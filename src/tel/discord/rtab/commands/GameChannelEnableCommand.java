package tel.discord.rtab.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

public class GameChannelEnableCommand extends Command
{
	
	public GameChannelEnableCommand()
	{
		this.name = "enablechannel";
		this.help = "enables a game channel, allowing the game to begin!";
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
			//Find this channel in the list
			for(int i=0; i<=list.size(); i++)
			{
				//If we're at the end of the list, tell them and suggest an alternative
				if(i == list.size())
				{
					event.reply("Channel not found in database. Try !addchannel instead.");
					return;
				}
				String[] record = list.get(i).split("#");
				if(record[0].equals(channelID))
				{
					//Cool, we found it, now remake the entry with the flipped bit
					record[1] = "enabled";
					StringBuilder fullLine = new StringBuilder();
					for(String next : record)
					{
						fullLine.append("#");
						fullLine.append(next);
					}
					//Remove the opening #
					fullLine.deleteCharAt(0);
					list.set(i, fullLine.toString());
					break;
				}
			}
			//Next, save the settings file
			Path file = Paths.get("guilds","guild"+event.getGuild().getId()+".csv");
			Path oldFile = Files.move(file, file.resolveSibling("guild"+event.getGuild().getId()+"old.csv"));
			Files.write(file, list);
			Files.delete(oldFile);
			//Then start the game!
			RaceToABillionBot.game.add(new GameController(event.getTextChannel()));
		}
		catch (IOException e)
		{
			event.reply("Save failed. Try again later.");
			e.printStackTrace();
		}	
	}
	
}