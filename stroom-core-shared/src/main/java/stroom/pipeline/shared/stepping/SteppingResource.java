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

package stroom.pipeline.shared.stepping;

import stroom.docref.DocRef;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.fusesource.restygwt.client.DirectRestService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Tag(name = "Stepping")
@Path("/stepping" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SteppingResource extends RestResource, DirectRestService {

    @POST
    @Path("/getPipelineForStepping")
    @Operation(summary = "Get a pipeline for stepping")
    DocRef getPipelineForStepping(
            @Parameter(description = "request", required = true) GetPipelineForMetaRequest request);

    @POST
    @Path("/findElementDoc")
    @Operation(summary = "Load the document for an element")
    DocRef findElementDoc(FindElementDocRequest request);

    @POST
    @Path("/step")
    @Operation(summary = "Step a pipeline")
    SteppingResult step(@Parameter(description = "request", required = true) PipelineStepRequest request);
}
