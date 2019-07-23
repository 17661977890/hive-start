package com.example.hivestart.testconn;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
 
/**
 * hive连接测试类
 * @author sixmonth
 * @Date 2019年5月13日
 *
 */
public class HiveTest {
	
	//9019是自定义远程连接的端口，默认是10000
	private static final String URLHIVE = "jdbc:hive2://192.168.2.31:10000/default;auth=noSasl";
    private static Connection connection = null;
 
    public static Connection getHiveConnection() {
        if (null == connection) {
            synchronized (HiveTest.class) {
                if (null == connection) {
                    try {
                        Class.forName("org.apache.hive.jdbc.HiveDriver");
                        connection = DriverManager.getConnection(URLHIVE, "hiveroot", "123456");
                        System.out.println("hive启动连接成功！");
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return connection;
    }
 
 
    public static void main(String args[]) throws SQLException{
    	//在defaul库中建表hiveroot
    	String sql1="select * from hiveroot limit 1";
    	PreparedStatement pstm = getHiveConnection().prepareStatement(sql1);
    	ResultSet rs= pstm.executeQuery(sql1);
    	
    	while (rs.next()) {
			System.out.println(rs.getString(2));
		}
    	pstm.close();
    	rs.close();
    	
    }
 
}