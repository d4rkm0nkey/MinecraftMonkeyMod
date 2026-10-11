package me.teajay.cheeky.monkeys.block.entity;

import me.teajay.cheeky.monkeys.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/// The nine slots of loot in a [me.teajay.cheeky.monkeys.block.MonkeyStashBlock]. Its contents spill out when it's broken.
public class MonkeyStashBlockEntity extends BaseContainerBlockEntity {
    private static final int SIZE = 9;
    private static final Component DEFAULT_NAME = Component.translatable("container.cheeky_monkeys.monkey_stash");
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public MonkeyStashBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MONKEY_STASH.get(), pos, state);
    }

    /// Tucks as much of `stack` away as fits, returning what didn't
    public ItemStack stash(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int slot = 0; slot < SIZE && !rest.isEmpty(); slot++) {
            ItemStack inSlot = this.items.get(slot);
            if (inSlot.isEmpty()) {
                this.items.set(slot, rest);
                rest = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(inSlot, rest)) {
                int moved = Math.min(rest.getCount(), inSlot.getMaxStackSize() - inSlot.getCount());
                inSlot.grow(moved);
                rest.shrink(moved);
            }
        }

        this.setChanged();
        return rest;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, this, 1);
    }
}
