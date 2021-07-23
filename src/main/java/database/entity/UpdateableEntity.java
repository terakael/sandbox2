package database.entity;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import database.DbConnection;
import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Operation;
import database.entity.annotations.Table;

public abstract class UpdateableEntity {
	private static Statement stmt = null;
	private String tableName = "";
	
	public boolean process() {
		Operation operationAnnotation = getClass().getAnnotation(Operation.class);
		if (operationAnnotation == null) {
			System.out.println("no operation annotation found");
			return false;
		}
		
		if (operationAnnotation.value().equals("keepalive")) {
			return executeQuery("select 'keepalive';");
		}
		
		Table tableAnnotation = getClass().getAnnotation(Table.class);
		if (tableAnnotation == null) {
			System.out.println("no table annotation found");
			return false;
		}
		
		tableName = tableAnnotation.value();
		if (tableName.isEmpty()) {
			System.out.println("table name empty");
			return false;
		}
		
		Map<String, String> fields = new LinkedHashMap<>();
		Map<String, String> whereClause = new LinkedHashMap<>();
		
		extractFieldsAndWhereClause(fields, whereClause, getClass());
		
		if (whereClause.isEmpty()) {
			System.out.println("where clause empty for table " + tableName);
			return false;
		}
		
		String query;
		switch (operationAnnotation.value()) {
		case "update":
			query = update(fields, whereClause);
			break;
			
		case "insert":
			query = insert(fields, whereClause);
			break;
			
		case "delete":
			query = delete(fields, whereClause);
			break;
			
		default:
			query = "";
		}
		
		return executeQuery(query);
	}
	
	private boolean executeQuery(String query) {
		try {
			if (stmt == null)
				stmt = DbConnection.get().createStatement();
//			System.out.println(query);
			stmt.execute(query);
		} catch (SQLException e) {
			System.out.println("ERROR:" + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private String insert(Map<String, String> fieldsToInsert, Map<String, String> whereClause) {		
		final Map<String, String> allFields = Stream
				.concat(whereClause.entrySet().stream(), fieldsToInsert.entrySet().stream())
				.collect(Collectors.toMap(
							Map.Entry::getKey, 
							Map.Entry::getValue,
							(u, v) -> {
						        throw new IllegalStateException(String.format("Duplicate key %s", u));
						    },
							LinkedHashMap::new));
		
		final String headers = allFields.keySet().stream().collect(Collectors.joining(","));
		final String values = allFields.values().stream().map(e -> "'" + e + "'").collect(Collectors.joining(","));
		
		return String.format("insert into %s (%s) values (%s)", tableName, headers, values);
	}
	
	private String delete(Map<String, String> fieldsToDelete, Map<String, String> whereClause) {
		return String.format("delete from %s where %s", tableName, extractWhereClause(whereClause));
	}
	
	private String update(Map<String, String> fieldsToSet, Map<String, String> whereClause) {
		if (fieldsToSet.isEmpty()) {
			System.out.println("fields to set emtpy for table " + tableName);
			return "";
		}
		
		String fieldsToUpdate = fieldsToSet.entrySet()
										   .stream()
										   .map(e -> String.format("%s='%s'", e.getKey(), e.getValue()))
										   .collect(Collectors.joining(","));
		
		return String.format("update %s set %s where %s", tableName, fieldsToUpdate, extractWhereClause(whereClause));
	}
	
	private void extractFieldsAndWhereClause(Map<String, String> fields, Map<String, String> whereClause, Class<?> type) {
		if (type.getSuperclass() != null)
			extractFieldsAndWhereClause(fields, whereClause, type.getSuperclass());
		
		for (Field field : type.getDeclaredFields()) {
			field.setAccessible(true);
			Object f = null;
			try {
				if ((f = field.get(this)) == null) {
					continue;
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				System.out.println(e.getMessage());
				return;
			}
			
			Id id = field.getAnnotation(Id.class);
			Column column = field.getAnnotation(Column.class);
			if (id != null) {
				whereClause.put(column != null ? column.value() : field.getName(), String.valueOf(f));
			}
			
			if (column != null && id == null) { // dont allow updates to ids
				fields.put(column.value(), String.valueOf(f));
			}
		}
	}
	
	private String extractWhereClause(Map<String, String> whereClause) {
		return whereClause.entrySet()
						  .stream()
						  .map(e -> String.format("%s='%s'", e.getKey(), e.getValue()))
						  .collect(Collectors.joining(" and "));
	}
}
