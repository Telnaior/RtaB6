package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

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
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.getId().equals(event.getChannel().getId()))
			{
				//Check that there's a minigame and that the current player is the minigame's owner
				if(game.currentGame == null)
					return;
				int player = game.findPlayerInGame(event.getAuthor().getId());
				if(player != game.currentTurn)
					return;
				//Cool, tell the game to skip and let it handle that
				game.currentGame.skipMessages();
				//We found the right channel, so 
				return;
			}
		}
		//We aren't in a game channel? Uh...
		event.reply("This is not a game channel.");
	}
}