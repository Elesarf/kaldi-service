/*
 * Kaldi speach recognition REST API
 * Simple REST interface for posting tasks for non realtime speach recognition
 *
 * OpenAPI spec version: 0.9.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.mvshyvk.kaldi.service.models;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ServiceStatus
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2020-04-23T21:31:33.644Z[GMT]")
public class ServiceStatus {
	@JsonProperty("workersCount")
	private Integer workersCount = null;

	@JsonProperty("queueDepth")
	private Integer queueDepth = null;

	@JsonProperty("availableQueueSlots")
	private Integer availableQueueSlots = null;
	
	public ServiceStatus() { }
	
	public ServiceStatus(ServiceStatus origin) {
		
		workersCount = origin.workersCount;
		queueDepth = origin.queueDepth;
		availableQueueSlots = origin.availableQueueSlots;
	}

	public ServiceStatus workersCount(Integer workersCount) {
		this.workersCount = workersCount;
		return this;
	}

	/**
	 * Get workersCount
	 * 
	 * @return workersCount
	 **/
	@JsonProperty("workersCount")
	@Schema(example = "8", description = "")
	public Integer getWorkersCount() {
		return workersCount;
	}

	public void setWorkersCount(Integer workersCount) {
		this.workersCount = workersCount;
	}

	public ServiceStatus queueDepth(Integer queueDepth) {
		this.queueDepth = queueDepth;
		return this;
	}

	/**
	 * Get queueDepth
	 * 
	 * @return queueDepth
	 **/
	@JsonProperty("queueDepth")
	@Schema(example = "32", description = "")
	public Integer getQueueDepth() {
		return queueDepth;
	}

	public void setQueueDepth(Integer queueDepth) {
		this.queueDepth = queueDepth;
	}

	public ServiceStatus availableQueueSlots(Integer availableQueueSlots) {
		this.availableQueueSlots = availableQueueSlots;
		return this;
	}

	/**
	 * Get availableQueueSlots
	 * 
	 * @return availableQueueSlots
	 **/
	@JsonProperty("availableQueueSlots")
	@Schema(example = "27", description = "")
	public Integer getAvailableQueueSlots() {
		return availableQueueSlots;
	}

	public void setAvailableQueueSlots(Integer availableQueueSlots) {
		this.availableQueueSlots = availableQueueSlots;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ServiceStatus serviceStatus = (ServiceStatus) o;
		return Objects.equals(this.workersCount, serviceStatus.workersCount)
				&& Objects.equals(this.queueDepth, serviceStatus.queueDepth)
				&& Objects.equals(this.availableQueueSlots, serviceStatus.availableQueueSlots);
	}

	@Override
	public int hashCode() {
		return Objects.hash(workersCount, queueDepth, availableQueueSlots);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class ServiceStatus {\n");

		sb.append("    workersCount: ").append(toIndentedString(workersCount)).append("\n");
		sb.append("    queueDepth: ").append(toIndentedString(queueDepth)).append("\n");
		sb.append("    availableQueueSlots: ").append(toIndentedString(availableQueueSlots)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
}
