package stroom.node.api;

public final class NodeCallUtil {
    private NodeCallUtil() {
    }

    public static boolean executeLocally(final NodeService nodeService,
                                         final NodeInfo nodeInfo,
                                         final String nodeName) {
        final String thisNodeName = nodeInfo.getThisNodeName();
        if (thisNodeName == null) {
            throw new RuntimeException("This node has no name");
        }

        // If this is the node that was contacted then just return our local info.
        return thisNodeName.equals(nodeName);
    }

    public static String getBaseEndpointUrl(final NodeService nodeService, final String nodeName) {
        String url = nodeService.getBaseEndpointUrl(nodeName);
        if (url == null || url.isBlank()) {
            throw new RuntimeException("Remote node '" + nodeName + "' has no URL set");
        }

        // A normal url is something like "http://fqdn:8080"

//        int index = url.lastIndexOf("/stroom/clustercall.rpc");
//        if (index != -1) {
//            url = url.substring(0, index);
//        }
//        index = url.lastIndexOf("/clustercall.rpc");
//        if (index != -1) {
//            url = url.substring(0, index);
//        }
        return url;
    }
}
