#include <sys/types.h>
#include "com_phono_audio_codec_g722_NativeG722Codec.h"
#include "g722.h"
struct codec {
    g722_encode_state_t encoder_st;
    g722_decode_state_t decoder_st;
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
 * Class:     com_phono_audio_codec_g722_NativeG722Codec
 * Method:    initCodec
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_phono_audio_codec_g722_NativeG722Codec_initCodec
  (JNIEnv *env , jobject this){
	
	jbyteArray jcodec;
	struct codec *co;
	
	
	jcodec = (*env)->NewByteArray(env, sizeof(struct codec));
	co = getCodec(env,jcodec);
        g722_encode_init(&(co->encoder_st), 64000, 0);
        g722_decode_init(&(co->decoder_st), 64000, 0);
	
	releaseCodec(env,jcodec,co);
	
	return jcodec;
}

/*
 * Class:     com_phono_audio_codec_g722_NativeG722Codec
 * Method:    g722Encode
 * Signature: ([B[S[B)V
 */
JNIEXPORT void JNICALL Java_com_phono_audio_codec_g722_NativeG722Codec_g722Encode
  (JNIEnv *env, jobject this, jbyteArray jcodec, jshortArray jaudio, jbyteArray jwire ){

	
	
	  struct codec *co;
	  jint nbBits = 0;
	  jshort *ip;
	  jbyte *offs;
	  
	  // memory faffing
	  co = getCodec(env,jcodec);
	  ip =  (*env)->GetShortArrayElements(env, jaudio, 0);
	  offs = (*env)->GetByteArrayElements(env, jwire, 0);
	  
	  g722_encode(&(co->encoder_st), offs,ip, 320);
	  // memory unfaffing
	  
	  (*env)->ReleaseShortArrayElements(env, jaudio, ip, 0);
	  (*env)->ReleaseByteArrayElements(env, jwire, offs, 0);
	  releaseCodec(env,jcodec,co);
}

/*
 * Class:     com_phono_audio_codec_g722_NativeG722Codec
 * Method:    g722Decode
 * Signature: ([B[B[S)V
 */
JNIEXPORT void JNICALL Java_com_phono_audio_codec_g722_NativeG722Codec_g722Decode
  (JNIEnv *env, jobject this , jbyteArray jcodec , jbyteArray jwire , jshortArray jaudio) {
	  
	  struct codec *co;
	  jshort *op;
	  jbyte *offs;
	  
	  // memory faffing
	  co = getCodec(env,jcodec);
	  op =  (*env)->GetShortArrayElements(env, jaudio, 0);
	  offs = (*env)->GetByteArrayElements(env, jwire, 0);
          g722_decode(&(co->decoder_st), op, offs, 160);
	  
	  // memory unfaffing
	  
	  (*env)->ReleaseShortArrayElements(env, jaudio, op, 0);
	  (*env)->ReleaseByteArrayElements(env, jwire, offs, 0);
	  releaseCodec(env,jcodec,co);
}

/*
 * Class:     com_phono_audio_codec_g722_NativeG722Codec
 * Method:    freeCodec
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_com_phono_audio_codec_g722_NativeG722Codec_freeCodec
  (JNIEnv *env, jobject this , jbyteArray jcodec){
}
