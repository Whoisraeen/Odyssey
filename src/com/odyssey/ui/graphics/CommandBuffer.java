package com.odyssey.ui.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import java.util.ArrayList;
import java.util.List;

/**
 * CommandBuffer for recording and executing GPU commands
 * Provides deferred rendering capabilities similar to modern graphics APIs
 */
public class CommandBuffer {
    private static final String TAG = "CommandBuffer";
    
    private final List<RenderCommand> commands;
    private final List<StateCommand> stateCommands;
    private boolean isRecording;
    private boolean isExecuting;
    private String name;
    
    // Command types
    public enum CommandType {
        DRAW_ARRAYS,
        DRAW_ELEMENTS,
        BIND_TEXTURE,
        BIND_SHADER,
        BIND_VERTEX_ARRAY,
        BIND_BUFFER,
        SET_UNIFORM,
        SET_VIEWPORT,
        CLEAR,
        BLEND_STATE,
        DEPTH_STATE,
        SCISSOR_TEST
    }
    
    /**
     * Base class for render commands
     */
    public abstract static class RenderCommand {
        protected final CommandType type;
        protected final long timestamp;
        
        public RenderCommand(CommandType type) {
            this.type = type;
            this.timestamp = System.nanoTime();
        }
        
        public abstract void execute();
        
        public CommandType getType() {
            return type;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * State command for GPU state changes
     */
    public abstract static class StateCommand extends RenderCommand {
        public StateCommand(CommandType type) {
            super(type);
        }
    }
    
    /**
     * Draw arrays command
     */
    public static class DrawArraysCommand extends RenderCommand {
        private final int mode;
        private final int first;
        private final int count;
        
        public DrawArraysCommand(int mode, int first, int count) {
            super(CommandType.DRAW_ARRAYS);
            this.mode = mode;
            this.first = first;
            this.count = count;
        }
        
        @Override
        public void execute() {
            GL11.glDrawArrays(mode, first, count);
        }
    }
    
    /**
     * Draw elements command
     */
    public static class DrawElementsCommand extends RenderCommand {
        private final int mode;
        private final int count;
        private final int type;
        private final long indices;
        
        public DrawElementsCommand(int mode, int count, int type, long indices) {
            super(CommandType.DRAW_ELEMENTS);
            this.mode = mode;
            this.count = count;
            this.type = type;
            this.indices = indices;
        }
        
        @Override
        public void execute() {
            GL11.glDrawElements(mode, count, type, indices);
        }
    }
    
    /**
     * Bind texture command
     */
    public static class BindTextureCommand extends StateCommand {
        private final int target;
        private final int texture;
        
        public BindTextureCommand(int target, int texture) {
            super(CommandType.BIND_TEXTURE);
            this.target = target;
            this.texture = texture;
        }
        
        @Override
        public void execute() {
            GL11.glBindTexture(target, texture);
        }
    }
    
    /**
     * Bind shader command
     */
    public static class BindShaderCommand extends StateCommand {
        private final int program;
        
        public BindShaderCommand(int program) {
            super(CommandType.BIND_SHADER);
            this.program = program;
        }
        
        @Override
        public void execute() {
            GL20.glUseProgram(program);
        }
    }
    
    /**
     * Bind vertex array command
     */
    public static class BindVertexArrayCommand extends StateCommand {
        private final int vao;
        
        public BindVertexArrayCommand(int vao) {
            super(CommandType.BIND_VERTEX_ARRAY);
            this.vao = vao;
        }
        
        @Override
        public void execute() {
            GL30.glBindVertexArray(vao);
        }
    }
    
    /**
     * Bind buffer command
     */
    public static class BindBufferCommand extends StateCommand {
        private final int target;
        private final int buffer;
        
        public BindBufferCommand(int target, int buffer) {
            super(CommandType.BIND_BUFFER);
            this.target = target;
            this.buffer = buffer;
        }
        
        @Override
        public void execute() {
            GL15.glBindBuffer(target, buffer);
        }
    }
    
    /**
     * Set viewport command
     */
    public static class SetViewportCommand extends StateCommand {
        private final int x, y, width, height;
        
        public SetViewportCommand(int x, int y, int width, int height) {
            super(CommandType.SET_VIEWPORT);
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        @Override
        public void execute() {
            GL11.glViewport(x, y, width, height);
        }
    }
    
    /**
     * Clear command
     */
    public static class ClearCommand extends RenderCommand {
        private final int mask;
        private final float r, g, b, a;
        
        public ClearCommand(int mask, float r, float g, float b, float a) {
            super(CommandType.CLEAR);
            this.mask = mask;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
        
        @Override
        public void execute() {
            GL11.glClearColor(r, g, b, a);
            GL11.glClear(mask);
        }
    }
    
    /**
     * Blend state command
     */
    public static class BlendStateCommand extends StateCommand {
        private final boolean enabled;
        private final int srcFactor;
        private final int dstFactor;
        
        public BlendStateCommand(boolean enabled, int srcFactor, int dstFactor) {
            super(CommandType.BLEND_STATE);
            this.enabled = enabled;
            this.srcFactor = srcFactor;
            this.dstFactor = dstFactor;
        }
        
        @Override
        public void execute() {
            if (enabled) {
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(srcFactor, dstFactor);
            } else {
                GL11.glDisable(GL11.GL_BLEND);
            }
        }
    }
    
    /**
     * Constructor
     */
    public CommandBuffer() {
        this("CommandBuffer");
    }
    
    /**
     * Constructor with name
     */
    public CommandBuffer(String name) {
        this.name = name;
        this.commands = new ArrayList<>();
        this.stateCommands = new ArrayList<>();
        this.isRecording = false;
        this.isExecuting = false;
    }
    
    /**
     * Begin recording commands
     */
    public void begin() {
        if (isRecording) {
            throw new IllegalStateException("CommandBuffer is already recording");
        }
        if (isExecuting) {
            throw new IllegalStateException("CommandBuffer is currently executing");
        }
        
        isRecording = true;
        commands.clear();
        stateCommands.clear();
    }
    
    /**
     * End recording commands
     */
    public void end() {
        if (!isRecording) {
            throw new IllegalStateException("CommandBuffer is not recording");
        }
        
        isRecording = false;
    }
    
    /**
     * Execute all recorded commands
     */
    public void execute() {
        if (isRecording) {
            throw new IllegalStateException("Cannot execute while recording");
        }
        if (isExecuting) {
            throw new IllegalStateException("CommandBuffer is already executing");
        }
        
        isExecuting = true;
        
        try {
            // Execute state commands first
            for (StateCommand cmd : stateCommands) {
                cmd.execute();
            }
            
            // Then execute render commands
            for (RenderCommand cmd : commands) {
                cmd.execute();
            }
        } finally {
            isExecuting = false;
        }
    }
    
    /**
     * Add a command to the buffer
     */
    private void addCommand(RenderCommand command) {
        if (!isRecording) {
            throw new IllegalStateException("CommandBuffer is not recording");
        }
        
        if (command instanceof StateCommand) {
            stateCommands.add((StateCommand) command);
        } else {
            commands.add(command);
        }
    }
    
    // Command recording methods
    
    public void drawArrays(int mode, int first, int count) {
        addCommand(new DrawArraysCommand(mode, first, count));
    }
    
    public void drawElements(int mode, int count, int type, long indices) {
        addCommand(new DrawElementsCommand(mode, count, type, indices));
    }
    
    public void bindTexture(int target, int texture) {
        addCommand(new BindTextureCommand(target, texture));
    }
    
    public void bindShader(int program) {
        addCommand(new BindShaderCommand(program));
    }
    
    public void bindVertexArray(int vao) {
        addCommand(new BindVertexArrayCommand(vao));
    }
    
    public void bindBuffer(int target, int buffer) {
        addCommand(new BindBufferCommand(target, buffer));
    }
    
    public void setViewport(int x, int y, int width, int height) {
        addCommand(new SetViewportCommand(x, y, width, height));
    }
    
    public void clear(int mask, float r, float g, float b, float a) {
        addCommand(new ClearCommand(mask, r, g, b, a));
    }
    
    public void setBlendState(boolean enabled, int srcFactor, int dstFactor) {
        addCommand(new BlendStateCommand(enabled, srcFactor, dstFactor));
    }
    
    /**
     * Draw rectangle command implementation
     */
    private static class DrawRectCommand extends RenderCommand {
        private final float x, y, width, height, cornerRadius;
        private final int color;
        
        public DrawRectCommand(float x, float y, float width, float height, int color, float cornerRadius) {
            super(CommandType.DRAW_ARRAYS);
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
            this.cornerRadius = cornerRadius;
        }
        
        @Override
        public void execute() {
            // Extract color components
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = ((color >> 24) & 0xFF) / 255.0f;
            
            // TODO: Implement actual rectangle rendering with vertex data
            // This is a placeholder for the actual rendering logic
            GL11.glColor4f(r, g, b, a);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x, y);
            GL11.glVertex2f(x + width, y);
            GL11.glVertex2f(x + width, y + height);
            GL11.glVertex2f(x, y + height);
            GL11.glEnd();
        }
        
        @Override
        public String toString() {
            return String.format("DrawRectCommand[x=%.1f, y=%.1f, w=%.1f, h=%.1f, color=0x%08X]", 
                    x, y, width, height, color);
        }
    }
    
    /**
     * Draw text command implementation
     */
    private static class DrawTextCommand extends RenderCommand {
        private final String text;
        private final float x, y;
        private final int color;
        
        public DrawTextCommand(String text, float x, float y, int color) {
            super(CommandType.DRAW_ARRAYS);
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
        }
        
        @Override
        public void execute() {
            // Extract color components
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            
            // TODO: Implement actual text rendering with font atlas
            // This is a placeholder for the actual text rendering logic
            GL11.glColor3f(r, g, b);
            // Text rendering would go here
        }
        
        @Override
        public String toString() {
            return String.format("DrawTextCommand[text='%s', x=%.1f, y=%.1f, color=0x%08X]", 
                    text, x, y, color);
        }
    }
    
    /**
     * Record a rectangle draw command
     */
    public void recordDrawRect(float x, float y, float width, float height, int color, float cornerRadius) {
        if (!isRecording) {
            throw new IllegalStateException("CommandBuffer is not recording");
        }
        commands.add(new DrawRectCommand(x, y, width, height, color, cornerRadius));
    }
    
    /**
     * Record a text draw command
     */
    public void recordDrawText(String text, float x, float y, int color) {
        if (!isRecording) {
            throw new IllegalStateException("CommandBuffer is not recording");
        }
        commands.add(new DrawTextCommand(text, x, y, color));
    }
    
    /**
     * Reset the command buffer
     */
    public void reset() {
        if (isRecording || isExecuting) {
            throw new IllegalStateException("Cannot reset while recording or executing");
        }
        
        commands.clear();
        stateCommands.clear();
    }
    
    /**
     * Get the number of commands
     */
    public int getCommandCount() {
        return commands.size() + stateCommands.size();
    }
    
    /**
     * Check if recording
     */
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * Check if executing
     */
    public boolean isExecuting() {
        return isExecuting;
    }
    
    /**
     * Get the name of this command buffer
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of this command buffer
     */
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return String.format("CommandBuffer[name=%s, commands=%d, recording=%b, executing=%b]", 
                name, getCommandCount(), isRecording, isExecuting);
    }
}