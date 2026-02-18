package io.github.onu_eccs1621_sp2025.westward.game;

import com.google.gson.annotations.SerializedName;
import io.github.onu_eccs1621_sp2025.westward.data.StatusContainer;
import io.github.onu_eccs1621_sp2025.westward.utils.ShallowClone;
import io.github.onu_eccs1621_sp2025.westward.utils.registry.Registry;
import io.github.onu_eccs1621_sp2025.westward.utils.text.Translations;

import java.util.Optional;

/**
 * Keeps the type and number of a certain item
 * @author Dylan Catte, Ben Westover, Noah Sumerauer, Micah Lee
 * @version 1.0
 * @since 1.0.0 Alpha 1
 */
public class ItemStack implements ShallowClone<ItemStack> {
    private final String id;
    private short count;
    private float barterValue;
    private final ItemType type;
    private final String statusApplied;

    /**
     * The type of Item in the ItemStack
     */
    public enum ItemType {
        @SerializedName("food")
        FOOD,
        @SerializedName("foodIngredient")
        FOOD_INGREDIENT,
        @SerializedName("weapon")
        WEAPON,
        @SerializedName("ammo")
        AMMUNITION,
        @SerializedName("wagonParts")
        WAGON_PARTS,
        @SerializedName("medicine")
        MEDICINE,
        @SerializedName("supplies")
        SUPPLIES,
        @SerializedName("tool")
        TOOL,
        @SerializedName("clothes")
        CLOTHES
    }

    /**
     * Constructor that sets the local variables to the inputted variable.
     * @param id Item id
     */
    public ItemStack(String id) {
        this.id = id;
        this.type = ItemType.SUPPLIES;
        this.statusApplied = null;
    }

    /**
     * Constructor that sets the local variables to the inputted variable.
     * @param id Item id
     * @param type Type of the item stack
     */
    public ItemStack(String id, ItemType type) {
        this.id = id;
        this.type = type;
        this.statusApplied = null;
    }

    /**
     * Constructor that sets the local variables to the inputted variables.
     * @param id Item id
     * @param count Number of item in the item stack
     */
    public ItemStack(String id, short count) {
        this(id);
        this.count = count;
    }

    /**
     * Constructor that sets the local variables to the inputted variables.
     * @param id Item id
     * @param type Type of the item stack
     * @param count Number of item in the item stack
     */
    public ItemStack(String id, ItemType type, short count) {
        this(id, type);
        this.count = count;
    }

    /**
     * Constructor that sets the local variables to the inputted variables.
     * @param id Item id
     * @param count Number of item in the item stack
     * @param barterValue The item's intrinsic value for trading
     */
    public ItemStack(String id, short count, float barterValue) {
        this(id, count);
        this.barterValue = barterValue;
    }

    /**
     * Constructor that sets the local variables to the inputted variables.
     * @param id Item id
     * @param count Number of item in the item stack
     * @param type Type of the item stack
     */
    public ItemStack(String id, short count, ItemType type, float barterValue) {
        this(id, type);
        this.count = count;
        this.barterValue = barterValue;
    }

    /**
     *Constructor that sets the local variables to the inputted variables.
     * @param id Item id
     * @param count Number of item in the item stack
     * @param barterValue The item's intrinsic value for trading
     * @param statusApplied The status applied to the item stack when consumed
     */
    public ItemStack(String id, short count, String statusApplied, float barterValue, ItemType type) {
        this.id = id;
        this.count = count;
        this.type = type;
        this.barterValue = barterValue;
        this.statusApplied = statusApplied;
    }

    /**
     * Gets the id used for translation
     * @return Translation id
     */
    public String getId() {
        return this.id;
    }

    /**
     * Gets the translated name of the item
     * @return ItemStack's name
     */
    public String getName() {
        return Translations.getTranslatedText(this.id);
    }

    /**
     * Gets the number of items in the ItemStack<p>
     * Use {@link ItemStack#setCount(short)} to set the size of the ItemStack
     * @return Size of the ItemStack
     */
    public short getCount() {
        return this.count;
    }

    /**
     * Sets the size of the ItemStack
     * @param value Size to set to
     */
    public void setCount(short value) {
        this.count = value;
    }

    /**
     * Gets the item's intrinsic value for trading
     * Use {@link ItemStack#setBarterValue(float)} to set the barter value of the ItemStack
     * @return Value of the ItemStack
     */
    public float getBarterValue() {
        return this.barterValue;
    }

    /**
     * Set the item's intrinsic value for trading
     * @param value Value to set to
     */
    public void setBarterValue(float value) {
        this.barterValue = value;
    }

    /**
     * Gets the type of item in the ItemStack
     * @return Type of item
     */
    public ItemType getType() {
        return this.type;
    }

    /**
     * Checks if the amount of items can be consumed
     * @param amount The amount of items to consume
     * @return True if the consumption is possible
     */
    public boolean canConsume(short amount) {
        return this.count >= amount;
    }

    /**
     * Consumes a specified amount of items.
     * Make sure canConsume() is checked first.
     * @param amount The amount of items to consume
     * @return If there are 0 of this item left
     */
    public boolean consume(short amount) {
        this.count -= amount;
        return this.count <= 0;
    }

    /**
     * Merges the amounts of two ItemStacks
     * @param otherStack Other ItemStack to merge with this one
     */
    public void mergeItemStacks(ItemStack otherStack) {
        this.count += otherStack.count;
    }

    /**
     * Checks if the two ItemStacks are of the same type.
     * This method ignores the amount
     * @param obj Other object to check with
     * @return True if the ItemStacks are of the same type
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof ItemStack stack && stack.id.equals(this.id);
    }

    private Optional<StatusContainer> getStatus() {
        // Required to move around Atomics
        var ref = new Object() {
            Optional<StatusContainer> status;
        };
        if (statusApplied != null) {
            ref.status = Optional.of((StatusContainer) Registry.getAsset(Registry.AssetType.STATUS, statusApplied));
        } else {
            ref.status = Optional.empty();
        }
        return ref.status;
    }

    /**
     * Clones the ItemStack's identifier and status (if applicable)
     * @return A clone of the ItemStack.
     */
    @Override
    public ItemStack shallowClone() {
        // Avoid Atomics
        var ref = new Object() {
            ItemStack stack;
        };
        if (this.statusApplied != null) {
            ref.stack = new ItemStack(this.id, (short) 0, statusApplied, this.barterValue, this.type);
        } else {
            ref.stack = new ItemStack(this.id, (short) 0, this.type, this.barterValue);
        }
        return ref.stack;
    }

    @Override
    public String toString() {
        return this.getName() + " (" + this.count + ")";
    }
}
