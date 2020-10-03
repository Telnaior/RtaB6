package tel.discord.rtab.commands.hidden;

import tel.discord.rtab.GameController;
import tel.discord.rtab.GameStatus;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.board.HiddenCommand;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class WagerCommand extends Command
{
	public WagerCommand()
	{
		this.name = "wager";
		this.help = "start a wager, forcing everyone alive to put down money that the round winners will claim";
		this.hidden = true;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				int player = game.findPlayerInGame(event.getAuthor().getId());
				//Check that it's valid (the game is running, they're alive, and they have the command)
				if(game.gameStatus != GameStatus.IN_PROGRESS || player == -1
						|| game.players.get(player).status != PlayerStatus.ALIVE || game.players.get(player).hiddenCommand != HiddenCommand.WAGER)
					event.reply("You can't do this right now.");
				else
					game.useWager(player);
				return;
			}
		}
		//We aren't in a game channel? Uh...
		event.reply("This is not a game channel.");
	}
}