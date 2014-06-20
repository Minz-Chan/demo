package com.minz.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.minz.util.LogUtil;


/**
 * Task to be run sequentially
 * @author minz
 *
 */
public class TaskExecutor {
	
	private static Log log = LogFactory.getLog(TaskExecutor.class);

	private static final String TAG = LogUtil
			.makeLogTag(TaskExecutor.class);
	
	private ExecutorService executor;
	
	private TaskSubmitter taskSubmitter;
	
	private TaskTracker taskTracker;
	
	private List<Runnable> taskList;

    private boolean running = false;

    private Future<?> futureTask;
	
	
	public TaskExecutor() {
		executor = Executors.newSingleThreadExecutor();
		
		taskSubmitter =  new TaskSubmitter(this);
		taskTracker = new TaskTracker(this);
		
		taskList = new ArrayList<Runnable>();
	}
	
	
	
	public ExecutorService getExecutor() {
		return executor;
	}

	public TaskTracker getTaskTracker() {
		return taskTracker;
	}
	
	public TaskExecutor getTaskExecutor() {
		return this;
	}

	
	public void addTask(Runnable task) {
		log.debug("addTask(task)...");

		taskTracker.increase();
		synchronized (taskList) {
			if (taskList.isEmpty() && !running) {
				running = true;
				futureTask = taskSubmitter.submit(task);
				if (futureTask == null) {
					taskTracker.decrease();
				}				
			} else {
				taskList.add(task);
			}
		}
		
		log.debug("addTask(task)...done");
	}
	
	public void runTask() {
		log.debug("runTask()...");
		synchronized (taskList) {
			running = false;
			futureTask = null;
			if (!taskList.isEmpty()) {
				running = true;
				
				futureTask = taskSubmitter.submit(taskList.get(0));
				taskList.remove(0);
				
				if (futureTask == null) {
					taskTracker.decrease();
				}
			}
		}
		
		taskTracker.decrease();
		log.debug("runTask()...done");
		
	}
	
	public void test() {
		addTask(new Task1());
		addTask(new Task2());
		addTask(new Task3());
		
		addTask(new Runnable() {

			@Override
			public void run() {
				log.info("executor.shutdown");
				getExecutor().shutdown();
				
			}
			
		});
		
//		executor.submit(new Task1());
//		executor.submit(new Task2());
//		executor.submit(new Task3());
//		executor.submit(new Task2());
//		executor.submit(new Task3());
//		executor.submit(new Task3());
//		executor.submit(new Task1());
//		executor.submit(new Task1());
//		executor.submit(new Task3());
//		executor.shutdown();
	}
	
	
	private class Task1 implements Runnable {
		final TaskExecutor taskExecutor;
		
		public Task1() {
			this.taskExecutor = getTaskExecutor();
		}

		@Override
		public void run() {
			log.info("Task1.run()...");
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			taskExecutor.runTask();
		}
	}
	
	private class Task2 implements Runnable {
		final TaskExecutor taskExecutor;
		
		public Task2() {
			this.taskExecutor = getTaskExecutor();
		}

		@Override
		public void run() {
			log.info("Task2.run()...");
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			taskExecutor.runTask();
		}
	}
	
	
	private class Task3 implements Runnable {
		final TaskExecutor taskExecutor;
		
		public Task3() {
			this.taskExecutor = getTaskExecutor();
		}

		@Override
		public void run() {
			log.info("Task3.run()...");
			
			taskExecutor.runTask();
		}
	}



	/**
     * Class for summiting a new runnable task.
     */
    public class TaskSubmitter {
    	
    	final TaskExecutor taskExecutor;
    	
    	public TaskSubmitter(TaskExecutor taskExecutor) {
    		this.taskExecutor = taskExecutor;
    	}   
    	
    	
    	@SuppressWarnings("rawtypes")
		public Future submit(Runnable task) {
    		Future result = null;
    		
    		if (!taskExecutor.getExecutor().isTerminated()
    				&& !taskExecutor.getExecutor().isShutdown()
    				&& task != null) {
    			result = taskExecutor.getExecutor().submit(task);
    		}
    		
    		return result;
    	}

    }
	
    
    /**
     * Class for monitoring the running task
     */
    public class TaskTracker {
    	
    	final TaskExecutor taskExecutor;
    	
    	public int count;
    	
    	public TaskTracker(TaskExecutor taskExecutor) {
    		this.taskExecutor = taskExecutor;
    		count = 0;
    	}
    	
    	public void increase() {
    		synchronized (taskExecutor.getTaskTracker()) {
    			taskExecutor.getTaskTracker().count++;
    			log.debug("Increase task count to " + count);
    		}
    	}
    	
    	public void decrease() {
    		synchronized (taskExecutor.getTaskTracker()) {
    			taskExecutor.getTaskTracker().count--;
    			log.debug("Decrease task count to " + count);
    		}
    	}
    	
    }
	
}
