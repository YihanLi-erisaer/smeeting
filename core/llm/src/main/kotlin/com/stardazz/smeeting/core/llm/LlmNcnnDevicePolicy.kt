package com.stardazz.smeeting.core.llm

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.util.Log

/**
 * Chooses ncnn Vulkan (GPU) vs CPU for the bundled Qwen3 ncnn model.
 *
 * **Default is CPU.** On many Android devices, Vulkan reports a GPU but
 * `vkCreateComputePipelines` fails with `VK_ERROR_INITIALIZATION_FAILED` (-3) for ncnn’s
 * compute pipelines (`new_pipeline failed` in logcat). The model may still “load”, then
 * [prefill] crashes. Only enable Vulkan after validating on target hardware.
 */
object LlmNcnnDevicePolicy {

    private const val TAG = "LlmNcnnDevicePolicy"

    /**
     * Set to true only after you confirm ncnn LLM runs without `vkCreateComputePipelines failed`
     * on your devices (Adreno/Mali checks apply only when this is true).
     */
    private const val ENABLE_LLM_VULKAN = false

    fun preferNcnnVulkan(): Boolean {
        if (!ENABLE_LLM_VULKAN) {
            Log.i(TAG, "ncnn LLM using CPU (ENABLE_LLM_VULKAN=false; Vulkan pipelines often fail on Android)")
            Log.i(TAG, "LLM inference (policy): CPU")
            return false
        }
        val renderer = glRendererString() ?: run {
            Log.i(TAG, "GL_RENDERER unavailable, using CPU for ncnn LLM")
            Log.i(TAG, "LLM inference (policy): CPU")
            return false
        }
        Log.i(TAG, "GL_RENDERER=$renderer")
        val r = renderer.lowercase()
        val useVk = r.contains("adreno") || r.contains("mali")
        Log.i(
            TAG,
            "LLM inference (policy): ${if (useVk) "GPU (Vulkan)" else "CPU"} (Adreno/Mali only)",
        )
        return useVk
    }

    private fun glRendererString(): String? {
        return try {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return null
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return null
            val attribList = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(display, attribList, 0, configs, 0, 1, numConfigs, 0)) {
                EGL14.eglTerminate(display)
                return null
            }
            val ctx = EGL14.eglCreateContext(
                display,
                configs[0],
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0,
            )
            if (ctx == EGL14.EGL_NO_CONTEXT) {
                EGL14.eglTerminate(display)
                return null
            }
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, ctx)
            val s = GLES20.glGetString(GLES20.GL_RENDERER)
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroyContext(display, ctx)
            EGL14.eglTerminate(display)
            s
        } catch (e: Exception) {
            Log.w(TAG, "glRendererString failed", e)
            null
        }
    }
}
