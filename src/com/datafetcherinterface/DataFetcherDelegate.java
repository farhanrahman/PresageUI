package com.datafetcherinterface;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public interface DataFetcherDelegate {
	public LinkedHashMap<String, LinkedHashMap<String, Object>> getRow(String ID);
	public LinkedHashMap<String, LinkedHashMap<String, Object>> getAll();
	public LinkedHashMap<String, LinkedHashMap<String, Object>> getRows(ArrayList<String> ids);
}
