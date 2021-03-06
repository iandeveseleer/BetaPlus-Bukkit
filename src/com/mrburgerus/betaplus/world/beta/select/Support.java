package com.mrburgerus.betaplus.world.beta.select;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.v1_15_R1.BiomeBase;
import net.minecraft.server.v1_15_R1.Biomes;

import java.util.List;
import java.util.Optional;

public class Support
{
	// FIELDS //

	// Values are within [-0.5, 2.0]
	// This has to be big enough to select a few biomes
	private static final double INIT_TEMP = 0.3;
	private static final double INIT_HUMID = 0.15;
	// Smaller Values increase processing time.
	private static final double TEMP_INCREMENT = 0.2;
	private static final double HUMID_INCREMENT = 0.1;


	// List of Land Biomes and corresponding "hills" biomes, if they exist
	public static List<Pair<BiomeBase, Optional<BiomeBase>>> landBiomes;
	// Pair of Ocean Biomes, both normal and deep
	public static List<Pair<BiomeBase, BiomeBase>> oceanBiomes;
	// Mountain Biomes, these are specialty.
	public static List<BiomeBase> mountainBiomes;
	// List of Beach & Coastal Biomes, could come in handy.
	public static List<BiomeBase> coastBiomes;
	// List of Island Biomes
	public static List<BiomeBase> islandBiomes;

	// METHODS //

	// Get a standard BiomeBase from parameters
	public static BiomeBase getBiomeFromParams(double temperature, double humidity, double selectNoise, List<BiomeBase> candidates)
	{
		double tempThreshold = INIT_TEMP;
		double humidThreshold = INIT_HUMID;
		List<BiomeBase> validCandidates = Lists.newArrayList();
		// Get closest match, expand search if no found
		while (validCandidates.size() < 1)
		{
			for (BiomeBase b : candidates)
			{
				// Get Differences, if within range add to select.
				if (Math.abs(b.getTemperature() - temperature) <= tempThreshold && Math.abs(b.getHumidity() - humidity) <= humidThreshold)
				{
					validCandidates.add(b);
				}
			}
			tempThreshold += TEMP_INCREMENT;
			humidThreshold += HUMID_INCREMENT;
		}
		int randSelect = (int) (validCandidates.size() * selectNoise);
		return validCandidates.get(randSelect);
	}

	// Get a Land BiomeBase and it's hill variant
	public static Pair<BiomeBase, Optional<BiomeBase>> getLandBiomeFromParams(double temperature, double humidity, double selectNoise, List<Pair<BiomeBase, Optional<BiomeBase>>> candidates)
	{
		double tempThreshold = INIT_TEMP;
		double humidThreshold = INIT_HUMID;
		List<Pair<BiomeBase, Optional<BiomeBase>>> validCandidates = Lists.newArrayList();
		// Get closest match
		while (validCandidates.size() < 1)
		{
			for (Pair<BiomeBase, Optional<BiomeBase>> b : candidates)
			{
				// Get Differences, if within range add to select.
				if (Math.abs(b.getFirst().getTemperature() - temperature) <= tempThreshold && Math.abs(b.getFirst().getHumidity() - humidity) <= humidThreshold)
				{
					validCandidates.add(b);
				}
			}
			tempThreshold += TEMP_INCREMENT;
			humidThreshold += HUMID_INCREMENT;
		}
		int randSelect = (int) (validCandidates.size() * selectNoise);
		return validCandidates.get(randSelect);
	}

	// Get an ocean BiomeBase
	// TODO: FIX SINCE ALL OCEANS ARE THE SAME TEMP & DOWNFALL
	public static Pair<BiomeBase, BiomeBase> getOceanBiomePair(double temperature, double humidity, double selectNoise, List<Pair<BiomeBase, BiomeBase>> candidates)
	{
		double tempThreshold = INIT_TEMP;
		double humidThreshold = INIT_HUMID;
		List<Pair<BiomeBase, BiomeBase>> validCandidates = Lists.newArrayList();
		while (validCandidates.size() < 1)
		{
			for (Pair<BiomeBase, BiomeBase> b : candidates)
			{
				// Get Differences, if within range add to select.
				if (Math.abs(b.getFirst().getTemperature() - temperature) <= tempThreshold && Math.abs(b.getFirst().getHumidity() - humidity) <= humidThreshold)
				{
					validCandidates.add(b);
				}
			}
			tempThreshold += TEMP_INCREMENT;
			humidThreshold += HUMID_INCREMENT;
		}
		int randSelect = (int) (validCandidates.size() * selectNoise);
		return validCandidates.get(randSelect);
	}


	// DECLARATIONS //
	static
	{
		// Land Biomes, This will contain ALL Land Biomes and hills.
		landBiomes = Lists.newArrayList(
				Pair.of(Biomes.PLAINS, Optional.empty()),
				Pair.of(Biomes.DESERT, Optional.of(Biomes.DESERT_HILLS)),
				Pair.of(Biomes.FOREST, Optional.of(Biomes.FLOWER_FOREST)),
				Pair.of(Biomes.SNOWY_TAIGA, Optional.of(Biomes.SNOWY_TAIGA_HILLS)),
				Pair.of(Biomes.SWAMP, Optional.of(Biomes.SWAMP_HILLS)),
				Pair.of(Biomes.JUNGLE, Optional.of(Biomes.JUNGLE_HILLS)),
				Pair.of(Biomes.BIRCH_FOREST, Optional.of(Biomes.BIRCH_FOREST_HILLS)),
				Pair.of(Biomes.DARK_FOREST, Optional.of(Biomes.DARK_FOREST_HILLS)),
				Pair.of(Biomes.SAVANNA, Optional.of(Biomes.SAVANNA_PLATEAU)), // Test the shattered.
				Pair.of(Biomes.SUNFLOWER_PLAINS, Optional.empty()),
				Pair.of(Biomes.TALL_BIRCH_FOREST, Optional.of(Biomes.TALL_BIRCH_HILLS)),
				Pair.of(Biomes.GIANT_SPRUCE_TAIGA, Optional.of(Biomes.GIANT_SPRUCE_TAIGA_HILLS)),
				Pair.of(Biomes.BAMBOO_JUNGLE, Optional.of(Biomes.BAMBOO_JUNGLE_HILLS))
		);

		oceanBiomes = Lists.newArrayList(
				Pair.of(Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN),
				Pair.of(Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN),
				Pair.of(Biomes.OCEAN, Biomes.DEEP_OCEAN),
				Pair.of(Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN),
				Pair.of(Biomes.WARM_OCEAN, Biomes.DEEP_WARM_OCEAN)
		);

		mountainBiomes = Lists.newArrayList(
				Biomes.SNOWY_MOUNTAINS,
				//Biomes.MOUNTAINS,
				Biomes.WOODED_MOUNTAINS,
				Biomes.SHATTERED_SAVANNA_PLATEAU // Testing
		);

		coastBiomes = Lists.newArrayList(
				Biomes.BEACH,
				Biomes.SNOWY_BEACH
		);

		islandBiomes = Lists.newArrayList(
				Biomes.MUSHROOM_FIELDS
		);
	}
}
