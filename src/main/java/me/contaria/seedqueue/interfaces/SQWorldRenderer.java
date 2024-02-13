package me.contaria.seedqueue.interfaces;

import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

public interface SQWorldRenderer {

    void seedQueue$buildChunks(MatrixStack matrices, Camera camera, Matrix4f projectionMatrix);
}
