package com.ui.presagewebui.customcomponents;

import com.vaadin.ui.Label;

@SuppressWarnings({ "unchecked", "serial" })
public class CustomLabel extends Label implements ValueUpdates{

	/**
	 * 
	 */
	
	public CustomLabel(){
		super();
	}

	public void update(Object ob) throws ClassCastException {
		try{
			if(ob == null){
				return;
			}
			if(ob instanceof String){
				this.setValue((String)ob);
			}
			else
				throw new ClassCastException("ob is not of type String");
		} catch(ClassCastException e){
			e.printStackTrace();
		}
	}

}
