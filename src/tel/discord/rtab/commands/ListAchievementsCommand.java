package tel.discord.rtab.commands;

import java.io.IOException;

import com.jagrosh.jdautilities.command.CommandEvent;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.Achievement.AchievementType;

public class ListAchievementsCommand extends ParsingCommand
{
	public ListAchievementsCommand()
    {
        this.name = "achievements";
        this.help = "view your unlocked achievements";
        this.guildOnly = true;
    }
	
	@Override
	protected void execute(CommandEvent event)
	{
		//Start by getting their achievement list
		try
		{
			String[] record = Achievement.getAchievementList(event.getAuthor().getId(), event.getGuild().getId());
			String name = event.getMember().getEffectiveName();
			StringBuilder output = new StringBuilder();
			output.append("```\n");
			AchievementType desiredAchievementType = null;
			switch(event.getArgs().toUpperCase())
			{
			case "1":
			case "A":
			case "EVENT":
				desiredAchievementType = AchievementType.EVENT;
				output.append("Event Achievements - "+name+"\n\n");
				break;
			case "2":
			case "B":
			case "MINIGAME":
				desiredAchievementType = AchievementType.MINIGAME;
				output.append("Minigame Achivements - "+name+"\n\n");
				break;
			case "3":
			case "C":
			case "MILESTONE":
				desiredAchievementType = AchievementType.MILESTONE;
				output.append("Milestone Achievements - "+name+"\n\n");
				break;
			default:
				//Display a summary of achievements earned
				output = getAchievementSummary(record, name);
			}
			//Get a list of the desired achievements
			if(desiredAchievementType != null)
			{
				int achievementFlags = Integer.parseInt(record[desiredAchievementType.recordLocation]);
				for(Achievement next : Achievement.values())
				{
					if(next.achievementType == desiredAchievementType)
					{
						output.append("["+((achievementFlags>>>next.bitLocation)%2==1?"X":" ")+"] ");
						output.append(next.publicName+"\n");
						output.append("  "+next.unlockCondition+"\n\n");
					}
				}
			}
			//Close off the output and send it
			output.append("```");
			event.reply(output.toString());
		}
		catch(IOException e)
		{
			event.reply("Failed to load achievement list.");
		}
	}
	
	StringBuilder getAchievementSummary(String[] record, String name)
	{
		//Count earned achievements
		int[] earnedAchievements = new int[Achievement.AchievementType.values().length];
		int earnedAchievementsTotal = 0;
		for(int i=2; i-2<earnedAchievements.length; i++)
		{
			int achievementFlags = Integer.parseInt(record[i]);
			while(achievementFlags > 0)
			{
				if(achievementFlags % 2 == 1)
				{
					earnedAchievements[i-2] ++;
					earnedAchievementsTotal ++;
				}
				achievementFlags >>>= 1;
			}
		}
		//Count all achievements
		int[] allAchievements = new int[earnedAchievements.length];
		int allAchievementsTotal = 0;
		for(Achievement next : Achievement.values())
		{
			allAchievements[next.achievementType.recordLocation-2] ++;
			allAchievementsTotal ++;
		}
		//Put together summary
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		output.append("Achievement Summary - "+name+"\n\n");
		output.append(String.format("a - Event Achievements: %d/%d\n",earnedAchievements[0],allAchievements[0]));
		output.append(String.format("b - Minigame Achievements: %d/%d\n", earnedAchievements[1],allAchievements[1]));
		output.append(String.format("c - Milestone Achievements: %d/%d\n", earnedAchievements[2],allAchievements[2]));
		output.append(String.format("\nTOTAL ACHIEVEMENTS: %d/%d\n", earnedAchievementsTotal, allAchievementsTotal));
		output.append("Type !achievements followed by a letter to list that page of achievements.\n");
		return output;
	}
}
