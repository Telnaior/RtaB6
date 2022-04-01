package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.GameStatus;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.RaceToABillionBot;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class FlagCommand extends Command
{
	public FlagCommand()
	{
		this.name = "flag";
		this.help = "flag a bomb to disarm it";
		this.guildOnly = true;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				int player = game.findPlayerInGame(event.getAuthor().getId());
				//Make sure the player is in the game and just had their turn
				if(game.gameStatus != GameStatus.IN_PROGRESS || player == -1 || (player != game.previousTurn && game.playersAlive >= 1)
						|| game.players.get(player).status != PlayerStatus.ALIVE)
					event.reply("You cannot flag a space right now.");
				else
				{
					int location = game.getSpaceFromGrid(event.getArgs());
					if(game.pickedSpaces[location])
						event.reply("That space has already been picked.");
					else
						game.flagBomb(player, location);
				}
				return;
			}
		}
		//We aren't in a game channel? Uh...
		event.reply("This is not a game channel.");
	}
}