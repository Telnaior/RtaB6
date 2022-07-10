package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.GameStatus;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class PeekCommand extends Command
{
	public PeekCommand()
	{
		this.name = "peek";
		this.help = "use your peek (only available in-game)";
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				int player = game.findPlayerInGame(event.getAuthor().getId());
				//Make sure they're in the game, the game is running, and they actually have a peek
				if(game.gameStatus != GameStatus.IN_PROGRESS || player == -1 || game.players.get(player).peeks < 1)
				{
					event.reply("You don't have a peek to use.");
					return;
				}
				//If we're flipping a bomb, just don't thanks
				if(game.resolvingBomb && game.currentTurn == player)
				{
					event.reply("Your peek goes **BOOM**.");
					return;
				}
				//Make sure they're peeking a space that's on the board
				try
				{
					int location = Integer.parseInt(event.getArgs())-1;
					if(location < 0 || location >= game.boardSize || game.pickedSpaces[location])
					{
						event.reply("That is not a valid space.");
						return;
					}
					//We checked everything, pass it on to the game to actually peek it
					game.usePeek(player, location);
				}
				catch(NumberFormatException e)
				{
					event.reply("That is not a valid space.");
					return;
				}
				//We found the right channel, so 
				return;
			}
		}
		//We aren't in a game channel? Uh...
		event.reply("This is not a game channel.");
	}
}