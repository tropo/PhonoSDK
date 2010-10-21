package com.voxeo.gordon.server;

import java.io.File;
import java.util.Date;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.networkconnection.SdpPortManagerException;
import javax.media.mscontrol.resource.RTC;
import javax.servlet.ServletContext;

public class NCJoinBridgeImpl implements Bridge {
  private MediaSession mediaSession;

  private NetworkConnection rightNC;

  private NetworkConnection leftNC;

  private ServletContext _servletContext;

  Recorder recorder;

  public NCJoinBridgeImpl(MsControlFactory _msControlFactory, ServletContext servletContext) throws MsControlException {
    _servletContext = servletContext;

    mediaSession = _msControlFactory.createMediaSession();

    rightNC = mediaSession.createNetworkConnection(NetworkConnection.BASIC);

    leftNC = mediaSession.createNetworkConnection(NetworkConnection.BASIC);
    leftNC.join(Direction.DUPLEX, rightNC);
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

    MediaGroup group = mediaSession.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);

    leftNC.join(Direction.SEND, group);
    // construct record file location.
    final String path = _servletContext.getRealPath("test" + "_" + new Date().getTime() + "_Recording.au");
    File file = new File(path);
    final java.net.URI record = file.toURI();

    recorder = group.getRecorder();
    recorder.record(record, RTC.NO_RTC, Parameters.NO_PARAMETER);
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
      if (recorder != null) {
        recorder.stop();
      }

      mediaSession.release();
    }
  }
}
