package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class FinalCountdown implements EventSpace
{
	@Override
	public String getName()
	{
		return "Final Countdown";
	}

	@Override
	public void execute(GameController game, int player)
	{
		if(!game.finalCountdown)
		{
			//Send message with appropriate
			game.channel.sendMessage("It's the **Final Countdown**!").queue();
			game.finalCountdown = true;
			//Figure out turns left: max 50% remaining spaces, min players alive (max overrides min)
			if(game.spacesLeft/2 <= game.playersAlive)
				game.fcTurnsLeft = game.spacesLeft/2;
			else
				game.fcTurnsLeft = (int) (Math.random() * ((game.spacesLeft/2) - game.playersAlive + 1) + game.playersAlive);
		}
		else
		{
			game.channel.sendMessage("It's another **Final Countdown**! Turns remaining cut in half!").queue();
			game.fcTurnsLeft /= 2;
		}
	}

}
