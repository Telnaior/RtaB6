package tel.discord.rtab.board;

import tel.discord.rtab.events.*;

public enum EventType implements WeightedSpace
{
	INSTANT_ANNUITY		( 5) { public EventSpace getEvent() { return new InstantAnnuity(); } },
	BOOST_CHARGER		( 4) { public EventSpace getEvent() { return new BoostCharger(); } },
	DOUBLE_DEAL			( 3) { public EventSpace getEvent() { return new DoubleDeal(); } },
	BOOST_MAGNET		( 2) { public EventSpace getEvent() { return new BoostMagnet(); } },
	ONEBUCKBEHIND		( 1) { public EventSpace getEvent() { return new OneBuckBehind(); } },	//Author: JerryEris
	INSTANT_BILLION     ( 0) { public EventSpace getEvent() { return new InstantBillion(); } };

	int weight;
	EventType(int valueWeight)
	{
		weight = valueWeight;
	}
	@Override
	public int getWeight(int playerCount)
	{
		//This gets overriden by a few events that don't belong in small or large games
		return weight;
	}
	public String getName()
	{
		return getEvent().getName();
	}
	public abstract EventSpace getEvent();
}
