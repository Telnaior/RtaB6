package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.CommandEvent;

import tel.discord.rtab.PlayerLevel;

public class LevelCommand extends ParsingCommand
{
	public LevelCommand()
    {
        this.name = "level";
        this.aliases = new String[]{"lvl","xp"};
        this.help = "view your overall level";
        this.guildOnly = true;
    }
	
	@Override
	protected void execute(CommandEvent event)
	{
		//Get data
		PlayerLevel playerLevelData = new PlayerLevel(event.getGuild().getId(), event.getAuthor().getId(), event.getMember().getEffectiveName());
		int totalLevel = playerLevelData.getTotalLevel();
		int playerLevel = playerLevelData.getPlayerLevel();
		long playerXP = playerLevelData.getPlayerXP();
		long playerXPNeeded = playerLevelData.getRequiredXP();
		double playerXPPercent = (100.0*playerXP) / playerXPNeeded;
		int championLevel = playerLevelData.getChampLevel();
		long championXP = playerLevelData.getChampXP();
		long championXPNeeded = playerLevelData.getRequiredChampXP();
		double championXPPercent = (100.0*championXP) / championXPNeeded;
		int achievementLevel = playerLevelData.getAchievementLevel();
		
		//Format it into a message
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		//Header
		output.append("Current Levels for "+event.getMember().getEffectiveName()+"\n\n");
		//Player Level
		output.append("Player Level: ").append(playerLevel).append("\n");
		output.append(String.format("Progress to Next Level:\n$%,d / $%,d\n", playerXP, playerXPNeeded));
		for(int i=0; i<10; i++)
		{
			if((i*10) + 5 <= playerXPPercent)
				output.append("\u2588"); //Escaped full-block
			else
				output.append("\u2591"); //Escaped light-shade block
		}
		output.append(String.format(" %05.2f%%\n\n",playerXPPercent));
		//Champion level if applicable
		if(championLevel > 0)
		{
			output.append("Champion Level: ").append(championLevel).append("\n");
			output.append(String.format("Progress to Next Level:\n$%,d / $%,d\n", championXP, championXPNeeded));
			for(int i=0; i<10; i++)
			{
				if((i*10) + 5 <= championXPPercent)
					output.append("\u2588"); //Escaped full-block
				else
					output.append("\u2591"); //Escaped light-shade block
			}
			output.append(String.format(" %05.2f%%\n\n",championXPPercent));
		}
		//Achievement level
		output.append("Achievement Level: ").append(achievementLevel).append("\n\n");
		//Total level and footer
		output.append("TOTAL LEVEL: ").append(totalLevel).append("\n```");
		event.reply(output.toString());
	}

}
