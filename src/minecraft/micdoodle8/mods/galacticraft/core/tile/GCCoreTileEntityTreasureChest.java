package micdoodle8.mods.galacticraft.core.tile;

import micdoodle8.mods.galacticraft.core.blocks.GCCoreBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

/**
 * Copyright 2012-2013, micdoodle8
 * 
 *  All rights reserved.
 *
 */
public class GCCoreTileEntityTreasureChest extends TileEntity implements IInventory
{
    private ItemStack[] chestContents = new ItemStack[36];

    /** Determines if the check for adjacent chests has taken place. */
    public boolean adjacentChestChecked = false;

    /** Contains the chest tile located adjacent to this one (if any) */
    public GCCoreTileEntityTreasureChest adjacentChestZNeg;

    /** Contains the chest tile located adjacent to this one (if any) */
    public GCCoreTileEntityTreasureChest adjacentChestXPos;

    /** Contains the chest tile located adjacent to this one (if any) */
    public GCCoreTileEntityTreasureChest adjacentChestXNeg;

    /** Contains the chest tile located adjacent to this one (if any) */
    public GCCoreTileEntityTreasureChest adjacentChestZPosition;

    /** The current angle of the lid (between 0 and 1) */
    public float lidAngle;

    /** The angle of the lid last tick */
    public float prevLidAngle;

    /** The number of players currently using this chest */
    public int numUsingPlayers;

    /** Server sync counter (once per 20 ticks) */
    private int ticksSinceSync;

    /**
     * Returns the number of slots in the inventory.
     */
    @Override
	public int getSizeInventory()
    {
        return 27;
    }

    /**
     * Returns the stack in slot i
     */
    @Override
	public ItemStack getStackInSlot(int par1)
    {
        return this.chestContents[par1];
    }

    /**
     * Removes from an inventory slot (first arg) up to a specified number (second arg) of items and returns them in a
     * new stack.
     */
    @Override
	public ItemStack decrStackSize(int par1, int par2)
    {
        if (this.chestContents[par1] != null)
        {
            ItemStack var3;

            if (this.chestContents[par1].stackSize <= par2)
            {
                var3 = this.chestContents[par1];
                this.chestContents[par1] = null;
                this.onInventoryChanged();
                return var3;
            }
            else
            {
                var3 = this.chestContents[par1].splitStack(par2);

                if (this.chestContents[par1].stackSize == 0)
                {
                    this.chestContents[par1] = null;
                }

                this.onInventoryChanged();
                return var3;
            }
        }
        else
        {
            return null;
        }
    }

    /**
     * When some containers are closed they call this on each slot, then drop whatever it returns as an EntityItem -
     * like when you close a workbench GUI.
     */
    @Override
	public ItemStack getStackInSlotOnClosing(int par1)
    {
        if (this.chestContents[par1] != null)
        {
            final ItemStack var2 = this.chestContents[par1];
            this.chestContents[par1] = null;
            return var2;
        }
        else
        {
            return null;
        }
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    @Override
	public void setInventorySlotContents(int par1, ItemStack par2ItemStack)
    {
        this.chestContents[par1] = par2ItemStack;

        if (par2ItemStack != null && par2ItemStack.stackSize > this.getInventoryStackLimit())
        {
            par2ItemStack.stackSize = this.getInventoryStackLimit();
        }

        this.onInventoryChanged();
    }

    /**
     * Returns the name of the inventory.
     */
    @Override
	public String getInvName()
    {
        return "container.chest";
    }

    /**
     * Reads a tile entity from NBT.
     */
    @Override
	public void readFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readFromNBT(par1NBTTagCompound);
        final NBTTagList var2 = par1NBTTagCompound.getTagList("Items");
        this.chestContents = new ItemStack[this.getSizeInventory()];

        for (int var3 = 0; var3 < var2.tagCount(); ++var3)
        {
            final NBTTagCompound var4 = (NBTTagCompound)var2.tagAt(var3);
            final int var5 = var4.getByte("Slot") & 255;

            if (var5 >= 0 && var5 < this.chestContents.length)
            {
                this.chestContents[var5] = ItemStack.loadItemStackFromNBT(var4);
            }
        }
    }

    /**
     * Writes a tile entity to NBT.
     */
    @Override
	public void writeToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeToNBT(par1NBTTagCompound);
        final NBTTagList var2 = new NBTTagList();

        for (int var3 = 0; var3 < this.chestContents.length; ++var3)
        {
            if (this.chestContents[var3] != null)
            {
                final NBTTagCompound var4 = new NBTTagCompound();
                var4.setByte("Slot", (byte)var3);
                this.chestContents[var3].writeToNBT(var4);
                var2.appendTag(var4);
            }
        }

        par1NBTTagCompound.setTag("Items", var2);
    }

    /**
     * Returns the maximum stack size for a inventory slot. Seems to always be 64, possibly will be extended. *Isn't
     * this more of a set than a get?*
     */
    @Override
	public int getInventoryStackLimit()
    {
        return 64;
    }

    /**
     * Do not make give this method the name canInteractWith because it clashes with Container
     */
    @Override
	public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer)
    {
        return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false : par1EntityPlayer.getDistanceSq(this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D) <= 64.0D;
    }

    /**
     * Causes the TileEntity to reset all it's cached values for it's container block, blockID, metaData and in the case
     * of chests, the adjcacent chest check
     */
    @Override
	public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();
        this.adjacentChestChecked = false;
    }

    /**
     * Performs the check for adjacent chests to determine if this chest is double or not.
     */
    public void checkForAdjacentChests()
    {
        if (!this.adjacentChestChecked)
        {
            this.adjacentChestChecked = true;
            this.adjacentChestZNeg = null;
            this.adjacentChestXPos = null;
            this.adjacentChestXNeg = null;
            this.adjacentChestZPosition = null;

            if (this.worldObj.getBlockId(this.xCoord - 1, this.yCoord, this.zCoord) == GCCoreBlocks.treasureChest.blockID)
            {
                this.adjacentChestXNeg = (GCCoreTileEntityTreasureChest)this.worldObj.getBlockTileEntity(this.xCoord - 1, this.yCoord, this.zCoord);
            }

            if (this.worldObj.getBlockId(this.xCoord + 1, this.yCoord, this.zCoord) == GCCoreBlocks.treasureChest.blockID)
            {
                this.adjacentChestXPos = (GCCoreTileEntityTreasureChest)this.worldObj.getBlockTileEntity(this.xCoord + 1, this.yCoord, this.zCoord);
            }

            if (this.worldObj.getBlockId(this.xCoord, this.yCoord, this.zCoord - 1) == GCCoreBlocks.treasureChest.blockID)
            {
                this.adjacentChestZNeg = (GCCoreTileEntityTreasureChest)this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord - 1);
            }

            if (this.worldObj.getBlockId(this.xCoord, this.yCoord, this.zCoord + 1) == GCCoreBlocks.treasureChest.blockID)
            {
                this.adjacentChestZPosition = (GCCoreTileEntityTreasureChest)this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord + 1);
            }

            if (this.adjacentChestZNeg != null)
            {
                this.adjacentChestZNeg.updateContainingBlockInfo();
            }

            if (this.adjacentChestZPosition != null)
            {
                this.adjacentChestZPosition.updateContainingBlockInfo();
            }

            if (this.adjacentChestXPos != null)
            {
                this.adjacentChestXPos.updateContainingBlockInfo();
            }

            if (this.adjacentChestXNeg != null)
            {
                this.adjacentChestXNeg.updateContainingBlockInfo();
            }
        }
    }

    /**
     * Allows the entity to update its state. Overridden in most subclasses, e.g. the mob spawner uses this to count
     * ticks and creates a new spawn inside its implementation.
     */
    @Override
	public void updateEntity()
    {
        super.updateEntity();
        this.checkForAdjacentChests();

        if (++this.ticksSinceSync % 20 * 4 == 0)
        {
            ;
        }

        this.prevLidAngle = this.lidAngle;
        final float var1 = 0.05F;
        double var4;

        if (this.numUsingPlayers > 0 && this.lidAngle == 0.0F && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null)
        {
            double var2 = this.xCoord + 0.5D;
            var4 = this.zCoord + 0.5D;

            if (this.adjacentChestZPosition != null)
            {
                var4 += 0.5D;
            }

            if (this.adjacentChestXPos != null)
            {
                var2 += 0.5D;
            }

            this.worldObj.playSoundEffect(var2, this.yCoord + 0.5D, var4, "random.chestopen", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.6F);
        }

        if (this.numUsingPlayers == 0 && this.lidAngle > 0.0F || this.numUsingPlayers > 0 && this.lidAngle < 1.0F)
        {
            final float var8 = this.lidAngle;

            if (this.numUsingPlayers > 0)
            {
                this.lidAngle += var1;
            }
            else
            {
                this.lidAngle -= var1;
            }

            if (this.lidAngle > 1.0F)
            {
                this.lidAngle = 1.0F;
            }

            final float var3 = 0.5F;

            if (this.lidAngle < var3 && var8 >= var3 && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null)
            {
                var4 = this.xCoord + 0.5D;
                double var6 = this.zCoord + 0.5D;

                if (this.adjacentChestZPosition != null)
                {
                    var6 += 0.5D;
                }

                if (this.adjacentChestXPos != null)
                {
                    var4 += 0.5D;
                }

                this.worldObj.playSoundEffect(var4, this.yCoord + 0.5D, var6, "random.chestclosed", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.6F);
            }

            if (this.lidAngle < 0.0F)
            {
                this.lidAngle = 0.0F;
            }
        }
    }

    /**
     * Called when a client event is received with the event number and argument, see World.sendClientEvent
     */
    @Override
	public void receiveClientEvent(int par1, int par2)
    {
        if (par1 == 1)
        {
            this.numUsingPlayers = par2;
        }
    }

    @Override
	public void openChest()
    {
        ++this.numUsingPlayers;
        this.worldObj.addBlockEvent(this.xCoord, this.yCoord, this.zCoord, GCCoreBlocks.treasureChest.blockID, 1, this.numUsingPlayers);
    }

    @Override
	public void closeChest()
    {
        --this.numUsingPlayers;
        this.worldObj.addBlockEvent(this.xCoord, this.yCoord, this.zCoord, GCCoreBlocks.treasureChest.blockID, 1, this.numUsingPlayers);
    }

    /**
     * invalidates a tile entity
     */
    @Override
	public void invalidate()
    {
        this.updateContainingBlockInfo();
        this.checkForAdjacentChests();
        super.invalidate();
    }
}
