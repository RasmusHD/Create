package com.simibubi.create.content.contraptions.fluids.actors;

import com.simibubi.create.content.contraptions.processing.EmptyingByBasin;
import com.simibubi.create.content.contraptions.relays.belt.transport.TransportedItemStack;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

public class ItemDrainItemHandler extends SnapshotParticipant<Unit> implements SingleSlotStorage<ItemVariant> {

	private ItemDrainTileEntity te;
	private Direction side;

	public ItemDrainItemHandler(ItemDrainTileEntity te, Direction side) {
		this.te = te;
		this.side = side;
	}

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		if (!te.getHeldItemStack().isEmpty())
			return 0;

		ItemStack stack = resource.toStack();
		int toInsert = EmptyingByBasin.canItemBeEmptied(te.getLevel(), stack)
				? 1
				: Math.min((int) maxAmount, resource.getItem().getMaxStackSize());
		stack.setCount(toInsert);
		TransportedItemStack heldItem = new TransportedItemStack(stack);
		heldItem.prevBeltPosition = 0;
		te.snapshotParticipant.updateSnapshots(transaction);
		te.setHeldItem(heldItem, side.getOpposite());
		return toInsert;
	}

	@Override
	public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		TransportedItemStack held = te.heldItem;
		if (held == null)
			return 0;

		int toExtract = Math.min((int) maxAmount, held.stack.getCount());
		ItemStack stack = held.stack.copy();
		stack.shrink(toExtract);
		te.snapshotParticipant.updateSnapshots(transaction);
		te.heldItem.stack = stack;
		if (stack.isEmpty())
			te.heldItem = null;
		return toExtract;
	}

	@Override
	public boolean isResourceBlank() {
		return getResource().isBlank();
	}

	@Override
	public ItemVariant getResource() {
		return ItemVariant.of(getStack());
	}

	@Override
	public long getAmount() {
		ItemStack stack = getStack();
		return stack.isEmpty() ? 0 : stack.getCount();
	}

	@Override
	public long getCapacity() {
		return getStack().getMaxStackSize();
	}

	public ItemStack getStack() {
		TransportedItemStack held = te.heldItem;
		if (held == null || held.stack == null || held.stack.isEmpty())
			return ItemStack.EMPTY;
		return held.stack;
	}

	@Override
	protected Unit createSnapshot() {
		return Unit.INSTANCE;
	}

	@Override
	protected void readSnapshot(Unit snapshot) {
	}

	@Override
	protected void onFinalCommit() {
		super.onFinalCommit();
		te.notifyUpdate();
	}
}
