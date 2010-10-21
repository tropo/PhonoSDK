package com.voxeo.gordon.server;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.networkconnection.SdpPortManagerException;

public interface Bridge {

  void addRightListener(MediaEventListener<SdpPortManagerEvent> listener) throws MsControlException;

  void addLeftListener(MediaEventListener<SdpPortManagerEvent> listener) throws MsControlException;

  void generateSdpOfferRight() throws SdpPortManagerException, MsControlException;

  void processSdpAnswerRight(byte[] sdp) throws SdpPortManagerException, MsControlException;

  void processSdpOfferRight(byte[] sdp) throws SdpPortManagerException, MsControlException;

  void generateSdpOfferLeft() throws SdpPortManagerException, MsControlException;

  void processSdpAnswerLeft(byte[] sdp) throws SdpPortManagerException, MsControlException;

  void processSdpOfferLeft(byte[] sdp) throws SdpPortManagerException, MsControlException;

  void destroy();
}
