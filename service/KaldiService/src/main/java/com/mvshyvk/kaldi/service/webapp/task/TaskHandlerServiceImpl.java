package com.mvshyvk.kaldi.service.webapp.task;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.mvshyvk.kaldi.service.models.TaskId;
import com.mvshyvk.kaldi.service.webapp.KaldiServiceFactory;
import com.mvshyvk.kaldi.service.webapp.exception.ProcessingQueueFull;
import com.mvshyvk.kaldi.service.webapp.kaldiConnector.KaldiConnector;
import com.mvshyvk.kaldi.service.webapp.model.TaskData;
import com.mvshyvk.kaldi.service.webapp.model.TaskStatus;

/**
 * Class that controls global capacities of KaldiService and handles all incoming tasks
 * for speech recognition
 *
 */
public class TaskHandlerServiceImpl implements TaskHandlerService, CapacitiesService {
	
	private static Logger log = Logger.getLogger(TaskHandlerServiceImpl.class);
	
	private int queueCapacity;
	
	private Map<String, TaskData> completedTasks = new HashMap<String, TaskData>();
	private Map<String, TaskData> inProgressTasks = new HashMap<String, TaskData>();
	private BlockingQueue<TaskData> processingQueue;
	
	// Thread pool for running workers that execute tasks by passing them to Kaldi Connector 
	private ExecutorService executorService; 
	private int workersCount = 0;
	
	/**
	 * Constructor
	 */
	public TaskHandlerServiceImpl() {
		
		log.info("Initializing TaskHandlerServiceImpl ...");
		log.info("Queue capacity: " + queueCapacity);
		
		int workersToBe = (Runtime.getRuntime().availableProcessors() + 1) / 2 + 1;
		queueCapacity = workersToBe * 2; 

		processingQueue = new ArrayBlockingQueue<TaskData>(queueCapacity);
		executorService = Executors.newCachedThreadPool();
		
		for (int i = 0; i < workersToBe; ++i) {
			addWorker();
		}
	}

	/**
	 * Constructs worker and run it using thread pool 
	 */
	private void addWorker() {
		
		executorService.submit(new TaskExecutor(KaldiServiceFactory.createKaldiConnector(), processingQueue));
		workersCount++;
		
		log.info("Added worker. Current workers count: " + workersCount);
	}

	/**
	 * Adds data for processing 
	 * 
	 * @param data - data for processing
	 * @return identifier of the task added to queue
	 * @throws ProcessingQueueFull - in case if queue is full
	 */
	@Override
	public TaskId postTask(byte[] data) throws ProcessingQueueFull {
		
		TaskData task = new TaskData(data);
		log.debug(String.format("Received data for processing. TaskId: %s, Size: %d", task.getTaskId(), data.length));
		
		if (processingQueue.offer(task)) {
			
			log.debug(String.format("Task %s submitted to the queue", task.getTaskId()));
			return new TaskId().taskId(task.getTaskId());
		}
		
		throw new ProcessingQueueFull("Task wasn't posted for processing because queue is full");				
	}

	/**
	 * Prepares service for shutting down webapp
	 */
	@Override
	public void stopService() {
		
		executorService.shutdownNow();
		try {
			executorService.awaitTermination(12, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.error("Error happened during stopping service", e);
		}
	}
	
	/**
	 * Returns depth of processing queue
	 */
	@Override
	public int getQueueCapacity() {
		return queueCapacity + workersCount;
	}
	
	/**
	 * Returns count of available slots in processing queue at the moment 
	 */
	@Override
	public int getQueueAvailableCapacity() {
		return processingQueue.remainingCapacity() + workersCount - inProgressTasks.size();
	}
	
	/**
	 * Returns count of processing workers
	 */
	@Override
	public int getWorkersCount() {
		return workersCount;
	}
	
	/**
	 * Returns task status
	 * 
	 * @param taskId identifier of task to retrieve status
	 * @return status of task
	 */
	@Override
	public TaskStatus getTaskStatus(TaskId taskId) {
		
		if (processingQueue.stream().anyMatch(item -> item.getTaskId().equals(taskId.getTaskId()))) {
			return TaskStatus.enInQueue;
		}
		
		if (inProgressTasks.containsKey(taskId.getTaskId())) {
			return TaskStatus.enInProgress;
		}
		
		if (completedTasks.containsKey(taskId.getTaskId())) {
			return TaskStatus.enCompleted;
		}
		
		return TaskStatus.enUnknown;		
	}

	@Override
	public TaskData getTaskData(TaskId taskId) {		
		return completedTasks.get(taskId.getTaskId());
	}
	
	/**
	 * Class implements worker that executes tasks by passing them to Kaldi Connector 
	 */
	class TaskExecutor implements Runnable
	{
		private final int waitingTimeout = 1; // In seconds
		
		private KaldiConnector kaldiConnector;
		private BlockingQueue<TaskData> tasksQueue;
		
		/**
		 * Constructor 
		 * 
		 * @param kaldiConnector - Service that executes task consuming time of worker's thread 
		 * but passing work to Kaldi Connector
		 * @param queue - queue of tasks to process
		 */
		public TaskExecutor(KaldiConnector kaldiConnector, BlockingQueue<TaskData> queue) {
			
			this.kaldiConnector = kaldiConnector;
			this.tasksQueue = queue;
		}

		@Override
		public void run() {
			
			log.debug("Worker started");
			
			while (!Thread.currentThread().isInterrupted()) {
				try {
					
					TaskData task  = tasksQueue.poll(waitingTimeout, TimeUnit.SECONDS);
					if (task != null) {
						log.debug(String.format("Task %s was taken from the queue for processing", task.getTaskId()));
						executeTask(task);
					}
				}
				catch (InterruptedException e) {

					log.warn("Worker has interrupted");
					Thread.currentThread().interrupt();
				}
			}
			
			log.debug("Worker stopped");
		}

		/**
		 * Executes task in current thread
		 * 
		 * @param task - task to execute
		 * @throws InterruptedException
		 */
		private void executeTask(TaskData task) throws InterruptedException {
			
			inProgressTasks.put(task.getTaskId(), task);
			try {
				kaldiConnector.processSpeach(task);
			}
			finally {
				task.minimizeMemoryAllocation();
				completedTasks.put(task.getTaskId(), task);
				inProgressTasks.remove(task.getTaskId());
				
				log.debug(String.format("Task %s has been completed", task.getTaskId()));
			}
		}
		
	}

}
