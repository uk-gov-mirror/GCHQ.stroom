package stroom.proxy.repo;

import stroom.receive.common.StreamHandlers;

import java.util.List;

public interface ForwardStreamHandlers {

    List<String> getDestinationNames();

    StreamHandlers getProvider(String forwardUrl);
}
