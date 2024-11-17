package tel.discord.rtab.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class ChampionsCommand extends Command
{
    public ChampionsCommand()
    {
        this.name = "champions";
        this.help = "view the winners of seasons past";
        this.guildOnly = true;
    }

	@Override
	protected void execute(CommandEvent event)
	{
		try
		{
			StringBuilder output = new StringBuilder();
			output.append("```\n");
			output.append("Champions in "+event.getChannel().getName()+":\n");
			String channelID = event.getChannel().getId(); //The command is flagged guild-only, so we know this won't be null
			int season = 1;
			//We're going to keep reading history files as long as they're there
			while(Files.exists(Paths.get("scores","history"+channelID+"s"+season+".csv")))
			{
				List<String> list = Files.readAllLines(
						Paths.get("scores","history"+channelID+"s"+season+".csv"));
				String[] record = list.getFirst().split("#");
				output.append(String.format("Season %2d - %s%n", season, record[1]));
				season++;
			}
			output.append("```");
			event.reply(output.toString());
		}
		catch(IOException e)
		{
			event.reply("Failed to load hall of fame data. (Did you catch MISSINGNO?)");
		}
	}

}
