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
			switch (event.getArgs().toUpperCase()) {
				case "1", "A", "MILESTONE" -> output.append("Milestone Achievements - ").append(name).append("\n\n");
				case "2", "B", "EVENT" -> output.append("Event Achievements - ").append(name).append("\n\n");
				case "3", "C", "MINIGAME" -> output.append("Minigame Achivements - ").append(name).append("\n\n");
				case "4", "D", "LUCKY", "CHARMS", "LUCKY CHARMS" -> output.append("Lucky Charms - ").append(name).append("\n\n");
				default -> //Display a summary of achievements earned
                        output = getAchievementSummary(record, name);
			}
			//Get a list of the desired achievements
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
		output.append(String.format("a - Milestone Achievements: %d/%d", earnedAchievements[2],allAchievements[2]));
		output.append(earnedRetAchievements[2] > 0 ? String.format(" +%d%n", earnedRetAchievements[2]) : "\n");
		output.append(String.format("b - Event Achievements: %d/%d",earnedAchievements[0],allAchievements[0]));
		output.append(earnedRetAchievements[0] > 0 ? String.format(" +%d%n", earnedRetAchievements[0]) : "\n");
		output.append(String.format("c - Minigame Achievements: %d/%d", earnedAchievements[1],allAchievements[1]));
		output.append(earnedRetAchievements[1] > 0 ? String.format(" +%d%n", earnedRetAchievements[1]) : "\n");
		if(earnedRetAchievements[3] > 0)
			output.append(String.format("d -      +%2d Lucky Charms%n", earnedRetAchievements[3]));
		output.append(String.format("%nTOTAL ACHIEVEMENTS: %d/%d%n", earnedAchievementsTotal, allAchievementsTotal));
		if(earnedRetAchievementsTotal > 0)
			output.append(String.format("+ %d Total Hidden Achievement", earnedRetAchievementsTotal)).append(earnedRetAchievementsTotal == 1 ? "\n" : "s\n");
		output.append("Type !achievements followed by a letter to list that page of achievements.\n");
		return output;
	}
}
