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
 */

package stroom.dashboard.shared;

import stroom.util.shared.FetchWithUuid;
import stroom.util.shared.ResourceGeneration;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.fusesource.restygwt.client.DirectRestService;

import java.util.List;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(tags = "Dashboards")
@Path("/dashboard" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DashboardResource extends RestResource, DirectRestService, FetchWithUuid<DashboardDoc> {

    @GET
    @Path("/{uuid}")
    @ApiOperation("Fetch a dashboard doc by its UUID")
    DashboardDoc fetch(@PathParam("uuid") String uuid);

    @PUT
    @Path("/{uuid}")
    @ApiOperation("Update a dashboard doc")
    DashboardDoc update(@PathParam("uuid") String uuid, @ApiParam("doc") DashboardDoc doc);

    @POST
    @Path("/validateExpression")
    @ApiOperation(value = "Validate an expression")
    ValidateExpressionResult validateExpression(@ApiParam("expression") String expression);

    @POST
    @Path("/downloadQuery")
    @ApiOperation(value = "Download a query")
    ResourceGeneration downloadQuery(@ApiParam("downloadQueryRequest") DownloadQueryRequest downloadQueryRequest);

    @POST
    @Path("/downloadSearchResults")
    @ApiOperation(value = "Download search results")
    ResourceGeneration downloadSearchResults(@ApiParam("request") DownloadSearchResultsRequest request);

    @POST
    @Path("/poll")
    @ApiOperation("Poll for new search results")
    Set<DashboardSearchResponse> poll(@ApiParam("request") SearchBusPollRequest request);

    @GET
    @Path("/fetchTimeZones")
    @ApiOperation(value = "Fetch time zone data from the server")
    List<String> fetchTimeZones();

    @GET
    @Path("/functions")
    @ApiOperation(value = "Fetch all expression functions")
    List<FunctionSignature> fetchFunctions();
}
