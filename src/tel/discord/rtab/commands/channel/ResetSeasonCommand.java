package tel.discord.rtab.commands.channel;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static tel.discord.rtab.RaceToABillionBot.waiter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class ResetSeasonCommand extends Command
{
	public ResetSeasonCommand()
	{
		this.name = "resetseason";
		this.help = "erases the scoreboard for a season, resetting everything to 0";
		this.hidden = true;
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event)
	{
		if(!Files.exists(Paths.get("scores","scores"+event.getChannel().getId()+".csv")))
		{
			event.reply("No score data found in this channel.");
			return;
		}
		event.reply("**ARE YOU SURE?** This will reset everyone's score to 0, and no information will be saved! (yes/no)");
		waiter.waitForEvent(MessageReceivedEvent.class,
				//Make sure it's the one who sent the message and it's specifically "yes" or "no"
				e -> 
				{
					if(e.getAuthor().equals(event.getAuthor()))
					{
						String message = e.getMessage().getContentStripped();
						return message.equalsIgnoreCase("yes") || message.equalsIgnoreCase("no");
					}
					return false;
				},
				//If they said yes, delete the scoreboard files (actually save a backup because Murphy's Law)
				e ->
				{
					if(!e.getMessage().getContentStripped().equalsIgnoreCase("yes"))
					{
						event.reply("Very well.");
						return;
					}
					//Delete old backup files if they exist
					try
					{
						Path scoreBackupFile = Paths.get("scores","scores"+event.getChannel().getId()+"backup.csv");
						Path jackpotBackupFile = Paths.get("scores","jackpots"+event.getChannel().getId()+"backup.csv");
						Files.deleteIfExists(scoreBackupFile);
						Files.deleteIfExists(jackpotBackupFile);
						//Then move the current files into their place
						Files.move(scoreBackupFile.resolveSibling("scores"+event.getChannel().getId()+".csv"),scoreBackupFile);
						Files.move(jackpotBackupFile.resolveSibling("jackpots"+event.getChannel().getId()+".csv"),jackpotBackupFile);
						event.reply("Scores reset. Good luck in the new season!");
					}
					catch(IOException e1)
					{
						event.reply("Failed to update files.");
						e1.printStackTrace();
					}
				},
				30,TimeUnit.SECONDS, () ->
				{
					event.reply("Request expired.");
				}
				);
	}

}
