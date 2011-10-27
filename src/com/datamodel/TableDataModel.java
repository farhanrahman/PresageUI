package com.datamodel;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.lang.Integer;

public class TableDataModel {
	private int updateInterval;
	private Date timeStamp;
	private LinkedHashMap<String, Object> fields;
	
	public TableDataModel(LinkedHashMap<String, Object> innerMap){
		this.setFields(innerMap);
	}
	
	public void setId(Long id) {
		this.fields.put("ID", id);
	}

	public Long getId() {
		return (Long) this.fields.get("ID");
	}
	
	public void setParentId(Long parentId){
		this.fields.put("parentID", parentId);
	}
	
	public Long getParentId(){
		return (Long) this.fields.get("parentID");
	}
	
	public void setName(String sim_name) {
		this.fields.put("name", sim_name);
	}

	public String getName() {
		return (String) this.fields.get("name");
	}

	public void setClassName(String sim_type) {
		this.fields.put("classname", sim_type);
	}

	public String getClassName() {
		return (String) this.fields.get("classname");
	}

	public void setState(String state) {
		this.fields.put("state", state);
	}

	public String getState() {
		return (String) this.fields.get("state");
	}
	
	public void setCurrentTime(int currentTime){
		this.fields.put("currentTime", currentTime);
	}
	
	public int getCurrentTime(){
		return (Integer) this.fields.get("currentTime");
	}
	
	public void setFinisheTime(Integer finishTime){
		this.fields.put("finishTime", finishTime);
	}
	
	public Integer getFinishTime(){
		return (Integer) this.fields.get("finishTime");
	}

	public void setComment(String comment){
		this.fields.put("comment", comment);
	}
	
	public String getComment(){
		return (String) this.fields.get("comment");
	}
	
	public void setCompPercent(Float comp_percent) {
		this.fields.put("comp_percent", comp_percent);
	}

	public Float getCompPercent() {
		return (Float) this.fields.get("comp_percent");
	}

	public void setParameters(String params) {
		this.fields.put("parameters", params);
	}

	public String getParameters() {
		return (String) this.fields.get("parameters");
	}


	public void setFields(LinkedHashMap<String, Object> fields) {
		this.fields = fields;
	}

	public LinkedHashMap<String, Object> getFields() {
		return fields;
	}

	public String toString(){
		String retStr = "id: " 						+ this.getId()				+ "\n" +
						"parentId: "				+ this.getParentId()		+ "\n" +
						"Simulation name: " 		+ this.getName() 			+ "\n" + 
						"Simulation class name: " 	+ this.getClassName() 		+ "\n" +
						"Simulation state: " 		+ this.getState() 			+ "\n" +
						"Current time:	"			+ this.getCurrentTime()		+ "\n" +
						"Finish time: 	"			+ this.getFinishTime()		+ "\n" +
						"Comment: 	"				+ this.getComment()			+ "\n" +
						"Completion percent: " 		+ this.getCompPercent() 	+ "\n" +
						"Parameters: " 				+ this.getParameters() 		+ "\n" ;
						

		return retStr;
	}

	public void updateModel(LinkedHashMap<String, Object> updateSource){
		for(Iterator<String> it = updateSource.keySet().iterator();it.hasNext();){
			String key = (String) it.next();
			this.fields.put(key, updateSource.get(key));
		}
	}

	
	/*updateInterval and timestamp setters and getters*/

	
	public void setUpdateInterval(int updateInterval) {
		if (updateInterval < 0)
			this.updateInterval = 0;
		else
			this.updateInterval = updateInterval;
	}

	public int getUpdateInterval() {
		return updateInterval;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}
	
	
	
	
}
