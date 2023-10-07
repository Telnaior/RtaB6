package tel.discord.rtab.commands.mod;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;

import com.jagrosh.jdautilities.command.CommandEvent;

import tel.discord.rtab.PlayerLevel;
import tel.discord.rtab.commands.ParsingCommand;
import tel.discord.rtab.commands.channel.BooleanSetting;
import tel.discord.rtab.commands.channel.ChannelSetting;

public class RecalcLevelCommand extends ParsingCommand
{
	private static final String HISTORY = "history";
	private static final String SCORES = "scores";

	public RecalcLevelCommand()
	{
        this.name = "recalc";
        this.help = "recalculate the level of one or all players";
        this.guildOnly = true;
        this.ownerCommand = true;
		this.hidden = true;
	}
	
	@Override
	protected void execute(CommandEvent event)
	{
		//Start by getting the scope
		boolean recalcAll = event.getArgs().equalsIgnoreCase("all");
		String userID = "";
		String guildID = event.getGuild().getId();
		HashSet<String> checkedIDs;
		HashSet<String> checkedIDsOverall = new HashSet<>();
		if(!recalcAll)
			userID = event.getArgs();
		try
		{
			List<String> channelList = Files.readAllLines(Paths.get("guilds","guild"+guildID+".csv"));
			//Now we loop through each channel that contributes toward player level
			for(String nextChannel : channelList)
			{
				String[] channelRecord = nextChannel.split("#");
				if(BooleanSetting.parseSetting(channelRecord[ChannelSetting.CHANNEL_COUNTS_TO_PLAYER_LEVEL.getLocation()],false))
				{
					String channelID = channelRecord[0];
					//If we have a user ID, go straight to them
					if(!recalcAll)
					{
						event.reply(recalcPlayerForChannel(userID, channelID, guildID, checkedIDsOverall.add(userID)));
					}
					else
					{
						//Search through every season to find *everyone* and recalc them
						checkedIDs = new HashSet<>(); //Reset the list as we're starting a new channel
						int season = 1;
						//We're going to keep reading history files as long as they're there
						while(Files.exists(Paths.get(SCORES,HISTORY+channelID+"s"+season+".csv")))
						{
							//Load up the next one
							List<String> list = Files.readAllLines(Paths.get(SCORES,HISTORY+channelID+"s"+season+".csv"));
							for(String next : list)
							{
								String[] record = next.split("#");
								userID = record[0];
								if(checkedIDs.add(userID)) //Don't duplicate the same player within a single channel's history
								{
									event.reply(recalcPlayerForChannel(userID, channelID, guildID, checkedIDsOverall.add(userID)));
								}
							}
							season++;
						}
						//Finally, check the current season (if there is one) to find anyone playing in their first season
						if(Files.exists(Paths.get(SCORES,SCORES+channelID+".csv")))
						{
							List<String> list = Files.readAllLines(Paths.get(SCORES,SCORES+channelID+".csv"));
							for(String next : list)
							{
								String[] record = next.split("#");
								userID = record[0];
								if(checkedIDs.add(userID)) //Still don't duplicate a player
								{
									event.reply(recalcPlayerForChannel(userID, channelID, guildID, checkedIDsOverall.add(userID)));
								}
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			event.reply("File read fail.");
		}
	}
	
	private String recalcPlayerForChannel(String userID, String channelID, String guildID, boolean eraseOldData)
	{
		String name = "";
		PlayerLevel playerLevelData = new PlayerLevel(guildID, userID, name); //We'll set a name once we find one
		int season = 1;
		long championMoney = 0;
		long maingameMoney = 0;
		try
		{
			//We're going to keep reading history files as long as they're there
			while(Files.exists(Paths.get(SCORES,HISTORY+channelID+"s"+season+".csv")))
			{
				//Load up the next one
				List<String> list = Files.readAllLines(Paths.get(SCORES,HISTORY+channelID+"s"+season+".csv"));
				int index = findUserInList(list,userID,false);
				//If we find them, add their records to the pile
				if(index >= 0 && index < list.size())
				{
					String[] record = list.get(index).split("#");
					name = record[1];
					maingameMoney += Math.min(1_000_000_000, Long.parseLong(record[2]));
					if(index == 0)
						championMoney += Long.parseLong(record[2]);
				}
				season ++;
			}
			//Also check the current season, if there's one ongoing
			if(Files.exists(Paths.get(SCORES,SCORES+channelID+".csv")))
			{
				List<String> list = Files.readAllLines(Paths.get(SCORES,SCORES+channelID+".csv"));
				int index = findUserInList(list,userID,false);
				//If we find them, add their current season score as well
				if(index >= 0 && index < list.size())
				{
					String[] record = list.get(index).split("#");
					name = record[1];
					maingameMoney += Math.min(1_000_000_000, Long.parseLong(record[2]));
					if(index == 0)
						championMoney += Long.parseLong(record[2]);
				}
			}
			//Now update their level data
			if(eraseOldData)
			{
				playerLevelData.setXP(0);
				playerLevelData.setChampXP(0);
			}
			playerLevelData.setName(name);
			playerLevelData.addXP(maingameMoney);
			playerLevelData.addChampXP(championMoney);
			playerLevelData.saveLevel();
			return name + " is level " + playerLevelData.getTotalLevel();
		}
		catch(Exception e)
		{
			return "File read fail.";
		}
	}
}
