package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public interface EventSpace
{
	void execute(GameController game, int player);
}
