package com.mvshyvk.kaldi.service.api.impl;

import com.mvshyvk.kaldi.service.api.*;
import com.mvshyvk.kaldi.service.api.NotFoundException;
import com.mvshyvk.kaldi.service.models.TaskId;
import com.mvshyvk.kaldi.service.webapp.KaldiServiceAppContext;
import com.mvshyvk.kaldi.service.webapp.exception.ProcessingQueueFull;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2020-04-23T21:31:33.644Z[GMT]")
public class TaskApiServiceImpl extends TaskApiService {
	@Override
	public Response taskPost(Object body, SecurityContext securityContext) throws NotFoundException {
		
		TaskId taskId = null;
		try {
			taskId = KaldiServiceAppContext.taskHandler.postTask();
		} catch (ProcessingQueueFull e) {
			
			return Response.status(Status.TOO_MANY_REQUESTS).build();
		}
		return Response.accepted().entity(taskId).build();
	}

	@Override
	public Response taskTaskIdStatusGet(String taskId, SecurityContext securityContext) throws NotFoundException {
		// do some magic!
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
	}
}
