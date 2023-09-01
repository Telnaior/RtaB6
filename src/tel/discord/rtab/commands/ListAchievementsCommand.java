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
        this.aliases = new String[]{"ach","awards"};
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
			boolean replyInDm = true;
			StringBuilder output = new StringBuilder();
			output.append("```\n");
			AchievementType desiredAchievementType = null;
			switch(event.getArgs().toUpperCase())
			{
			case "1":
			case "A":
			case "EVENT":
				desiredAchievementType = AchievementType.EVENT;
				output.append("Event Achievements - ").append(name).append("\n\n");
				break;
			case "2":
			case "B":
			case "MINIGAME":
				desiredAchievementType = AchievementType.MINIGAME;
				output.append("Minigame Achivements - ").append(name).append("\n\n");
				break;
			case "3":
			case "C":
			case "MILESTONE":
				desiredAchievementType = AchievementType.MILESTONE;
				output.append("Milestone Achievements - ").append(name).append("\n\n");
				break;
			default:
				//Display a summary of achievements earned
				output = getAchievementSummary(record, name);
				replyInDm = false;
			}
			//Get a list of the desired achievements
			if(desiredAchievementType != null)
			{
				int achievementFlags = Integer.parseInt(record[desiredAchievementType.recordLocation]);
				for(Achievement next : Achievement.values())
				{
					if(next.achievementType == desiredAchievementType)
					{
						//Don't show retired achievements unless they're earned
						if(!next.retired || (achievementFlags>>>next.bitLocation)%2 == 1)
						{
							output.append("[").append((achievementFlags >>> next.bitLocation) % 2 == 1 ? "X" : " ").append("] ");
							output.append(next.publicName).append(next.retired ? "(hidden)" : "").append("\n");
							output.append("  ").append(next.unlockCondition).append("\n\n");
						}
					}
				}
			}
			//Close off the output and send it
			output.append("```");
			if(replyInDm)
				event.replyInDm(output.toString());
			else
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
		int achievementTypes = Achievement.AchievementType.values().length;
		int[] earnedAchievements = new int[achievementTypes];
		int[] allAchievements = new int[achievementTypes];
		int[] earnedRetAchievements = new int[achievementTypes];
		int earnedAchievementsTotal = 0;
		int allAchievementsTotal = 0;
		int earnedRetAchievementsTotal = 0;
		for(Achievement next : Achievement.values())
		{
			AchievementType type = next.achievementType;
			//If the achievement isn't retired, add it to the total count
			if(!next.retired)
			{
				allAchievements[type.ordinal()] ++;
				allAchievementsTotal ++;
			}
			int achievementFlags = Integer.parseInt(record[type.recordLocation]);
			//If it's earned, add it to the appropriate total depending on retirement state
			if((achievementFlags>>>next.bitLocation)%2 == 1)
			{
				if(next.retired)
				{
					earnedRetAchievements[type.ordinal()] ++;
					earnedRetAchievementsTotal ++;
				}
				else
				{
					earnedAchievements[type.ordinal()] ++;
					earnedAchievementsTotal ++;
				}
			}
		}
		//Put together summary
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		output.append("Achievement Summary - ").append(name).append("\n\n");
		output.append(String.format("a - Event Achievements: %d/%d",earnedAchievements[0],allAchievements[0]));
		output.append(earnedRetAchievements[0] > 0 ? String.format(" +%d%n", earnedRetAchievements[0]) : "\n");
		output.append(String.format("b - Minigame Achievements: %d/%d", earnedAchievements[1],allAchievements[1]));
		output.append(earnedRetAchievements[1] > 0 ? String.format(" +%d%n", earnedRetAchievements[1]) : "\n");
		output.append(String.format("c - Milestone Achievements: %d/%d", earnedAchievements[2],allAchievements[2]));
		output.append(earnedRetAchievements[2] > 0 ? String.format(" +%d%n", earnedRetAchievements[2]) : "\n");
		output.append(String.format("%nTOTAL ACHIEVEMENTS: %d/%d%n", earnedAchievementsTotal, allAchievementsTotal));
		if(earnedRetAchievementsTotal > 0)
			output.append(String.format("+ %d Hidden Achievement", earnedRetAchievementsTotal)).append(earnedRetAchievementsTotal == 1 ? "\n" : "s\n");
		output.append("Type !achievements followed by a letter to list that page of achievements.\n");
		return output;
	}
}
