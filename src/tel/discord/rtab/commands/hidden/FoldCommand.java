package tel.discord.rtab.commands.hidden;

import tel.discord.rtab.GameController;
import tel.discord.rtab.GameStatus;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.board.HiddenCommand;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class FoldCommand extends Command
{
	public FoldCommand()
	{
		this.name = "fold";
		this.help = "voluntarily exit the round, keeping your booster and minigames";
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
				//Make sure the player is in the game and the game is mid-round but not mid-turn
				//Also, that the player is alive, and they have a fold to use
				if(game.gameStatus != GameStatus.IN_PROGRESS || game.resolvingTurn || game.players.get(player).status != PlayerStatus.ALIVE || chosenCommand != HiddenCommand.FOLD && chosenCommand != HiddenCommand.WILD)
					event.reply("You can't do this right now.");
				else
					game.useFold(player);
				return;
			}
		}
		//We aren't in a game channel? Uh...
		event.reply("This is not a game channel.");
	}
}