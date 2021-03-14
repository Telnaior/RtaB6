package tel.discord.rtab.commands.channel;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static tel.discord.rtab.RaceToABillionBot.waiter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ArchiveSeasonCommand extends Command
{
	public ArchiveSeasonCommand()
	{
		this.name = "archiveseason";
		this.help = "saves a completed season to history and resets the scoreboard";
		this.hidden = true;
		this.userPermissions = new Permission[] {Permission.MANAGE_SERVER};
	}

	@Override
	protected void execute(CommandEvent event)
	{
		//Define paths and make sure this command makes sense to do
		Path scoreCurrentFile = Paths.get("scores","scores"+event.getChannel().getId()+".csv");
		Path scoreBackupFile = Paths.get("scores","scores"+event.getChannel().getId()+"backup.csv");
		Path jackpotCurrentFile = Paths.get("scores","jackpots"+event.getChannel().getId()+".csv");
		Path jackpotBackupFile = Paths.get("scores","jackpots"+event.getChannel().getId()+"backup.csv");
		if(!Files.exists(scoreCurrentFile))
		{
			event.reply("No score data found in this channel.");
			return;
		}
		//Make sure the winner actually has a billion
		try
		{
			List<String> list = Files.readAllLines(scoreCurrentFile);
			String[] winner = list.get(0).split("#");
			int winnerScore = Integer.parseInt(winner[2]);
			if(winnerScore < 1_000_000_000)
			{
				event.reply("An incomplete season cannot be archived.");
				return;
			}
		}
		catch(IOException e1)
		{
			event.reply("Failed to open save file.");
			e1.printStackTrace();
		}
		
		//Alright, we passed the checks, give them the option
		event.reply("Are you sure? This will save the season in history and reset everyone's score to 0! (yes/no)");
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
				//If they said yes, archive the scoreboard files (also save a backup because Murphy's Law)
				e ->
				{
					if(!e.getMessage().getContentStripped().equalsIgnoreCase("yes"))
					{
						event.reply("Very well.");
						return;
					}
					//Figure out how many seasons of history there are by looking for the first file that doesn't exist
					int thisSeason = 0;
					Path historyFile;
					do
					{
						thisSeason++;
						historyFile = Paths.get("scores","history"+event.getChannel().getId()+"s"+thisSeason+".csv");
					}
					while(Files.exists(Paths.get("scores","history"+event.getChannel().getId()+"s"+thisSeason+".csv")));
					try
					{
						//Create the history file
						Files.createFile(historyFile);
						List<String> list = Files.readAllLines(scoreCurrentFile);
						for(String next : list)
						{
							String[] record = next.split("#");
							String update = record[0]+"#"+record[1]+"#"+record[2]+"\n";
							Files.write(historyFile,update.getBytes(),StandardOpenOption.APPEND);
						}
						//Delete old backup files if they exist
						Files.deleteIfExists(scoreBackupFile);
						Files.deleteIfExists(jackpotBackupFile);
						//Then move the current files into their place
						Files.move(scoreCurrentFile,scoreBackupFile);
						if(Files.exists(jackpotCurrentFile))
							Files.move(jackpotCurrentFile,jackpotBackupFile);
						event.reply("Season archived. Good luck in the new season!");
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
