package tel.discord.rtab.commands.channel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.MinigameTournament.TournamentStatus;

public class GameChannelDisableCommand extends Command
{
	
	public GameChannelDisableCommand()
	{
		this.name = "disablechannel";
		this.help = "disables a game channel, preventing games from being played there";
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
			List<String> list = Files.readAllLines(Paths.get("guilds","guild"+event.getGuild().getId()+".csv"));
			//Find this channel in the list
			for(int i=0; i<=list.size(); i++)
			{
				//If we're at the end of the list, tell them and suggest an alternative
				if(i == list.size())
				{
					event.reply("Channel not found in database.");
					return;
				}
				String[] record = list.get(i).split("#");
				if(record[0].equals(channelID))
				{
					switch (record[1]) {
						case "enabled" -> {
							//Delete the appropriate game controller
							for (int j = 0; j < RaceToABillionBot.game.size(); j++)
								if (RaceToABillionBot.game.get(j).channel.getId().equals(channelID)) {
									event.reply("Channel disabled.");
									RaceToABillionBot.game.get(j).timer.purge();
									RaceToABillionBot.game.get(j).timer.shutdownNow();
									if (RaceToABillionBot.game.get(j).currentGame != null)
										RaceToABillionBot.game.get(j).currentGame.gameOver();
									RaceToABillionBot.game.remove(j);
									break;
								}
						}
						case "minigame" -> {
							//Delete the appropriate tournament handler
							for(int j=0; j < RaceToABillionBot.tournament.size(); j++)
								if(RaceToABillionBot.tournament.get(j).channel.getId().equals(channelID)) {
									event.reply("Channel disabled.");
									RaceToABillionBot.tournament.get(j).timer.purge();
									RaceToABillionBot.tournament.get(j).timer.shutdownNow();
									RaceToABillionBot.tournament.get(j).status = TournamentStatus.SHUTDOWN;
									if (RaceToABillionBot.tournament.get(j).currentGame != null)
										RaceToABillionBot.tournament.get(j).currentGame.gameOver();
									RaceToABillionBot.tournament.remove(j);
								}
						}
						case "sbc" -> {
							//Delete the appropriate game controller
							for (int j = 0; j < RaceToABillionBot.game.size(); j++)
								if (RaceToABillionBot.game.get(j).channel.getId().equals(channelID)) {
									event.reply("Channel disabled.");
									RaceToABillionBot.game.get(j).timer.shutdownNow();
									if (RaceToABillionBot.game.get(j).currentGame != null)
										RaceToABillionBot.game.get(j).currentGame.gameOver();
									RaceToABillionBot.game.remove(j);
									break;
								}
							//and the appropriate challenge handler
							for (int j = 0; j < RaceToABillionBot.challenge.size(); j++)
								if (RaceToABillionBot.challenge.get(j).channel.getId().equals(channelID)) {
									RaceToABillionBot.challenge.get(j).timer.purge();
									RaceToABillionBot.challenge.get(j).timer.shutdownNow();
									RaceToABillionBot.challenge.remove(j);
								}
						}
					}
					//Cool, we found it, now remake the entry with the flipped bit
					record[1] = "disabled";
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
		}
		catch (IOException e)
		{
			event.reply("Save failed. Try again later.");
			e.printStackTrace();
		}	
	}
	
}