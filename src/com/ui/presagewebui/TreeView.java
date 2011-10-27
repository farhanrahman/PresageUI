package com.ui.presagewebui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import com.datamodel.TableDataModel;
import com.ui.presagewebui.customcomponents.CustomLabel;
import com.ui.presagewebui.customcomponents.CustomProgressBar;
import com.vaadin.addon.treetable.TreeTable;


@SuppressWarnings("serial")
public class TreeView extends TreeTable{

	/**
	 * 
	 */
	
	public TreeView(LinkedHashMap<String,String> columnNames, LinkedHashMap<String, TableDataModel> tabledatasource){
		super();
		this.setImmediate(true);
		this.addColumn(columnNames);
		this.prepareLayout();
		this.fillTable(tabledatasource);
	}

	private void prepareLayout(){
        this.setWidth("100%");
        this.setSelectable(true);
        this.setHeight("-1px");
	}
	
	public void fillTable(LinkedHashMap<String, TableDataModel> tabledatasource){
		for(Iterator<String> it = tabledatasource.keySet().iterator();it.hasNext();){
			String datasourcekey = (String) it.next();
			this.addItem(datasourcekey);
			for(Iterator<String> modelIt = tabledatasource.get(datasourcekey).getFields().keySet().iterator(); 
				modelIt.hasNext();){
				String modelKey = (String) modelIt.next();
				if(this.getContainerPropertyIds().contains(modelKey)){
					if(modelKey.equals("comp_percent")){
						CustomProgressBar pi = new CustomProgressBar();
						pi.update(tabledatasource.get(datasourcekey).getFields().get(modelKey));
						this.getContainerProperty(datasourcekey, modelKey).setValue(pi);	
					} else if(modelKey.equals("createdAt") || modelKey.equals("name")){
						this.getContainerProperty(datasourcekey, modelKey).setValue(tabledatasource.get(datasourcekey).getFields().get(modelKey));
					}
					else{
						CustomLabel label = new CustomLabel();
						label.update(tabledatasource.get(datasourcekey).getFields().get(modelKey));
						this.getContainerProperty(datasourcekey, modelKey).setValue(label);
					}
					
				}
			}
			this.setChildrenAllowed(datasourcekey, false);
		}
		this.setUpParentIds(tabledatasource);
	}
	
	public void setUpParentIds(LinkedHashMap<String, TableDataModel> tabledatasource){
		/*should also be used for updating the table view: assuming that the 
		 * tabledatasource has already been updated*/
		
		for(Iterator<String> it = tabledatasource.keySet().iterator(); it.hasNext();){
			String datasourcekey = (String) it.next();
			if(tabledatasource.get(datasourcekey).getParentId() == null){
				continue;
			}
			String parentId = tabledatasource.get(datasourcekey).getParentId().toString();
			this.setChildrenAllowed(parentId, true);
			
			if(this.hasChildren(parentId)){
				//String[] children = (String[]) this.getChildren(parentId).toArray();
				List<Object> children = Arrays.asList(this.getChildren(parentId).toArray());
				try{
					if (children.size() == 0){
						throw new ArrayIndexOutOfBoundsException();
					}
					
					this.setParent(datasourcekey, parentId);
					this.setParent(children.get(0), datasourcekey);
				}
				catch(ArrayIndexOutOfBoundsException e){
					e.printStackTrace();
				}				
			}
			else{
				this.setParent(datasourcekey, parentId);
			}
		}
	}
	
	

	
	public void deleteItem(ArrayList<String> deleteIds){
		for(Iterator<String> it = deleteIds.iterator(); it.hasNext();){
			String key = (String) it.next();
			System.out.println("key: " + key);
			if(this.containsId(key)){
				this.removeItem(key);
			}
			else{
				System.out.println("problem look into this shit");
			}
		}
	}
	

	
	public void addColumn(LinkedHashMap<String,String> columnLabels){
		Iterator<String> it = columnLabels.keySet().iterator();
		String current = null;
		while(it.hasNext()){
			current = it.next();
			if(current.equals("comp_percent")){
				this.addContainerProperty(current, CustomProgressBar.class, null, 
						columnLabels.get(current), null, null);
			} else if(current.equals("createdAt") || current.equals("name")){
				this.addContainerProperty(current, String.class, null, 
						columnLabels.get(current), null, null);			
			
			}
			else{
				this.addContainerProperty(current, CustomLabel.class, null, 
										columnLabels.get(current), null, null);
			}
			this.setColumnExpandRatio(current, 1);
		}
	}
}
