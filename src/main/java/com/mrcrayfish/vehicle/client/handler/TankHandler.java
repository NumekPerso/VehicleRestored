package com.mrcrayfish.vehicle.client.handler;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Locale;
import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.entity.TankBulletEntity;
import com.mrcrayfish.vehicle.entity.TankShellEntity;
import com.mrcrayfish.vehicle.entity.vehicle.TankAmmo;
import com.mrcrayfish.vehicle.entity.vehicle.TankArmorFace;
import com.mrcrayfish.vehicle.entity.vehicle.TankEntity;
import com.mrcrayfish.vehicle.item.ArmorPlateItem;
import com.mrcrayfish.vehicle.entity.vehicle.TankWeapon;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import com.mrcrayfish.vehicle.network.PacketHandler;
import com.mrcrayfish.vehicle.network.message.MessageFireTank;
import com.mrcrayfish.vehicle.network.message.MessageTankAim;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;

/**
 * Everything the crew of a tank sees and does on the client:
 * <ul>
 * <li>Aiming: the turret and the gun point at what is under the crosshair (block or entity); the
 * elevation includes the drop of the projectile of the selected weapon. The aim is sent to the
 * server (MessageTankAim).</li>
 * <li>Weapons: keys 1 to 5 choose the weapon (5: all at once) in a weapon bar that replaces the hotbar; the attack
 * key fires (the machine guns and the light shells keep firing while it is held).</li>
 * <li>Zoom: the mouse wheel zooms progressively (x1 to x10, 10 % per notch), Alt + wheel by
 * 0.01, Ctrl + wheel by 0.1; the mouse turns slower when zoomed in (see MouseHandlerMixin).</li>
 * <li>Sight: a custom crosshair at the centre and a marker where the projectile will land with the
 * gun as it points now; the marker joins the crosshair when the turret has caught up.</li>
 * <li>The crew is inside: riders and the first person hand are not drawn, no hotbar.</li>
 * </ul>
 */
public class TankHandler
{
	private static final ResourceLocation RETICLE = new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/reticle.png");
	private static final ResourceLocation GUN_MARKER = new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/gun_marker.png");
	private static final ResourceLocation SLOT = new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/slot.png");
	private static final ResourceLocation SLOT_SELECTED = new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/slot_selected.png");
	private static final ResourceLocation[] WEAPON_ICONS = {
		new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/weapon_cannon.png"),
		new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/weapon_coax.png"),
		new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/weapon_hmg.png"),
		new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/weapon_light_shells.png"),
		new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/weapon_all.png")
	};
	/** Icons and colours of the bars of the tank's state: health, armour, fuel, speed */
	private static final ResourceLocation[] STATUS_ICONS = {
		new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/hud_health.png"),
		new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/hud_armor.png"),
		new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/hud_fuel.png"),
		new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/hud_speed.png")
	};
	private static final int[] STATUS_COLORS = {0xD94141, 0x8FB2D6, 0xE5A53A, 0x7CCBF2};
	/**
	 * The bars of the state: leaning to the right (each row of pixels one further right than the row below), outlined,
	 * made of segments leaning like them, SEGMENT wide with a gap of 1; rows: outline, gap, the segments, gap, outline
	 */
	private static final int BAR_SEGMENTS = 16, SEGMENT = 4, BAR_HEIGHT = 9;
	private static final int BAR_WIDTH = 2 + BAR_SEGMENTS * (SEGMENT + 1) - 1 + 2;
	private static final int BAR_OUTLINE = 0xE6101010, BAR_TRACK = 0x55000000;
	/**
	 * The armour bar is cut in one part per plate (TankArmorFace order: front, left, top, right, rear), ARMOR_PART
	 * wide and 2 apart, leaning like the bar, with the same ends as its 16 segments
	 */
	private static final int ARMOR_PART = 14, ARMOR_PART_STEP = 16;
	/** Height the weapon bar takes above the vanilla hotbar height (rounds counts and weapon name) */
	private static final int WEAPON_BAR_EXTRA_HEIGHT = 14;

	private static final float MIN_ZOOM = 1F;
	private static final float MAX_ZOOM = 10F;
	private static final double AIM_RANGE = 400;

	private static float zoomTarget = 1F;
	private static float zoom = 1F;
	private static long lastZoomUpdate;
	private static double lastFov = 70;
	private static TankWeapon weapon = TankWeapon.CANNON;
	/** Point under the crosshair at the last aim (the light shells are aimed at it) */
	@Nullable
	private static Vec3 lastTarget;

	/** Ticks before each weapon can be fired again (client side rate of fire) */
	private final int[] fireCooldowns = new int[TankWeapon.values().length];
	/** Ticks before the next click of an empty weapon */
	private int dryFireCooldown;
	private boolean attackWasDown;
	private float sentYaw = Float.NaN;
	private float sentPitch = Float.NaN;
	private int sentAge;

	/** The tank the local player drives, or null */
	@Nullable
	private static TankEntity getDrivenTank()
	{
		Player player = Minecraft.getInstance().player;
		if(player != null && player.getVehicle() instanceof TankEntity tank && tank.getControllingPassenger() == player)
		{
			return tank;
		}
		return null;
	}

	private static boolean isInTank()
	{
		Player player = Minecraft.getInstance().player;
		return player != null && player.getVehicle() instanceof TankEntity;
	}

	/** Mouse turn multiplier: the view turns slower when zoomed in (MouseHandlerMixin) */
	public static double getSensitivityScale()
	{
		return zoom > 1.001F && getDrivenTank() != null ? 1.0 / zoom : 1.0;
	}

	/* ------------------------------------------------------------------ input */

	@SubscribeEvent
	public void onInteraction(InputEvent.InteractionKeyMappingTriggered event)
	{
		// The attack key fires (see onClientTick) instead of hitting
		if(event.isAttack() && getDrivenTank() != null)
		{
			event.setCanceled(true);
			event.setSwingHand(false);
		}
	}

	@SubscribeEvent
	public void onScroll(InputEvent.MouseScrollingEvent event)
	{
		if(getDrivenTank() == null || Minecraft.getInstance().screen != null)
			return;
		double delta = event.getScrollDelta();
		if(delta == 0)
			return;
		int step = delta > 0 ? 1 : -1;
		// Alt for the fine step (Shift would get the player out of the tank)
		if(Screen.hasAltDown())
		{
			zoomTarget = Math.round((zoomTarget + 0.01F * step) * 100F) / 100F;
		}
		else if(Screen.hasControlDown())
		{
			zoomTarget = Math.round((zoomTarget + 0.1F * step) * 100F) / 100F;
		}
		else
		{
			zoomTarget = Math.round(zoomTarget * (float) Math.pow(1.1, step) * 100F) / 100F;
		}
		zoomTarget = Mth.clamp(zoomTarget, MIN_ZOOM, MAX_ZOOM);
		event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onComputeFov(ViewportEvent.ComputeFov event)
	{
		if(!event.usedConfiguredFov())
			return;
		// the zoom eases towards its target
		long now = System.nanoTime();
		float seconds = lastZoomUpdate == 0 ? 0F : Math.min(0.1F, (now - lastZoomUpdate) / 1.0E9F);
		lastZoomUpdate = now;
		zoom += (zoomTarget - zoom) * Math.min(1F, seconds * 14F);
		if(Math.abs(zoomTarget - zoom) < 0.0005F)
		{
			zoom = zoomTarget;
		}
		if(zoom > 1.0005F && getDrivenTank() != null)
		{
			event.setFOV(event.getFOV() / zoom);
		}
		lastFov = event.getFOV();
	}

	/** The view kicks up a little when the gun fires */
	@SubscribeEvent
	public void onCameraAngles(ViewportEvent.ComputeCameraAngles event)
	{
		TankEntity tank = getDrivenTank();
		if(tank == null)
			return;
		float recoil = Mth.lerp((float) event.getPartialTick(), tank.prevRecoil, tank.recoil);
		if(recoil > 0.001F)
		{
			event.setPitch(event.getPitch() - recoil * 2.2F);
		}
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event)
	{
		Minecraft mc = Minecraft.getInstance();
		TankEntity tank = getDrivenTank();
		if(event.phase == TickEvent.Phase.START)
		{
			// Keys 1 to 5 choose the weapon (5: all at once); the other number keys do nothing in the tank
			if(tank != null)
			{
				for(int i = 0; i < mc.options.keyHotbarSlots.length; i++)
				{
					while(mc.options.keyHotbarSlots[i].consumeClick())
					{
						if(i < TankWeapon.values().length)
						{
							weapon = TankWeapon.values()[i];
						}
					}
				}
			}
			return;
		}

		if(tank == null)
		{
			zoomTarget = 1F;
			this.attackWasDown = false;
			this.sentYaw = Float.NaN;
			return;
		}

		this.updateAim(mc, tank);

		// Fire: one shot per click (gun), or while the key is held (machine guns, light shells). With
		// all the weapons: the click fires the gun, holding the key fires everything else.
		for(int i = 0; i < this.fireCooldowns.length; i++)
		{
			if(this.fireCooldowns[i] > 0)
			{
				this.fireCooldowns[i]--;
			}
		}
		if(this.dryFireCooldown > 0)
		{
			this.dryFireCooldown--;
		}
		boolean down = mc.screen == null && mc.options.keyAttack.isDown();
		boolean click = down && !this.attackWasDown;
		if(weapon == TankWeapon.ALL)
		{
			if(click) this.fire(tank, TankWeapon.CANNON);
			if(down)
			{
				this.fire(tank, TankWeapon.COAX);
				this.fire(tank, TankWeapon.HEAVY_MG);
				this.fire(tank, TankWeapon.LIGHT_SHELLS);
			}
		}
		else if(click || (down && weapon.isAutomatic()))
		{
			this.fire(tank, weapon);
		}
		this.attackWasDown = down;
	}

	/**
	 * Sends a shot of one weapon if its rate of fire allows it. Without ammunition (and not in
	 * creative mode) the weapon only clicks.
	 */
	private void fire(TankEntity tank, TankWeapon weapon)
	{
		if(this.fireCooldowns[weapon.ordinal()] > 0)
			return;
		Minecraft mc = Minecraft.getInstance();
		TankAmmo ammo = TankAmmo.of(weapon);
		if(ammo != null && mc.player != null && !mc.player.isCreative() && tank.getAmmoCount(ammo) <= 0)
		{
			if(this.dryFireCooldown <= 0)
			{
				mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.5F, 0.5F));
				this.dryFireCooldown = 8;
			}
			return;
		}
		PacketHandler.instance.sendToServer(new MessageFireTank(weapon));
		this.fireCooldowns[weapon.ordinal()] = weapon.getInterval();
	}

	/* ------------------------------------------------------------------ aiming */

	/** Projectile of a weapon: speed (blocks per tick) and drop per tick */
	private static double speed(TankWeapon weapon)
	{
		return switch(weapon)
		{
			case COAX -> TankEntity.COAX_SPEED;
			case HEAVY_MG -> TankEntity.HEAVY_MG_SPEED;
			case LIGHT_SHELLS -> TankEntity.LIGHT_SHELL_SPEED;
			default -> TankEntity.SHELL_SPEED;
		};
	}

	private static double gravity(TankWeapon weapon)
	{
		return weapon == TankWeapon.COAX || weapon == TankWeapon.HEAVY_MG ? TankBulletEntity.GRAVITY : TankShellEntity.GRAVITY;
	}

	/**
	 * Points the turret and the gun at what is under the crosshair: the turret turns towards the
	 * target and the gun gets the elevation that drops the projectile on it.
	 */
	private void updateAim(Minecraft mc, TankEntity tank)
	{
		Camera camera = mc.gameRenderer.getMainCamera();
		if(!camera.isInitialized() || mc.level == null || mc.player == null)
			return;
		Vec3 target = findTarget(mc.level, mc.player, tank, camera);

		Vec3 ring = tank.getTurretPoint(Vec3.ZERO, 1F);
		double dx = target.x - ring.x, dz = target.z - ring.z;
		float yaw = tank.getAimYaw();
		if(dx * dx + dz * dz > 1.0)
		{
			float worldYaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
			yaw = Mth.wrapDegrees(worldYaw - tank.getYRot());
		}
		// the gun gets the elevation of its own shell, or of the machine gun beside it (the light
		// shells leave from the launch tubes: the gun stays ready for its shell)
		TankWeapon gunWeapon = weapon == TankWeapon.LIGHT_SHELLS || weapon == TankWeapon.ALL ? TankWeapon.CANNON : weapon;
		float aimYaw = yaw;
		float pitch = TankEntity.solveElevation(p -> tank.getGunPoint(new Vec3(0, 0, TankEntity.GUN_LENGTH), tank.getYRot(), aimYaw, p, tank.position()),
				target, speed(gunWeapon), gravity(gunWeapon), TankEntity.MIN_GUN_PITCH, TankEntity.MAX_GUN_PITCH);

		tank.setAim(yaw, pitch, target);
		lastTarget = target;
		this.sentAge++;
		if(Float.isNaN(this.sentYaw) || Math.abs(Mth.wrapDegrees(yaw - this.sentYaw)) > 0.05F || Math.abs(pitch - this.sentPitch) > 0.05F || this.sentAge > 5)
		{
			PacketHandler.instance.sendToServer(new MessageTankAim(yaw, pitch, target));
			this.sentYaw = yaw;
			this.sentPitch = pitch;
			this.sentAge = 0;
		}
	}

	/** What is under the crosshair: the first block or entity (not the tank or its crew) */
	private static Vec3 findTarget(Level level, Player player, TankEntity tank, Camera camera)
	{
		Vec3 eye = camera.getPosition();
		Vector3f look = camera.getLookVector();
		Vec3 end = eye.add(look.x() * AIM_RANGE, look.y() * AIM_RANGE, look.z() * AIM_RANGE);
		BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 target = block.getType() != HitResult.Type.MISS ? block.getLocation() : end;
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, player, eye, target, new AABB(eye, target).inflate(1.0),
				e -> !e.isSpectator() && e.isPickable() && e != tank && e.getRootVehicle() != tank);
		return entity != null ? entity.getLocation() : target;
	}

	/**
	 * Where the projectile of the selected weapon lands with the gun as it points now (first block on
	 * its path), or null
	 */
	@Nullable
	private static Vec3 predictImpact(Level level, TankEntity tank, float partialTicks)
	{
		Vec3 pos = switch(weapon)
		{
			case COAX -> tank.getGunPoint(TankEntity.COAX_PORT, partialTicks);
			case HEAVY_MG -> tank.getTurretPoint(TankEntity.HEAVY_MG_MUZZLE, partialTicks);
			case LIGHT_SHELLS -> tank.getTurretPoint(TankEntity.LAUNCH_TUBES_CENTRE, partialTicks);
			default -> tank.getMuzzlePosition(partialTicks);
		};
		// the light shells go along the turret towards the target, the others along the gun
		Vec3 direction = weapon == TankWeapon.LIGHT_SHELLS ? tank.getLaunchDirection(pos, lastTarget, partialTicks) : tank.getGunDirection(partialTicks);
		Vec3 velocity = direction.scale(speed(weapon));
		double gravity = gravity(weapon);
		double travelled = 0;
		for(int i = 0; i < 400 && travelled < AIM_RANGE; i++)
		{
			Vec3 next = pos.add(velocity);
			BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity) null));
			if(hit.getType() != HitResult.Type.MISS)
			{
				return hit.getLocation();
			}
			travelled += velocity.length();
			pos = next;
			velocity = velocity.add(0, -gravity, 0);
		}
		return pos;
	}

	/** Screen position (GUI pixels) of a point in the world, or null if it is behind the camera */
	@Nullable
	private static float[] project(Camera camera, Vec3 point, int width, int height)
	{
		Vec3 d = point.subtract(camera.getPosition());
		Vector3f f = camera.getLookVector(), u = camera.getUpVector(), l = camera.getLeftVector();
		double z = d.x * f.x() + d.y * f.y() + d.z * f.z();
		if(z < 0.1)
			return null;
		double x = d.x * l.x() + d.y * l.y() + d.z * l.z();
		double y = d.x * u.x() + d.y * u.y() + d.z * u.z();
		double scale = (height / 2.0) / Math.tan(Math.toRadians(lastFov / 2.0));
		return new float[]{(float) (width / 2.0 - x / z * scale), (float) (height / 2.0 - y / z * scale)};
	}

	/* ------------------------------------------------------------------ rendering */

	@SubscribeEvent
	public void onRenderLiving(RenderLivingEvent.Pre<?, ?> event)
	{
		// The crew is inside the tank
		if(event.getEntity().getVehicle() instanceof TankEntity)
		{
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onRenderHand(RenderHandEvent event)
	{
		if(isInTank())
		{
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onRenderOverlay(RenderGuiOverlayEvent.Pre event)
	{
		Player player = Minecraft.getInstance().player;
		if(player == null || !(player.getVehicle() instanceof TankEntity inTank))
			return;
		ResourceLocation id = event.getOverlay().id();
		boolean hotbar = id.equals(VanillaGuiOverlay.HOTBAR.id());
		boolean crosshair = id.equals(VanillaGuiOverlay.CROSSHAIR.id());
		// no hotbar, item name or experience bar in the tank: the weapon bar takes their place
		if(!hotbar && !crosshair && !id.equals(VanillaGuiOverlay.ITEM_NAME.id()) && !id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id()))
			return;
		TankEntity tank = getDrivenTank();
		if(crosshair && tank == null)
			return; // the other crew members keep the normal crosshair
		event.setCanceled(true);
		if(Minecraft.getInstance().options.hideGui)
			return;
		int width = event.getWindow().getGuiScaledWidth(), height = event.getWindow().getGuiScaledHeight();
		if(hotbar)
		{
			// the whole crew sees the state of the tank, the driver his weapons
			this.drawStatus(event.getGuiGraphics(), inTank, height);
			if(tank != null)
			{
				this.drawWeaponBar(event.getGuiGraphics(), tank, player, width, height);
			}
		}
		else if(crosshair)
		{
			this.drawSight(event.getGuiGraphics(), tank, event.getPartialTick(), width, height);
		}
	}

	private void drawSight(GuiGraphics graphics, TankEntity tank, float partialTicks, int width, int height)
	{
		Minecraft mc = Minecraft.getInstance();
		int cx = width / 2, cy = height / 2;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		// Where the shot will land with the gun as it points now
		Vec3 impact = mc.level != null ? predictImpact(mc.level, tank, partialTicks) : null;
		float[] marker = impact != null ? project(mc.gameRenderer.getMainCamera(), impact, width, height) : null;
		boolean onTarget = marker != null && Math.abs(marker[0] - cx) < 2.5F && Math.abs(marker[1] - cy) < 2.5F;

		// Crosshair: where the driver wants the shot to land
		graphics.blit(RETICLE, cx - 24, cy - 24, 48, 48, 0, 0, 128, 128, 128, 128);
		if(marker != null)
		{
			if(onTarget) RenderSystem.setShaderColor(0.45F, 1F, 0.45F, 1F);
			else RenderSystem.setShaderColor(1F, 0.75F, 0.35F, 0.95F);
			int size = 18;
			graphics.pose().pushPose();
			graphics.pose().translate(marker[0] - size / 2F, marker[1] - size / 2F, 0);
			graphics.blit(GUN_MARKER, 0, 0, size, size, 0, 0, 64, 64, 64, 64);
			graphics.pose().popPose();
			RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		}

		// Zoom and distance to where the shot lands
		graphics.drawString(mc.font, String.format("x%.2f", zoom), cx + 28, cy + 4, 0xFFFFFF, true);
		if(impact != null)
		{
			String range = String.format("%d m", (int) Math.round(impact.distanceTo(tank.getMuzzlePosition(partialTicks))));
			graphics.drawString(mc.font, range, cx + 28, cy - 12, 0xD0D0D0, true);
		}
		RenderSystem.disableBlend();
	}

	private void drawWeaponBar(GuiGraphics graphics, TankEntity tank, Player player, int width, int height)
	{
		Minecraft mc = Minecraft.getInstance();
		TankWeapon[] weapons = TankWeapon.values();
		int slot = 22, gap = 2, total = weapons.length * slot + (weapons.length - 1) * gap;
		int x0 = (width - total) / 2, y = height - slot - 1;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		for(int i = 0; i < weapons.length; i++)
		{
			int x = x0 + i * (slot + gap);
			graphics.blit(SLOT, x, y, slot, slot, 0, 0, 22, 22, 22, 22);
			graphics.blit(WEAPON_ICONS[i], x + 3, y + 3, 16, 16, 0, 0, 32, 32, 32, 32);
			// the gun reloading (also on "all", whose click fires the gun): a light veil over the icon that
			// goes down as the reload ends, like the cooldown of a vanilla item
			if(weapons[i] == TankWeapon.CANNON || weapons[i] == TankWeapon.ALL)
			{
				int left = this.fireCooldowns[TankWeapon.CANNON.ordinal()];
				if(left > 0)
				{
					float part = Mth.clamp((left - mc.getFrameTime()) / TankWeapon.CANNON.getInterval(), 0F, 1F);
					int h = Mth.ceil(16 * part);
					graphics.fill(x + 3, y + 3 + 16 - h, x + 19, y + 19, 0x7FFFFFFF);
				}
			}
			if(weapons[i] == weapon)
			{
				graphics.blit(SLOT_SELECTED, x - 1, y - 1, slot + 2, slot + 2, 0, 0, 24, 24, 24, 24);
			}
			// key number, small, in the corner
			graphics.pose().pushPose();
			graphics.pose().translate(x + 2, y + 2, 200);
			graphics.pose().scale(0.6F, 0.6F, 1F);
			graphics.drawString(mc.font, String.valueOf(i + 1), 0, 0, weapons[i] == weapon ? 0xFFFFA0 : 0xB0B0B0, true);
			graphics.pose().popPose();
			// rounds left above the weapon (red when empty; none needed in creative mode)
			TankAmmo ammo = TankAmmo.of(weapons[i]);
			if(ammo != null)
			{
				int rounds = tank.getAmmoCount(ammo);
				String text = rounds == 0 && player.isCreative() ? "∞" : String.valueOf(rounds);
				int color = rounds > 0 || player.isCreative() ? (weapons[i] == weapon ? 0xFFFFA0 : 0xFFFFFF) : 0xFF5555;
				float scale = 0.75F;
				graphics.pose().pushPose();
				graphics.pose().translate(x + slot / 2F - mc.font.width(text) * scale / 2F, y - 8, 200);
				graphics.pose().scale(scale, scale, 1F);
				graphics.drawString(mc.font, text, 0, 0, color, true);
				graphics.pose().popPose();
			}
		}
		// name of the weapon above the counts
		Component name = Component.translatable(weapon.getTranslationKey());
		graphics.drawString(mc.font, name, (width - mc.font.width(name)) / 2, y - 19, 0xFFFFFF, true);
		RenderSystem.disableBlend();
		// the player's hearts, armour and food go up above them (drawn after the hotbar)
		if(mc.gui instanceof ForgeGui gui)
		{
			gui.leftHeight += WEAPON_BAR_EXTRA_HEIGHT;
			gui.rightHeight += WEAPON_BAR_EXTRA_HEIGHT;
		}
	}

	/**
	 * State of the tank in the bottom left corner, for the whole crew: health, armour, fuel and
	 * speed, each with its icon, a bar and its value
	 */
	private void drawStatus(GuiGraphics graphics, TankEntity tank, int height)
	{
		Minecraft mc = Minecraft.getInstance();
		float health = tank.getMaxHealth() > 0 ? tank.getHealth() / tank.getMaxHealth() : 0F;
		float armor = tank.getMaxArmor() > 0 ? tank.getArmor() / tank.getMaxArmor() : 0F;
		boolean usesFuel = tank.requiresFuel() && tank.getFuelCapacity() > 0;
		float fuel = usesFuel ? tank.getCurrentFuel() / tank.getFuelCapacity() : 1F;
		double blocksPerSecond = tank.getKilometersPreHour(); // named so in the vehicle, but in blocks (metres) per second
		float speed = (float) (blocksPerSecond / Math.max(1F, tank.getMaxSpeed()));
		float[] values = {health, armor, fuel, speed};
		String[] texts = {
			percent(health), percent(armor), usesFuel ? percent(fuel) : "∞",
			Component.translatable("gui.vehicle_restored.tank.speed", String.format(Locale.ROOT, "%.1f", blocksPerSecond)).getString()
		};
		// what each plate has left, -1 without a plate
		float[] plates = new float[TankArmorFace.values().length];
		for(TankArmorFace face : TankArmorFace.values())
		{
			int points = tank.getArmorPoints(face);
			plates[face.ordinal()] = points < 0 ? -1F : points / (float) ArmorPlateItem.POINTS;
		}

		drawStatusRows(graphics, mc.font, 6, height - 6 - STATUS_ICONS.length * STATUS_ROW_HEIGHT, values, texts, plates);
	}

	private static final int STATUS_ROW_HEIGHT = 13;

	/**
	 * The rows of the state from (x0, y0): no panel, each row an icon, its bar and its value straight over the view
	 * (the values with their shadow); health, armour, fuel, speed. Also drawn by the image exporter of the
	 * development client.
	 */
	public static void drawStatusRows(GuiGraphics graphics, net.minecraft.client.gui.Font font, int x0, int y0, float[] values, String[] texts, float[] plates)
	{
		int icon = 12;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		graphics.drawManaged(() ->
		{
			for(int i = 0; i < STATUS_ICONS.length; i++)
			{
				int y = y0 + i * STATUS_ROW_HEIGHT;
				graphics.blit(STATUS_ICONS[i], x0, y, icon, icon, 0, 0, 24, 24, 24, 24);
				int barX = x0 + icon + 5, barY = y + (icon - BAR_HEIGHT) / 2;
				drawBar(graphics, barX, barY, values[i], STATUS_COLORS[i], i == 1 ? plates : null);
				graphics.drawString(font, texts[i], barX + BAR_WIDTH + BAR_HEIGHT - 1 + 5, y + 2, 0xFFFFFF, true);
			}
		});
		RenderSystem.disableBlend();
	}

	private static String percent(float value)
	{
		return Component.translatable("gui.vehicle_restored.tank.percent", Math.round(Mth.clamp(value, 0F, 1F) * 100F)).getString();
	}

	/**
	 * A bar of the state (see BAR_SEGMENTS), its bottom left corner at (x, y + BAR_HEIGHT - 1), filled to the given
	 * part by whole segments: the filled ones in the colour (lit top row, shaded bottom row), the others faint in it.
	 * With `parts` (the armour: what each plate has left, -1 without a plate) the bar is cut in one part per plate
	 * instead of its segments, each filled by pixel; a part without a plate shows only the track.
	 */
	private static void drawBar(GuiGraphics graphics, int x, int y, float value, int color, float[] parts)
	{
		int filled = Math.round(Mth.clamp(value, 0F, 1F) * BAR_SEGMENTS);
		int full = 0xFF000000 | color, lit = 0xFF000000 | mix(color, 0xFFFFFF, 0.4F), shade = 0xFF000000 | mix(color, 0x000000, 0.28F);
		int empty = 0x48000000 | color;
		for(int row = 0; row < BAR_HEIGHT; row++)
		{
			int left = x + BAR_HEIGHT - 1 - row, top = y + row;
			if(row == 0 || row == BAR_HEIGHT - 1)
			{
				graphics.fill(left, top, left + BAR_WIDTH, top + 1, BAR_OUTLINE);
				continue;
			}
			graphics.fill(left, top, left + 1, top + 1, BAR_OUTLINE);
			graphics.fill(left + BAR_WIDTH - 1, top, left + BAR_WIDTH, top + 1, BAR_OUTLINE);
			graphics.fill(left + 1, top, left + BAR_WIDTH - 1, top + 1, BAR_TRACK);
			if(row == 1 || row == BAR_HEIGHT - 2)
				continue;
			int on = row == 2 ? lit : row == BAR_HEIGHT - 3 ? shade : full;
			if(parts != null)
			{
				for(int p = 0; p < parts.length; p++)
				{
					if(parts[p] < 0F)
						continue;
					int px = left + 2 + p * ARMOR_PART_STEP, w = Math.round(Mth.clamp(parts[p], 0F, 1F) * ARMOR_PART);
					graphics.fill(px, top, px + w, top + 1, on);
					graphics.fill(px + w, top, px + ARMOR_PART, top + 1, empty);
				}
				continue;
			}
			for(int s = 0; s < BAR_SEGMENTS; s++)
			{
				int sx = left + 2 + s * (SEGMENT + 1);
				graphics.fill(sx, top, sx + SEGMENT, top + 1, s >= filled ? empty : on);
			}
		}
	}

	private static int mix(int a, int b, float t)
	{
		int r = Math.round(Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255));
		int g = Math.round(Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255));
		int bl = Math.round(Mth.lerp(t, a & 255, b & 255));
		return r << 16 | g << 8 | bl;
	}
}
