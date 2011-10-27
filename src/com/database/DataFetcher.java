package com.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import com.datafetcherinterface.*;

/*Note that this class is a test class as specified.
 * Later, users can write their own class but has to
 * implement some interface methods in order to interface
 * with the MainScreenController class*/

public class DataFetcher extends Thread implements DataFetcherDelegate  {
	private Connection con = null;
	private String 	database;
	private String 	username;
	private String 	password;
	private Integer port;
	private String tablename;
	

	
	/*=====================CONSTRUCTOR=====================*/
		
	public DataFetcher(String database, String username, String password, Integer port, String tablename){
		this.setDatabaseName(database);
		this.setUsername(username);
		this.setPassword(password);
		this.setPort(port);
		this.setTableName(tablename);
	}
	
	/*=====================CONSTRUCTOR END=====================*/
	
	/*=====================GETTERS AND SETTERS=====================*/
	
	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}
	

	
	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() { 
		return username;
	}

	
	

	
	public String getDatabaseName(){ 
		return this.database;
	}
	
	public void setDatabaseName(String database){ 
		this.database = database;
	}
	

	
	
	
	public void setPort(Integer port){
		this.port = port;
	}
	
	public Integer getPort(){
		return this.port;
	}
	

	
	public void setTableName(String tablename) {
		this.tablename = tablename;
	}

	public String getTableName() {
		return tablename;
	}
	


	
	/*=====================GETTERS AND SETTERS END=====================*/

	
	public void openConnection(){
		try {
			if((this.con == null) || (this.con.isClosed())){
			
				this.con = DriverManager.getConnection("jdbc:mysql://localhost:"+ this.getPort() 
																+ "/" + this.getDatabaseName(), 
																this.getUsername(), 
																this.getPassword());
				}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void closeConnection(){
		try {
			if((this.con != null) && !(this.con.isClosed())){
				this.con.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int getNumOfColumnsInDatabase(){
		return this.getColumnNames().size();
	}
	
	private ArrayList<String> getColumnNames(){
		ArrayList<String> array = new ArrayList<String>();
		this.openConnection();
		ResultSet rs = null;
		Statement stmt = null;
		try {
			if (con == null){
				throw new NullPointerException("con is null");
			}
			stmt = con.createStatement();
			rs = stmt.executeQuery("SHOW COLUMNS FROM " + this.getTableName());
			while(rs.next()){
				String columnname = rs.getString(1);
				array.add(columnname);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		catch (NullPointerException e){
			e.printStackTrace();
		}
		finally{
			this.closeConnection();
		}
		
		return array;
	}
	

	
	
	public synchronized LinkedHashMap<String, LinkedHashMap<String, Object>> getAll(){
		LinkedHashMap<String, LinkedHashMap<String, Object>> retMap = new LinkedHashMap<String, LinkedHashMap<String, Object>>();
		ResultSet rs = null;
		Statement stmt = null;
		ArrayList<String> list = this.getColumnNames();

		this.openConnection();
		try {
			if(con == null){
				throw new NullPointerException("con is null");
			}
			stmt = con.createStatement();

		rs = stmt.executeQuery("SELECT * FROM " + this.getTableName());
		
		while(rs.next()){
			/*Create a temporary map which maps the column names in the db to its fields*/
			LinkedHashMap<String, Object> temp = new LinkedHashMap<String, Object>();
			/*Prepare the map*/
			for(int i = 0; i < list.size(); i++){
				temp.put(list.get(i), rs.getObject(list.get(i)));
			}

			retMap.put(rs.getString("ID"), temp); //Map id with row data 
		}		
		this.closeConnection();
		
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		catch(NullPointerException e){
			e.printStackTrace();
		}

		return retMap;
	}
	
	public synchronized LinkedHashMap<String, LinkedHashMap<String, Object>> getRows(ArrayList<String> ids){
		LinkedHashMap<String,LinkedHashMap<String,Object>> retMap = new LinkedHashMap<String, LinkedHashMap<String,Object>>();
			
	
		if(ids.size() == 0){
			return retMap;
		}
		
		this.openConnection();
		Statement stmt = null;
		ResultSet rs = null;
		ResultSetMetaData rsmd = null;
		String orClause = "";
		
		
		for(Iterator<String> it = ids.iterator(); it.hasNext();){
			String current = it.next();
			if(orClause.equals("")){
				orClause = "ID=" + current;
			}
			else{
				orClause += " OR ID=" + current;
			}
		}
		
		try{
			if(con == null){
				throw new NullPointerException("con is null");
			}
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM " + this.tablename + " WHERE " + orClause);
			
			while(rs.next()){
				LinkedHashMap<String, Object> innerMap = new LinkedHashMap<String, Object>();
				rsmd = rs.getMetaData();
				for(int i = 1; i <= rsmd.getColumnCount(); i++){
					innerMap.put(rsmd.getColumnLabel(i), rs.getObject(i));
				}
				
				retMap.put(rs.getString("ID"), innerMap);
			}
			
		} catch(SQLException e){
			e.printStackTrace();
		}
		 catch (NullPointerException e){
			 e.printStackTrace();
		 }
		return retMap;
	}
	
	

	public synchronized LinkedHashMap<String, LinkedHashMap<String, Object>> getRow(String ID){
		LinkedHashMap<String, LinkedHashMap<String, Object>> retMap = new LinkedHashMap<String, LinkedHashMap<String, Object>>();
		
		this.openConnection();
		Statement stmt = null;
		ResultSet rs = null;
		ResultSetMetaData rsmd = null;
		String orClause = "ID = " + ID;
		
		
		
		try{
			if(con == null){
				throw new NullPointerException("con is null");
			}
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM " + this.tablename + " WHERE " + orClause);
			
			while(rs.next()){
				LinkedHashMap<String, Object> innerMap = new LinkedHashMap<String, Object>();
				rsmd = rs.getMetaData();
				for(int i = 1; i <= rsmd.getColumnCount(); i++){
					innerMap.put(rsmd.getColumnLabel(i), rs.getObject(i));
				}
				
				retMap.put(rs.getString("ID"), innerMap);
			}
			
		} catch(SQLException e){
			e.printStackTrace();
		}
		 catch (NullPointerException e){
			 e.printStackTrace();
		 }
		return retMap;
	}
}
