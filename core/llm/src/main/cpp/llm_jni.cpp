#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <memory>
#include <mutex>
#include <string>

#include "ncnn_llm_gpt.h"

#if NCNN_VULKAN
#include <gpu.h>
#endif

#define TAG "LLM-Native"

std::atomic<bool> g_ncnn_llm_abort{false};

static std::unique_ptr<ncnn_llm_gpt> g_model;
static std::mutex g_mutex;

#if NCNN_VULKAN
static std::atomic<bool> g_gpu_instance_created{false};
/** Set when [loadModelNative] succeeds; true means ncnn_llm uses Vulkan compute. */
static bool g_llm_inference_is_vulkan = false;
#endif

static size_t utf8_valid_prefix_len(const char* data, size_t len) {
    size_t i = 0;
    while (i < len) {
        auto c = static_cast<unsigned char>(data[i]);
        size_t need = 1;
        if (c < 0x80U) {
            i++;
            continue;
        }
        if ((c & 0xE0U) == 0xC0U) need = 2;
        else if ((c & 0xF0U) == 0xE0U) need = 3;
        else if ((c & 0xF8U) == 0xF0U) need = 4;
        else
            return i;
        if (i + need > len)
            return i;
        for (size_t j = 1; j < need; j++) {
            if ((static_cast<unsigned char>(data[i + j]) & 0xC0U) != 0x80U)
                return i;
        }
        i += need;
    }
    return len;
}

static size_t utf8_emit_prefix_len(const char* data, size_t len) {
    size_t vp = utf8_valid_prefix_len(data, len);
    if (vp > 0)
        return vp;
    if (len == 0)
        return 0;
    auto c0 = static_cast<unsigned char>(data[0]);
    size_t need = 0;
    if (c0 < 0x80U)
        need = 1;
    else if ((c0 & 0xE0U) == 0xC0U)
        need = 2;
    else if ((c0 & 0xF0U) == 0xE0U)
        need = 3;
    else if ((c0 & 0xF8U) == 0xF0U)
        need = 4;
    else
        return 1;
    if (len < need)
        return 0;
    return 1;
}

static jstring utf8_bytes_to_jstring(JNIEnv* env, const char* data, size_t len) {
    if (len == 0)
        return env->NewStringUTF("");
    jbyteArray jbytes = env->NewByteArray(static_cast<jsize>(len));
    if (!jbytes)
        return env->NewStringUTF("");
    env->SetByteArrayRegion(jbytes, 0, static_cast<jsize>(len), reinterpret_cast<const jbyte*>(data));

    jclass str_class = env->FindClass("java/lang/String");
    if (!str_class) {
        env->DeleteLocalRef(jbytes);
        return env->NewStringUTF("");
    }
    jmethodID ctor = env->GetMethodID(str_class, "<init>", "([BLjava/lang/String;)V");
    jstring enc = env->NewStringUTF("UTF-8");
    if (!ctor || !enc) {
        env->DeleteLocalRef(jbytes);
        env->DeleteLocalRef(str_class);
        if (enc)
            env->DeleteLocalRef(enc);
        return env->NewStringUTF("");
    }
    jstring out = (jstring)env->NewObject(str_class, ctor, jbytes, enc);
    env->DeleteLocalRef(jbytes);
    env->DeleteLocalRef(enc);
    env->DeleteLocalRef(str_class);
    return out ? out : env->NewStringUTF("");
}

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* /*vm*/, void* /*reserved*/) {
#if NCNN_VULKAN
    // Required before any ncnn::Net uses Vulkan in this .so (separate static libncnn from ASR).
    const int err = ncnn::create_gpu_instance();
    if (err == 0) {
        g_gpu_instance_created.store(true, std::memory_order_relaxed);
        __android_log_print(ANDROID_LOG_INFO, TAG, "ncnn create_gpu_instance ok, gpu_count=%d", ncnn::get_gpu_count());
    } else {
        __android_log_print(ANDROID_LOG_WARN, TAG, "ncnn create_gpu_instance failed (%d), LLM will use CPU only", err);
    }
#endif
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* /*vm*/, void* /*reserved*/) {
#if NCNN_VULKAN
    if (g_gpu_instance_created.exchange(false, std::memory_order_relaxed)) {
        ncnn::destroy_gpu_instance();
        __android_log_print(ANDROID_LOG_INFO, TAG, "ncnn destroy_gpu_instance");
    }
#endif
}

JNIEXPORT jboolean JNICALL
Java_com_stardazz_smeeting_core_llm_NcnnLlmBridge_loadModelNative(
        JNIEnv* env, jobject thiz, jstring model_path, jboolean use_vulkan, jint n_threads, jint vulkan_device) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_ncnn_llm_abort = false;
    g_model.reset();

    const char* path = env->GetStringUTFChars(model_path, nullptr);

    bool use_vk = use_vulkan != JNI_FALSE;
#if NCNN_VULKAN
    if (use_vk) {
        if (!g_gpu_instance_created.load(std::memory_order_relaxed) || ncnn::get_gpu_count() <= 0) {
            __android_log_print(ANDROID_LOG_WARN, TAG, "Vulkan requested but GPU not ready — using CPU");
            use_vk = false;
        }
    }
#else
    use_vk = false;
#endif

    __android_log_print(ANDROID_LOG_INFO, TAG, "Loading ncnn_llm from: %s (vulkan=%d threads=%d vulkan_dev=%d)",
                        path, use_vk ? 1 : 0, n_threads, vulkan_device);

    try {
        g_model = std::make_unique<ncnn_llm_gpt>(
            std::string(path),
            use_vk,
            static_cast<int>(n_threads),
            static_cast<int>(vulkan_device));
#if NCNN_VULKAN
        g_llm_inference_is_vulkan = use_vk;
#endif
    } catch (const std::exception& e) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "ncnn_llm load failed: %s", e.what());
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    } catch (...) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "ncnn_llm load failed (unknown)");
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }
    env->ReleaseStringUTFChars(model_path, path);
#if NCNN_VULKAN
    __android_log_print(ANDROID_LOG_INFO, TAG, "LLM inference backend: %s",
                        g_llm_inference_is_vulkan ? "GPU (Vulkan)" : "CPU");
#else
    __android_log_print(ANDROID_LOG_INFO, TAG, "LLM inference backend: CPU (ncnn built without Vulkan)");
#endif
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_stardazz_smeeting_core_llm_NcnnLlmBridge_releaseModelNative(JNIEnv* env, jobject thiz) {
    g_ncnn_llm_abort = true;
    std::lock_guard<std::mutex> lock(g_mutex);
    g_model.reset();
#if NCNN_VULKAN
    g_llm_inference_is_vulkan = false;
#endif
    __android_log_print(ANDROID_LOG_INFO, TAG, "ncnn_llm model released");
}

JNIEXPORT void JNICALL
Java_com_stardazz_smeeting_core_llm_NcnnLlmBridge_abortNative(JNIEnv* env, jobject thiz) {
    g_ncnn_llm_abort = true;
}

JNIEXPORT jstring JNICALL
Java_com_stardazz_smeeting_core_llm_NcnnLlmBridge_generateNative(
        JNIEnv* env, jobject thiz, jstring prompt_str, jint max_tokens, jint n_threads_unused) {
    (void)n_threads_unused;

    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_model) {
        return env->NewStringUTF("");
    }

#if NCNN_VULKAN
    __android_log_print(ANDROID_LOG_INFO, TAG, "LLM generate: backend=%s",
                        g_llm_inference_is_vulkan ? "GPU (Vulkan)" : "CPU");
#else
    __android_log_print(ANDROID_LOG_INFO, TAG, "LLM generate: backend=CPU");
#endif

    const char* prompt_cstr = env->GetStringUTFChars(prompt_str, nullptr);
    std::string prompt(prompt_cstr);
    env->ReleaseStringUTFChars(prompt_str, prompt_cstr);

    g_ncnn_llm_abort = false;

    jclass bridge_class = env->GetObjectClass(thiz);
    jmethodID on_token_mid = env->GetMethodID(bridge_class, "onTokenGenerated", "(Ljava/lang/String;)V");

    std::string result;
    std::string utf8_stream_buf;

    try {
        auto ctx = g_model->prefill(prompt);

        GenerateConfig cfg;
        cfg.max_new_tokens = max_tokens > 0 ? max_tokens : 512;
        cfg.temperature = 0.3f;
        cfg.top_p = 0.9f;
        cfg.top_k = 50;
        cfg.repetition_penalty = 1.1f;
        cfg.do_sample = 1;

        g_model->generate(ctx, cfg, [&](const std::string& piece) {
            result += piece;
            if (!on_token_mid)
                return;
            utf8_stream_buf += piece;
            for (;;) {
                size_t n = utf8_emit_prefix_len(utf8_stream_buf.data(), utf8_stream_buf.size());
                if (n == 0)
                    break;
                std::string chunk(utf8_stream_buf.data(), n);
                utf8_stream_buf.erase(0, n);
                jstring j_chunk = utf8_bytes_to_jstring(env, chunk.data(), chunk.size());
                env->CallVoidMethod(thiz, on_token_mid, j_chunk);
                env->DeleteLocalRef(j_chunk);
            }
        });

        if (on_token_mid && !utf8_stream_buf.empty()) {
            jstring j_tail = utf8_bytes_to_jstring(env, utf8_stream_buf.data(), utf8_stream_buf.size());
            env->CallVoidMethod(thiz, on_token_mid, j_tail);
            env->DeleteLocalRef(j_tail);
        }
    } catch (const std::exception& e) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "generate failed: %s", e.what());
        return env->NewStringUTF("");
    }

    return utf8_bytes_to_jstring(env, result.data(), result.size());
}

JNIEXPORT jboolean JNICALL
Java_com_stardazz_smeeting_core_llm_NcnnLlmBridge_isModelLoadedNative(JNIEnv* env, jobject thiz) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_model != nullptr ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_stardazz_smeeting_core_llm_NcnnLlmBridge_inferenceBackendNative(JNIEnv* env, jobject thiz) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_model) {
        return env->NewStringUTF("not_loaded");
    }
#if NCNN_VULKAN
    return env->NewStringUTF(g_llm_inference_is_vulkan ? "GPU (Vulkan)" : "CPU");
#else
    return env->NewStringUTF("CPU");
#endif
}

} // extern "C"
