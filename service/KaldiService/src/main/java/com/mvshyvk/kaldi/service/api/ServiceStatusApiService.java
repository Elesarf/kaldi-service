package com.mvshyvk.kaldi.service.api;

import com.mvshyvk.kaldi.service.api.NotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2020-04-23T21:31:33.644Z[GMT]")
public abstract class ServiceStatusApiService {
	public abstract Response serviceStatusGet(SecurityContext securityContext) throws NotFoundException;
}
