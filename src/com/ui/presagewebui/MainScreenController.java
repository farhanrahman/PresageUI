package com.ui.presagewebui;

import java.util.ArrayList;
//import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;

import com.database.DataFetcher;
import com.datafetcherinterface.*;
import com.vaadin.ui.VerticalLayout;

import com.datamodel.TableDataModel;
import com.github.wolfie.refresher.Refresher;
import com.github.wolfie.refresher.Refresher.RefreshListener;

@SuppressWarnings("serial")
public class MainScreenController extends VerticalLayout implements Runnable{
	private DataFetcher df 	= null;
	private TreeView tv 	= null;
	private LinkedHashMap<String,TableDataModel> tabledatasource = null;
	
	public DataFetcherDelegate delegate;
	/*Readonly: datafetcherthread
	 * ReadWrite: updateManagerThread*/
	public Queue<String> runningQueue; 
	public boolean runningQueueLock = false;
	
	
	/*Readonly: datafetcherthread
	 * ReadWrite: updateManagerThread*/
	public Queue<String> loadingQueue; 
	public boolean loadingQueueLock = false;
	
	/*Readonly: datafetcherthread
	 * ReadWrite: updateManagerThread*/
	public Queue<String> readyQueue; 
	public boolean readyQueueLock = false;
	
	/*Readonly: datafetcherthread
	 * ReadWrite: updateManagerThread*/
	public Queue<String> initialisingQueue; 
	public boolean initialisingQueueLock = false;
	
	/*Readonly: datafetcherthread
	 * ReadWrite: updateManagerThread*/
	public Queue<String> pausedQueue; 
	public boolean pausedQueueLock = false;
	
	/*Readonly: datafetcherthread
	 * ReadWrite: updateManagerThread*/
	public Queue<String> stoppedQueue; 
	public boolean stoppedQueueLock = false;
	
	/*Readonly: datafetcherthread
	 * ReadWrite: updateManagerThread*/
	public Queue<String> finishingQueue; 
	public boolean finishingQueueLock = false;
	
	/*Readonly: datafetcherthread
	 * ReadWrite: updateManagerThread*/
	public Queue<String> completeQueue; 
	public boolean completeQueueLock = false;
	
	/*Readonly: datafetcherthread
	 * ReadWrite: updateManagerThread*/
	public Queue<String> otherQueue; 
	public boolean otherQueueLock = false;
	
	/*ReadWrite: datafetcher thread*/
	public Queue<String> deleteQueue;
	public boolean deleteQueueLock = false;
	
	
	
	private boolean lock 		= false;
	private boolean done 		= false;
	private boolean isDifferent = false;
	
	public final String kStateLoading 		= "LOADING";
	public final String kStateReady  		= "READY";
	public final String kStateRunnig  		= "RUNNING";
	public final String kStateInitialising 	= "INITIALISING";
	public final String kStatePaused		= "PAUSED";
	public final String kStateStopped		= "STOPPED";
	public final String kStateFinishing 	= "FINISHING";
	public final String kStateComplete		= "COMPLETE";
	

	
	
	public enum SimulationState {
		LOADING, READY, INITIALISING, RUNNING, PAUSED, STOPPED, FINISHING, COMPLETE, UNDEFINED
	}
	
	/*===================THREAD SPECIFIC METHODS===================*/

	/*Refresher to update the UI automatically by polling
	 * for any changes in the values of UI components in the server*/
	public class treeViewUpdater implements RefreshListener{

		public void refresh(Refresher source) {
			//System.out.println("refresher called");
			updateTableView(); //Does not take care of delete. Later an algorithm will sort out entries to be deleted.
		}
		
	}
	
	public class UpdaterThread extends Thread{
		public boolean done = false;
		public SimulationState command;
		private long delay;
		
		public UpdaterThread(SimulationState state, long delay){
			this.command 	= state;
			this.delay 		= delay;
		}
		
		public void run(){
			while(!done){
			try{
				sleep(this.delay);
				ArrayList<String> ids = new ArrayList<String>();
				switch(this.command){
				case RUNNING:
					ids = getFromRunningQueue();
					break;
				case READY:
					ids = getFromReadyQueue();
					break;
				case LOADING:
					ids = getFromLoadingQueue();
					break;
				case INITIALISING:
					ids = getFromInitialisingQueue();
					break;
				case PAUSED:
					ids = getFromPausedQueue();
					break;
				case STOPPED:
					ids = getFromStoppedQueue();
					break;
				case FINISHING:
					ids = getFromFinishingQueue();
					break;
				case COMPLETE:
					ids = getFromCompleteQueue();
					break;
				default:
					/*for others*/
					ids = getFromOthersQueue();
					break;
				}
				
				updateDataModel(delegate.getRows(ids));
				
			}catch (InterruptedException e){
				e.printStackTrace();
			}
			}
		}
		
		public void setDelay(long delay){
			this.delay = delay;
		}
	}
	
	public UpdaterThread readyStateUpdaterThread;
	public UpdaterThread loadingStateUpdaterThread;
	public UpdaterThread initialisingStateUpdaterThread;
	public UpdaterThread pausedStateUpdaterThread;
	public UpdaterThread stoppedStateUpdaterThread;
	public UpdaterThread finishingStateUpdaterThread;
	public UpdaterThread completeStateUpdaterThread;
	public UpdaterThread otherStateUpdaterThread;
	public UpdaterThread runningStateUpdaterThread;
	
	
	/**
	 * 
	 */
	
	/*This is a thread function which will sort out queues that will be
	 * read by the datafetcher to fetch only the entries in the queue*/
	public void run() {
		while(!done){
			try{
				Thread.sleep(5000);
				//System.out.println("Sorting out queues now");
				this.sortAllQueues();
				
			} catch (InterruptedException e){
				e.printStackTrace();
			}
		}
		System.out.println("Thread stopped");
	}
	
	public void shutDownThread(){
		this.done = true;
	}
	
	/*sortAllQueues() is responsible for managing all the queues
	 * that DataFetcher will read from in order to fetch the
	 * batch of ids that are in those queues*/
	public synchronized void sortAllQueues(){
		if(this.isLock()){
			while(this.isLock()){
				continue;
			}
		}
		
		LinkedHashMap<String, String> ids 	= new LinkedHashMap<String,String>();
		ArrayList<String> running 			= new ArrayList<String>();
		ArrayList<String> ready 			= new ArrayList<String>();
		ArrayList<String> loading 			= new ArrayList<String>();
		ArrayList<String> initialising		= new ArrayList<String>();
		ArrayList<String> paused			= new ArrayList<String>();
		ArrayList<String> stopped			= new ArrayList<String>();
		ArrayList<String> finishing 		= new ArrayList<String>();
		ArrayList<String> complete			= new ArrayList<String>();
		ArrayList<String> others 			= new ArrayList<String>();
		
		/*tabledatasource critical region*/
		this.setLock(true);
			for(Iterator<String> it = this.tabledatasource.keySet().iterator() ; it.hasNext();){
				String key = (String) it.next();
				ids.put(key, this.tabledatasource.get(key).getState());
				if(this.tabledatasource.get(key).getState().equals(this.kStateRunnig)){
					
					running.add(key); //add key to the runnning ArrayList
					
				} else if(this.tabledatasource.get(key).getState().equals(this.kStateReady)){
					
					ready.add(key); //add key to the ready ArrayList
					
				} else if(this.tabledatasource.get(key).getState().equals(this.kStateLoading)){
					
					loading.add(key);
					
				} else if(this.tabledatasource.get(key).getState().equals(this.kStateInitialising)){
					
					initialising.add(key);
					
				} else if(this.tabledatasource.get(key).getState().equals(this.kStatePaused)){
					
					paused.add(key);
					
				} else if(this.tabledatasource.get(key).getState().equals(this.kStateStopped)){
					
					stopped.add(key);
					
				} else if(this.tabledatasource.get(key).getState().equals(this.kStateFinishing)){
					
					finishing.add(key);
					
				} else if (this.tabledatasource.get(key).getState().equals(this.kStateComplete)){
					
					complete.add(key);
				
				} else {
					
					others.add(key);
					
				}
			}
		this.setLock(false);
		/*tabledatasource critical region end*/
		
		
		
		
		
		if(this.isRunningQueueLocked()){
			while(this.isRunningQueueLocked()){
				continue;
			}
		}
		/*mainQueue critical region*/
		this.setRunningQueue(true);
			for(int i = 0; i < running.size(); i++){
				if(!this.runningQueue.contains(running.get(i)))
					this.runningQueue.offer(running.get(i));
				else
					continue;
			}
		this.setRunningQueue(false);
		/*mainQueue critical region end*/
		
		if(this.isReadyQueueLocked()){
			while(this.isReadyQueueLocked()){
				continue;
			}
		}
		/*readyQueue critical region*/
		this.setReadyQueue(true);
			for(int i = 0; i < ready.size(); i++){
				if(!this.readyQueue.contains(ready.get(i)))
					this.readyQueue.offer(ready.get(i));
				else
					continue;
			}
		this.setReadyQueue(false);
		/*readyQueue critical region*/
		
		if(this.isLoadingQueueLocked()){
			while(this.isLoadingQueueLocked()){
				continue;
			}
		}
		/*loadingQueue critical region*/
		this.setLoadingQueue(true);
			for(int i = 0; i < loading.size(); i++){
				if(!this.loadingQueue.contains(loading.get(i)))
					this.loadingQueue.offer(loading.get(i));
				else
					continue;
			}
		this.setLoadingQueue(false);
		/*loadingQueue critical region*/
		
		if(this.isInitialisingQueueLocked()){
			while(this.isInitialisingQueueLocked()){
				continue;
			}
		}
		
		/*initialisingQueue critical region*/
		this.setInitialisingQueue(true);
		for(int i = 0; i < initialising.size(); i++){
			if(!this.initialisingQueue.contains(initialising.get(i))){
				this.initialisingQueue.offer(initialising.get(i));
			}
			else{
				continue;
			}
		}
		this.setInitialisingQueue(false);
		/*initialisingQueue critical region end*/
		
		if(this.isPausedQueueLocked()){
			while(this.isPausedQueueLocked()){
				continue;
			}
		}
		
		/*pausedQueue critical region*/
		this.setPausedQueue(true);
		for(int i = 0; i < paused.size(); i++){
			if(!this.pausedQueue.contains(paused.get(i))){
				this.pausedQueue.offer(paused.get(i));
			}
		}
		this.setPausedQueue(false);
		/*pausedQueue critical region end*/
		
		if(this.isStoppedQueueLocked()){
			while(this.isStoppedQueueLocked()){
				continue;
			}
		}
		
		/*stoppedQueue critical region*/
		this.setStoppedQueue(true);
		for(int i = 0; i < stopped.size(); i++){
			if(!this.stoppedQueue.contains(stopped.get(i))){
				this.stoppedQueue.offer(stopped.get(i));
			}
		}
		this.setStoppedQueue(false);
		/*stoppedQueue critical region end*/
		
		if(this.isFinishingQueueLocked()){
			while(this.isFinishingQueueLocked()){
				continue;
			}
		}
		
		/*finishingQueue critical region*/
		this.setFinishingQueue(true);
		for(int i = 0; i < finishing.size(); i++){
			if(!this.finishingQueue.contains(finishing.get(i))){
				this.finishingQueue.offer(finishing.get(i));
			}
		}
		this.setFinishingQueue(false);
		/*finishingQueue criticial region end*/
		
		if(this.isCompleteQueueLocked()){
			while(this.isCompleteQueueLocked()){
				continue;
			}
		}
		
		/*completeQueue critical region*/
		this.setCompleteQueue(true);
		for(int i = 0; i < complete.size(); i++){
			if(!this.completeQueue.contains(complete.get(i))){
				this.completeQueue.offer(complete.get(i));
			}
		}
		this.setCompleteQueue(false);
		/*completeQueue critical region end*/
		
		if(this.isOtherQueueLocked()){
			while(this.isOtherQueueLocked()){
				continue;
			}
		}
		
		/*otherQueue critical region*/
		this.setOtherQueue(true);
		for(int i = 0; i < others.size(); i++){
			if(!this.otherQueue.contains(others.get(i))){
				this.otherQueue.offer(others.get(i));
			}
		}
		this.setOtherQueue(false);
		/*otherQueue critical region end*/
	}
	

	public synchronized void updateTableView() {
		if(this.isLock()){
			while(this.isLock()){
				continue;
			}
		}
		
		
		this.setLock(true);
		/*Critical region*/
		if(!this.isDifferent()){
			/*Check if isDifferent is false. If so
			 * then free the tabledatasource and
			 * from the method.*/
			//System.out.println("this is not different, exiting from updateTableView()");
			this.setLock(false);
			return;
		}
		
		LinkedHashMap <String, TableDataModel> clone = new LinkedHashMap<String, TableDataModel>(this.getTableDataSource());
		/*Critical region end*/
		
		/*after updating the tabledatasource set isDifferent to false*/
		this.setDifferent(false); 
		this.setLock(false); /*free the tabledatasource*/
		if(clone == this.tabledatasource){
			//System.out.println("clone points to the same datasource");
		}else{
			System.out.println("Clone size: " + clone.size());
			this.tv.fillTable(clone);
		}
		
	}
	/*===================THREAD SPECIFIC METHODS END===============*/
	
	public MainScreenController(){
		super();
		this.setImmediate(true);
		this.df = new DataFetcher("farhan_db","root","root",new Integer(8889),"simulations");
		this.delegate = df;
		
		this.tv = new TreeView(this.setUpColumns(), this.setUpTableDataSource());
        this.addComponent(tv);
        
//        ArrayList<String> ids = new ArrayList<String>();
//        ids.add("1");
//        ids.add("2");
//        ids.add("40");
//        LinkedHashMap<String, LinkedHashMap<String, Object>> test = this.df.getRows(ids);
//        
//        for(Iterator<String> it = test.keySet().iterator(); it.hasNext();){
//        	String key = (String) it.next();
//        	System.out.println("key: " + key);
//        }
//        
//        if(ids.size() != test.keySet().size()){
//        	Collection<?> setA = test.keySet();
//        	ids.removeAll(setA);
//        }
//        
//        for(int i = 0; i < ids.size(); i++){
//        	System.out.println("id: " + ids.get(i));
//        }
        
        this.runningQueue 		= new LinkedList<String>();
        this.readyQueue   		= new LinkedList<String>();
        this.deleteQueue  		= new LinkedList<String>();
        this.loadingQueue 		= new LinkedList<String>();
        this.initialisingQueue	= new LinkedList<String>();
        this.pausedQueue		= new LinkedList<String>();
        this.stoppedQueue		= new LinkedList<String>();
        this.finishingQueue		= new LinkedList<String>();
        this.completeQueue		= new LinkedList<String>();
        this.otherQueue			= new LinkedList<String>();
        
	
        
        new Thread(this, "MainScreenController thread").start();
        

        this.readyStateUpdaterThread = new UpdaterThread(SimulationState.READY, 6000);
        this.loadingStateUpdaterThread = new UpdaterThread(SimulationState.LOADING, 8000);
        this.initialisingStateUpdaterThread = new UpdaterThread(SimulationState.INITIALISING, 9000);
        this.pausedStateUpdaterThread = new UpdaterThread(SimulationState.PAUSED, 25000);
        this.stoppedStateUpdaterThread = new UpdaterThread(SimulationState.STOPPED, 30000);
        this.finishingStateUpdaterThread = new UpdaterThread(SimulationState.FINISHING, 60000);
        this.completeStateUpdaterThread = new UpdaterThread(SimulationState.COMPLETE, 120000);
        this.otherStateUpdaterThread = new UpdaterThread(SimulationState.UNDEFINED, 80000);
        this.runningStateUpdaterThread = new UpdaterThread(SimulationState.RUNNING, 2000);
        
        new Thread(this.readyStateUpdaterThread, "Ready State Updater").start();
        new Thread(this.loadingStateUpdaterThread, "Loading State Updater").start();
        new Thread(this.initialisingStateUpdaterThread, "Initialising State Updater").start();
        new Thread(this.pausedStateUpdaterThread, "Paused State Updater").start();
        new Thread(this.stoppedStateUpdaterThread, "Stopped State Updater").start();
        new Thread(this.finishingStateUpdaterThread, "Finishing State Updater").start();
        new Thread(this.completeStateUpdaterThread, "Complete State Updater").start();
        new Thread(this.otherStateUpdaterThread, "Other State Updater").start();
        new Thread(this.runningStateUpdaterThread, "Running State").start();
        
        /*The Refresher thread is now being used instead of the 
         * thread commented off above. The reason for this is that
         * the refresher will poll for changes of UI components 
         * on the server and update the UI accordingly.*/
    	final Refresher refresher = new Refresher();
		refresher.addListener(new treeViewUpdater());
		refresher.setRefreshInterval(1000);
		this.addComponent(refresher);
	}

	/*=======================DELEGATE METHODS=======================*/
	
	public synchronized void updateDataModel(LinkedHashMap<String, LinkedHashMap<String, Object>> map) {
		/*First wait for lock to be released*/
		if(this.isLock()){
			while(this.isLock()){
				continue;
			}
		}
		
		
		/*Critical region*/

		/*once lock is released, set the lock to get into critical region and
		 * update the tabledatasource*/
		this.setLock(true);
		//System.out.println("lock is set to true");
		
		/*First check if the incoming map is a subset of the tabledatasource.
		 * if it isn't for example, it may contain new entries or new updates of
		 * existing entries, then and only then update the tabledatasource and 
		 * allow update of the treeview. The reason for this is so that the
		 * main thread is not kept busy most of the time for updating the user interface.
		 * we want to minimise the number of times the main screen is updated.*/
		
			for(Iterator<String> it = map.keySet().iterator(); it.hasNext();){
				String key = (String) it.next();
				/*First check if the key exists in the table datasource. If it doesn't,
				 * then there is a new entry and so the treeview needs updates*/
				if(!this.tabledatasource.containsKey(key)){
					this.setDifferent(true);
					break;
				}
				
				/*Then check if any of the datafields are new. If the datafields have newer values then
				 * the tabledatasource is updated and the treeview is updated as well*/
				
				for(Iterator<String> innerIterator = this.tabledatasource.get(key).getFields().keySet().iterator();
					innerIterator.hasNext();){
					String innerKey = (String) innerIterator.next();
					if(this.tabledatasource.get(key).getFields().get(innerKey) == null){ // exception case for null objects
						if(map.get(key).get(innerKey) != null){
							this.setDifferent(true);
							break;
						}
					}
					else if(!this.tabledatasource.get(key).getFields().get(innerKey).equals(map.get(key).get(innerKey))){ // check the inner objects
						this.setDifferent(true);
						break;
					}
				}
				if(this.isDifferent()){ //if isDifferent is true then break from the method; no need to check any further
					break;
				}
			}
		
		
		if(!(this.isDifferent())){
			/*Check if isDifferent is false:
			 * if isDifferent is true then continue on updating
			 * the datasource else set the lock to false and free
			 * the tabledatasource and return from this method*/
			this.setLock(false);
			/*Critical region ends early*/
			//System.out.println("this is not Different so not updating");
			//System.out.println("Lock is set to false \n");
			return;
		}
		
		//if isDifferent is true, update the datasource and later, update the treeview
		
		for(Iterator<String> it = map.keySet().iterator(); it.hasNext();){
			String current = (String) it.next();
			this.tabledatasource.put(current, new TableDataModel(map.get(current)));
		}
		
		
		/*Take care of entries that needs to be deleted here*/
		ArrayList<String> deleteIds = new ArrayList<String>();
		if(this.isDeleteQueueLocked()){
			while(this.isDeleteQueueLocked()){
				continue;
			}
		}
		
		//System.out.println("setting deleteQueueLock to true");
		/*Critical region for deleteQueue begin*/
		this.setDeleteQueueLock(true);
		while(!this.deleteQueue.isEmpty()){
			deleteIds.add(this.deleteQueue.poll());
		}
		
		//System.out.println("setting deleteQueueLock to false");
		this.setDeleteQueueLock(false);
		/*Critical region for deleteQueue end*/
		
			for(int i = 0; i < deleteIds.size(); i++){
				//System.out.println("removing index: " + i + " from tabledatasource");
				this.tabledatasource.remove(deleteIds.get(i));
			}
		
			//TODO /*THE FOLOWING SHOULD EXECUTE IN REFRESHER THREAD AND NOT HERE. WILL LOOK INTO IT LATER*/
//			if(!deleteIds.isEmpty()){
//				this.tv.deleteItem(deleteIds); //Special case for interacting with TreeView
//			}

		this.setLock(false); /*once finished, set the lock to false for other methods to access
							  * the field*/
		/*Critical region end*/

		//System.out.println("Lock is set to false \n");
		
	}
	
	
	public synchronized boolean isTableDataModelEmpty(){
		return this.tabledatasource.isEmpty();
	}
	
	/*THESE METHODS ARE USED TO FETCH FROM THE VARIOUS QUEUS BY THE THREADS IN THE DATAFETCHER CLASS*/
	
	/*getFromRunningQueue(): function that returns an ArrayList<String>
	 * of ids that were fetched from the runningQueue, so that the
	 * datafetcher can fetch those ids from the backend database*/
	public synchronized ArrayList<String> getFromRunningQueue(){
		ArrayList<String> ids = new ArrayList<String>();
		if(this.isRunningQueueLocked()){
			while(this.isRunningQueueLocked()){
				continue;
			}
		}
		/*runningQueue critical region begin*/
		this.setRunningQueue(true);
		while(!this.runningQueue.isEmpty()){
			System.out.println("key: " + this.runningQueue.peek());
			ids.add(this.runningQueue.poll());
		}
		this.setRunningQueue(false);
		/*runningQueue critical region end*/
		return ids;
	}

	/*getFromReadyQueue(): function that returns an ArrayList<String> of
	 * ids from the readyQueue so that the datafetcher can fetch the ids
	 * inside the readyQueue from the backend database*/
	public synchronized ArrayList<String> getFromReadyQueue() {
		ArrayList<String> ids = new ArrayList<String>();
		if(this.isReadyQueueLocked()){
			while(this.isReadyQueueLocked()){
				continue;
			}
		}
		/*readyQueue critical region begin*/
		this.setReadyQueue(true);
		while(!this.readyQueue.isEmpty()){
			System.out.println("key: " + this.readyQueue.peek());
			ids.add(this.readyQueue.poll());
		}
		this.setReadyQueue(false);
		/*readyQueue critical region end*/
		return ids;	
	}

	/*getFromLoadingQueue(): function that returns an ArrrayList<String> of
	 * ids from the loadinQueue so that the datafetcher can fetch
	 * those ids from the backend database*/
	public synchronized ArrayList<String> getFromLoadingQueue(){
		ArrayList<String> ids = new ArrayList<String>();
		if(this.isLoadingQueueLocked()){
			while(this.isLoadingQueueLocked()){
				continue;
			}
		}
		/*loadingQueue critical region*/
		this.setLoadingQueue(true);
		while(!this.loadingQueue.isEmpty()){
			System.out.println("key: " + this.loadingQueue.peek());
			ids.add(this.loadingQueue.poll());
		}
		this.setLoadingQueue(false);
		/*loadingQueue critical region*/
		return ids;
	}
	
	/*getFromInitialisingQueue(): function that returns and ArrayList<String>
	 * of ids that were taken from the initialisingQueue for the datafetcher
	 * to fetch from the backend database.*/
	public synchronized ArrayList<String> getFromInitialisingQueue(){
		ArrayList<String> ids = new ArrayList<String>();
		if(this.isInitialisingQueueLocked()){
			while(this.isInitialisingQueueLocked()){
				continue;
			}
		}
		
		/*initialisingQueue critical region*/
		this.setInitialisingQueue(true);
		while(!this.initialisingQueue.isEmpty()){
			System.out.println("key: " + this.initialisingQueue.peek());
			ids.add(this.initialisingQueue.poll());
		}
		this.setInitialisingQueue(false);
		/*initialisingQueue critical region end*/
		return ids;
	}
	
	/*getFromPausedQueue(): method to allow the DataFetcher
	 * to access the pausedQueue to update simulations with
	 * a registered state of PAUSED*/
	public synchronized ArrayList<String> getFromPausedQueue(){
		ArrayList<String> ids = new ArrayList<String>();
		if(this.isPausedQueueLocked()){
			while(this.isPausedQueueLocked()){
				continue;
			}
		}
		
		/*pausedQueue critical region*/
		this.setPausedQueue(true);
		while(!this.pausedQueue.isEmpty()){
			System.out.println("key: " + this.pausedQueue.peek());
			ids.add(this.pausedQueue.poll());
		}
		this.setPausedQueue(false);
		/*pausedQueue critical region end*/
		return ids;
	}
	
	/*getFromStoppedQueue(): delegate method to allow
	 * the DataFetcher class to access the stoppedQueue of
	 * this class.*/
	public synchronized ArrayList<String> getFromStoppedQueue(){
		ArrayList<String> ids = new ArrayList<String>();
		if(this.isStoppedQueueLocked()){
			while(this.isStoppedQueueLocked()){
				continue;
			}
		}
		
		/*stoppedQueue critical region*/
		this.setStoppedQueue(true);
		while(!this.stoppedQueue.isEmpty()){
			System.out.println("key: " + this.stoppedQueue.peek());
			ids.add(this.stoppedQueue.poll());
		}
		this.setStoppedQueue(false);
		/*stoppedQueue critical region end*/
		return ids;
	}
	
	/*getFromFinishingQueue(): method used by DataFetcher to 
	 * get ids from finishingQueue to fetch ids with currently
	 * registered state as FINISHING*/
	public synchronized ArrayList<String> getFromFinishingQueue(){
		ArrayList<String> ids = new ArrayList<String>();
		if(this.isFinishingQueueLocked()){
			while(this.isFinishingQueueLocked()){
				continue;
			}
		}
		
		/*finishingQueue critical region*/
		this.setFinishingQueue(true);
		while(!this.finishingQueue.isEmpty()){
			System.out.println("key: " + this.finishingQueue.peek());
			ids.add(this.finishingQueue.poll());
		}
		this.setFinishingQueue(false);
		/*finishingQueue critical region end*/
		return ids;
	}
	
	public synchronized ArrayList<String> getFromCompleteQueue(){
		ArrayList<String> ids = new ArrayList<String>();
		if(this.isCompleteQueueLocked()){
			while(this.isCompleteQueueLocked()){
				continue;
			}
		}
		
		/*completeQueue critical region*/
		this.setCompleteQueue(true);
		while(!this.completeQueue.isEmpty()){
			System.out.println("key: " + this.completeQueue.peek());
			ids.add(this.completeQueue.poll());
		}
		this.setCompleteQueue(false);
		/*completeQueue critical region end*/
		return ids;
	}
	
	public synchronized ArrayList<String> getFromOthersQueue(){
		ArrayList<String> ids = new ArrayList<String>();
		if(this.isOtherQueueLocked()){
			while(this.isOtherQueueLocked()){
				continue;
			}
		}
		
		/*otherQueue critical region */
		this.setOtherQueue(true);
		while(!this.otherQueue.isEmpty()){
			System.out.println("key: " + this.otherQueue.peek());
			ids.add(this.otherQueue.poll());
		}
		this.setOtherQueue(false);
		/*otherQueue critical region end*/
		return ids;
	}
	
	/*=======================DELEGATE METHODS END=======================*/

	public void setTV(TreeView tv) {
		this.tv = tv;
	}

	public TreeView getTV() {
		return this.tv;
	}
	
	private LinkedHashMap<String, String> setUpColumns(){
		/*Use this method to control how many columns the tree table
		 * will have.
		 * Later functionalities will be added such that 
		 * this function will dynamically create columns from the 
		 * database. User can then alter the name or delete columns from
		 * the settings panel.
		 * In order to add columns the following needs to be done:
		 * 1) Update the MySQL database first
		 * 2) Add the extra column in the code below
		 * 3) Update the setters and getters for the
		 * extra field in the TableDataModel class*/
		LinkedHashMap<String, String> columnNames = new LinkedHashMap<String, String>();
		columnNames.put("name","Simulation");
		columnNames.put("classname","Simulation Type");
		columnNames.put("state","Simulation State");
		columnNames.put("comp_percent","Progress");
		columnNames.put("parameters","Parameters");
		columnNames.put("createdAt","Time of creation");
		//Add additional column names here
		return columnNames;
	}

	public void setTabledatasource(LinkedHashMap<String,TableDataModel> tabledatasource) {
		this.tabledatasource = tabledatasource;
	}

	public LinkedHashMap<String,TableDataModel> getTableDataSource() {
		return tabledatasource;
	}

	/*Initial method to setup the tabledatasource. It is called
	 * only in after the instantiation of this class*/
	public LinkedHashMap<String,TableDataModel> setUpTableDataSource(){
		this.tabledatasource = new LinkedHashMap<String, TableDataModel>();
		LinkedHashMap<String, LinkedHashMap<String, Object>> tempMap = delegate.getAll();
		for(Iterator<String> it = tempMap.keySet().iterator(); it.hasNext();){
			String str = it.next();
			LinkedHashMap<String, Object> innerMap = tempMap.get(str);
			this.tabledatasource.put(str, new TableDataModel(innerMap));
		}

		return this.tabledatasource;
	}

	public synchronized void setLock(boolean lock) {
		this.lock = lock;
	}

	public synchronized boolean isLock() {
		return lock;
	}
	
	
	/*========================QUEUE CHECKER AND QUEUE LOCKS BEGIN========================*/
	
	/*Running queue locks and checker*/
	public synchronized void setRunningQueue(boolean mainqueue){
		this.runningQueueLock = mainqueue;
	}
	
	public synchronized boolean isRunningQueueLocked(){
		return this.runningQueueLock;
	}
	
	/*Other queue locks and checker*/
	public synchronized void setOtherQueue(boolean otherqueue){
		this.otherQueueLock = otherqueue;
	}
	
	public synchronized boolean isOtherQueueLocked(){
		return this.otherQueueLock;
	}
	
	/*Paused queue locks and checker*/
	public synchronized void setPausedQueue(boolean pausedqueue){
		this.pausedQueueLock = pausedqueue;
	}
	
	public synchronized boolean isPausedQueueLocked(){
		return this.pausedQueueLock;
	}
	
	/*Ready queue locks and checker*/
	public synchronized void setReadyQueue(boolean readyqueue){
		this.readyQueueLock = readyqueue;
	}
	
	public synchronized boolean isReadyQueueLocked(){
		return this.readyQueueLock;
	}
	
	/*Stopped queue locks and checker*/
	public synchronized void setStoppedQueue(boolean stoppedqueue){
		this.stoppedQueueLock = stoppedqueue;
	}
	
	public synchronized boolean isStoppedQueueLocked(){
		return this.stoppedQueueLock;
	}
	
	/*Completed queue locks and checker*/
	public synchronized void setCompleteQueue(boolean completequeue){
		this.completeQueueLock = completequeue;
	}
	
	public synchronized boolean isCompleteQueueLocked(){
		return this.completeQueueLock;
	}
	
	/*Finishing queue locks and checker*/
	public synchronized void setFinishingQueue(boolean finishingqueue){
		this.finishingQueueLock = finishingqueue;
	}
	
	public synchronized boolean isFinishingQueueLocked(){
		return this.finishingQueueLock;
	}
	
	/*Loading queue locks and checker*/
	public synchronized void setLoadingQueue(boolean loadingqueue){
		this.loadingQueueLock = loadingqueue;
	}
	
	public synchronized boolean isLoadingQueueLocked(){
		return this.loadingQueueLock;
	}
	
	/*Initialising queue locks and checkers*/
	public synchronized void setInitialisingQueue(boolean initialisingqueue){
		this.initialisingQueueLock = initialisingqueue;
	}
	
	public synchronized boolean isInitialisingQueueLocked(){
		return this.initialisingQueueLock;
	}
	
	
	/*Delete queue locks and checker*/
	public synchronized void setDeleteQueueLock(boolean deletequeuelock){
		this.deleteQueueLock = deletequeuelock;
	}

	public synchronized boolean isDeleteQueueLocked(){
		return this.deleteQueueLock;
	}
	
	/*========================QUEUE CHECKER AND QUEUE LOCKS END========================*/
	
	
	public void setDifferent(boolean isDifferent) {
		System.out.println("setting isDifferent to " + isDifferent);
		this.isDifferent = isDifferent;
	}

	public boolean isDifferent() {
		return isDifferent;
	}

}
