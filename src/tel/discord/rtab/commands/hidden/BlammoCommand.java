package tel.discord.rtab.commands.hidden;

import tel.discord.rtab.GameController;
import tel.discord.rtab.GameStatus;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.board.HiddenCommand;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class BlammoCommand extends Command
{
	public BlammoCommand()
	{
		this.name = "blammo";
		this.help = "summon a blammo to attack the next player";
		this.hidden = true;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.getId().equals(event.getChannel().getId()))
			{
				int player = game.findPlayerInGame(event.getAuthor().getId());
				HiddenCommand chosenCommand = game.players.get(player).hiddenCommand;
				//Check that it's valid (the game is running, they're alive, they have the command, and there's no blammo already waiting)
				if(game.gameStatus != GameStatus.IN_PROGRESS || player == -1 || game.futureBlammo
						|| game.players.get(player).status != PlayerStatus.ALIVE ||
						(chosenCommand != HiddenCommand.BLAMMO && chosenCommand != HiddenCommand.WILD))
					event.reply("You can't do this right now.");
				else
					game.useBlammoSummoner(player);
				return;
			}
		}
		//We aren't in a game channel? Uh...
		event.reply("This is not a game channel.");
	}
}