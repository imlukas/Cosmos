package me.illusion.cosmos.template.impl;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import java.util.concurrent.CompletableFuture;
import me.illusion.cosmos.event.CosmosPasteAreaEvent;
import me.illusion.cosmos.serialization.CosmosSerializer;
import me.illusion.cosmos.template.PastedArea;
import me.illusion.cosmos.template.TemplatedArea;
import me.illusion.cosmos.utilities.geometry.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 * A Schematic Templated Area is a templated area which is based on a WorldEdit clipboard.
 * <p>
 *
 * @author Illusion
 */
public class SchematicTemplatedArea implements TemplatedArea {

    private final CosmosSerializer serializer;
    private final Clipboard clipboard;

    public SchematicTemplatedArea(CosmosSerializer serializer, Clipboard clipboard) {
        this.serializer = serializer;
        this.clipboard = clipboard;
    }

    @Override
    public CompletableFuture<PastedArea> paste(Location location) {
        System.out.println("Pasting at " + location);

        World worldEditWorld = new BukkitWorld(location.getWorld());

        try (EditSession session = WorldEdit.getInstance().newEditSession(worldEditWorld)) {
            Operation operation = new ClipboardHolder(clipboard)
                .createPaste(session)
                .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                .ignoreAirBlocks(false)
                .build();

            Operations.complete(operation);
        } catch (WorldEditException e) {
            return CompletableFuture.failedFuture(e);
        }

        return CompletableFuture.completedFuture(new SchematicPastedArea(location));
    }

    @Override
    public Cuboid getDimensions() {
        double minX = clipboard.getMinimumPoint().getX();
        double minY = clipboard.getMinimumPoint().getY();
        double minZ = clipboard.getMinimumPoint().getZ();

        double maxX = clipboard.getMaximumPoint().getX();
        double maxY = clipboard.getMaximumPoint().getY();
        double maxZ = clipboard.getMaximumPoint().getZ();

        return new Cuboid(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public CosmosSerializer getSerializer() {
        return serializer;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    /**
     * A schematic pasted area is an area that has already been pasted, and can be unloaded.
     */
    private class SchematicPastedArea implements PastedArea {

        private final Location pasteLocation;

        public SchematicPastedArea(Location pasteLocation) {
            this.pasteLocation = pasteLocation;
            Bukkit.getPluginManager().callEvent(new CosmosPasteAreaEvent(this));
        }

        @Override
        public CompletableFuture<Void> unload() {
            System.out.println("Unloading at " + pasteLocation);
            
            World worldEditWorld = new BukkitWorld(pasteLocation.getWorld());

            Cuboid dimensions = getDimensions();
            CuboidRegion cuboidRegion = new CuboidRegion(
                worldEditWorld,
                BlockVector3.at(pasteLocation.getX() - dimensions.getWidth() / 2, pasteLocation.getY() - dimensions.getHeight() / 2, pasteLocation.getZ() - dimensions.getLength() / 2),
                BlockVector3.at(pasteLocation.getX() + dimensions.getWidth() / 2, pasteLocation.getY() + dimensions.getHeight() / 2, pasteLocation.getZ() + dimensions.getLength() / 2)
            );



            BlockState air = BukkitAdapter.adapt(Material.AIR.createBlockData());

            try (EditSession session = WorldEdit.getInstance().newEditSession(worldEditWorld)) {
                session.setBlocks(cuboidRegion, air);
                session.commit();
            } catch (MaxChangedBlocksException e) {
                throw new RuntimeException(e);
            }

            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Location getPasteLocation() {
            return pasteLocation;
        }

        @Override
        public CompletableFuture<PastedArea> paste(Location location) {
            return SchematicTemplatedArea.this.paste(location);
        }

        @Override
        public Cuboid getDimensions() {
            return SchematicTemplatedArea.this.getDimensions();
        }

        @Override
        public CosmosSerializer getSerializer() {
            return SchematicTemplatedArea.this.getSerializer();
        }
    }
}
