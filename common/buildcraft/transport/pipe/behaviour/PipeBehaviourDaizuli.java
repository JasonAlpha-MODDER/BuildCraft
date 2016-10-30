package buildcraft.transport.pipe.behaviour;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.RayTraceResult;

import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.PipeEventHandler;
import buildcraft.api.transport.PipeEventItem;
import buildcraft.api.transport.neptune.IPipe;
import buildcraft.api.transport.neptune.IPipeHolder.PipeMessageReceiver;

import buildcraft.lib.misc.EntityUtil;
import buildcraft.lib.misc.NBTUtils;

public class PipeBehaviourDaizuli extends PipeBehaviourDirectional {
    private EnumDyeColor colour = EnumDyeColor.WHITE;

    public PipeBehaviourDaizuli(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourDaizuli(IPipe pipe, NBTTagCompound nbt) {
        super(pipe, nbt);
        colour = NBTUtils.readEnum(nbt.getTag("colour"), EnumDyeColor.class);
        if (colour == null) {
            colour = EnumDyeColor.WHITE;
        }
    }

    @Override
    public NBTTagCompound writeToNbt() {
        NBTTagCompound nbt = super.writeToNbt();
        nbt.setTag("colour", NBTUtils.writeEnum(colour));
        return nbt;
    }

    @Override
    public void writePayload(PacketBuffer buffer, Side side) {
        super.writePayload(buffer, side);
        if (side == Side.SERVER) {
            buffer.writeByte(colour.getMetadata());
        }
    }

    @Override
    public void readPayload(PacketBuffer buffer, Side side, MessageContext ctx) {
        super.readPayload(buffer, side, ctx);
        if (side == Side.CLIENT) {
            colour = EnumDyeColor.byMetadata(buffer.readUnsignedByte());
        }
    }

    @Override
    public int getTextureIndex(EnumFacing face) {
        if (face == currentDir.face) {
            return 16;
        }
        return colour.getMetadata();
    }

    @Override
    protected boolean canFaceDirection(EnumFacing dir) {
        return true;
    }

    @Override
    public boolean onPipeActivate(EntityPlayer player, RayTraceResult trace, float hitX, float hitY, float hitZ, EnumPipePart part) {
        if (part != EnumPipePart.CENTER && part != currentDir) {
            // Activating the centre of a pipe always falls back to changing the colour
            // And so does clicking on the current facing side
            return super.onPipeActivate(player, trace, hitX, hitY, hitZ, part);
        }
        if (player.worldObj.isRemote) {
            return EntityUtil.getWrenchHand(player) != null;
        }
        if (EntityUtil.getWrenchHand(player) != null) {
            EntityUtil.activateWrench(player);
            int n = colour.getMetadata() + (player.isSneaking() ? 15 : 1);
            colour = EnumDyeColor.byMetadata(n & 15);
            pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
            return true;
        }
        return false;
    }

    @PipeEventHandler
    public void sideCheck(PipeEventItem.SideCheck sideCheck) {
        if (colour == sideCheck.colour) {
            sideCheck.disallowAllExcept(currentDir.face);
        } else {
            sideCheck.disallow(currentDir.face);
        }
    }
}