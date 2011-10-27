package com.ui.presagewebui.customcomponents;

import com.vaadin.ui.ProgressIndicator;


@SuppressWarnings("serial")
public class CustomProgressBar extends ProgressIndicator implements ValueUpdates{

	/**
	 * 
	 */
	
	private static final Integer MAX_VALUE = 100;
	
	public CustomProgressBar(){
		super();
	}

	public void update(Object ob) {
		Float updatedValue = 0f;
		try{
			if(ob == null){
				updatedValue = 0f;
			} else if((ob instanceof Float)){
				updatedValue = (Float) ob;
			}
			else
				throw new ClassCastException("ob is not of type Float");
		} catch(ClassCastException e){
			e.printStackTrace();
		}
		finally{
			this.setValue(updatedValue/this.getMaxValue());
		}
		
		if ((Float) this.getValue() == 0f){
			this.setEnabled(false);
		}
		else{
			this.setEnabled(true);
		}
	}

	public Integer getMaxValue() {
		return MAX_VALUE;
	}
}
