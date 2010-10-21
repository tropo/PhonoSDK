package com.voxeo.gordon.server;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.networkconnection.SdpPortManagerException;

public class MixerBridgeImpl implements Bridge {

  private MediaSession mediaSession;

  private MediaMixer mixer;

  private NetworkConnection rightNC;

  private NetworkConnection leftNC;

  public MixerBridgeImpl(MsControlFactory _msControlFactory) throws MsControlException {
    mediaSession = _msControlFactory.createMediaSession();
    mixer = mediaSession.createMediaMixer(MediaMixer.AUDIO);

    rightNC = mediaSession.createNetworkConnection(NetworkConnection.BASIC);
    rightNC.join(Direction.DUPLEX, mixer);

    leftNC = mediaSession.createNetworkConnection(NetworkConnection.BASIC);
    leftNC.join(Direction.DUPLEX, mixer);
  }

  public void addLeftListener(MediaEventListener<SdpPortManagerEvent> listener) throws MsControlException {
    leftNC.getSdpPortManager().addListener(listener);
  }

  public void addRightListener(MediaEventListener<SdpPortManagerEvent> listener) throws MsControlException {
    rightNC.getSdpPortManager().addListener(listener);
  }

  public void generateSdpOfferLeft() throws SdpPortManagerException, MsControlException {
    leftNC.getSdpPortManager().generateSdpOffer();
  }

  public void generateSdpOfferRight() throws SdpPortManagerException, MsControlException {
    rightNC.getSdpPortManager().generateSdpOffer();
  }

  public void processSdpAnswerLeft(byte[] sdp) throws SdpPortManagerException, MsControlException {
    leftNC.getSdpPortManager().processSdpAnswer(sdp);
  }

  public void processSdpAnswerRight(byte[] sdp) throws SdpPortManagerException, MsControlException {
    rightNC.getSdpPortManager().processSdpAnswer(sdp);
  }

  public void processSdpOfferLeft(byte[] sdp) throws SdpPortManagerException, MsControlException {
    leftNC.getSdpPortManager().processSdpOffer(sdp);
  }

  public void processSdpOfferRight(byte[] sdp) throws SdpPortManagerException, MsControlException {
    rightNC.getSdpPortManager().processSdpOffer(sdp);
  }

  public void destroy() {
    if (mediaSession != null) {
      mediaSession.release();
    }
  }
}
