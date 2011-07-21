LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libg722
LOCAL_SRC_FILES := \
g722_decode.c \
g722_encode.c \
g722_jni.c 


include $(BUILD_SHARED_LIBRARY)
