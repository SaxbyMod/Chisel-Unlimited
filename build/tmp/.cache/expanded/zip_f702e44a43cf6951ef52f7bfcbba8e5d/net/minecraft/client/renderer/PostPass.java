package net.minecraft.client.renderer;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class PostPass {
    private final String name;
    private final CompiledShaderProgram shader;
    private final ResourceLocation outputTargetId;
    private final List<PostChainConfig.Uniform> uniforms;
    private final List<PostPass.Input> inputs = new ArrayList<>();

    public PostPass(String pName, CompiledShaderProgram pShader, ResourceLocation pOutputTargetId, List<PostChainConfig.Uniform> pUniforms) {
        this.name = pName;
        this.shader = pShader;
        this.outputTargetId = pOutputTargetId;
        this.uniforms = pUniforms;
    }

    public void addInput(PostPass.Input pInput) {
        this.inputs.add(pInput);
    }

    public void addToFrame(FrameGraphBuilder pFrameGraphBuilder, Map<ResourceLocation, ResourceHandle<RenderTarget>> pTargets, Matrix4f pProjectionMatrix) {
        FramePass framepass = pFrameGraphBuilder.addPass(this.name);

        for (PostPass.Input postpass$input : this.inputs) {
            postpass$input.addToPass(framepass, pTargets);
        }

        ResourceHandle<RenderTarget> resourcehandle = pTargets.computeIfPresent(
            this.outputTargetId, (p_366255_, p_363433_) -> framepass.readsAndWrites((ResourceHandle<RenderTarget>)p_363433_)
        );
        if (resourcehandle == null) {
            throw new IllegalStateException("Missing handle for target " + this.outputTargetId);
        } else {
            framepass.executes(() -> {
                RenderTarget rendertarget = resourcehandle.get();
                RenderSystem.viewport(0, 0, rendertarget.width, rendertarget.height);

                for (PostPass.Input postpass$input1 : this.inputs) {
                    postpass$input1.bindTo(this.shader, pTargets);
                }

                this.shader.safeGetUniform("OutSize").set((float)rendertarget.width, (float)rendertarget.height);

                for (PostChainConfig.Uniform postchainconfig$uniform : this.uniforms) {
                    Uniform uniform = this.shader.getUniform(postchainconfig$uniform.name());
                    if (uniform != null) {
                        uniform.setFromConfig(postchainconfig$uniform.values(), postchainconfig$uniform.values().size());
                    }
                }

                rendertarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                rendertarget.clear();
                rendertarget.bindWrite(false);
                RenderSystem.depthFunc(519);
                RenderSystem.setShader(this.shader);
                RenderSystem.backupProjectionMatrix();
                RenderSystem.setProjectionMatrix(pProjectionMatrix, ProjectionType.ORTHOGRAPHIC);
                BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                bufferbuilder.addVertex(0.0F, 0.0F, 500.0F);
                bufferbuilder.addVertex((float)rendertarget.width, 0.0F, 500.0F);
                bufferbuilder.addVertex((float)rendertarget.width, (float)rendertarget.height, 500.0F);
                bufferbuilder.addVertex(0.0F, (float)rendertarget.height, 500.0F);
                BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                RenderSystem.depthFunc(515);
                RenderSystem.restoreProjectionMatrix();
                rendertarget.unbindWrite();

                for (PostPass.Input postpass$input2 : this.inputs) {
                    postpass$input2.cleanup(pTargets);
                }

                this.restoreDefaultUniforms();
            });
        }
    }

    private void restoreDefaultUniforms() {
        for (PostChainConfig.Uniform postchainconfig$uniform : this.uniforms) {
            String s = postchainconfig$uniform.name();
            Uniform uniform = this.shader.getUniform(s);
            ShaderProgramConfig.Uniform shaderprogramconfig$uniform = this.shader.getUniformConfig(s);
            if (uniform != null && shaderprogramconfig$uniform != null && !postchainconfig$uniform.values().equals(shaderprogramconfig$uniform.values())) {
                uniform.setFromConfig(shaderprogramconfig$uniform);
            }
        }
    }

    public CompiledShaderProgram getShader() {
        return this.shader;
    }

    @OnlyIn(Dist.CLIENT)
    public interface Input {
        void addToPass(FramePass pPass, Map<ResourceLocation, ResourceHandle<RenderTarget>> pTargets);

        void bindTo(CompiledShaderProgram pShaderProgram, Map<ResourceLocation, ResourceHandle<RenderTarget>> pTargets);

        default void cleanup(Map<ResourceLocation, ResourceHandle<RenderTarget>> pTargets) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record TargetInput(String samplerName, ResourceLocation targetId, boolean depthBuffer, boolean bilinear) implements PostPass.Input {
        private ResourceHandle<RenderTarget> getHandle(Map<ResourceLocation, ResourceHandle<RenderTarget>> pTargets) {
            ResourceHandle<RenderTarget> resourcehandle = pTargets.get(this.targetId);
            if (resourcehandle == null) {
                throw new IllegalStateException("Missing handle for target " + this.targetId);
            } else {
                return resourcehandle;
            }
        }

        @Override
        public void addToPass(FramePass p_369983_, Map<ResourceLocation, ResourceHandle<RenderTarget>> p_369342_) {
            p_369983_.reads(this.getHandle(p_369342_));
        }

        @Override
        public void bindTo(CompiledShaderProgram p_361150_, Map<ResourceLocation, ResourceHandle<RenderTarget>> p_363476_) {
            ResourceHandle<RenderTarget> resourcehandle = this.getHandle(p_363476_);
            RenderTarget rendertarget = resourcehandle.get();
            rendertarget.setFilterMode(this.bilinear ? 9729 : 9728);
            p_361150_.bindSampler(this.samplerName + "Sampler", this.depthBuffer ? rendertarget.getDepthTextureId() : rendertarget.getColorTextureId());
            p_361150_.safeGetUniform(this.samplerName + "Size").set((float)rendertarget.width, (float)rendertarget.height);
        }

        @Override
        public void cleanup(Map<ResourceLocation, ResourceHandle<RenderTarget>> p_363639_) {
            if (this.bilinear) {
                this.getHandle(p_363639_).get().setFilterMode(9728);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record TextureInput(String samplerName, AbstractTexture texture, int width, int height) implements PostPass.Input {
        @Override
        public void addToPass(FramePass p_364568_, Map<ResourceLocation, ResourceHandle<RenderTarget>> p_370060_) {
        }

        @Override
        public void bindTo(CompiledShaderProgram p_369708_, Map<ResourceLocation, ResourceHandle<RenderTarget>> p_364335_) {
            p_369708_.bindSampler(this.samplerName + "Sampler", this.texture.getId());
            p_369708_.safeGetUniform(this.samplerName + "Size").set((float)this.width, (float)this.height);
        }
    }
}