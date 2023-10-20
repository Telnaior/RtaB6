package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.RtaBMath;

public class BananaScramble implements EventSpace
{
	@Override
	public String getName()
	{
		return "\uD83C\uDF4C";
	}

	@Override
	public void execute(GameController game, int player)
	{
		game.channel.sendMessage("\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C"
				+ "\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C").queue();
		game.gameboard.superScramble();
		//shuffle picked spaces too, though this is a bit of a pain
		for(int i=0; i<game.boardSize-1; i++)
		{
			int swapPosition = (int)(RtaBMath.random()*(game.boardSize-i))+1;
			boolean temp = game.pickedSpaces[i];
			game.pickedSpaces[i] = game.pickedSpaces[swapPosition];
			game.pickedSpaces[swapPosition] = temp;
		}
		//clear out peeks and known bombs for the sake of AI logic
		for(Player next : game.players)
		{
			next.knownBombs.clear();
			next.safePeeks.clear();
			next.allPeeks.clear();
		}
	}

}
