package tel.discord.rtab.commands;

import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.games.MiniGame;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class SkipCommand extends Command
{
	public SkipCommand()
	{
		this.name = "skip";
		this.help = "skip the instructions in a minigame";
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(MiniGame game : RaceToABillionBot.minigame)
		{
			if(game.getChannelID().equals(event.getChannel().getId()))
			{
				if(!game.getPlayerID().equals(event.getAuthor().getId()))
					return;
				//Cool, tell the game to skip and let it handle that
				game.skipMessages();
				//We found the right channel, so 
				return;
			}
		}
		//We aren't in a game channel? Uh...
		event.reply("No minigame found.");
	}
}