package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.ModSetting;
import net.minecraft.client.util.MacWindowUtil;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cubes Without Borders: Vollbild als rahmenloses Fenster in Monitorgroesse statt exklusivem Vollbild.
 * So geht Alt+Tab ohne Flackern und ohne dass Minecraft sich minimiert.
 * Nachgebaut nach Cubes Without Borders (Kira NT, MIT), nur der Kern fuer Windows/Linux.
 */
@Mixin(Window.class)
public abstract class WindowMixin {
    @Shadow
    @Final
    private MonitorTracker monitorTracker;

    @Shadow
    @Final
    private long handle;

    @Shadow
    private boolean fullscreen;

    @Shadow
    private int x;

    @Shadow
    private int y;

    @Shadow
    private int width;

    @Shadow
    private int height;

    @Shadow
    private int windowedX;

    @Shadow
    private int windowedY;

    @Shadow
    private int windowedWidth;

    @Shadow
    private int windowedHeight;

    @Inject(method = "updateWindowRegion", at = @At("HEAD"), cancellable = true, require = 0)
    private void arnestyles$borderlessFullscreen(CallbackInfo ci) {
        boolean borderless = GLFW.glfwGetWindowAttrib(this.handle, GLFW.GLFW_DECORATED) == GLFW.GLFW_FALSE;
        if (!this.fullscreen) {
            // Zurueck ins Fenster: Rahmen wieder an, Position und Groesse stellt Minecraft selbst wieder her
            if (borderless) {
                GLFW.glfwSetWindowAttrib(this.handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
            }
            return;
        }
        if (!ModSetting.Toggle.BORDERLESS_FULLSCREEN.isActive() || MacWindowUtil.IS_MAC) {
            if (borderless) {
                GLFW.glfwSetWindowAttrib(this.handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
            }
            return;
        }
        Monitor monitor = this.monitorTracker.getMonitor((Window) (Object) this);
        if (monitor == null) {
            return;
        }
        boolean exclusive = GLFW.glfwGetWindowMonitor(this.handle) != 0L;
        if (!exclusive && !borderless) {
            // Aus dem normalen Fenster kommend: Groesse und Position fuer spaeter merken
            this.windowedX = this.x;
            this.windowedY = this.y;
            this.windowedWidth = this.width;
            this.windowedHeight = this.height;
        }
        VideoMode mode = monitor.getCurrentVideoMode();
        this.x = monitor.getViewportX();
        this.y = monitor.getViewportY();
        this.width = mode.getWidth();
        this.height = mode.getHeight();
        GLFW.glfwSetWindowAttrib(this.handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, GLFW.GLFW_DONT_CARE);
        ci.cancel();
    }
}
