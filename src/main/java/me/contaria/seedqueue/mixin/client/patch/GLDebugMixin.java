package me.contaria.seedqueue.mixin.client.patch;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gl.GlDebug;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;

@Mixin(GlDebug.class)
public abstract class GLDebugMixin {

    @Unique
    private static boolean suppressedOpenGLErrorId1281;

    @WrapOperation(
            method = "info",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"
            )
    )
    private static void suppressOpenGLDebugMessageSpam(Logger logger, String message, Object id, Object source, Object type, Object severity, Object msg, Operation<Void> original) {
        if (Objects.equals(id, 1281)) {
            if (!suppressedOpenGLErrorId1281) {
                original.call(logger, message + " Suppressing further log spam!", id, source, type, severity, msg);
                suppressedOpenGLErrorId1281 = true;
            }
            return;
        }
        original.call(logger, message, id, source, type, severity, msg);
    }
}
