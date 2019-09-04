package com.mrburgerus.betaplus.world.beta.beta_api;

import com.mrburgerus.betaplus.BetaPlusPlugin;
import net.minecraft.server.v1_14_R1.*;
import nl.rutgerkok.worldgeneratorapi.*;
import nl.rutgerkok.worldgeneratorapi.WorldGenerator;
import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;
import nl.rutgerkok.worldgeneratorapi.event.WorldGeneratorInitEvent;
import nl.rutgerkok.worldgeneratorapi.internal.*;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.InjectedChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.NoiseToTerrainGenerator;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class WorldGeneratorBetaPlus implements WorldGenerator
{
	@Nullable
	private InjectedChunkGenerator injected;
	private final World world;
	private final WorldRef worldRef;
	private final BetaPlusPlugin p;


	public WorldGeneratorBetaPlus(World world)
	{
		p = JavaPlugin.getPlugin(BetaPlusPlugin.class);
		this.world = (World) Objects.requireNonNull(world, "world");
		this.worldRef = WorldRef.of(world);
	}

	@Override
	public BaseChunkGenerator getBaseChunkGenerator() throws UnsupportedOperationException
	{
		return this.getBaseTerrainGenerator();
	}

	@Override
	public BaseTerrainGenerator getBaseTerrainGenerator() throws UnsupportedOperationException
	{
		return BetaPlusTerrainGenerator.fromMinecraft(this.world);
	}

	@Override
	public BiomeGenerator getBiomeGenerator()
	{
		InjectedChunkGenerator injected = this.injected;
		if (injected != null) {
			return injected.getBiomeGenerator();
		} else {
			WorldChunkManager worldChunkManager = this.getWorldHandle().getChunkProvider().getChunkGenerator().getWorldChunkManager();
			//return new BiomeGeneratorImpl(worldChunkManager);
			return new BetaPlusBiomeGenerator(this.getWorld().getSeed());
		}
	}

	private WorldServer getWorldHandle()
	{
		return ((CraftWorld)this.world).getHandle();
	}

	@Override
	public World getWorld()
	{
		return this.world;
	}

	@Override
	public WorldDecorator getWorldDecorator() throws UnsupportedOperationException
	{
		InjectedChunkGenerator injected = this.injected;
		if (injected == null) {
			this.replaceChunkGenerator(BetaPlusTerrainGenerator.fromMinecraft(this.world));
			return this.injected.worldDecorator;
		} else {
			return injected.worldDecorator;
		}
	}

	@Override
	public WorldRef getWorldRef()
	{
		return this.worldRef;
	}

	@Override
	public void setBaseChunkGenerator(final BaseChunkGenerator base) {
		this.setBaseTerrainGenerator(new BaseTerrainGenerator() {
			public int getHeight(int x, int z, HeightType type) {
				return 65;
			}

			public void setBlocksInChunk(GeneratingChunk chunk) {
				base.setBlocksInChunk(chunk);
			}
		});
	}

	@Override
	public BaseTerrainGenerator setBaseNoiseGenerator(BaseNoiseGenerator base) {
		WorldChunkManager worldChunkManager = this.getWorldHandle().getChunkProvider().getChunkGenerator().getWorldChunkManager();
		BiomeGenerator biomeGenerator = this.getBiomeGenerator();
		BaseTerrainGenerator generator = new NoiseToTerrainGenerator(this.getWorldHandle(), worldChunkManager, biomeGenerator, base);
		this.setBaseTerrainGenerator(generator);
		return generator;
	}

	@Override
	public void setBaseTerrainGenerator(BaseTerrainGenerator base) {
		Objects.requireNonNull(base, "base");
		InjectedChunkGenerator injected = this.injected;
		if (injected == null) {
			this.replaceChunkGenerator(base);
		} else {
			injected.setBaseChunkGenerator(base);
		}

	}

	// Copied
	static BaseTerrainGenerator fromMinecraft(World world)
	{
		WorldServer worldServer = ((CraftWorld)world).getHandle();
		ChunkGenerator<?> chunkGenerator = worldServer.getChunkProvider().getChunkGenerator();
		if (chunkGenerator instanceof InjectedChunkGenerator) {
			return ((InjectedChunkGenerator)chunkGenerator).getBaseTerrainGenerator();
		} else if (BetaPlusTerrainGenerator.isSupported(chunkGenerator)) {
			return new BetaPlusTerrainGenerator(worldServer, chunkGenerator);
		} else {
			throw new UnsupportedOperationException("Cannot extract base chunk generator from " + chunkGenerator.getClass() + ". \nYou can only customize worlds where the base terrain is generated by Minecraft or using the WorldGeneratorApi methods. If you are using the WorldGeneratorApi methods, make sure that a BaseChunkGenerator was set before other aspects of terrain generation were modified.");
		}
	}

	private void replaceChunkGenerator(BaseTerrainGenerator base) {
		InjectedChunkGenerator injected = new InjectedChunkGenerator(this.getWorldHandle(), base);
		ChunkProviderServer chunkProvider = this.getWorldHandle().getChunkProvider();

		try {
			Field chunkGeneratorField = ReflectionUtil.getFieldOfType(chunkProvider, ChunkGenerator.class);
			chunkGeneratorField.set(chunkProvider, injected);
			chunkGeneratorField = ReflectionUtil.getFieldOfType(chunkProvider.playerChunkMap, ChunkGenerator.class);
			chunkGeneratorField.set(chunkProvider.playerChunkMap, injected);

			try {
				Field chunkTaskSchedulerField = ReflectionUtil.getFieldOfType(chunkProvider, this.nmsClass("ChunkTaskScheduler"));
				Object scheduler = chunkTaskSchedulerField.get(chunkProvider);
				chunkGeneratorField = ReflectionUtil.getFieldOfType(scheduler, ChunkGenerator.class);
				chunkGeneratorField.set(scheduler, injected);
			} catch (ClassNotFoundException var7) {
				;
			}

			this.injected = injected;
			this.getWorldHandle().generator = null;
		} catch (ReflectiveOperationException var8) {
			throw new RuntimeException("Failed to inject world generator", var8);
		}
	}

	private Class<?> nmsClass(String simpleName) throws ClassNotFoundException {
		Class<?> exampleNmsClass = ChunkGenerator.class;
		String name = exampleNmsClass.getName().replace(exampleNmsClass.getSimpleName(), simpleName);
		return Class.forName(name);
	}

	public static BetaPlusPlugin getInstance(Plugin plugin, int major, int minor) {
		JavaPlugin ourselves = JavaPlugin.getProvidingPlugin(WorldGeneratorBetaPlus.class);
		BetaPlusPlugin api = (BetaPlusPlugin)ourselves;
		if (!api.getApiVersion().isCompatibleWith(major, minor)) {
			ourselves.getLogger().warning(plugin.getName() + " expects " + ourselves.getName() + " v" + major + "." + minor + ". However, this is version v" + api.getApiVersion() + ", which is not compatible. Things may break horribly! You have been warned.");
		}

		return api;
	}

}