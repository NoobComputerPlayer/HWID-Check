/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  org.lwjgl.input.Mouse
 */
package rip.autumn.module.impl.movement;

import me.zane.basicbus.api.annotations.Listener;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S45PacketTitle;
import org.lwjgl.input.Mouse;
import rip.autumn.annotations.Label;
import rip.autumn.events.packet.ReceivePacketEvent;
import rip.autumn.events.packet.SendPacketEvent;
import rip.autumn.events.player.MotionUpdateEvent;
import rip.autumn.events.player.MoveEvent;
import rip.autumn.module.Module;
import rip.autumn.module.ModuleCategory;
import rip.autumn.module.annotations.Bind;
import rip.autumn.module.annotations.Category;
import rip.autumn.module.impl.movement.FlightMod.Mode;
import rip.autumn.module.option.impl.EnumOption;
import rip.autumn.utils.Logger;
import rip.autumn.utils.MovementUtils;
import rip.autumn.utils.PlayerUtils;
import rip.autumn.utils.Stopwatch;
import rip.autumn.utils.pathfinding.CustomVec3;
import rip.autumn.utils.pathfinding.PathfindingUtils;

@Label(value = "AutoZeroZero")
@Category(value = ModuleCategory.MOVEMENT)
public final class AutuZeroZeroMod extends Module {
	private final EnumOption<Mode> mode = new EnumOption<Mode>("Mode", Mode.HYPIXEL);
	private final Stopwatch timer = new Stopwatch();
	private CustomVec3 target;
	private int stage;
	boolean tp;

	public AutuZeroZeroMod() {
		this.addOptions(mode);
	}

	@Override
	public void onEnabled() {
		if (mc.thePlayer == null)
			this.toggle();
		this.stage = 0;
		tp = false;
		if (this.mode.getValue() == Mode.HYPIXEL) {
			mc.getNetHandler().addToSendQueueSilent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
					mc.thePlayer.posY + 0.17, mc.thePlayer.posZ, true));
			mc.getNetHandler().addToSendQueueSilent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
					mc.thePlayer.posY + 0.06, mc.thePlayer.posZ, true));
			mc.thePlayer.stepHeight = 0.0f;
			mc.thePlayer.motionX = 0.0;
			mc.thePlayer.motionZ = 0.0;
		}
	}

	@Override
	public void onDisabled() {
		EntityPlayerSP player = HypixelDisablerMod.mc.thePlayer;
		HypixelDisablerMod.mc.timer.timerSpeed = 1.0f;
		player.stepHeight = 0.625f;
		player.motionX = 0.0;
		player.motionZ = 0.0;
	}

	@Listener(value = SendPacketEvent.class)
	public final void onSendPacket(SendPacketEvent event) {
		if (this.stage == 1 && !this.timer.elapsed(6000L)
				|| this.mode.getValue() == Mode.HYPIXEL && !tp && event.getPacket() instanceof C03PacketPlayer) {
			event.setCancelled();
		}
	}

	@Listener(value = ReceivePacketEvent.class)
	public final void onReceivePacket(ReceivePacketEvent event) {
		if (this.mode.getValue() == Mode.HYPIXEL && event.getPacket() instanceof S08PacketPlayerPosLook) {
			if (!this.tp) {
				this.tp = true;
			}
		}
	}

	@Listener(value = MotionUpdateEvent.class)
	public final void onMotionUpdate(MotionUpdateEvent event) {
		if (mode.getValue() == Mode.HYPIXEL) {
			if (tp) {
				this.setEnabled(false);
				mc.getNetHandler().addToSendQueueSilent(new C0CPacketInput(0.0f, 0.0f, true, true));
				double lastY = mc.thePlayer.posY, downY = 0;
				for (CustomVec3 vec3 : PathfindingUtils.computePath(
						new CustomVec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ),
						new CustomVec3(0, 128, 0), 100)) {
					if (vec3.getY() < lastY) {
						downY += (lastY - vec3.getY());
					}
					if (downY > 2.5) {
						downY = 0;
						mc.getNetHandler().addToSendQueueSilent(new C03PacketPlayer.C04PacketPlayerPosition(vec3.getX(),
								vec3.getY(), vec3.getZ(), true));
					} else {
						mc.getNetHandler().addToSendQueueSilent(new C03PacketPlayer.C04PacketPlayerPosition(vec3.getX(),
								vec3.getY(), vec3.getZ(), false));
					}
					lastY = vec3.getY();
				}
				mc.thePlayer.setPosition(0, 128, 0);
				Logger.log("Teleported");
			}
		}
	}

	@Listener(value = MoveEvent.class)
	public final void onMove(MoveEvent event) {
		if ((this.stage == 1 & !this.timer.elapsed(6000L)) || (mode.getValue() == Mode.HYPIXEL)) {
			MovementUtils.setSpeed(event, 0.0);
			mc.thePlayer.motionY = 0.0;
			event.y = 0.0;
		}
	}

	private void killPlayer() {
		NetHandlerPlayClient netHandler = mc.getNetHandler();
		for (int i = 0; i < 20; ++i) {
			double offset = 0.0601f;
			int j = 0;
			while ((double) j < (double) PlayerUtils.getMaxFallDist() / (double) 0.0601f + 1.0) {
				netHandler.addToSendQueueSilent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
						mc.thePlayer.posY + (double) 0.0601f, mc.thePlayer.posZ, false));
				netHandler.addToSendQueueSilent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
						mc.thePlayer.posY + (double) 5.0E-4f, mc.thePlayer.posZ, false));
				++j;
			}
		}
		netHandler.addToSendQueueSilent(new C03PacketPlayer(true));
	}

	private static enum Mode {
		HYPIXEL;
	}
}
