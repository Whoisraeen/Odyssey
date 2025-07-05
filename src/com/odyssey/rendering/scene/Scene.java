package com.odyssey.rendering.scene;

import java.util.Collections;
import java.util.List;

public class Scene {
    // Scene graph and management
    public List<RenderObject> getOpaqueObjects() { return Collections.emptyList(); }
    public List<RenderObject> getTransparentObjects() { return Collections.emptyList(); }
    public List<Light> getLights() { return Collections.emptyList(); }
    public List<RenderObject> getObjects() { return Collections.emptyList(); }
} 