#include <jni.h>
#include <string>
#include <android/log.h>
#include <mutex>
#include <atomic>
#include <vector>

#include "llama.h"

#define TAG "LLM-Native"

static llama_model* g_model = nullptr;
static llama_sampler* g_sampler = nullptr;
static std::mutex g_mutex;
static std::atomic<bool> g_abort(false);
static JavaVM* g_jvm = nullptr;

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    llama_backend_init();
    __android_log_print(ANDROID_LOG_INFO, TAG, "llama.cpp backend initialized");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_sampler) {
        llama_sampler_free(g_sampler);
        g_sampler = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    llama_backend_free();
}

JNIEXPORT jboolean JNICALL
Java_com_stardazz_smeeting_core_llm_LlamaCppBridge_loadModelNative(
        JNIEnv* env, jobject thiz, jstring model_path, jint n_threads) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (g_model) {
        __android_log_print(ANDROID_LOG_WARN, TAG, "Model already loaded, releasing first");
        if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
        llama_model_free(g_model);
        g_model = nullptr;
    }

    const char* path = env->GetStringUTFChars(model_path, nullptr);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Loading model from: %s", path);

    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = true;

    g_model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (!g_model) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to load model");
        return JNI_FALSE;
    }

    g_sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(g_sampler, llama_sampler_init_temp(0.3f));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_dist(0));

    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Model loaded, n_vocab=%d",
                        llama_vocab_n_tokens(vocab));
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_stardazz_smeeting_core_llm_LlamaCppBridge_releaseModelNative(
        JNIEnv* env, jobject thiz) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_abort = true;
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    __android_log_print(ANDROID_LOG_INFO, TAG, "Model released");
}

JNIEXPORT void JNICALL
Java_com_stardazz_smeeting_core_llm_LlamaCppBridge_abortNative(
        JNIEnv* env, jobject thiz) {
    g_abort = true;
}

JNIEXPORT jstring JNICALL
Java_com_stardazz_smeeting_core_llm_LlamaCppBridge_generateNative(
        JNIEnv* env, jobject thiz, jstring prompt_str, jint max_tokens, jint n_threads) {

    if (!g_model || !g_sampler) {
        return env->NewStringUTF("");
    }

    const char* prompt_cstr = env->GetStringUTFChars(prompt_str, nullptr);
    std::string prompt(prompt_cstr);
    env->ReleaseStringUTFChars(prompt_str, prompt_cstr);

    g_abort = false;

    const llama_vocab* vocab = llama_model_get_vocab(g_model);

    int n_prompt_max = prompt.size() + 256;
    std::vector<llama_token> tokens(n_prompt_max);
    int n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.size(),
                                  tokens.data(), n_prompt_max, true, true);
    if (n_tokens < 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Tokenization failed");
        return env->NewStringUTF("");
    }
    tokens.resize(n_tokens);

    int n_ctx = n_tokens + max_tokens + 64;
    if (n_ctx > 4096) n_ctx = 4096;

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = n_ctx;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;
    ctx_params.n_batch = 512;
    ctx_params.no_perf = true;

    llama_context* ctx = llama_init_from_model(g_model, ctx_params);
    if (!ctx) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to create context");
        return env->NewStringUTF("");
    }

    llama_batch batch = llama_batch_get_one(tokens.data(), n_tokens);
    if (llama_decode(ctx, batch) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Prompt decode failed");
        llama_free(ctx);
        return env->NewStringUTF("");
    }

    jclass bridge_class = env->GetObjectClass(thiz);
    jmethodID on_token_mid = env->GetMethodID(bridge_class, "onTokenGenerated",
                                               "(Ljava/lang/String;)V");

    std::string result;

    for (int i = 0; i < max_tokens && !g_abort; i++) {
        llama_token new_token = llama_sampler_sample(g_sampler, ctx, -1);

        if (llama_vocab_is_eog(vocab, new_token)) {
            break;
        }

        char buf[256];
        int len = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, true);
        if (len > 0) {
            std::string piece(buf, len);
            result += piece;

            if (on_token_mid) {
                jstring j_piece = env->NewStringUTF(piece.c_str());
                env->CallVoidMethod(thiz, on_token_mid, j_piece);
                env->DeleteLocalRef(j_piece);
            }
        }

        llama_batch next_batch = llama_batch_get_one(&new_token, 1);
        if (llama_decode(ctx, next_batch) != 0) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Decode step failed at token %d", i);
            break;
        }
    }

    llama_free(ctx);
    llama_sampler_reset(g_sampler);

    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_stardazz_smeeting_core_llm_LlamaCppBridge_isModelLoadedNative(
        JNIEnv* env, jobject thiz) {
    return g_model != nullptr ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
