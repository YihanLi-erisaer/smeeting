#include <net.h>

// Qwen3-0.6B ncnn weights do not use GatedDeltaRule / ShortConv; the full gdr.cpp pulls in
// ncnn::Layer RTTI that the prebuilt libncnn.a does not provide. Keep registration as no-op.
void register_gdr_layers(ncnn::Net& /*net*/) {}
