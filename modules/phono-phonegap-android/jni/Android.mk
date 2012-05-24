LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libphono-speexw
LOCAL_SRC_FILES := \
speexw_jni.c \
bits.c		   hexc_10_32_table.c  mdf.c		  \
cb_search.c	   hexc_table.c        misc.c		  \
exc_10_16_table.c  high_lsp_tables.c   modes.c		  \
exc_10_32_table.c  jitter.c	       nb_celp.c	  \
exc_20_32_table.c  kiss_fft.c	       preprocess.c	  \
exc_5_256_table.c  kiss_fftr.c	       quant_lsp.c	  vbr.c \
exc_5_64_table.c   lbr_48k_tables.c    sb_celp.c	  vorbis_psy.c \
exc_8_128_table.c  lpc.c	       smallft.c	  vq.c \
fftwrap.c	   lsp.c	       speex.c		  window.c \
filters.c	   lsp_tables_nb.c     speex_callbacks.c \
gain_table.c	   ltp.c	       speex_header.c \
gain_table_lbr.c   math_approx.c       stereo.c  \


include $(BUILD_SHARED_LIBRARY)
