package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;

import java.security.SecureRandom;

public class ClusterBomb implements Bomb
{
	public void explode(GameController game, int victim, int penalty)
	{
		SecureRandom r = new SecureRandom();
		game.channel.sendMessage("It goes **BOOM**...").queue();
		int chain = 1;
		do
		{
			chain *= 2;
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			if(chain <= 398) //A bomb bigger than this would exceed Discord's character limit
			{
				StringBuilder nextLevel = new StringBuilder();
				nextLevel.append("**");
				for(int i=0; i<chain; i++)
				{
					nextLevel.append("BOOM");
					if(i+1 < chain)
						nextLevel.append(" ");
				}
				nextLevel.append("**");
				if(chain < 8)
					nextLevel.append("...");
				else
					nextLevel.append("!!!");
				game.channel.sendMessage(nextLevel).queue();
			}
			else //Congratulations on being the unluckiest player in the world (a 1/68,719,476,736 chance)
			{
				try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
				game.channel.sendMessage("...").queue();
				try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
				game.channel.sendMessage(game.players.get(victim).getName()+" was disintegrated by the force of the bomb.").queue();
			}
		}
		while(r.nextDouble(chain) < 1 && chain <= 398);
		try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
		if(chain <= 398)
		{
			game.channel.sendMessage(String.format("**$%,d** penalty!",Math.abs(chain*penalty))).queue();
			StringBuilder extraResult = game.players.get(victim).blowUp(chain*penalty,false);
			if(extraResult != null)
				game.channel.sendMessage(extraResult).queue();
		}
		else //"Disintegrated" isn't a joke
		{
			game.players.get(victim).blowUp(0,false);
			game.players.get(victim).money = -1_000_000_000;
		}
	}
}
