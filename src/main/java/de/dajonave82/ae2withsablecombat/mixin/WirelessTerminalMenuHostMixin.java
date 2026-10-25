package de.dajonave82.ae2withsablecombat.mixin;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.helpers.WirelessTerminalMenuHost;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//calculating the signal distance

@Mixin(value = WirelessTerminalMenuHost.class, remap = false)
public abstract class WirelessTerminalMenuHostMixin {

    @Inject(
            method = "getAccessPointSignal",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void sableae2compat$projectSubLevelAccessPoint(
            IWirelessAccessPoint wirelessAccessPoint,
            CallbackInfoReturnable<WirelessTerminalMenuHost.AccessPointSignal> cir) {

        final Player player = ((ItemMenuHost<?>) (Object) this).getPlayer();
        final var location = wirelessAccessPoint.getLocation();
        final Level accessPointLevel = location.getLevel();

        // Keep vanilla hmmmm
        if (accessPointLevel != player.level()) {
            return;
        }

        final SubLevelContainer container = SubLevelContainer.getContainer(accessPointLevel);
        if (container == null) {
            return;
        }

        final BlockPos accessPointPos = location.getPos();
        if (!container.inBounds(accessPointPos)) {
            return;
        }

        final LevelPlot plot = container.getPlot(new ChunkPos(accessPointPos));
        if (plot == null) {
            return;
        }

        final SubLevel subLevel = plot.getSubLevel();
        if (subLevel == null || subLevel.isRemoved()) {
            return;
        }

        final Vector3d projectedPos = new Vector3d(
                accessPointPos.getX(),
                accessPointPos.getY(),
                accessPointPos.getZ());
        subLevel.logicalPose().transformPosition(projectedPos);

        final double dx = projectedPos.x() - player.getX();
        final double dy = projectedPos.y() - player.getY();
        final double dz = projectedPos.z() - player.getZ();
        final double distanceSquared = dx * dx + dy * dy + dz * dz;

        final double range = wirelessAccessPoint.getRange();
        final double rangeSquared = range * range;

        if (distanceSquared < rangeSquared && wirelessAccessPoint.isActive()) {
            cir.setReturnValue(new WirelessTerminalMenuHost.AccessPointSignal(
                    distanceSquared,
                    rangeSquared - distanceSquared));
        } else {
            cir.setReturnValue(new WirelessTerminalMenuHost.AccessPointSignal(
                    Double.MAX_VALUE,
                    Double.MIN_VALUE));
        }
    }
}
