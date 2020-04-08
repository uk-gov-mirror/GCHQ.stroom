package stroom.config.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonRootName;
import stroom.activity.impl.db.ActivityConfig;
import stroom.annotation.impl.AnnotationConfig;
import stroom.authentication.config.AuthenticationConfig;
import stroom.cluster.api.ClusterConfig;
import stroom.cluster.lock.impl.db.ClusterLockConfig;
import stroom.cluster.task.impl.ClusterTaskConfig;
import stroom.config.common.ApiGatewayConfig;
import stroom.config.common.CommonDbConfig;
import stroom.core.benchmark.BenchmarkClusterConfig;
import stroom.core.db.CoreConfig;
import stroom.core.receive.ProxyAggregationConfig;
import stroom.core.receive.ReceiveDataConfig;
import stroom.dashboard.impl.DashboardConfig;
import stroom.dashboard.impl.datasource.DataSourceUrlConfig;
import stroom.docstore.impl.db.DocStoreConfig;
import stroom.explorer.impl.db.ExplorerConfig;
import stroom.feed.impl.FeedConfig;
import stroom.importexport.impl.ContentPackImportConfig;
import stroom.importexport.impl.ExportConfig;
import stroom.index.impl.IndexConfig;
import stroom.index.impl.selection.VolumeConfig;
import stroom.job.impl.JobSystemConfig;
import stroom.kafka.impl.KafkaConfig;
import stroom.lifecycle.impl.LifecycleConfig;
import stroom.node.impl.NodeConfig;
import stroom.pipeline.PipelineConfig;
import stroom.processor.impl.ProcessorConfig;
import stroom.search.impl.SearchConfig;
import stroom.search.solr.SolrConfig;
import stroom.searchable.impl.SearchableConfig;
import stroom.security.impl.SecurityConfig;
import stroom.security.impl.ValidationSeverity;
import stroom.servicediscovery.impl.ServiceDiscoveryConfig;
import stroom.storedquery.impl.StoredQueryConfig;
import stroom.ui.config.shared.UiConfig;
import stroom.util.io.PathConfig;
import stroom.util.shared.AbstractConfig;

import javax.inject.Singleton;
import javax.validation.constraints.AssertTrue;

@JsonRootName(AppConfig.NAME)
@Singleton
public class AppConfig extends AbstractConfig {

    public static final String NAME = "stroom";

    public static final String PROP_NAME_HALT_BOOT_ON_CONFIG_VALIDATION_FAILURE = "haltBootOnConfigValidationFailure";
    public static final String PROP_NAME_ACTIVITY = "activity";
    public static final String PROP_NAME_ANNOTATION = "annotation";
    public static final String PROP_NAME_API_GATEWAY = "apiGateway";
    public static final String PROP_NAME_AUTHENTICATION = "authentication";
    public static final String PROP_NAME_BENCHMARK = "benchmark";
    public static final String PROP_NAME_CLUSTER = "cluster";
    public static final String PROP_NAME_CLUSTER_LOCK = "clusterLock";
    public static final String PROP_NAME_CLUSTER_TASK = "clusterTask";
    public static final String PROP_NAME_COMMON_DB_DETAILS = "commonDbDetails";
    public static final String PROP_NAME_CONTENT_PACK_IMPORT = "contentPackImport";
    public static final String PROP_NAME_CORE = "core";
    public static final String PROP_NAME_DASHBOARD = "dashboard";
    public static final String PROP_NAME_DATA = "data";
    public static final String PROP_NAME_DATA_SOURCE_URL = "dataSourceUrl";
    public static final String PROP_NAME_DOCSTORE = "docstore";
    public static final String PROP_NAME_EXPLORER = "explorer";
    public static final String PROP_NAME_EXPORT = "export";
    public static final String PROP_NAME_FEED = "feed";
    public static final String PROP_NAME_INDEX = "index";
    public static final String PROP_NAME_JOB = "job";
    public static final String PROP_NAME_KAFKA = "kafka";
    public static final String PROP_NAME_LIFECYCLE = "lifecycle";
    public static final String PROP_NAME_NODE = "node";
    public static final String PROP_NAME_PATH = "path";
    public static final String PROP_NAME_PIPELINE = "pipeline";
    public static final String PROP_NAME_PROCESSOR = "processor";
    public static final String PROP_NAME_PROPERTIES = "properties";
    public static final String PROP_NAME_PROXY_AGGREGATION = "proxyAggregation";
    public static final String PROP_NAME_QUERY_HISTORY = "queryHistory";
    public static final String PROP_NAME_RECEIVE = "receive";
    public static final String PROP_NAME_SEARCH = "search";
    public static final String PROP_NAME_SEARCHABLE = "searchable";
    public static final String PROP_NAME_SECURITY = "security";
    public static final String PROP_NAME_SERVICE_DISCOVERY = "serviceDiscovery";
    public static final String PROP_NAME_SESSION_COOKIE = "sessionCookie";
    public static final String PROP_NAME_SOLR = "solr";
    public static final String PROP_NAME_STATISTICS = "statistics";
    public static final String PROP_NAME_UI = "ui";
    public static final String PROP_NAME_VOLUMES = "volumes";

    private boolean haltBootOnConfigValidationFailure = true;

    private ActivityConfig activityConfig = new ActivityConfig();
    private AnnotationConfig annotationConfig = new AnnotationConfig();
    private ApiGatewayConfig apiGatewayConfig = new ApiGatewayConfig();
    private AuthenticationConfig authenticationConfig = new AuthenticationConfig();
    private BenchmarkClusterConfig benchmarkClusterConfig = new BenchmarkClusterConfig();
    private ClusterConfig clusterConfig = new ClusterConfig();
    private ClusterLockConfig clusterLockConfig = new ClusterLockConfig();
    private ClusterTaskConfig clusterTaskConfig = new ClusterTaskConfig();
    private CommonDbConfig commonDbConfig = new CommonDbConfig();
    private ContentPackImportConfig contentPackImportConfig = new ContentPackImportConfig();
    private CoreConfig coreConfig = new CoreConfig();
    private DashboardConfig dashboardConfig = new DashboardConfig();
    private DataConfig dataConfig = new DataConfig();
    private DataSourceUrlConfig dataSourceUrlConfig = new DataSourceUrlConfig();
    private DocStoreConfig docStoreConfig = new DocStoreConfig();
    private ExplorerConfig explorerConfig = new ExplorerConfig();
    private ExportConfig exportConfig = new ExportConfig();
    private FeedConfig feedConfig = new FeedConfig();
    private IndexConfig indexConfig = new IndexConfig();
    private JobSystemConfig jobSystemConfig = new JobSystemConfig();
    private KafkaConfig kafkaConfig = new KafkaConfig();
    private LifecycleConfig lifecycleConfig = new LifecycleConfig();
    private NodeConfig nodeConfig = new NodeConfig();
    private PathConfig pathConfig = new PathConfig();
    private PipelineConfig pipelineConfig = new PipelineConfig();
    private ProcessorConfig processorConfig = new ProcessorConfig();
    private PropertyServiceConfig propertyServiceConfig = new PropertyServiceConfig();
    private ProxyAggregationConfig proxyAggregationConfig = new ProxyAggregationConfig();
    private ReceiveDataConfig receiveDataConfig = new ReceiveDataConfig();
    private SearchConfig searchConfig = new SearchConfig();
    private SearchableConfig searchableConfig = new SearchableConfig();
    private SecurityConfig securityConfig = new SecurityConfig();
    private ServiceDiscoveryConfig serviceDiscoveryConfig = new ServiceDiscoveryConfig();
    private SessionCookieConfig sessionCookieConfig = new SessionCookieConfig();
    private SolrConfig solrConfig = new SolrConfig();
    private StatisticsConfig statisticsConfig = new StatisticsConfig();
    private StoredQueryConfig storedQueryConfig = new StoredQueryConfig();
    private UiConfig uiConfig = new UiConfig();
    private VolumeConfig volumeConfig = new VolumeConfig();

    @AssertTrue(
        message = "stroom." + PROP_NAME_HALT_BOOT_ON_CONFIG_VALIDATION_FAILURE + " is set to false. If there is " +
            "invalid configuration the system may behave in unexpected ways. This setting is not advised.",
        payload = ValidationSeverity.Warning.class)
    @JsonProperty(PROP_NAME_HALT_BOOT_ON_CONFIG_VALIDATION_FAILURE)
    public boolean isHaltBootOnConfigValidationFailure() {
        return haltBootOnConfigValidationFailure;
    }

    @SuppressWarnings("unused")
    public void setHaltBootOnConfigValidationFailure(final boolean haltBootOnConfigValidationFailure) {
        this.haltBootOnConfigValidationFailure = haltBootOnConfigValidationFailure;
    }

    @JsonProperty(PROP_NAME_ACTIVITY)
    public ActivityConfig getActivityConfig() {
        return activityConfig;
    }

    @SuppressWarnings("unused")
    public void setActivityConfig(final ActivityConfig activityConfig) {
        this.activityConfig = activityConfig;
    }

    @JsonProperty(PROP_NAME_ANNOTATION)
    public AnnotationConfig getAnnotationConfig() {
        return annotationConfig;
    }

    @SuppressWarnings("unused")
    public void setAnnotationConfig(final AnnotationConfig annotationConfig) {
        this.annotationConfig = annotationConfig;
    }

    @JsonProperty(PROP_NAME_API_GATEWAY)
    public ApiGatewayConfig getApiGatewayConfig() {
        return apiGatewayConfig;
    }

    @SuppressWarnings("unused")
    public void setApiGatewayConfig(final ApiGatewayConfig apiGatewayConfig) {
        this.apiGatewayConfig = apiGatewayConfig;
    }

    @JsonProperty(PROP_NAME_AUTHENTICATION)
    public AuthenticationConfig getAuthenticationConfig() {
        return authenticationConfig;
    }

    @SuppressWarnings("unused")
    public void setAuthenticationConfig(final AuthenticationConfig authenticationConfig) {
        this.authenticationConfig = authenticationConfig;
    }

    @JsonProperty(PROP_NAME_BENCHMARK)
    public BenchmarkClusterConfig getBenchmarkClusterConfig() {
        return benchmarkClusterConfig;
    }

    @SuppressWarnings("unused")
    public void setBenchmarkClusterConfig(final BenchmarkClusterConfig benchmarkClusterConfig) {
        this.benchmarkClusterConfig = benchmarkClusterConfig;
    }

    @JsonProperty(PROP_NAME_CLUSTER)
    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    @SuppressWarnings("unused")
    public void setClusterConfig(final ClusterConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
    }

    @JsonProperty(PROP_NAME_CLUSTER_LOCK)
    public ClusterLockConfig getClusterLockConfig() {
        return clusterLockConfig;
    }

    @SuppressWarnings("unused")
    public void setClusterLockConfig(ClusterLockConfig clusterLockConfig) {
        this.clusterLockConfig = clusterLockConfig;
    }

    @JsonProperty(PROP_NAME_CLUSTER_TASK)
    public ClusterTaskConfig getClusterTaskConfig() {
        return clusterTaskConfig;
    }

    @SuppressWarnings("unused")
    public void setClusterTaskConfig(final ClusterTaskConfig clusterTaskConfig) {
        this.clusterTaskConfig = clusterTaskConfig;
    }

    @JsonProperty(PROP_NAME_COMMON_DB_DETAILS)
    @JsonPropertyDescription("Defines a set of common database connection details to use if no connection details are " +
            "defined for a service area in stroom, e.g. core or config. This means you can have all service areas " +
            "running in a single database, have each in their own database or a mixture.")
    public CommonDbConfig getCommonDbConfig() {
        return commonDbConfig;
    }

    @SuppressWarnings("unused")
    public void setCommonDbConfig(final CommonDbConfig commonDbConfig) {
        this.commonDbConfig = commonDbConfig;
    }

    @JsonProperty(PROP_NAME_CONTENT_PACK_IMPORT)
    public ContentPackImportConfig getContentPackImportConfig() {
        return contentPackImportConfig;
    }

    @SuppressWarnings("unused")
    public void setContentPackImportConfig(final ContentPackImportConfig contentPackImportConfig) {
        this.contentPackImportConfig = contentPackImportConfig;
    }

    @JsonProperty(PROP_NAME_CORE)
    @JsonPropertyDescription("Configuration for the core stroom DB")
    public CoreConfig getCoreConfig() {
        return coreConfig;
    }

    @SuppressWarnings("unused")
    public void setCoreConfig(final CoreConfig coreConfig) {
        this.coreConfig = coreConfig;
    }

    @JsonProperty(PROP_NAME_DASHBOARD)
    public DashboardConfig getDashboardConfig() {
        return dashboardConfig;
    }

    @SuppressWarnings("unused")
    public void setDashboardConfig(final DashboardConfig dashboardConfig) {
        this.dashboardConfig = dashboardConfig;
    }

    @JsonProperty(PROP_NAME_DATA)
    @JsonPropertyDescription("Configuration for the data layer of stroom")
    public DataConfig getDataConfig() {
        return dataConfig;
    }

    @SuppressWarnings("unused")
    public void setDataConfig(final DataConfig dataConfig) {
        this.dataConfig = dataConfig;
    }

    @JsonProperty(PROP_NAME_DATA_SOURCE_URL)
    public DataSourceUrlConfig getDataSourceUrlConfig() {
        return dataSourceUrlConfig;
    }

    @SuppressWarnings("unused")
    public void setDataSourceUrlConfig(final DataSourceUrlConfig dataSourceUrlConfig) {
        this.dataSourceUrlConfig = dataSourceUrlConfig;
    }

    @JsonProperty(PROP_NAME_DOCSTORE)
    public DocStoreConfig getDocStoreConfig() {
        return docStoreConfig;
    }

    @SuppressWarnings("unused")
    public void setDocStoreConfig(final DocStoreConfig docStoreConfig) {
        this.docStoreConfig = docStoreConfig;
    }

    @JsonProperty(PROP_NAME_EXPLORER)
    public ExplorerConfig getExplorerConfig() {
        return explorerConfig;
    }

    @SuppressWarnings("unused")
    public void setExplorerConfig(final ExplorerConfig explorerConfig) {
        this.explorerConfig = explorerConfig;
    }

    @JsonProperty(PROP_NAME_FEED)
    public FeedConfig getFeedConfig() {
        return feedConfig;
    }

    @SuppressWarnings("unused")
    public void setFeedConfig(final FeedConfig feedConfig) {
        this.feedConfig = feedConfig;
    }

    @JsonProperty(PROP_NAME_EXPORT)
    public ExportConfig getExportConfig() {
        return exportConfig;
    }

    @SuppressWarnings("unused")
    public void setExportConfig(final ExportConfig exportConfig) {
        this.exportConfig = exportConfig;
    }

    @JsonProperty(PROP_NAME_INDEX)
    public IndexConfig getIndexConfig() {
        return indexConfig;
    }

    @SuppressWarnings("unused")
    public void setIndexConfig(final IndexConfig indexConfig) {
        this.indexConfig = indexConfig;
    }

    @JsonProperty(PROP_NAME_JOB)
    public JobSystemConfig getJobSystemConfig() {
        return jobSystemConfig;
    }

    @SuppressWarnings("unused")
    public void setJobSystemConfig(final JobSystemConfig jobSystemConfig) {
        this.jobSystemConfig = jobSystemConfig;
    }

    @JsonProperty(PROP_NAME_KAFKA)
    public KafkaConfig getKafkaConfig() {
        return kafkaConfig;
    }

    public void setKafkaConfig(final KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
    }

    @JsonProperty(PROP_NAME_LIFECYCLE)
    public LifecycleConfig getLifecycleConfig() {
        return lifecycleConfig;
    }

    @SuppressWarnings("unused")
    public void setLifecycleConfig(final LifecycleConfig lifecycleConfig) {
        this.lifecycleConfig = lifecycleConfig;
    }

    @JsonProperty(PROP_NAME_NODE)
    public NodeConfig getNodeConfig() {
        return nodeConfig;
    }

    @SuppressWarnings("unused")
    public void setNodeConfig(final NodeConfig nodeConfig) {
        this.nodeConfig = nodeConfig;
    }

    @JsonProperty(PROP_NAME_PATH)
    public PathConfig getPathConfig() {
        return pathConfig;
    }

    @SuppressWarnings("unused")
    public void setPathConfig(final PathConfig pathConfig) {
        this.pathConfig = pathConfig;
    }

    @JsonProperty(PROP_NAME_PIPELINE)
    public PipelineConfig getPipelineConfig() {
        return pipelineConfig;
    }

    @SuppressWarnings("unused")
    public void setPipelineConfig(final PipelineConfig pipelineConfig) {
        this.pipelineConfig = pipelineConfig;
    }

    @JsonProperty(PROP_NAME_PROCESSOR)
    public ProcessorConfig getProcessorConfig() {
        return processorConfig;
    }

    @SuppressWarnings("unused")
    public void setProcessorConfig(final ProcessorConfig processorConfig) {
        this.processorConfig = processorConfig;
    }

    @JsonProperty(PROP_NAME_PROPERTIES)
    @JsonPropertyDescription("Configuration for the stroom property service")
    public PropertyServiceConfig getPropertyServiceConfig() {
        return propertyServiceConfig;
    }

    @SuppressWarnings("unused")
    public void setPropertyServiceConfig(final PropertyServiceConfig propertyServiceConfig) {
        this.propertyServiceConfig = propertyServiceConfig;
    }

    @JsonProperty(PROP_NAME_PROXY_AGGREGATION)
    public ProxyAggregationConfig getProxyAggregationConfig() {
        return proxyAggregationConfig;
    }

    @SuppressWarnings("unused")
    public void setProxyAggregationConfig(final ProxyAggregationConfig proxyAggregationConfig) {
        this.proxyAggregationConfig = proxyAggregationConfig;
    }

    @JsonProperty(PROP_NAME_QUERY_HISTORY)
    public StoredQueryConfig getStoredQueryConfig() {
        return storedQueryConfig;
    }

    @SuppressWarnings("unused")
    public void setStoredQueryConfig(final StoredQueryConfig storedQueryConfig) {
        this.storedQueryConfig = storedQueryConfig;
    }

    @JsonProperty(PROP_NAME_RECEIVE)
    public ReceiveDataConfig getReceiveDataConfig() {
        return receiveDataConfig;
    }

    @SuppressWarnings("unused")
    public void setReceiveDataConfig(final ReceiveDataConfig receiveDataConfig) {
        this.receiveDataConfig = receiveDataConfig;
    }

    @JsonProperty(PROP_NAME_SEARCH)
    public SearchConfig getSearchConfig() {
        return searchConfig;
    }

    @SuppressWarnings("unused")
    public void setSearchConfig(final SearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    @JsonProperty(PROP_NAME_SEARCHABLE)
    public SearchableConfig getSearchableConfig() {
        return searchableConfig;
    }

    @SuppressWarnings("unused")
    public void setSearchableConfig(final SearchableConfig searchableConfig) {
        this.searchableConfig = searchableConfig;
    }

    @JsonProperty(PROP_NAME_SOLR)
    public SolrConfig getSolrConfig() {
        return solrConfig;
    }

    @SuppressWarnings("unused")
    public void setSolrConfig(final SolrConfig solrConfig) {
        this.solrConfig = solrConfig;
    }

    @JsonProperty(PROP_NAME_SECURITY)
    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    @SuppressWarnings("unused")
    public void setSecurityConfig(final SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @JsonProperty(PROP_NAME_SERVICE_DISCOVERY)
    public ServiceDiscoveryConfig getServiceDiscoveryConfig() {
        return serviceDiscoveryConfig;
    }

    @SuppressWarnings("unused")
    public void setServiceDiscoveryConfig(final ServiceDiscoveryConfig serviceDiscoveryConfig) {
        this.serviceDiscoveryConfig = serviceDiscoveryConfig;
    }

    @JsonProperty(PROP_NAME_SESSION_COOKIE)
    public SessionCookieConfig getSessionCookieConfig() {
        return sessionCookieConfig;
    }

    @SuppressWarnings("unused")
    public void setSessionCookieConfig(final SessionCookieConfig sessionCookieConfig) {
        this.sessionCookieConfig = sessionCookieConfig;
    }

    @JsonProperty(PROP_NAME_STATISTICS)
    @JsonPropertyDescription("Configuration for the stroom statistics service")
    public StatisticsConfig getStatisticsConfig() {
        return statisticsConfig;
    }

    @SuppressWarnings("unused")
    public void setStatisticsConfig(final StatisticsConfig statisticsConfig) {
        this.statisticsConfig = statisticsConfig;
    }

    @JsonProperty(PROP_NAME_UI)
    public UiConfig getUiConfig() {
        return uiConfig;
    }

    @SuppressWarnings("unused")
    public void setUiConfig(final UiConfig uiConfig) {
        this.uiConfig = uiConfig;
    }

    @JsonProperty(PROP_NAME_VOLUMES)
    public VolumeConfig getVolumeConfig() {
        return volumeConfig;
    }

    @SuppressWarnings("unused")
    public void setVolumeConfig(final VolumeConfig volumeConfig) {
        this.volumeConfig = volumeConfig;
    }
}
