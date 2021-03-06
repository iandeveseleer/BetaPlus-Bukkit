package com.mrburgerus.betaplus.world.beta;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mrburgerus.betaplus.BetaPlusPlugin;
import com.mrburgerus.betaplus.util.TerrainType;
import com.mrburgerus.betaplus.world.beta.select.AbstractBiomeSelector;
import com.mrburgerus.betaplus.world.beta.select.BetaPlusBiomeSelector;
import com.mrburgerus.betaplus.world.beta.sim.BetaPlusSimulator;
import com.mrburgerus.betaplus.world.noise.NoiseGeneratorOctavesBiome;
import com.mrburgerus.betaplus.world.noise.PerlinNoise;
import com.mrburgerus.betaplus.world.noise.VoronoiNoiseGenerator;
import net.minecraft.server.v1_15_R1.*;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import static com.mrburgerus.betaplus.world.beta.ChunkGeneratorBetaPlus.CHUNK_SIZE;

public class WorldChunkManagerBeta_OLD extends WorldChunkManager
{
	public final BetaPlusSimulator simulator;
	private AbstractBiomeSelector selector;
	private VoronoiNoiseGenerator voronoi;
	private PerlinNoise biomeNoise;
	private NoiseGeneratorOctavesBiome temperatureOctave;
	private NoiseGeneratorOctavesBiome humidityOctave;
	private NoiseGeneratorOctavesBiome noiseOctave;
	public double[] temperatures;
	public double[] humidities;
	public double[] noise;
	public final double scaleVal;
	public final double mult;
	private double offsetVoronoi = 1024;

	// BIOME CACHE
	//protected HashMap<ChunkCoordIntPair, >
	private static final Set<BiomeBase> biomeList;

	public WorldChunkManagerBeta_OLD(World world)
	{
		super(biomeList);
		//BetaPlusPlugin.LOGGER.log(Level.INFO, "Calling Provider");
		long seed = world.getSeed();
		scaleVal = (1.0D / 39.999999404);
		mult = 2;
		temperatureOctave = new NoiseGeneratorOctavesBiome(new Random(seed * 9871), 4);
		humidityOctave = new NoiseGeneratorOctavesBiome(new Random(seed * 39811), 4);
		noiseOctave = new NoiseGeneratorOctavesBiome(new Random(seed * 543321), 2);

		//this.simulator = new BetaPlusSimulator(seed, this);
		this.simulator = null;
		selector = new BetaPlusBiomeSelector();
		//selector = new BiomeSelectorBetaPlus();

		voronoi = new VoronoiNoiseGenerator(seed, (short) 0);
		biomeNoise = new PerlinNoise(seed);
	}

	public BiomeBase[] a(int x, int z, int xSize, int zSize, boolean b)
	{
		return this.generateBiomes(x, z, xSize, zSize, false);
	}

	// Add centerY
	public Set<BiomeBase> a(int centerX, int centerY, int centerZ, int sideLength)
	{
		int startX = centerX - sideLength >> 2;
		int startZ = centerZ - sideLength >> 2;
		int endX = centerX + sideLength >> 2;
		int endZ = centerZ + sideLength >> 2;
		Set<BiomeBase> set = Sets.newHashSet();
		Collections.addAll(set, this.generateBiomes(centerX, centerZ, sideLength, sideLength, true));
		return set;
	}

	public BlockPosition a(int x, int z, int range, List<BiomeBase> list, Random random)
	{
		int i = x - range >> 2;
		int j = z - range >> 2;
		int k = x + range >> 2;
		int l = z + range >> 2;
		int xSize = k - i + 1;
		int zSize = l - j + 1;
		BiomeBase[] biomeArr = this.generateBiomes(i, j, xSize, zSize, false);

		BlockPosition blockpos = null;
		int k1 = 0;

		for(int counter = 0; counter < xSize * zSize; ++counter) {
			int i2 = i + counter % xSize << 2;
			int j2 = j + counter / xSize << 2;
			if (list.contains(biomeArr[counter]))
			{
				if (blockpos == null || random.nextInt(k1 + 1) == 0) {
					blockpos = new BlockPosition(i2, 0, j2);
				}

				++k1;
			}
		}
		return blockpos;
	}

	@Override
	public boolean a(StructureGenerator<?> structureGenerator)
	{
		return this.a.computeIfAbsent(structureGenerator, (param1) -> {
			for(BiomeBase biome : biomeList) // Go through list of declared Biomes
			{
				if (biome.a(param1))
				{
					return true;
				}
			}

			return false;
		});
	}

	private BiomeBase[] generateBiomes(int startX, int startZ, int xSize, int zSize, final boolean useAverage)
	{
		// Required for legacy operations
		temperatures = temperatureOctave.generateOctaves(temperatures, (double) startX, (double) startZ, xSize, xSize, scaleVal, scaleVal, 0.25);
		humidities = humidityOctave.generateOctaves(humidities, (double) startX, (double) startZ, xSize, xSize, scaleVal * mult, scaleVal * mult, 0.3333333333333333);
		noise = noiseOctave.generateOctaves(noise, (double) startX, (double) startZ, xSize, xSize, 0.25, 0.25, 0.5882352941176471);

		double vNoise;
		double offsetX;
		double offsetZ;
		double noiseVal;
		BiomeBase[] biomeArr = new BiomeBase[xSize * zSize];
		int counter = 0;
		BiomeBase selected;
		// First, get initial terrain and simulate sand
		Pair<BlockPosition, TerrainType>[][] pairArr = this.getInitialTerrain(startX, startZ, xSize, zSize);
		// Moved up here.

		// THIS WILL NOT WORK WITHOUT ADAPTATION. IT NEEDS MORE SAMPLES OF SURROUNDING AREAS
		TerrainType[][] terrainTypes = TerrainType.processTerrain(pairArr);

		// Process
		for (int x = 0; x < xSize; ++x)
		{
			for (int z = 0; z < zSize; ++z)
			{
				// REQUIRED
				double var9 = noise[counter] * 1.1 + 0.5;
				double oneHundredth = 0.01;
				double point99 = 1.0 - oneHundredth;
				double temperatureVal = (temperatures[counter] * 0.15 + 0.7) * point99 + var9 * oneHundredth;
				oneHundredth = 0.002;
				point99 = 1.0 - oneHundredth;
				double humidityVal = (humidities[counter] * 0.15 + 0.5) * point99 + var9 * oneHundredth;
				temperatureVal = 1.0 - (1.0 - temperatureVal) * (1.0 - temperatureVal);
				temperatureVal = MathHelper.a(temperatureVal, 0.0, 1.0);
				humidityVal = MathHelper.a(humidityVal, 0.0, 1.0);
				temperatures[counter] = temperatureVal;
				humidities[counter] = humidityVal;

				// Begin New Declarations
				BlockPosition pos = new BlockPosition(x + startX, 0, z + startZ);

				// Frequency is 1, Amplitude is halved and then offset for absolute.
				//vNoise = (voronoi.noise((x + startX + offsetVoronoi) / offsetVoronoi, (z + startZ + offsetVoronoi) / offsetVoronoi, 1) * 0.5) + 0.5;
				//vNoise = voronoi.noise2((float) (startX + x / scaleVal), (float) (startZ + z / scaleVal));
				//double noiseVal = MathHelper.clamp(vNoise, 0.0, 0.99999999999999);

				// An 0.3 throwback.
				offsetX = biomeNoise.noise2((float) ((startX + x) / scaleVal), (float) ((startZ + z) / scaleVal)) * 80 + biomeNoise.noise2((startX + x) / 7, (startZ + z) / 7) * 20;
				offsetZ = biomeNoise.noise2((float) ((startX + x) / scaleVal), (float) ((startZ + z) / scaleVal)) * 80 + biomeNoise.noise2((startX + x - 1000) / 7, (startX + x) / 7) * 20;
				vNoise = (voronoi.noise((startX + x + offsetX + offsetVoronoi) / offsetVoronoi, (startZ + z - offsetZ) / offsetVoronoi, 1) * 0.5f) + 0.5f;
				noiseVal = (voronoi.noise((startX + x + offsetX + 2000) / 180, (startZ + z - offsetZ) / 180, 1) * 0.5) + 0.5;
				noiseVal = MathHelper.a(noiseVal, 0, 0.9999999);


				// If Average used, we only cared about a very top-level view, and will operate as such.
				// Typically used for Ocean Monuments
				if (useAverage)
				{
					Pair<Integer, Boolean> avg = simulator.simulateYAvg(pos);

					// OCEAN MONUMENT CATCHER
					if (avg.getFirst() < BetaPlusPlugin.seaLevel - 1)
					{
						if (avg.getFirst() < MathHelper.floor(BetaPlusPlugin.seaLevel - (BetaPlusPlugin.seaDepth / BetaPlusPlugin.oceanYScale)))
						{
							// Overwrite.
							terrainTypes[x][z] = TerrainType.deepSea;
						}
						else
						{
							terrainTypes[x][z] = TerrainType.sea;
						}
					}
				}
				selected = selector.getBiome(temperatureVal, humidityVal, noiseVal, terrainTypes[x][z]);


				biomeArr[counter] = selected;
				counter++;
			}
		}
		return biomeArr;
	}

	public Pair<BlockPosition, TerrainType>[][] getInitialTerrain(int startX, int startZ, int xSize, int zSize)
	{
		int xChunkSize = MathHelper.f(xSize / (CHUNK_SIZE * 1.0D));
		int zChunkSize = MathHelper.f(zSize / (CHUNK_SIZE * 1.0D));
		Pair<BlockPosition, TerrainType>[][] terrainPairs = new Pair[xChunkSize * CHUNK_SIZE][zChunkSize * CHUNK_SIZE];

		for (int xChunk = 0; xChunk < xChunkSize; xChunk++)
		{
			for (int zChunk = 0; zChunk < zChunkSize; zChunk++)
			{
				ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(new BlockPosition(startX + (xChunk * CHUNK_SIZE), 0, startZ + (zChunk * CHUNK_SIZE)));

				// Get simulated chunk
				int[][] yVals = simulator.simulateChunkYFull(chunkPos).getFirst();


				// Enter into initial Terrain list
				for (int x = 0; x < CHUNK_SIZE; x++)
				{
					for (int z = 0; z < CHUNK_SIZE; z++)
					{
						BlockPosition pos = new BlockPosition(x + chunkPos.d(), 0 ,z + chunkPos.e());
						if ((simulator.isBlockBeach(pos))
								&& yVals[x][z] <= BetaPlusPlugin.seaLevel + 1 && yVals[x][z] >= BetaPlusPlugin.seaLevel - 1)
						{
							terrainPairs[x + xChunk * CHUNK_SIZE][z + zChunk * CHUNK_SIZE] = Pair.of(pos, TerrainType.coastal);
						}
						else
						{
							terrainPairs[x + xChunk * CHUNK_SIZE][z + zChunk * CHUNK_SIZE] = Pair.of(pos, TerrainType.getTerrainNoIsland(yVals, x, z));
						}
					}
				}
			}
		}
		return terrainPairs;
	}

	// 1.15 makes this kinda nasty with 3D Biomes
	@Override
	public BiomeBase getBiome(int x, int y, int z)
	{
		// Is this more accurate?
		return this.generateBiomes(x, z,1,1, false)[0];
	}

	static
	{
		biomeList = ImmutableSet.of(Biomes.PLAINS, Biomes.DESERT, Biomes.FOREST, Biomes.TAIGA, Biomes.SWAMP, Biomes.SNOWY_TUNDRA, Biomes.JUNGLE, Biomes.JUNGLE_EDGE, Biomes.BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.SNOWY_TAIGA, Biomes.GIANT_TREE_TAIGA, Biomes.SAVANNA, Biomes.BADLANDS, Biomes.WOODED_BADLANDS_PLATEAU, Biomes.BADLANDS_PLATEAU, Biomes.SUNFLOWER_PLAINS, Biomes.DESERT_LAKES, Biomes.FLOWER_FOREST, Biomes.ICE_SPIKES, Biomes.MODIFIED_JUNGLE, Biomes.MODIFIED_JUNGLE_EDGE, Biomes.TALL_BIRCH_FOREST, Biomes.GIANT_SPRUCE_TAIGA, Biomes.SHATTERED_SAVANNA_PLATEAU, Biomes.ERODED_BADLANDS);
	}
}
