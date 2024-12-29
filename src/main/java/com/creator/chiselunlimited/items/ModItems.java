package com.creator.chiselunlimited.items;

import com.creator.chiselunlimited.ChiselUnlimited;
import com.nimbusds.oauth2.sdk.id.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    // Make sure my Items are being Registered
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ChiselUnlimited.MOD_ID);

    // Test Item
    public static final RegistryObject<Item> Alexandrite = ITEMS.register("alexandrite", () -> new Item(new Item.Properties().useItemDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ChiselUnlimited.MOD_ID, "alexandrite")))));
    public static final RegistryObject<Item> Raw_Alexandrite = ITEMS.register("raw_alexandrite", () -> new Item(new Item.Properties().useItemDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ChiselUnlimited.MOD_ID, "raw_alexandrite")))));

    // Pass the EventBus to the Registry
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
