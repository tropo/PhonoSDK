LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_CFLAGS := -DHAVE_CONFIG_H  -DUSE_KISS_FFT
LOCAL_MODULE    := libphono-speex
LOCAL_ARM_MODE := thumb
LOCAL_SRC_FILES := \
speex_jni.c \
bits.c\
buffer.c\
cb_search.c\
exc_10_16_table.c\
exc_10_32_table.c\
exc_20_32_table.c\
exc_5_256_table.c\
exc_5_64_table.c\
exc_8_128_table.c\
fftwrap.c\
filterbank.c\
filters.c\
gain_table.c\
gain_table_lbr.c\
hexc_10_32_table.c\
hexc_table.c\
high_lsp_tables.c\
jitter.c\
kiss_fft.c\
kiss_fftr.c\
lpc.c\
lsp.c\
lsp_tables_nb.c\
ltp.c\
mdf.c\
modes.c\
modes_wb.c\
nb_celp.c\
preprocess.c\
quant_lsp.c\
resample.c\
sb_celp.c\
scal.c\
smallft.c\
speex.c\
speex_callbacks.c\
speex_header.c\
stereo.c\
vbr.c\
vq.c\
window.c

include $(BUILD_SHARED_LIBRARY)
