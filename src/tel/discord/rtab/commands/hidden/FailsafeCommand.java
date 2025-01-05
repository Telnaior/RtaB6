package tel.discord.rtab.commands.hidden;

import tel.discord.rtab.GameController;
import tel.discord.rtab.GameStatus;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.board.HiddenCommand;

import java.util.concurrent.TimeUnit;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class FailsafeCommand extends Command
{
	public FailsafeCommand()
	{
		this.name = "failsafe";
		this.help = "immediately win the round if only bombs remain";
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
				//Also, that the player is alive, and they have a failsafe to use
				if(game.gameStatus != GameStatus.IN_PROGRESS || game.resolvingTurn || game.players.get(player).status != PlayerStatus.ALIVE || chosenCommand != HiddenCommand.FAILSAFE && chosenCommand != HiddenCommand.WILD)
					event.reply("You can't do this right now.");
				else
					//We schedule a timer here so it uses the same thread as the turn timeout (hence blocking the two from overlapping)
					game.timer.schedule(() -> game.useFailsafe(player), 500, TimeUnit.MILLISECONDS);
				return;
			}
		}
		//We aren't in a game channel? Uh...
		event.reply("This is not a game channel.");
	}
}