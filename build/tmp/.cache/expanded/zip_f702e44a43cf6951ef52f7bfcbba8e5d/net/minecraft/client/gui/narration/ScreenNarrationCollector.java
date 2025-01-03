package net.minecraft.client.gui.narration;

import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScreenNarrationCollector {
    int generation;
    final Map<ScreenNarrationCollector.EntryKey, ScreenNarrationCollector.NarrationEntry> entries = Maps.newTreeMap(
        Comparator.<ScreenNarrationCollector.EntryKey, NarratedElementType>comparing(p_169196_ -> p_169196_.type)
            .thenComparing(p_169185_ -> p_169185_.depth)
    );

    public void update(Consumer<NarrationElementOutput> pUpdater) {
        this.generation++;
        pUpdater.accept(new ScreenNarrationCollector.Output(0));
    }

    public String collectNarrationText(boolean pCollectAll) {
        final StringBuilder stringbuilder = new StringBuilder();
        Consumer<String> consumer = new Consumer<String>() {
            private boolean firstEntry = true;

            public void accept(String p_169204_) {
                if (!this.firstEntry) {
                    stringbuilder.append(". ");
                }

                this.firstEntry = false;
                stringbuilder.append(p_169204_);
            }
        };
        this.entries.forEach((p_169193_, p_169194_) -> {
            if (p_169194_.generation == this.generation && (pCollectAll || !p_169194_.alreadyNarrated)) {
                p_169194_.contents.getText(consumer);
                p_169194_.alreadyNarrated = true;
            }
        });
        return stringbuilder.toString();
    }

    @OnlyIn(Dist.CLIENT)
    static class EntryKey {
        final NarratedElementType type;
        final int depth;

        EntryKey(NarratedElementType pType, int pDepth) {
            this.type = pType;
            this.depth = pDepth;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class NarrationEntry {
        NarrationThunk<?> contents = NarrationThunk.EMPTY;
        int generation = -1;
        boolean alreadyNarrated;

        public ScreenNarrationCollector.NarrationEntry update(int pGeneration, NarrationThunk<?> pContents) {
            if (!this.contents.equals(pContents)) {
                this.contents = pContents;
                this.alreadyNarrated = false;
            } else if (this.generation + 1 != pGeneration) {
                this.alreadyNarrated = false;
            }

            this.generation = pGeneration;
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    class Output implements NarrationElementOutput {
        private final int depth;

        Output(final int pDepth) {
            this.depth = pDepth;
        }

        @Override
        public void add(NarratedElementType p_169226_, NarrationThunk<?> p_169227_) {
            ScreenNarrationCollector.this.entries
                .computeIfAbsent(new ScreenNarrationCollector.EntryKey(p_169226_, this.depth), p_169229_ -> new ScreenNarrationCollector.NarrationEntry())
                .update(ScreenNarrationCollector.this.generation, p_169227_);
        }

        @Override
        public NarrationElementOutput nest() {
            return ScreenNarrationCollector.this.new Output(this.depth + 1);
        }
    }
}