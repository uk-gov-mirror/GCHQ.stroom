/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package stroom.node.mock;

import stroom.node.api.FindNodeCriteria;
import stroom.node.api.NodeService;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.inject.Singleton;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

/**
 * Mock class that manages one node.
 */
@Singleton
public class MockNodeService implements NodeService {

    private MockNodeInfo nodeInfo = new MockNodeInfo();
    private String lastUrl = null;

    @Override
    public String getBaseEndpointUrl(final String nodeName) {
        return null;
    }

    @Override
    public boolean isEnabled(final String nodeName) {
        return nodeName.equals(nodeInfo.getThisNodeName());
    }

    @Override
    public int getPriority(final String nodeName) {
        if (nodeName.equals(nodeInfo.getThisNodeName())) {
            return 1;
        }
        return 0;
    }

    @Override
    public List<String> getEnabledNodesByPriority() {
        return Collections.singletonList(nodeInfo.getThisNodeName());
    }

    @Override
    public List<String> findNodeNames(final FindNodeCriteria criteria) {
        return Collections.singletonList(nodeInfo.getThisNodeName());
    }

    @Override
    public <T_RESP> T_RESP remoteRestResult(final String nodeName,
                                            final Supplier<String> fullPathSupplier,
                                            final Supplier<T_RESP> localSupplier,
                                            final Function<Builder, Response> responseBuilderFunc,
                                            final Function<Response, T_RESP> responseMapper) {
        // Always return the value from the local supplier on this node
        // TestNodeServiceImpl tests calling local vs remote

        // Capture the path that it would use for a remote call
        lastUrl = fullPathSupplier.get();
        return localSupplier.get();
    }

    @Override
    public void remoteRestCall(final String nodeName,
                               final Supplier<String> fullPathSupplier,
                               final Runnable localRunnable,
                               final Function<Builder, Response> responseBuilderFunc) {
        // Always run the local runnable on this node
        // TestNodeServiceImpl tests calling local vs remote

        // Capture the path that it would use for a remote call
        lastUrl = fullPathSupplier.get();
        localRunnable.run();
    }

    public String getLastUrl() {
        return lastUrl;
    }
}
