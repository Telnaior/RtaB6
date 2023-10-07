package tel.discord.rtab.commands.channel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;

public class GameChannelModifyCommand extends Command
{
	private static final String GUILD = "guild";

	public GameChannelModifyCommand()
	{
		this.name = "modifychannel";
		this.aliases = new String[]{"modify","modifysettings"};
		this.help = "allows you to modify a game channel's settings before enabling it";
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
			List<String> list = Files.readAllLines(Paths.get("guilds",GUILD+event.getGuild().getId()+".csv"));
			boolean channelFound = false;
			boolean needToSave = false;
			StringBuilder fullLine = new StringBuilder();
			//Find this channel in the list
			for(int i=0; i<list.size(); i++)
			{
				String[] record = list.get(i).split("#");
				if(record[0].equals(channelID))
				{
					if(!record[1].equals("disabled"))
					{
						event.reply("This channel must be disabled with !disablechannel before it can be modified.");
						return;
					}
					//Cool, we found it
					channelFound = true;
					//If they didn't send any arguments, display the list of settings to them
					if(event.getArgs().equals(""))
					{
						StringBuilder output = new StringBuilder().append("```\n");
						output.append("Settings for #"+event.getTextChannel().getName()+":\n");
						for(ChannelSetting nextSetting : ChannelSetting.values())
							output.append(nextSetting.getName()).append(": ").append(record[nextSetting.getLocation()]).append("\n");
						output.append("```\nTo modify a setting, type \"!modifychannel settingname newvalue\".");
						event.reply(output.toString());
					}
					//If they did, let's figure out what they're modifying and change it
					else
					{
						String[] args = event.getArgs().split(" ");
						ChannelSetting setting = null;
						//Look for the matching setting
						for(ChannelSetting nextSetting : ChannelSetting.values())
							if(args[0].equalsIgnoreCase(nextSetting.getName()))
							{
								setting = nextSetting;
								break;
							}
						//If we didn't find it, tell them
						if(setting == null)
						{
							event.reply("Setting not found.");
							return;
						}
						//If they didn't supply a value to set it to, just tell them what it is
						if(args.length == 1)
						{
							event.reply(setting.getName()+": "+record[setting.getLocation()]);
							return;
						}
						//If the value they supplied isn't valid for the setting, tell them
						if(!args[1].equalsIgnoreCase("default") && !setting.isValidSetting(args[1]))
						{
							event.reply("Invalid value. To reset the value, set it to 'default'.");
							return;
						}
						//Okay, now we have a value that we know is valid, so set it accordingly
						if(args[1].equalsIgnoreCase("default"))
							record[setting.getLocation()] = setting.getDefault();
						else
							record[setting.getLocation()] = args[1];
						//Now rebuild the channel string and save it
						for(String next : record)
						{
							fullLine.append("#");
							fullLine.append(next);
						}
						//Remove the opening #
						fullLine.deleteCharAt(0);
						list.set(i, fullLine.toString());
						needToSave = true;
					}
					break;
				}
			}
			//If we never found it, fullLine will have never been written to
			if(!channelFound)
			{
				event.reply("Channel not found in database. Try !addchannel instead.");
				return;
			}
			if(needToSave)
			{
				//Finish by saving the settings file if neccessary
				Path file = Paths.get("guilds",GUILD+event.getGuild().getId()+".csv");
				Path oldFile = Files.move(file, file.resolveSibling(GUILD+event.getGuild().getId()+"old.csv"));
				Files.write(file, list);
				Files.delete(oldFile);
				event.reply("Updated successfully.");
			}
		}
		catch (IOException e)
		{
			event.reply("Save failed. Try again later.");
			e.printStackTrace();
		}	
	}
	
}