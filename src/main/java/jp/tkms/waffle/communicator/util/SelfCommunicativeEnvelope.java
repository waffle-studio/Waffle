package jp.tkms.waffle.communicator.util;

import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.EnvelopeTransceiver;

import java.nio.file.Path;

public class SelfCommunicativeEnvelope extends Envelope {
  EnvelopeTransceiver transceiver;

  public SelfCommunicativeEnvelope(Path baseDirectory, EnvelopeTransceiver transceiver) {
    super(baseDirectory);
    this.transceiver = transceiver;
  }
}
