package tel.discord.rtab.commands.hidden;

import tel.discord.rtab.GameController;
import tel.discord.rtab.GameStatus;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.board.HiddenCommand;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class DefuseCommand extends Command
{
	public DefuseCommand()
	{
		this.name = "defuse";
		this.aliases = new String[]{"shuffle"};
		this.help = "reshuffle a space, removing any bomb there and rerolling the contents";
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
				String rawSpace = event.getArgs();
				//Check that it's valid (the game is running, the space is legit, they're alive, and they have the command)
				if(game.gameStatus != GameStatus.IN_PROGRESS || !game.checkValidNumber(rawSpace) || game.players.get(player).status != PlayerStatus.ALIVE || chosenCommand != HiddenCommand.DEFUSE && chosenCommand != HiddenCommand.WILD)
					event.reply("You can't do this right now.");
				//Cool, we're good, pass it over
				else
				{
					int space = Integer.parseInt(rawSpace) - 1;
					if(game.pickedSpaces[space])
						event.reply("That space has already been picked.");
					else
						game.useShuffler(player, space);
				}
				return;
			}
		}
		//We aren't in a game channel? Uh...
		event.reply("This is not a game channel.");
	}
}