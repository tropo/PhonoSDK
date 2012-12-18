#include <sys/types.h>
#include <strings.h>
#include <alloca.h>
#include "speex/speex.h"
#include "com_phono_codecs_speex_NativeSpeexCodec.h"

struct codec {
    void * encoder_st;
    void * decoder_st;
        SpeexBits eBits;
        SpeexBits dBits;

}  ;

struct codec *getCodec(JNIEnv *env, jbyteArray jcodec){

     struct codec  *co;

     co = (struct codec *) (*env)->GetByteArrayElements(env, jcodec, 0);
     return (co);

}

void * releaseCodec(JNIEnv * env, jbyteArray jcodec, struct codec * co){
     (*env)->ReleaseByteArrayElements(env, jcodec, (jbyte *) co, 0);
}

/*
 * Method:    initCodec
 */
JNIEXPORT jbyteArray JNICALL Java_com_phono_codecs_speex_NativeSpeexCodec_initCodec
  (JNIEnv *env , jobject this,jint wide, jint qual, jint comp, jint rate){
	
	jbyteArray jcodec;
	struct codec *co;
	
	
	jcodec = (*env)->NewByteArray(env, sizeof(struct codec));
	co = getCodec(env,jcodec);
        int tmp=0;
        if ( wide == 1) {
           co->decoder_st = speex_decoder_init(&speex_wb_mode);
           co->encoder_st = speex_encoder_init(&speex_wb_mode);
        } else {
           co->decoder_st = speex_decoder_init(&speex_nb_mode);
           co->encoder_st = speex_encoder_init(&speex_nb_mode);
        }
        speex_encoder_ctl(co->encoder_st, SPEEX_SET_VBR, &tmp);
        tmp=qual;
        speex_encoder_ctl(co->encoder_st, SPEEX_SET_QUALITY, &tmp);
        tmp=comp;
        speex_encoder_ctl(co->encoder_st, SPEEX_SET_COMPLEXITY, &tmp);
        tmp = rate;
        speex_encoder_ctl(co->encoder_st,SPEEX_SET_SAMPLING_RATE,&tmp);
        speex_bits_init(&(co->eBits));
        speex_bits_init(&(co->dBits));

	
	releaseCodec(env,jcodec,co);
	
	return jcodec;
}

/*
 * Method:    speexEncode
 * Signature: ([B[S[B)V
 */
JNIEXPORT jbyteArray JNICALL Java_com_phono_codecs_speex_NativeSpeexCodec_speexEncode
  (JNIEnv *env, jobject this, jbyteArray jcodec, jshortArray jaudio ){

	
	
	  struct codec *co;
	  jint nbBits = 0;
	  jshort *ip;
	  jbyte *offs;
	  char * wire;
 
	  // memory faffing
	  co = getCodec(env,jcodec);
	  ip =  (*env)->GetShortArrayElements(env, jaudio, 0);
          speex_bits_reset(&(co->eBits));
          speex_encode_int(co->encoder_st, ip, &(co->eBits));
          wire = alloca(160);
          int nbBytes = speex_bits_write(&(co->eBits), (char*) wire, 160);

          jbyteArray jwire = (*env)->NewByteArray(env, nbBytes);
	  offs = (*env)->GetByteArrayElements(env, jwire, 0);
	  memcpy(offs,wire,nbBytes);

	  // memory unfaffing
	  
	  (*env)->ReleaseShortArrayElements(env, jaudio, ip, 0);
	  (*env)->ReleaseByteArrayElements(env, jwire, offs, 0);
	  releaseCodec(env,jcodec,co);
          return jwire;
}

/*
 * Method:    speexDecode
 * Signature: ([B[B[S)V
 */
JNIEXPORT void JNICALL Java_com_phono_codecs_speex_NativeSpeexCodec_speexDecode
  (JNIEnv *env, jobject this , jbyteArray jcodec , jbyteArray jwire , jshortArray jaudio) {
	  
	  struct codec *co;
	  jshort *op;
	  jbyte *offs;
          jsize wlen =0;
	  
	  // memory faffing
	  co = getCodec(env,jcodec);
	  op =  (*env)->GetShortArrayElements(env, jaudio, 0);
	  offs = (*env)->GetByteArrayElements(env, jwire, 0);
          wlen = (*env)->GetArrayLength(env, jwire);
          speex_bits_reset(&(co->dBits));
          speex_bits_read_from(&(co->dBits), (char *) offs,wlen);
          speex_decode_int(co->decoder_st, &(co->dBits), op);
	  
	  // memory unfaffing
	  
	  (*env)->ReleaseShortArrayElements(env, jaudio, op, 0);
	  (*env)->ReleaseByteArrayElements(env, jwire, offs, 0);
	  releaseCodec(env,jcodec,co);
}

/*
 * Method:    freeCodec
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_com_phono_codecs_speex_NativeSpeexCodec_freeCodec
  (JNIEnv *env, jobject this , jbyteArray jcodec){
}
